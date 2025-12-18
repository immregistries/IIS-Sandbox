package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FhirReadRequester {

	@Autowired
	DaoRegistry daoRegistry;

	/**
	 *
	 * @param fhirType FHIR Resource Class
	 * @param id       resource id
	 * @return resource found
	 */
	public IBaseResource read(String fhirType, String id) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(fhirType);
		return dao.read(new IdType(id), TenantUtil.get().requestDetailsWithPartitionName());
	}



}
