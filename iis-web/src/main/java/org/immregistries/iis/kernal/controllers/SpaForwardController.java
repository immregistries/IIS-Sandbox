package org.immregistries.iis.kernal.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards Angular SPA deep-link routes to index.html.
 * Only matches paths whose last segment has no file extension (no dot),
 * so static assets (*.js, *.css, *.html, *.ico) are served directly
 * by Spring's default resource handler from classpath:/static/app/.
 */
@Controller
public class SpaForwardController {

	@GetMapping({"/app", "/app/"})
	public String forwardRoot() {
		return "forward:/app/index.html";
	}

	@GetMapping("/app/{segment:[^\\.]+}")
	public String forwardOneLevel() {
		return "forward:/app/index.html";
	}

	@GetMapping("/app/{seg1:[^\\.]+}/{seg2:[^\\.]+}")
	public String forwardTwoLevels() {
		return "forward:/app/index.html";
	}

	@GetMapping("/app/{seg1:[^\\.]+}/{seg2:[^\\.]+}/{seg3:[^\\.]+}")
	public String forwardThreeLevels() {
		return "forward:/app/index.html";
	}

	@GetMapping("/app/{seg1:[^\\.]+}/{seg2:[^\\.]+}/{seg3:[^\\.]+}/{seg4:[^\\.]+}")
	public String forwardFourLevels() {
		return "forward:/app/index.html";
	}
}
