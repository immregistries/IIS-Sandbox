package org.immregistries.iis.kernal.servlet.shlink;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.logic.shlink.IisShLinkContentService;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.model.persisted.IisShLinkContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.servlet.shlink.ShLinkContentController.SHLINK_CONTENT_PATH;

@RestController
@RequestMapping(SHLINK_CONTENT_PATH)
public class ShLinkContentController {

	public static final String SHLINK_CONTENT_PATH = "/shlink/files";

	@Autowired
	ShLinkUtilService shLinkUtilService;
	@Autowired
	PartitionCreationInterceptor partitionCreationInterceptor;
	@Autowired
	IisShLinkContentService iisShLinkContentService;


	@GetMapping("/{id}")
	public IisShLinkContent getContent(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String contentId, @RequestParam(value = "recipient", required = false) String recipient) {
		resp.setContentType("application/json");
		return iisShLinkContentService.getContent(contentId);
	}
}
