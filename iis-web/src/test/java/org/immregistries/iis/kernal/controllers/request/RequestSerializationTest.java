package org.immregistries.iis.kernal.controllers.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.immregistries.iis.kernal.controllers.request.shlink.ShLinkCreationRequestDTO;
import org.immregistries.iis.kernal.controllers.request.shlink.ShLinkManifestRequestDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class RequestSerializationTest {
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testSerialization() throws Exception {
		testClass(new ShLinkManifestRequestDTO());
		testClass(new ShLinkCreationRequestDTO());
	}

	private void testClass(Object obj) throws Exception {
		String json = mapper.writeValueAsString(obj);
		Object deserialized = mapper.readValue(json, obj.getClass());
		assertNotNull(deserialized);
		System.out.println("Successfully serialized and deserialized " + obj.getClass().getSimpleName());
	}
}
