package com.sun.aivideo.ai.image.strategy;

import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.ai.image.model.GeneratedImage;
import com.sun.aivideo.ai.image.model.ImageGenerateRequest;
import com.sun.aivideo.ai.image.model.ImageGenerateResult;
import com.sun.aivideo.common.enums.ImageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 文生图策略：本地模拟。未配置 API Key 时使用，不产生费用。
 * 按请求的画幅本地绘制一张渐变占位图（不含文字），约 2 秒后返回。
 */
@Slf4j
@Component
public class MockImageStrategy implements ImageGenerationStrategy {

    private static final int BASE = 768;
    private static final long FAKE_LATENCY_MS = 2000;

    @Override
    public ImageProvider provider() {
        return ImageProvider.MOCK;
    }

    @Override
    public ImageGenerateResult generate(ImageGenerateRequest req) {
        try {
            Thread.sleep(FAKE_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int[] wh = size(req.getAspectRatio());
        BufferedImage img = new BufferedImage(wh[0], wh[1], BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 只画渐变和图形、不画文字：首次绘制文字会扫描系统字体，在 Windows 上可能要几十秒
        g.setPaint(new GradientPaint(0, 0, new Color(76, 141, 246), wh[0], wh[1], new Color(209, 107, 165)));
        g.fillRect(0, 0, wh[0], wh[1]);
        int r = Math.min(wh[0], wh[1]) / 6;
        int cx = wh[0] / 2;
        int cy = wh[1] / 2;
        g.setColor(new Color(255, 255, 255, 60));
        g.fillOval(cx - r * 2, cy - r * 2, r * 4, r * 4);
        g.setColor(new Color(255, 255, 255, 220));
        // 一个四角星，表示"AI 生成"
        int[] xs = {cx, cx + r / 4, cx + r, cx + r / 4, cx, cx - r / 4, cx - r, cx - r / 4};
        int[] ys = {cy - r, cy - r / 4, cy, cy + r / 4, cy + r, cy + r / 4, cy, cy - r / 4};
        g.fillPolygon(xs, ys, xs.length);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", out);
        } catch (IOException e) {
            throw new AiClientException("MOCK_ERROR", "生成模拟图片失败：" + e.getMessage(), e);
        }
        log.info("[Mock] 文生图完成 model={} bizTaskId={}", req.getModelCode(), req.getBizTaskId());
        return ImageGenerateResult.builder()
                .images(List.of(GeneratedImage.ofData(out.toByteArray(), "image/png")))
                .build();
    }

    private static int[] size(String ratio) {
        if (ratio == null || !ratio.matches("\\d+:\\d+")) {
            return new int[]{BASE, BASE};
        }
        String[] p = ratio.split(":");
        double w = Double.parseDouble(p[0]);
        double h = Double.parseDouble(p[1]);
        return w >= h ? new int[]{BASE, (int) Math.round(BASE * h / w)} : new int[]{(int) Math.round(BASE * w / h), BASE};
    }
}
