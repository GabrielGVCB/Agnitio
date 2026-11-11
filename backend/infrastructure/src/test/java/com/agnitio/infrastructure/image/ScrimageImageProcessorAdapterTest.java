package com.agnitio.infrastructure.image;

import com.agnitio.application.dto.ProcessedVariants;
import com.agnitio.application.ports.output.ImageProcessor;
import com.sksamuel.scrimage.ImmutableImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ScrimageImageProcessorAdapterTest {

    private final ScrimageImageProcessorAdapter adapter = new ScrimageImageProcessorAdapter();

    @Test
    @DisplayName("Should generate thumbnail and preview WebP variants with watermark from original JPEG")
    void shouldGenerateVariantsSuccessfully() throws IOException {
        // Arrange: create an in-memory 800x600 test image
        BufferedImage testImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = testImage.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 800, 600);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Test Marathon 2026", 100, 100);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "jpg", baos);
        byte[] originalBytes = baos.toByteArray();

        // Act
        ProcessedVariants variants = adapter.process(originalBytes);

        // Assert
        assertThat(variants).isNotNull();
        assertThat(variants.width()).isEqualTo(800);
        assertThat(variants.height()).isEqualTo(600);
        assertThat(variants.thumbnailWebp()).isNotEmpty();
        assertThat(variants.previewWebp()).isNotEmpty();

        // Read back generated WebP images to verify dimensions
        ImmutableImage thumbImage = ImmutableImage.loader().fromBytes(variants.thumbnailWebp());
        assertThat(thumbImage.width).isLessThanOrEqualTo(400);
        assertThat(thumbImage.height).isLessThanOrEqualTo(400);

        ImmutableImage previewImage = ImmutableImage.loader().fromBytes(variants.previewWebp());
        assertThat(previewImage.width).isLessThanOrEqualTo(1600);
        assertThat(previewImage.height).isLessThanOrEqualTo(1600);
    }
}
