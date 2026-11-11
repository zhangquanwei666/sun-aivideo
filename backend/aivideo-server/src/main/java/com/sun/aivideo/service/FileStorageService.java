package com.sun.aivideo.service;

import com.sun.aivideo.model.vo.UploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

/**
 * 本地文件存储：上传的首帧图片、下载到本地的视频。
 */
public interface FileStorageService {

    /** 保存上传的图片，返回 /files/images/xxx 访问路径 */
    UploadVO saveImage(MultipartFile file);

    /** 本地 /files/** 路径（或其他非公网地址）转换为厂商可接收的 data URI；公网地址原样返回 */
    String toProviderImageSource(String imageUrl);

    /** 异步把厂商视频下载到本地，返回 /files/videos/xxx.mp4；失败时返回 null */
    CompletableFuture<String> downloadVideoAsync(Long taskId, String remoteUrl);
}
