package org.immregistries.iis.kernal.serialization;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SerializerModuleConfig {

	@Bean
	public SimpleModule customModule(FhirContext fhirContext) {
		IParser parser = fhirContext.newJsonParser();
		SimpleModule module = new SimpleModule();
		// Register the custom serializer for the Fhir Serializers
		module.addSerializer(IAnyResource.class, new CustomFhirJacksonSerializer<IAnyResource>(parser));
		return module;
	}
}