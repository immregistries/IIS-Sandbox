package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.r5.model.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestUrlUtil.REST_TENANT_PATH + "/group")
public class GroupRestController {

    @GetMapping("/$generate")
    public Group generateGroup(HttpServletRequest req) {

        Group group = new Group();
        group.setManagingEntity(new Reference()
                .setIdentifier(new Identifier().setType(new CodeableConcept(new Coding().setCode("Organization")))
                        .setSystem("AIRA_TEST").setValue("test")));
        group.setDescription(
                "Generated Group in IIS sandbox, for Bulk data export use case and Synchronisation with subscription synchronisation");

        return group;
    }
}
