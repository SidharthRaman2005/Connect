package com.connect.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.connect.exception.BadRequestException;

/** Validates, strips metadata from, and stores profile pictures locally. */
@Service
public class FileStorageService {
    private static final long MAX_UPLOAD_BYTES = 2L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 16_000_000L;
    public static final String PUBLIC_PREFIX = "/uploads/";
    private final Path root;

    public FileStorageService(@Value("${app.uploads.dir:uploads}") String uploadsDir) {
        this.root = Paths.get(uploadsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create uploads directory: " + root, e);
        }
    }

    public Path getRoot() { return root; }

    /** The stored PNG is rebuilt from decoded pixels, dropping source metadata such as EXIF. */
    public String storeProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("No file was uploaded.");
        if (file.getSize() > MAX_UPLOAD_BYTES) throw new BadRequestException("Image must be 2 MB or smaller.");
        try {
            byte[] input = file.getBytes();
            if (!hasSupportedSignature(input)) {
                throw new BadRequestException("Unsupported image data. Use a real PNG, JPEG, or GIF image.");
            }
            BufferedImage decoded;
            try {
                try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                    if (!readers.hasNext()) throw new BadRequestException("Could not decode this image.");
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(imageInput, true, true);
                        int sourceWidth = reader.getWidth(0);
                        int sourceHeight = reader.getHeight(0);
                        if (sourceWidth < 1 || sourceHeight < 1 || sourceWidth > MAX_DIMENSION
                                || sourceHeight > MAX_DIMENSION || (long) sourceWidth * sourceHeight > MAX_PIXELS) {
                            throw new BadRequestException("Image dimensions are invalid or too large.");
                        }
                        decoded = reader.read(0);
                    } finally {
                        reader.dispose();
                    }
                }
            } catch (IOException decodeException) {
                throw new BadRequestException("Could not decode valid image data.");
            }
            if (decoded == null) throw new BadRequestException("Could not decode this image.");

            double scale = Math.min(1d, 1024d / Math.max(decoded.getWidth(), decoded.getHeight()));
            int width = Math.max(1, (int) Math.round(decoded.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(decoded.getHeight() * scale));
            BufferedImage clean = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = clean.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(decoded, 0, 0, width, height, null);
            graphics.dispose();

            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            if (!ImageIO.write(clean, "png", encoded)) throw new BadRequestException("Could not process this image.");
            String filename = UUID.randomUUID().toString().replace("-", "") + ".png";
            Path target = root.resolve(filename).normalize();
            if (!target.getParent().equals(root)) throw new BadRequestException("Invalid file path.");
            Files.write(target, encoded.toByteArray());
            return PUBLIC_PREFIX + filename;
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to process or store uploaded image.", e);
        }
    }

    /** Deletes files created by this service and legacy generated avatar names only. */
    public void deleteManagedPicture(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(PUBLIC_PREFIX)) return;
        String filename = publicUrl.substring(PUBLIC_PREFIX.length());
        if (!filename.matches("[0-9a-fA-F]{32}\\.(png|jpg|jpeg|webp|gif)")) return;
        Path target = root.resolve(filename).normalize();
        if (!root.equals(target.getParent())) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not remove replaced profile picture.", e);
        }
    }

    private static boolean hasSupportedSignature(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d
                && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return true;
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) return true;
        return bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
    }
}
