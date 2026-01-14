package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.controllers.rest.RestUrlUtil;
import org.immregistries.iis.kernal.persisted.repository.IisShlinkContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkContentRestController.SHLINK_CONTENT_PATH;


@RestController
@RequestMapping(SHLINK_CONTENT_PATH)
public class ShLinkContentRestController {
	public static final String SHLINK_FILES = "shlink/files";
	public static final String SHLINK_CONTENT_PATH = RestUrlUtil.REST_PATH + "/" + SHLINK_FILES;
	public static final String CONTENT_ID = "contentId";
	public static final String CONTENT_ID_PLACEHOLDER = "/{" + CONTENT_ID + "}";

	@Autowired
    IisShlinkContentRepository iisShlinkContentRepository;

    @GetMapping(value = CONTENT_ID_PLACEHOLDER, produces = MediaType.TEXT_PLAIN_VALUE)
    public String getContent(@PathVariable(CONTENT_ID) String contentId,
            @RequestParam(value = "recipient", required = false) String recipient) {
        return iisShlinkContentRepository.findById(Integer.parseInt(contentId))
                .map(iisShLinkContent -> iisShLinkContent.getContent()).orElse(null);
    }
}
