package com.koreaconcrete.civilshop.common.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class UploadStorageStartupValidatorTest {
	@Test
	void rejectsLocalProviderOnCloudRun() {
		UploadStorageProperties properties = new UploadStorageProperties();
		properties.setProvider("local");
		MockEnvironment environment = new MockEnvironment().withProperty("K_SERVICE", "koreaconcrete-web");

		assertThatThrownBy(() -> validator(properties, environment).run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("APP_UPLOAD_PROVIDER=gcs");
	}

	@Test
	void rejectsLocalProviderOnProdProfile() {
		UploadStorageProperties properties = new UploadStorageProperties();
		properties.setProvider("local");
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");

		assertThatThrownBy(() -> validator(properties, environment).run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("APP_UPLOAD_PROVIDER=gcs");
	}

	@Test
	void requiresBucketWhenGcsProviderIsEnabled() {
		UploadStorageProperties properties = new UploadStorageProperties();
		properties.setProvider("gcs");
		MockEnvironment environment = new MockEnvironment();

		assertThatThrownBy(() -> validator(properties, environment).run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("GCS bucket");
	}

	@Test
	void allowsLocalProviderDuringLocalDevelopment() {
		UploadStorageProperties properties = new UploadStorageProperties();
		properties.setProvider("local");
		MockEnvironment environment = new MockEnvironment();

		assertThatCode(() -> validator(properties, environment).run(null))
				.doesNotThrowAnyException();
	}

	@Test
	void allowsGcsProviderWhenBucketIsConfigured() {
		UploadStorageProperties properties = new UploadStorageProperties();
		properties.setProvider("gcs");
		properties.setGcsBucket("koreaconcrete-web-uploads");
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");

		assertThatCode(() -> validator(properties, environment).run(null))
				.doesNotThrowAnyException();
	}

	private UploadStorageStartupValidator validator(UploadStorageProperties properties, MockEnvironment environment) {
		return new UploadStorageStartupValidator(properties, environment);
	}
}
