package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.controllers.RestConstants;

import org.immregistries.iis.kernal.persisted.repository.IisShlinkContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.controllers.RestConstants.Path.SHLINK_CONTENT_PATH;

@RestController
@RequestMapping(SHLINK_CONTENT_PATH)
public class ShLinkContentRestController {

    @Autowired
    IisShlinkContentRepository iisShlinkContentRepository;

    @GetMapping(value = RestConstants.PathVariable.PlaceHolder.CONTENT_ID_PLACEHOLDER, produces = MediaType.TEXT_PLAIN_VALUE)
    public String getContent(@PathVariable(RestConstants.PathVariable.Key.CONTENT_ID) String contentId,
            @RequestParam(value = "recipient", required = false) String recipient) {
        return iisShlinkContentRepository.findById(Integer.parseInt(contentId))
                .map(iisShLinkContent -> iisShLinkContent.getContent()).orElse(null);
    }
}
