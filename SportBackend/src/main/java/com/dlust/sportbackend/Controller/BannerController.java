package com.dlust.sportbackend.Controller;

import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Banner;
import com.dlust.sportbackend.Service.BannerService;
import com.dlust.sportbackend.util.OBSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    public Result<List<Banner>> getBannerList() {
        List<Banner> banners = bannerService.getActiveBanners();
        return Result.success(banners);
    }

    /**
     * 从 OBS 拉取 banner/ 目录下的图片，写入数据库
     * 启动后端后，浏览器访问 http://localhost:8080/api/banner/sync
     */
    @PostMapping("/sync")
    public Result<String> syncFromOBS() {
        List<String> urls = OBSUtil.listFiles("banner/");
        for (int i = 0; i < urls.size(); i++) {
            bannerService.addBanner(urls.get(i), "轮播图" + (i + 1), i + 1);
        }
        return Result.success("同步完成，共插入 " + urls.size() + " 条");
    }
}
