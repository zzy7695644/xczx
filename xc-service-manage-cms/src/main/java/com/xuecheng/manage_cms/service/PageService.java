package com.xuecheng.manage_cms.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
public class PageService {

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    GridFSBucket gridFSBucket;

    public void testGridFs() throws FileNotFoundException {
        File file = new File("D:/bbb/index_banner.ftl");
        FileInputStream fileInputStream = new FileInputStream(file);
        Object object = gridFsTemplate.store(fileInputStream,"轮播图测试文件01", "");
        String id = object.toString();
        System.out.println(id);

    }

    /**
     * 页面静态化
     * @param pageId
     * @return
     */
    public String getPageHtml(String pageId){
        //获取页面模型
        Map map = this.getModelByPageId(pageId);
        if (map == null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        //获取页面模板
        String templateContent = getTemplateByPageId(pageId);
        if (StringUtils.isEmpty(templateContent)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //执行静态化
        String html = generateHtml(templateContent,map);
        if(StringUtils.isEmpty(html)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        System.out.println(html);
        return html;
    }

    //执行页面静态化
    public String generateHtml(String template, Map model)
    {
        try {
            Configuration configuration = new Configuration(Configuration.getVersion());
            //模板加载类
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template", template);
            //配置模板加载器
            configuration.setTemplateLoader(stringTemplateLoader);
            //获取模板
            Template template1 = configuration.getTemplate("template");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template1, model);
            return html;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //获取页面模板
    public String getTemplateByPageId(String pageId){
        CmsPage cmsPage = this.findById(pageId);
        if (cmsPage == null){
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        String templateId = cmsPage.getTemplateId();
        if (StringUtils.isEmpty(templateId)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> optional = cmsTemplateRepository.findById(templateId);
        //查出的对象不为空
        if (optional.isPresent()){
            CmsTemplate cmsTemplate = optional.get();
            //得到在girds里存储的模板id
            String templateFileId = cmsTemplate.getTemplateFileId();
            //取出模板文件内容
            GridFSFile gridFSFile =gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
            //打开下载流对象
            GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
            //创建GridFsResource
            GridFsResource gridFsResource = new GridFsResource(gridFSFile,gridFSDownloadStream);
            try {
                String content = IOUtils.toString(gridFsResource.getInputStream(), "utf-8");
                return content;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //获取页面模型
    private Map getModelByPageId(String pageId){
        CmsPage cmsPage = this.findById(pageId);
        if (cmsPage == null){
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        String dataUrl = cmsPage.getDataUrl();
        if (StringUtils.isEmpty(dataUrl)){
            //如果为空
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        ResponseEntity<Map> responseEntity = restTemplate.getForEntity(dataUrl,Map.class);
        Map map = responseEntity.getBody();

        return map;
    }

    /**
     * 页面查询方法
     * @param page 页码，从1开始记数
     * @param size 每页记录数
     * @param queryPageRequest 查询条件
     * @return
     */
    public QueryResponseResult findList(@PathVariable int page, @PathVariable  int size, QueryPageRequest queryPageRequest){
        if (queryPageRequest == null){
            queryPageRequest = new QueryPageRequest();
        }
        //条件匹配器
        ExampleMatcher exampleMatcher = ExampleMatcher.matching().withMatcher("pageAliase",ExampleMatcher.GenericPropertyMatchers.contains());
        CmsPage cmsPage = new CmsPage();
        if (StringUtils.isNotEmpty(queryPageRequest.getSiteId())){
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        if(StringUtils.isNotEmpty(queryPageRequest.getPageAliase())){
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
        if(StringUtils.isNotEmpty(queryPageRequest.getTemplateId())){
            cmsPage.setTemplateId(queryPageRequest.getTemplateId());
        }
        //创建条件实例
        Example<CmsPage> example = Example.of(cmsPage,exampleMatcher);
        page = page - 1;
        //分页查询
        Pageable pageable = PageRequest.of(page,size);
        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);
        //返回结果
        QueryResult<CmsPage> queryResult = new QueryResult<CmsPage>();
        queryResult.setTotal(all.getTotalElements());
        queryResult.setList(all.getContent());
        return  new QueryResponseResult(CommonCode.SUCCESS, queryResult);
    }

    /**
     * 页面添加
     * @param cmsPage
     * @return
     */
    public CmsPageResult add(CmsPage cmsPage){
        //检查信息是否重复
        CmsPage cms = cmsPageRepository.findBySiteIdAndPageNameAndPageWebPath(cmsPage.getSiteId(), cmsPage.getPageName(), cmsPage.getPageWebPath());
        if (cms == null){
            //如果没查到，则可以添加,站点id由数据库自增生成
            cmsPage.setPageId(null);
            cmsPageRepository.save(cmsPage);
            CmsPageResult cmsPageResult = new CmsPageResult(CommonCode.SUCCESS, cmsPage);
            return cmsPageResult;
        }else if(cms != null){
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    /**
     * 根据id查页面信息
     * @param id
     * @return
     */
    public CmsPage findById(String id){
        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    /**
     * 更新页面
     * @param id
     * @param cmsPage
     * @return
     */
    public CmsPageResult update(String id, CmsPage cmsPage){
        CmsPage cmsPage1 = this.findById(id);
        if (cmsPage1 != null){
            //更新信息
            cmsPage1.setTemplateId(cmsPage.getTemplateId());
            cmsPage1.setSiteId(cmsPage.getSiteId());
            cmsPage1.setPageAliase(cmsPage.getPageAliase());
            cmsPage1.setPageName(cmsPage.getPageName());
            cmsPage1.setPageWebPath(cmsPage.getPageWebPath());
            cmsPage1.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
            cmsPage1.setDataUrl(cmsPage.getDataUrl());
            CmsPage save = cmsPageRepository.save(cmsPage1);
            if (save != null){
                //更新成功
                CmsPageResult cmsPageResult = new CmsPageResult(CommonCode.SUCCESS,save);
                return cmsPageResult;
            }
        }
        //返回失败信息
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    /**
     * 删除
     * @param id
     * @return
     */
    public ResponseResult delete(String id){
        CmsPage cmsPage = this.findById(id);
        if (cmsPage != null){
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

}
