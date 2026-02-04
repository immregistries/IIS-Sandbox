package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.MessageReceived;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.repository.MessageReceivedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(IisRestPath.REST_TENANT_PATH + IisRestPath.BasePath.MESSAGE_PATH)
public class MessageRestController {

    @Autowired
    MessageReceivedRepository messageReceivedRepository;

    @GetMapping(IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER)
    public List<MessageReceived> getPatientMessages(
		 @RequestAttribute(name = GlobalConstants.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId) {
        return messageReceivedRepository.findByTenantAndPatientReportedId(tenant, patientId);
    }

    @GetMapping
    public List<MessageReceived> getMessages(
		 @RequestAttribute(name = GlobalConstants.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestParam(required = false) String search) {

        List<MessageReceived> messageReceivedList = messageReceivedRepository
                .findByTenantOrderByReportedDateDesc(tenant);

        // TODO paging with repo
        if (search != null && !search.isEmpty()) {
            // Filter in memory as per original controller logic
            // Or better, filter in query? Original logic filters in loop.
            // To keep consistency with original logic which limits to 10 *after* filtering?
            // Original logic:
            // Iterate list
            // If search matches, increment count.
            // If count > 10 break.
            // We should return the filtered list.

            // Note: The original controller logic is a bit inefficient (fetches all, then
            // filters).
            // We will replicate the logic but return the list.

            return messageReceivedList.stream()
                    .filter(m -> m.getMessageRequest().contains(search) || m.getMessageResponse().contains(search))
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList());
        } else {
            return messageReceivedList.stream().limit(10).collect(java.util.stream.Collectors.toList());
        }
    }

}
