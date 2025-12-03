package org.immregistries.iis.kernal.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ModelSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSerialization() throws Exception {
        testClass(new PatientGuardian());
        testClass(new ModelName());
        testClass(new VaccinationMaster());
        testClass(new PatientMaster());
        testClass(new OrgLocation());
        testClass(new ObservationMaster());
        testClass(new ModelPerson());
        testClass(new ModelPhone());
        testClass(new ModelAddress());
        testClass(new BusinessIdentifier());
        testClass(new ShLinkFilePayload());
        testClass(new ShLinkManifestRequestBody());
        testClass(new VaccinationReported());
        testClass(new PatientReported());
        testClass(new ObservationReported());
    }

    private void testClass(Object obj) throws Exception {
        String json = mapper.writeValueAsString(obj);
        Object deserialized = mapper.readValue(json, obj.getClass());
        assertNotNull(deserialized);
        System.out.println("Successfully serialized and deserialized " + obj.getClass().getSimpleName());
    }
}
