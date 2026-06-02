package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.BannerService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Banner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/banner")
public class UserBannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    public Result<List<Banner>> getBannerList() {
        log.info("获取轮播图列表");
        List<Banner> banners = bannerService.getActiveBanners();
        return Result.success(banners);
    }
}
