package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.BannerService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.util.OBSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/banner")
public class AdminBannerController {

    @Autowired
    private BannerService bannerService;

    /**
     * 从 OBS 拉取 banner/ 目录下的图片，写入数据库
     */
    @PostMapping("/sync")
    public Result<String> syncFromOBS() {
        log.info("同步轮播图数据");
        List<String> urls = OBSUtil.listFiles("banner/");
        for (int i = 0; i < urls.size(); i++) {
            bannerService.addBanner(urls.get(i), "轮播图" + (i + 1), i + 1);
        }
        return Result.success("同步完成，共插入 " + urls.size() + " 条");
    }
}
