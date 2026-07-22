package com.restaurantmanager.util;

import java.nio.charset.StandardCharsets;

/**
 * Checks an uploaded file's actual header bytes against the magic number for its declared
 * Content-Type. The Content-Type header is entirely client-controlled, so trusting it alone
 * lets a caller relabel an arbitrary file (e.g. a script or executable) as "image/png".
 */
public final class ImageSignature {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87_SIGNATURE = "GIF87a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] GIF89_SIGNATURE = "GIF89a".getBytes(StandardCharsets.US_ASCII);

    private ImageSignature() {
    }

    public static boolean matches(byte[] bytes, String declaredContentType) {
        if (bytes == null || declaredContentType == null) {
            return false;
        }
        return switch (declaredContentType) {
            case "image/png" -> startsWith(bytes, PNG_SIGNATURE);
            case "image/jpeg" -> startsWith(bytes, JPEG_SIGNATURE);
            case "image/gif" -> startsWith(bytes, GIF87_SIGNATURE) || startsWith(bytes, GIF89_SIGNATURE);
            case "image/webp" -> isWebp(bytes);
            default -> false;
        };
    }

    // RIFF <4-byte little-endian size> WEBP
    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private static boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
