package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FhirSearchRequester {

	@Autowired
	DaoRegistry daoRegistry;


	public PatientMaster searchPatientMaster(SearchParameterMap searchParameterMap) {
		return (PatientMaster) searchMappedObjectMaster(PatientMapper.PATIENT, searchParameterMap);
	}

	public PatientReported searchPatientReported(SearchParameterMap searchParameterMap) {
		return (PatientReported) searchMappedObjectReportedWithMaster(PatientMapper.PATIENT, searchParameterMap);
	}

}
