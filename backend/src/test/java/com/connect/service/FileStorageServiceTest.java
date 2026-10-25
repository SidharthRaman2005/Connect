package com.connect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.connect.exception.BadRequestException;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service() {
        return new FileStorageService(tempDir.toString());
    }

    @Test
    void storesImageAndReturnsPublicPath() {
        MockMultipartFile image =
                new MockMultipartFile("file", "p.png", "image/png", new byte[] {1, 2, 3, 4});

        String url = service().storeProfilePicture(image);

        assertThat(url).startsWith(FileStorageService.PUBLIC_PREFIX).endsWith(".png");
        String name = url.substring(FileStorageService.PUBLIC_PREFIX.length());
        assertThat(Files.exists(tempDir.resolve(name))).isTrue();
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "p.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service().storeProfilePicture(empty))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsNonImageType() {
        MockMultipartFile text =
                new MockMultipartFile("file", "x.txt", "text/plain", "hi".getBytes());

        assertThatThrownBy(() -> service().storeProfilePicture(text))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsOversizeImage() {
        byte[] tooBig = new byte[2 * 1024 * 1024 + 1]; // just over 2 MB
        MockMultipartFile big = new MockMultipartFile("file", "p.png", "image/png", tooBig);

        assertThatThrownBy(() -> service().storeProfilePicture(big))
                .isInstanceOf(BadRequestException.class);
    }
}
