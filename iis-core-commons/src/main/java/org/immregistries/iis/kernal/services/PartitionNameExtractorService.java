package org.immregistries.iis.kernal.services;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.GlobalConstants;
import org.springframework.stereotype.Service;

@Service
public class PartitionNameExtractorService {

	public String extractPartitionName(RequestDetails requestDetails) {
		String tenantId = requestDetails.getTenantId();
		if (StringUtils.isBlank(tenantId)) {
			throw new InvalidRequestException(Msg.code(343) + "No tenant ID was specified");
		} else {
			if (requestDetails.getTenantId().equals("ConnectathonUnsafe")) {
				return GlobalConstants.CONNECTATHON_USER;
			}
//			String[] ids = tenantId.split(PARTITION_NAME_SEPARATOR);
//			if (ids.length < 2){
//				throw new InvalidRequestException(Msg.code(343) + "No facility ID has been specified, expected structure is fhir/{tenantId}-{facilityId}");
//			}
			return tenantId;
		}
	}
}
