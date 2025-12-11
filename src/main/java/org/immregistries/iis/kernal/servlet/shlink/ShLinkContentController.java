package org.immregistries.iis.kernal.servlet.shlink;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.persisted.repository.IisShlinkContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.servlet.shlink.ShLinkContentController.SHLINK_CONTENT_PATH;

@RestController
@RequestMapping(SHLINK_CONTENT_PATH)
public class ShLinkContentController {

	public static final String SHLINK_FILES = "shlink/files";
	public static final String SHLINK_CONTENT_PATH = "/" + SHLINK_FILES;

	@Autowired
	ShLinkUtilService shLinkUtilService;
	@Autowired
	IisShlinkContentRepository iisShlinkContentRepository;

	@GetMapping("/{id}")
	public String getContent(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String contentId,
			@RequestParam(value = "recipient", required = false) String recipient) {
		resp.setContentType("text/plain");
		return iisShlinkContentRepository.findById(Integer.parseInt(contentId))
				.map(iisShLinkContent -> iisShLinkContent.getContent()).orElse(null);
	}
}
