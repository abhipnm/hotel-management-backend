package com.restaurantmanager.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Normalizes an uploaded logo to a decent standard resolution and sharpens it, so a small or
 * slightly blurry upload still looks crisp. Always re-encodes as PNG (lossless, keeps transparency).
 */
public final class ImageEnhancer {

    private static final int MIN_DIMENSION = 512;
    private static final int MAX_DIMENSION = 1024;

    private ImageEnhancer() {
    }

    public static byte[] enhance(byte[] original) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
        if (source == null) {
            throw new IllegalArgumentException("Unrecognized image format");
        }

        int longSide = Math.max(source.getWidth(), source.getHeight());
        double scale = 1.0;
        if (longSide < MIN_DIMENSION) {
            scale = (double) MIN_DIMENSION / longSide;
        } else if (longSide > MAX_DIMENSION) {
            scale = (double) MAX_DIMENSION / longSide;
        }

        BufferedImage resized = scale == 1.0
                ? source
                : resize(source, (int) Math.round(source.getWidth() * scale), (int) Math.round(source.getHeight() * scale));
        BufferedImage sharpened = sharpen(resized);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(sharpened, "png", out);
        return out.toByteArray();
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    /** A mild unsharp-style kernel (weights sum to 1, so overall brightness is unchanged). */
    private static BufferedImage sharpen(BufferedImage source) {
        float[] kernelData = {
                0f, -0.25f, 0f,
                -0.25f, 2f, -0.25f,
                0f, -0.25f, 0f
        };
        BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, kernelData), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        op.filter(source, dest);
        return dest;
    }
}
