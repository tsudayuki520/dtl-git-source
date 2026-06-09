package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.NoticeService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Notice;
import com.dlust.sportbackend.util.OBSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/notice")
public class AdminNoticeController {

    @Autowired
    private NoticeService noticeService;

    @PostMapping("/add")
    public Result<String> add(@RequestBody Notice notice) {
        log.info("添加通知: title={}", notice.getTitle());
        noticeService.add(notice);
        return Result.success("添加成功");
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectKey = "notice/" + UUID.randomUUID() + ext;
            String fileUrl = OBSUtil.uploadFile(objectKey, file.getInputStream());
            if (fileUrl == null) {
                return Result.error(500, "上传到OBS失败");
            }
            // 返回 URL 和原始文件名，用逗号拼接
            return Result.success(fileUrl + "," + (originalName != null ? originalName : ""));
        } catch (Exception e) {
            log.error("上传通知附件失败", e);
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Notice notice) {
        log.info("更新通知: id={}", notice.getId());
        noticeService.update(notice);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除通知: id={}", id);
        Notice notice = noticeService.getById(id);
        if (notice != null && notice.getFileUrl() != null) {
            String objectKey = extractObjectKey(notice.getFileUrl());
            if (objectKey != null) {
                OBSUtil.deleteFile(objectKey);
                log.info("已删除OBS附件: {}", objectKey);
            }
        }
        noticeService.delete(id);
        return Result.success("删除成功");
    }

    private String extractObjectKey(String url) {
        String baseUrl = OBSUtil.getUrl("");
        if (url.startsWith(baseUrl)) {
            return url.substring(baseUrl.length());
        }
        return null;
    }
}
