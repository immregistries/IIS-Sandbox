package org.immregistries.iis.kernal.controllers.rest.shlink;

import org.immregistries.iis.kernal.controllers.IisPathVariable;

import org.immregistries.iis.kernal.persisted.repository.IisShlinkContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.controllers.IisRestParam.ShLink.RECIPIENT;
import static org.immregistries.iis.kernal.controllers.IisRestPath.SH_LINK_CONTENT_PATH;

@RestController
@RequestMapping(SH_LINK_CONTENT_PATH)
public class ShLinkContentRestController {

	 @Autowired
    private IisShlinkContentRepository iisShlinkContentRepository;

    @GetMapping(value = IisPathVariable.PlaceHolder.CONTENT_ID_PLACEHOLDER, produces = MediaType.TEXT_PLAIN_VALUE)
    public String getContent(@PathVariable(IisPathVariable.Key.CONTENT_ID) String contentId,
            @RequestParam(value = RECIPIENT, required = false) String recipient) {
        return iisShlinkContentRepository.findById(Integer.parseInt(contentId))
                .map(iisShLinkContent -> iisShLinkContent.getContent()).orElse(null);
    }
}
