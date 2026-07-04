package com.koreaconcrete.civilshop;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FrontendRoutingTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void mainPageIsServedByBackend() throws Exception {
		mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("assets/js/api.js")));
	}

	@Test
	void swaggerUiShortcutRedirectsToConfiguredPath() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/swagger-ui/index.html"));
	}

	@Test
	void adminFolderStylePageRedirectsToStaticFile() throws Exception {
		mockMvc.perform(get("/admin/products.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin-products.html"));
	}

	@Test
	void missingStaticResourceReturnsNotFound() throws Exception {
		mockMvc.perform(get("/missing-static-file.html"))
				.andExpect(status().isNotFound());
	}
}
