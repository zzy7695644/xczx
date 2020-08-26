package com.xuecheng.manage_cms.controller;

import com.xuecheng.api.cms.CmsConfigControllerApi;
import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.manage_cms.service.CmsConfigService;
import com.xuecheng.manage_cms.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;

@RestController
@RequestMapping("/cms/config")
public class CmsConfigController implements CmsConfigControllerApi {

    @Autowired
    CmsConfigService cmsConfigService;

    @Autowired
    PageService pageService;

    @Override
    @GetMapping("/getmodel/{id}")
    public CmsConfig getmodel(@PathVariable String id) {
        return cmsConfigService.getConfigById(id);
    }

    @GetMapping("/getmodel/grid")
    public void testGridFs()throws FileNotFoundException {
        pageService.testGridFs();
    }
}
