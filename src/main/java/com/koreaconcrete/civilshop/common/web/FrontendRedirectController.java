package com.koreaconcrete.civilshop.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FrontendRedirectController {

	@GetMapping("/swagger-ui.html")
	String swaggerUiShortcut() {
		return "redirect:/swagger-ui/index.html";
	}

	@GetMapping({"/admin", "/admin/"})
	String adminShortcut() {
		return "redirect:/admin.html";
	}

	@GetMapping("/admin/{page}.html")
	String adminPageShortcut(@PathVariable String page) {
		return "redirect:/admin-" + page + ".html";
	}
}
