package org.immregistries.iis.kernal;

import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;

/**
 * uses HAPIFHIR parser to serialize in API calls that are not in HAPIFHIR server
 * TODO solve XML issue
 * Currently requires controller to specify in Mapping annotation produces = {"application/json"}
 *
 * @param <resource>
 */
public class CustomFhirJacksonSerializer<resource extends IBaseResource> extends JsonSerializer<resource> {

	private final IParser parser;

	public CustomFhirJacksonSerializer(IParser parser) {
		this.parser = parser;
	}

	@Override
	public void serialize(resource resource, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
		String json = parser.encodeResourceToString(resource);
		jsonGenerator.writeRaw(json);
	}
}
