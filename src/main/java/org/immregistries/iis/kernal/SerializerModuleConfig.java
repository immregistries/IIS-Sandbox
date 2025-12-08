package org.immregistries.iis.kernal;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SerializerModuleConfig {

	@Bean
	public SimpleModule customModule(FhirContext fhirContext) {
		IParser parser = fhirContext.newJsonParser();
		SimpleModule module = new SimpleModule();
		// Register the custom serializer for the Fhir Serializers
		module.addSerializer(IBaseResource.class, new CustomFhirJacksonSerializer<IBaseResource>(parser));
		return module;
	}
}