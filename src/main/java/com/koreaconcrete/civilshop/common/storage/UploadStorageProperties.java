package com.koreaconcrete.civilshop.common.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadStorageProperties {
	private String provider = "local";
	private String baseDir = "uploads";
	private String publicUrlPrefix = "/uploads";
	private String gcsBucket;
	private String gcsPublicUrlPrefix;
	private long maxImageSizeBytes = 8L * 1024L * 1024L;

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

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

	public String getGcsBucket() {
		return gcsBucket;
	}

	public void setGcsBucket(String gcsBucket) {
		this.gcsBucket = gcsBucket;
	}

	public String getGcsPublicUrlPrefix() {
		return gcsPublicUrlPrefix;
	}

	public void setGcsPublicUrlPrefix(String gcsPublicUrlPrefix) {
		this.gcsPublicUrlPrefix = gcsPublicUrlPrefix;
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

	public boolean isGcs() {
		return "gcs".equalsIgnoreCase(provider == null ? "" : provider.trim());
	}

	public String objectName(String folder, String fileName) {
		return normalizeFolder(folder) + "/" + fileName;
	}

	public String publicUrl(String folder, String fileName) {
		String prefix = isGcs() ? gcsPublicUrlPrefix() : localPublicUrlPrefix();
		return trimTrailingSlashes(prefix) + "/" + objectName(folder, fileName);
	}

	public String localPublicUrlPrefix() {
		return publicUrlPrefix == null || publicUrlPrefix.isBlank() ? "/uploads" : publicUrlPrefix.trim();
	}

	public String gcsPublicUrlPrefix() {
		if (gcsPublicUrlPrefix != null && !gcsPublicUrlPrefix.isBlank()) {
			return gcsPublicUrlPrefix.trim();
		}
		return "https://storage.googleapis.com/" + requiredGcsBucket();
	}

	public String requiredGcsBucket() {
		if (gcsBucket == null || gcsBucket.isBlank()) {
			throw new IllegalStateException("GCS bucket is required when app.upload.provider=gcs");
		}
		return gcsBucket.trim();
	}

	private String normalizeFolder(String folder) {
		if (folder == null || folder.isBlank()) {
			throw new IllegalArgumentException("Upload folder is required");
		}
		String normalized = folder.trim();
		if (normalized.startsWith("/") || normalized.endsWith("/") || normalized.contains("..")) {
			throw new IllegalArgumentException("Invalid upload folder: " + folder);
		}
		return normalized;
	}

	private String trimTrailingSlashes(String value) {
		String prefix = value == null || value.isBlank() ? "/uploads" : value.trim();
		while (prefix.endsWith("/")) {
			prefix = prefix.substring(0, prefix.length() - 1);
		}
		return prefix;
	}
}
