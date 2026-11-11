package com.sun.aivideo.service;

import com.sun.aivideo.ai.image.model.GeneratedImage;
import com.sun.aivideo.model.vo.UploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

/**
 * 本地文件存储：上传的图片、下载到本地的视频、文生图结果。
 */
public interface FileStorageService {

    /** 保存上传的图片，返回 /files/images/xxx 访问路径 */
    UploadVO saveImage(MultipartFile file);

    /** 本地 /files/** 路径（或其他非公网地址）转换为厂商可接收的 data URI；公网地址原样返回 */
    String toProviderImageSource(String imageUrl);

    /** 异步把厂商视频下载到本地，返回 /files/videos/xxx.mp4；失败时返回 null */
    CompletableFuture<String> downloadVideoAsync(Long taskId, String remoteUrl);

    /** 厂商直接返回视频内容时（如 Gemini Omni），保存到本地，返回 /files/videos/xxx.mp4 */
    String saveVideoBytes(Long taskId, byte[] data);

    /**
     * 保存文生图结果到本地（图片字节直接写入，图片地址则下载），返回 /files/generated/xxx.png。
     * 百炼返回的图片地址 24 小时后失效，所以生成后立即保存。
     */
    String saveGeneratedImage(Long taskId, int index, GeneratedImage image);
}
