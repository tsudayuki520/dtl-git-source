package com.dlust.sportbackend.util;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsObject;
import com.obs.services.model.PutObjectRequest;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OBSUtil {

    // ===== 连接数据（请替换为你的实际值） =====
    private static final String AK = "HPUALIKJ3TIBPTVUDVRF";
    private static final String SK = "AtR5nr15JX7NHAY0B6IQXS4hqjtNHSbnWBb9cP9k";
    private static final String ENDPOINT = "https://obs.cn-north-4.myhuaweicloud.com";
    private static final String BUCKET_NAME = "wxsportproject";
    // ==========================================

    /**
     * 获取 ObsClient 实例
     */
    private static ObsClient getObsClient() {
        return new ObsClient(AK, SK, ENDPOINT);
    }

    /**
     * 上传文件到 OBS
     *
     * @param objectKey  对象在桶中的路径（如 "banner/1.jpg"）
     * @param localFile  本地文件
     * @return 上传成功返回文件的完整访问 URL，失败返回 null
     */
    public static String uploadFile(String objectKey, File localFile) {
        ObsClient obsClient = getObsClient();
        try {
            PutObjectRequest request = new PutObjectRequest();
            request.setBucketName(BUCKET_NAME);
            request.setObjectKey(objectKey);
            request.setFile(localFile);
            obsClient.putObject(request);
            return getUrl(objectKey);
        } catch (ObsException e) {
            System.out.println("上传失败 HTTP Code: " + e.getResponseCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Error Message: " + e.getErrorMessage());
            return null;
        } finally {
            closeClient(obsClient);
        }
    }

    /**
     * 上传文件流到 OBS
     *
     * @param objectKey  对象在桶中的路径
     * @param inputStream 文件输入流
     * @return 上传成功返回文件的完整访问 URL，失败返回 null
     */
    public static String uploadFile(String objectKey, InputStream inputStream) {
        ObsClient obsClient = getObsClient();
        try {
            obsClient.putObject(BUCKET_NAME, objectKey, inputStream);
            return getUrl(objectKey);
        } catch (ObsException e) {
            System.out.println("上传失败 HTTP Code: " + e.getResponseCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Error Message: " + e.getErrorMessage());
            return null;
        } finally {
            closeClient(obsClient);
        }
    }

    /**
     * 下载文件，返回输入流（调用方需负责关闭流）
     *
     * @param objectKey 对象在桶中的路径
     * @return 文件输入流，失败返回 null
     */
    public static InputStream downloadFile(String objectKey) {
        ObsClient obsClient = getObsClient();
        try {
            ObsObject obsObject = obsClient.getObject(BUCKET_NAME, objectKey);
            return obsObject.getObjectContent();
        } catch (ObsException e) {
            System.out.println("下载失败 HTTP Code: " + e.getResponseCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Error Message: " + e.getErrorMessage());
            return null;
        }
    }

    /**
     * 删除文件
     *
     * @param objectKey 对象在桶中的路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String objectKey) {
        ObsClient obsClient = getObsClient();
        try {
            obsClient.deleteObject(BUCKET_NAME, objectKey);
            return true;
        } catch (ObsException e) {
            System.out.println("删除失败 HTTP Code: " + e.getResponseCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Error Message: " + e.getErrorMessage());
            return false;
        } finally {
            closeClient(obsClient);
        }
    }

    /**
     * 列举指定前缀下的所有对象
     *
     * @param prefix 对象前缀（如 "banner/"）
     * @return 对象的完整 URL 列表
     */
    public static List<String> listFiles(String prefix) {
        ObsClient obsClient = getObsClient();
        try {
            ListObjectsRequest request = new ListObjectsRequest(BUCKET_NAME);
            request.setPrefix(prefix);
            ObjectListing result = obsClient.listObjects(request);
            List<String> urls = new ArrayList<>();
            for (ObsObject obj : result.getObjects()) {
                // 过滤掉目录标记对象（以 "/" 结尾的 0 字节对象）
                if (!obj.getObjectKey().endsWith("/")) {
                    urls.add(getUrl(obj.getObjectKey()));
                }
            }
            return urls;
        } catch (ObsException e) {
            System.out.println("列举失败 HTTP Code: " + e.getResponseCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Error Message: " + e.getErrorMessage());
            return new ArrayList<>();
        } finally {
            closeClient(obsClient);
        }
    }

    /**
     * 根据对象 Key 获取完整的访问 URL
     *
     * @param objectKey 对象在桶中的路径
     * @return 完整的 HTTP URL
     */
    public static String getUrl(String objectKey) {
        // endpoint 格式: https://obs.cn-north-4.myhuaweicloud.com
        String endpoint = ENDPOINT.replace("https://", "");
        return "https://" + BUCKET_NAME + "." + endpoint + "/" + objectKey;
    }

    private static void closeClient(ObsClient obsClient) {
        if (obsClient != null) {
            try {
                obsClient.close();
            } catch (Exception ignored) {
            }
        }
    }
}
