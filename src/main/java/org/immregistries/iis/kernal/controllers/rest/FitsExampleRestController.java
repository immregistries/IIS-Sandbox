package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.logic.hl7v2.FitsExamples;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(RestUrlUtil.REST + FitsExampleRestController.FITS_EXAMPLE_PATH)
public class FitsExampleRestController {

	public static final String FITS_EXAMPLE_PATH = "/fits/example";

	@GetMapping("all")
    public Map<String, String> getAllExamples() {
        return FitsExamples.exampleMap;
    }

    @GetMapping()
    public String getExampleByName(@RequestParam("name") String name) {
        return FitsExamples.exampleMap.get(name);
    }
}
