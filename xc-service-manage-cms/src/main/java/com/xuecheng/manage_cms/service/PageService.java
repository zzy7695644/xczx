package com.xuecheng.manage_cms.service;

import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class PageService {

    @Autowired
    CmsPageRepository cmsPageRepository;


    /**
     * 页面查询方法
     * @param page 页码，从1开始记数
     * @param size 每页记录数
     * @param queryPageRequest 查询条件
     * @return
     */
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest){
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


    public CmsPageResult add(CmsPage cmsPage){
        //检查信息是否重复
        CmsPage cms = cmsPageRepository.findBySiteIdAndPageNameAndPageWebPath(cmsPage.getSiteId(), cmsPage.getPageName(), cmsPage.getPageWebPath());
        if (cms == null){
            //如果没查到，则可以添加,站点id由数据库自增生成
            cmsPage.setPageId(null);
            cmsPageRepository.save(cmsPage);
            CmsPageResult cmsPageResult = new CmsPageResult(CommonCode.SUCCESS, cmsPage);
            return cmsPageResult;
        }
        return new CmsPageResult(CommonCode.FAIL, null);
    }

}
