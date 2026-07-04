package com.koreaconcrete.civilshop.common.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadStorageProperties {
	private String baseDir = "uploads";
	private String publicUrlPrefix = "/uploads";
	private long maxImageSizeBytes = 8L * 1024L * 1024L;

	public String getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	public String getPublicUrlPrefix() {
		return publicUrlPrefix;
	}

	public void setPublicUrlPrefix(String publicUrlPrefix) {
		this.publicUrlPrefix = publicUrlPrefix;
	}

	public long getMaxImageSizeBytes() {
		return maxImageSizeBytes;
	}

	public void setMaxImageSizeBytes(long maxImageSizeBytes) {
		this.maxImageSizeBytes = maxImageSizeBytes;
	}

	public Path basePath() {
		return Paths.get(baseDir).toAbsolutePath().normalize();
	}

	public String publicUrl(String folder, String fileName) {
		String prefix = publicUrlPrefix == null || publicUrlPrefix.isBlank() ? "/uploads" : publicUrlPrefix.trim();
		while (prefix.endsWith("/")) {
			prefix = prefix.substring(0, prefix.length() - 1);
		}
		return prefix + "/" + folder + "/" + fileName;
	}
}
