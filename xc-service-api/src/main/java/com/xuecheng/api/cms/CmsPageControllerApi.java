package com.xuecheng.api.cms;

import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;

public interface CmsPageControllerApi {
    //条件查询
    public QueryResponseResult findList(int size, int page, QueryPageRequest queryPageRequest);
    //页面添加
    public CmsPageResult add(CmsPage cmsPage);
    //根据id查询
    public CmsPage findById(String id);
    //页面修改
    public CmsPageResult update(String id, CmsPage cmsPage);
    //删除页面
    public ResponseResult delete(String id);
}
