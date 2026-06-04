package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.BannerService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Banner;
import com.dlust.sportbackend.util.OBSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/banner")
public class AdminBannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    public Result<List<Banner>> list() {
        log.info("查询所有轮播图");
        return Result.success(bannerService.getAll());
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("title") String title,
                                 @RequestParam(value = "sortOrder", defaultValue = "0") Integer sortOrder) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectKey = "banner/" + UUID.randomUUID() + ext;
            String imageUrl = OBSUtil.uploadFile(objectKey, file.getInputStream());
            if (imageUrl == null) {
                return Result.error(500, "上传到OBS失败");
            }
            bannerService.addBanner(imageUrl, title, sortOrder);
            return Result.success("上传成功");
        } catch (Exception e) {
            log.error("上传轮播图失败", e);
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Banner banner) {
        log.info("更新轮播图: id={}", banner.getId());
        bannerService.update(banner);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除轮播图: id={}", id);
        Banner banner = bannerService.getById(id);
        if (banner != null && banner.getImageUrl() != null) {
            String objectKey = extractObjectKey(banner.getImageUrl());
            if (objectKey != null) {
                OBSUtil.deleteFile(objectKey);
                log.info("已删除OBS文件: {}", objectKey);
            }
        }
        bannerService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 从完整URL中提取OBS对象Key
     */
    private String extractObjectKey(String imageUrl) {
        String baseUrl = OBSUtil.getUrl("");
        if (imageUrl.startsWith(baseUrl)) {
            return imageUrl.substring(baseUrl.length());
        }
        return null;
    }
}
