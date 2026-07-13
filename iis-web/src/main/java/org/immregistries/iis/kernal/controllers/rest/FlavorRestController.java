package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.enums.ProcessingFlavor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.FLAVORS_PATH)
public class FlavorRestController {

	@GetMapping
	public List<Map<String, String>> getFlavors() {
		return Arrays.stream(ProcessingFlavor.values())
			.map(f -> Map.of(
				"key", f.getKey(),
				"behaviorDescription", f.getBehaviorDescription()
			))
			.collect(Collectors.toList());
	}

}