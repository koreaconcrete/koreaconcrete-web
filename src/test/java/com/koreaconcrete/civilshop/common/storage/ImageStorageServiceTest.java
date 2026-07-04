package com.koreaconcrete.civilshop.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageServiceTest {
	@TempDir
	Path tempDir;

	@Test
	void storesAndDeletesLocalImage() throws Exception {
		UploadStorageProperties properties = new UploadStorageProperties();
		properties.setBaseDir(tempDir.toString());
		properties.setPublicUrlPrefix("/uploads");
		ImageStorageService service = new ImageStorageService(properties);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"sample.png",
				"image/png",
				new byte[] {1, 2, 3}
		);

		String url = service.store(file, "products");
		Path storedPath = tempDir.resolve(url.substring("/uploads/".length())).normalize();

		assertThat(url).startsWith("/uploads/products/");
		assertThat(Files.exists(storedPath)).isTrue();

		service.delete(url);

		assertThat(Files.exists(storedPath)).isFalse();
	}
}
