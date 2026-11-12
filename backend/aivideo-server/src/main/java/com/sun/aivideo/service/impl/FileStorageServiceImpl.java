package com.sun.aivideo.service.impl;

import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.ai.image.model.GeneratedImage;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.config.AiVideoProperties;
import com.sun.aivideo.config.AsyncConfig;
import com.sun.aivideo.model.vo.UploadVO;
import com.sun.aivideo.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final String FILES_PREFIX = "/files/";
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png",
            "webp", "image/webp", "bmp", "image/bmp");

    private final AiVideoProperties properties;
    private final HttpClient downloadClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public UploadVO saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.IMAGE_REQUIRED);
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BizException(ErrorCode.IMAGE_TOO_LARGE);
        }
        String ext = extension(file.getOriginalFilename());
        if (!IMAGE_TYPES.containsKey(ext)) {
            throw new BizException(ErrorCode.IMAGE_TYPE_UNSUPPORTED);
        }
        String relative = "images/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = filesRoot().resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BizException(ErrorCode.SAVE_IMAGE_FAILED, e.getMessage());
        }
        return new UploadVO(FILES_PREFIX + relative, file.getOriginalFilename(), file.getSize());
    }

    @Override
    public String toProviderImageSource(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        if (!imageUrl.startsWith(FILES_PREFIX)) {
            return imageUrl;
        }
        Path root = filesRoot();
        Path path = root.resolve(imageUrl.substring(FILES_PREFIX.length())).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new BizException(ErrorCode.LOCAL_IMAGE_NOT_FOUND, imageUrl);
        }
        String mime = IMAGE_TYPES.getOrDefault(extension(path.getFileName().toString()), "image/png");
        try {
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new BizException(ErrorCode.READ_IMAGE_FAILED, e.getMessage());
        }
    }

    @Override
    @Async(AsyncConfig.VIDEO_EXECUTOR)
    public CompletableFuture<String> downloadVideoAsync(Long taskId, String remoteUrl) {
        if (!StringUtils.hasText(remoteUrl)) {
            return CompletableFuture.completedFuture(null);
        }
        String relative = "videos/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "/task-" + taskId + ".mp4";
        Path target = filesRoot().resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            HttpRequest request = HttpRequest.newBuilder(URI.create(remoteUrl))
                    .timeout(Duration.ofMinutes(5))
                    .GET().build();
            HttpResponse<Path> resp = downloadClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if (resp.statusCode() / 100 != 2) {
                Files.deleteIfExists(target);
                log.warn("下载视频失败 taskId={} HTTP {}", taskId, resp.statusCode());
                return CompletableFuture.completedFuture(null);
            }
            log.info("视频已保存到本地 taskId={} {} ({} KB)", taskId, target, Files.size(target) / 1024);
            return CompletableFuture.completedFuture(FILES_PREFIX + relative);
        } catch (IOException e) {
            log.warn("下载视频失败 taskId={}：{}", taskId, e.getMessage());
            return CompletableFuture.completedFuture(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public String saveVideoBytes(Long taskId, byte[] data) {
        String relative = "videos/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "/task-" + taskId + ".mp4";
        Path target = filesRoot().resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
            log.info("视频已保存到本地 taskId={} {} ({} KB)", taskId, target, data.length / 1024);
            return FILES_PREFIX + relative;
        } catch (IOException e) {
            throw new BizException(ErrorCode.SAVE_VIDEO_FAILED, e.getMessage());
        }
    }

    @Override
    public String saveGeneratedImage(Long taskId, int index, GeneratedImage image) {
        String ext = switch (image.getMimeType() == null ? "image/png" : image.getMimeType()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "png";
        };
        String relative = "generated/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "/img-" + taskId + "-" + index + "." + ext;
        Path target = filesRoot().resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            if (image.getData() != null) {
                Files.write(target, image.getData());
            } else if (StringUtils.hasText(image.getUrl())) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(image.getUrl()))
                        .timeout(Duration.ofMinutes(2))
                        .GET().build();
                HttpResponse<Path> resp = downloadClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
                if (resp.statusCode() / 100 != 2) {
                    Files.deleteIfExists(target);
                    throw new BizException(ErrorCode.DOWNLOAD_IMAGE_FAILED, resp.statusCode());
                }
            } else {
                throw new BizException(ErrorCode.EMPTY_PROVIDER_IMAGE);
            }
            log.info("图片已保存到本地 taskId={} {} ({} KB)", taskId, target, Files.size(target) / 1024);
            return FILES_PREFIX + relative;
        } catch (IOException e) {
            throw new BizException(ErrorCode.SAVE_GENERATED_IMAGE_FAILED, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.IMAGE_SAVE_INTERRUPTED);
        }
    }

    private Path filesRoot() {
        return Path.of(properties.getStorage().getBaseDir(), "files").toAbsolutePath().normalize();
    }

    private static String extension(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }
}
