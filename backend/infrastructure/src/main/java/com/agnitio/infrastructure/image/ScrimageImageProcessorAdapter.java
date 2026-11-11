package com.agnitio.infrastructure.image;

import com.agnitio.application.dto.ProcessedVariants;
import com.agnitio.application.ports.output.ImageProcessor;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

@Component
public class ScrimageImageProcessorAdapter implements ImageProcessor {

    private static final int THUMB_MAX_DIMENSION = 400;
    private static final int PREVIEW_MAX_DIMENSION = 1600;
    private static final String WATERMARK_TEXT = "AGNITIO";

    @Override
    public ProcessedVariants process(byte[] originalImageBytes) {
        Objects.requireNonNull(originalImageBytes, "originalImageBytes must not be null");

        try {
            ImmutableImage original = ImmutableImage.loader().fromBytes(originalImageBytes);
            int origWidth = original.width;
            int origHeight = original.height;

            // 1. Generate thumbnail (max 400x400)
            ImmutableImage thumb = original.max(THUMB_MAX_DIMENSION, THUMB_MAX_DIMENSION);
            byte[] thumbWebp = thumb.bytes(WebpWriter.DEFAULT);

            // 2. Generate preview (max 1600x1600)
            ImmutableImage preview = original.max(PREVIEW_MAX_DIMENSION, PREVIEW_MAX_DIMENSION);
            ImmutableImage watermarkedPreview = applyWatermark(preview, WATERMARK_TEXT);
            byte[] previewWebp = watermarkedPreview.bytes(WebpWriter.DEFAULT);

            return new ProcessedVariants(thumbWebp, previewWebp, origWidth, origHeight);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process image: " + e.getMessage(), e);
        }
    }

    private ImmutableImage applyWatermark(ImmutableImage image, String watermarkText) {
        BufferedImage awt = image.awt();
        Graphics2D g2d = awt.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = awt.getWidth();
            int height = awt.getHeight();

            int fontSize = Math.max(28, width / 14);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics(font);
            int textWidth = metrics.stringWidth(watermarkText);
            int textHeight = metrics.getHeight();

            // Diagonal repeating watermark pattern
            g2d.setColor(new Color(255, 255, 255, 80)); // Semi-transparent white
            g2d.rotate(Math.toRadians(-30), width / 2.0, height / 2.0);

            int stepX = textWidth + (width / 5);
            int stepY = textHeight * 4;

            for (int x = -width; x < width * 2; x += stepX) {
                for (int y = -height; y < height * 2; y += stepY) {
                    g2d.drawString(watermarkText, x, y);
                }
            }
        } finally {
            g2d.dispose();
        }
        return ImmutableImage.fromAwt(awt);
    }
}
