package com.koreaconcrete.civilshop.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.koreaconcrete.civilshop.common.exception.BusinessException;

@Service
public class ImageStorageService {
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
