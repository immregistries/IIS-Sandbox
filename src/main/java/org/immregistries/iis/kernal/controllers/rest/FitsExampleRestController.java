package org.immregistries.iis.kernal.controllers.rest;

import java.util.Map;
import org.immregistries.iis.kernal.logic.FitsExamples;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestUrlUtil.REST_KEY + "/fits/example")
public class FitsExampleRestController {

    @GetMapping("all")
    public Map<String, String> getAllExamples() {
        return FitsExamples.exampleMap;
    }

    @GetMapping
    public String getExampleByName(@RequestParam("name") String name) {
        return FitsExamples.exampleMap.get(name);
    }
}
