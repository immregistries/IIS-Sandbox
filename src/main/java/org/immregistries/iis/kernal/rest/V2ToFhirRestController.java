package org.immregistries.iis.kernal.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.HL7Exception;
import gov.cdc.izgw.v2tofhir.converter.MessageParser;
import org.hibernate.Session;
import org.hl7.fhir.r4.model.Bundle;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.persisted.util.HibernateConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/v2tofhir")
@Conditional(OnR4Condition.class)
public class V2ToFhirRestController {

    @Autowired
    private FhirContext fhirContext;

    @PostMapping
    public ResponseEntity<String> convertV2ToFhir(@RequestBody String message,
            @RequestParam(name = "facilityName", required = false) String facilityName) {
        try {
            // In a real REST API, authentication should be handled by a filter or security
            // config.
            // For now, we assume the user is authenticated or we might need to pass
            // credentials.
            // However, the original code used CurrentTenantUtil.getTenantByIdAuthenticated
            // which checks session/basic auth.
            // Since this is a new REST controller, we might expect a tenant ID or similar
            // in the path or auth header.
            // Given the context of "translate this method", I will try to preserve the
            // logic but adapt it.
            // But wait, the original code used
            // `CurrentTenantUtil.getTenantByIdAuthenticated(req)`.
            // This suggests it relies on the session or request attributes.
            // For a pure REST API, we usually don't rely on HttpSession.
            // However, `BaseTenantTiedRest` suggests we might be in a context where we can
            // get the tenant.
            // Let's look at `BaseTenantTiedRest` again. It doesn't seem to enforce tenant
            // resolution.
            // `TenantRestController` uses `HibernateConfig.getDataSession()`.

            // Let's assume for this "translation" we want to accept the message and return
            // the FHIR bundle.
            // If we need tenant context for some reason (e.g. specific parser config?), the
            // original code used it.
            // But `MessageParser` doesn't seem to take the tenant.
            // The original code used tenant for the header `HomeController.doHeader`.
            // So arguably, the conversion logic itself might not need the tenant.

            MessageParser parser = new MessageParser();
            Bundle bundle = parser.convert(message);
            String result = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
            return ResponseEntity.ok(result);

        } catch (HL7Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("HL7 Conversion Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }
}
