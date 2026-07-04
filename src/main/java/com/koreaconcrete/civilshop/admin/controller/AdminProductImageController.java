package com.koreaconcrete.civilshop.admin.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.koreaconcrete.civilshop.common.storage.ImageStorageService;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductImageController {
	private final ImageStorageService imageStorageService;

	public AdminProductImageController(ImageStorageService imageStorageService) {
		this.imageStorageService = imageStorageService;
	}

	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, String> upload(@RequestPart("file") MultipartFile file) {
		return Map.of("url", imageStorageService.store(file, "products"));
	}
}
