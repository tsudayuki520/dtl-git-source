package com.dlust.sportbackend;

import com.dlust.sportbackend.Service.BannerService;
import com.dlust.sportbackend.util.OBSUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class BannerTest {

    @Autowired
    private BannerService bannerService;

    /**
     * 从华为云 OBS 获取 banner 目录下的所有图片，依次写入数据库
     */
    @Test
    void testInsertBannersFromOBS() {
        // 1. 从 OBS 的 banner/ 目录下获取所有图片 URL
        List<String> imageUrls = OBSUtil.listFiles("article/");
        System.out.println("OBS 中找到 " + imageUrls.size() + " 张图片");

        // 2. 依次写入数据库
        for (int i = 0; i < imageUrls.size(); i++) {
            String url = imageUrls.get(i);
            bannerService.addBanner(url, "轮播图" + (i + 1), i + 1);
            System.out.println("已插入第 " + (i + 1) + " 张: " + url);
        }

        System.out.println("全部插入完成，共 " + imageUrls.size() + " 条");
    }
}
