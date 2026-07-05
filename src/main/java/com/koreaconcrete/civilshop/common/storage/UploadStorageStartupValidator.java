package com.koreaconcrete.civilshop.common.storage;

import java.util.Arrays;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UploadStorageStartupValidator implements ApplicationRunner {
	private final UploadStorageProperties uploadStorageProperties;
	private final Environment environment;

	public UploadStorageStartupValidator(UploadStorageProperties uploadStorageProperties, Environment environment) {
		this.uploadStorageProperties = uploadStorageProperties;
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		boolean prodProfile = Arrays.asList(environment.getActiveProfiles()).contains("prod");
		boolean cloudRun = StringUtils.hasText(environment.getProperty("K_SERVICE"));
		if ((prodProfile || cloudRun) && !uploadStorageProperties.isGcs()) {
			throw new IllegalStateException("Cloud Run/prod requires APP_UPLOAD_PROVIDER=gcs and a persistent GCS upload bucket.");
		}
		if (uploadStorageProperties.isGcs()) {
			uploadStorageProperties.requiredGcsBucket();
		}
	}
}
