package com.koreaconcrete.civilshop.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.koreaconcrete.civilshop.common.exception.BusinessException;

@Service
public class ImageStorageService {
	private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

	private final UploadStorageProperties properties;

	public ImageStorageService(UploadStorageProperties properties) {
		this.properties = properties;
	}

	public String store(MultipartFile file, String folder) {
		if (file == null || file.isEmpty()) {
			throw BusinessException.badRequest("업로드할 이미지 파일을 선택해주세요.");
		}
		if (file.getSize() > properties.getMaxImageSizeBytes()) {
			throw BusinessException.badRequest("이미지는 8MB 이하로 업로드해주세요.");
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw BusinessException.badRequest("이미지 파일만 업로드할 수 있습니다.");
		}
		if (properties.isGcs()) {
			return storeToGcs(file, folder, contentType);
		}
		return storeToLocal(file, folder, contentType);
	}

	public void delete(String url) {
		if (url == null || url.isBlank()) {
			return;
		}
		try {
			if (properties.isGcs()) {
				deleteFromGcs(url.trim());
			} else {
				deleteFromLocal(url.trim());
			}
		} catch (RuntimeException exception) {
			log.warn("Failed to delete uploaded image: {}", url, exception);
		}
	}

	private String storeToLocal(MultipartFile file, String folder, String contentType) {
		Path baseDirectory = properties.basePath();
		Path directory = baseDirectory.resolve(folder).normalize();
		if (!directory.startsWith(baseDirectory)) {
			throw BusinessException.badRequest("이미지 저장 경로를 확인할 수 없습니다.");
		}
		String extension = extension(contentType, file.getOriginalFilename());
		String fileName = UUID.randomUUID() + extension;
		Path target = directory.resolve(fileName).normalize();
		try {
			Files.createDirectories(directory);
			try (InputStream input = file.getInputStream()) {
				Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			throw BusinessException.badRequest("이미지 업로드에 실패했습니다. 서버 저장소 설정을 확인해주세요.");
		}
		return properties.publicUrl(folder, fileName);
	}

	private String storeToGcs(MultipartFile file, String folder, String contentType) {
		String extension = extension(contentType, file.getOriginalFilename());
		String fileName = UUID.randomUUID() + extension;
		String objectName = properties.objectName(folder, fileName);
		BlobInfo blobInfo = BlobInfo.newBuilder(properties.requiredGcsBucket(), objectName)
				.setContentType(contentType)
				.setCacheControl("public, max-age=31536000")
				.build();
		try {
			storage().create(blobInfo, file.getBytes());
		} catch (IOException | RuntimeException exception) {
			throw BusinessException.badRequest("이미지 업로드에 실패했습니다. 서버 저장소 설정을 확인해주세요.");
		}
		return properties.publicUrl(folder, fileName);
	}

	private void deleteFromLocal(String url) {
		String prefix = trimTrailingSlashes(properties.localPublicUrlPrefix());
		if (!url.startsWith(prefix + "/")) {
			return;
		}
		String relativePath = url.substring(prefix.length() + 1);
		Path baseDirectory = properties.basePath();
		Path target = baseDirectory.resolve(relativePath).normalize();
		if (!target.startsWith(baseDirectory)) {
			return;
		}
		try {
			Files.deleteIfExists(target);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to delete local image", exception);
		}
	}

	private void deleteFromGcs(String url) {
		objectNameFromGcsUrl(url).ifPresent(objectName -> storage().delete(properties.requiredGcsBucket(), objectName));
	}

	private Optional<String> objectNameFromGcsUrl(String url) {
		String prefix = trimTrailingSlashes(properties.gcsPublicUrlPrefix());
		if (!url.startsWith(prefix + "/")) {
			return Optional.empty();
		}
		String objectName = url.substring(prefix.length() + 1);
		if (objectName.isBlank() || objectName.contains("..")) {
			return Optional.empty();
		}
		return Optional.of(objectName);
	}

	private Storage storage() {
		return StorageOptions.getDefaultInstance().getService();
	}

	private String trimTrailingSlashes(String value) {
		String result = value == null ? "" : value.trim();
		while (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	private String extension(String contentType, String originalFilename) {
		if ("image/png".equals(contentType)) {
			return ".png";
		}
		if ("image/webp".equals(contentType)) {
			return ".webp";
		}
		if ("image/gif".equals(contentType)) {
			return ".gif";
		}
		if (originalFilename != null) {
			String name = originalFilename.toLowerCase();
			if (name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
				return name.substring(name.lastIndexOf('.'));
			}
		}
		return ".jpg";
	}
}
