package com.connect.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.connect.exception.BadRequestException;

/**
 * Stores uploaded profile pictures on the local filesystem and returns the
 * public path they are served at ({@code /uploads/<name>}).
 *
 * <p>A pragmatic, single-node store suitable for development; a real deployment
 * would use object storage (S3/GCS) behind the same interface. The served
 * directory is configured by {@code app.uploads.dir}.
 */
@Service
public class FileStorageService {

    /** Allowed image content types mapped to the extension used on disk. */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2 MB

    /** Public URL prefix; must match the resource handler and security rule. */
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

    /** The absolute directory files are stored in (also used by the web layer). */
    public Path getRoot() {
        return root;
    }

    /**
     * Validates and stores an uploaded profile picture.
     *
     * @return the public path the image is served at, e.g. {@code /uploads/ab12.png}
     * @throws BadRequestException if the file is empty, too large, or not an
     *                             allowed image type
     */
    public String storeProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Image must be 2 MB or smaller.");
        }
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Unsupported image type. Use PNG, JPEG, WebP, or GIF.");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = root.resolve(filename).normalize();
        // Defense in depth: the generated name can't escape, but verify anyway.
        if (!target.getParent().equals(root)) {
            throw new BadRequestException("Invalid file path.");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file.", e);
        }
        return PUBLIC_PREFIX + filename;
    }
}
