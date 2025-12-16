package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.logic.shlink.ShLinkUtilService;
import org.immregistries.iis.kernal.persisted.repository.IisShlinkContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("rest/shlink/files")
public class ShLinkContentRestController {

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
