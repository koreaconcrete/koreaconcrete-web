package com.koreaconcrete.civilshop.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.koreaconcrete.civilshop.common.storage.UploadStorageProperties;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
	private final UploadStorageProperties uploadStorageProperties;

	public StaticResourceConfig(UploadStorageProperties uploadStorageProperties) {
		this.uploadStorageProperties = uploadStorageProperties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		if (uploadStorageProperties.isGcs()) {
			return;
		}
		String uploadLocation = uploadStorageProperties.basePath().toUri().toString();
		if (!uploadLocation.endsWith("/")) {
			uploadLocation += "/";
		}
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(uploadLocation);
	}
}
