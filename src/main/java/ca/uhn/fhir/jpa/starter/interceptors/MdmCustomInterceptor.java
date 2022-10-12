package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.mdm.dao.MdmLinkDaoSvc;
import ca.uhn.fhir.jpa.mdm.svc.MdmResourceDaoSvc;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.mdm.api.MdmLinkSourceEnum;
import ca.uhn.fhir.mdm.api.MdmMatchOutcome;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hibernate.Query;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.immregistries.vaccination_deduplication.computation_classes.Deterministic;
import org.immregistries.vaccination_deduplication.reference.ComparisonResult;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.immregistries.iis.kernal.repository.FhirRequests.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.repository.FhirRequests.GOLDEN_SYSTEM_TAG;

@Service
public class MdmCustomInterceptor {
	List<String> myNamesToIgnore = asList("John Doe", "Jane Doe");
	@Autowired
	MdmResourceDaoSvc mdmResourceDaoSvc;
	@Autowired
	IFhirResourceDao<Immunization> immunizationDao;
	@Autowired
	MdmLinkDaoSvc mdmLinkDaoSvc;

//	@Autowired
//	RepositoryClientFactory repositoryClientFactory;

	@Hook(Pointcut.MDM_BEFORE_PERSISTED_RESOURCE_CHECKED)
	public void before(IBaseResource theResource, RequestDetails theRequestDetails) {

//		switch (theResource)
		//TODO find a better way to figure type out
		try {
			Immunization immunization = (Immunization) theResource;
			Deterministic comparer = new Deterministic();
			ComparisonResult comparison;
			org.immregistries.vaccination_deduplication.Immunization i1 = toVaccDedupImmunization(immunization);
			org.immregistries.vaccination_deduplication.Immunization i2;

			OrgAccess orgAccess = (OrgAccess) ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession(false).getAttribute("orgAccess");
			RequestDetails requestDetails =  new SystemRequestDetails();
			requestDetails.setTenantId(orgAccess.getAccessName());
//			SearchParameterMap searchParameterMap = new SearchParameterMap().add("_tag:not", new TokenParam(GOLDEN_SYSTEM_TAG,GOLDEN_RECORD));
//			IBundleProvider bundleProvider = immunizationDao.search(searchParameterMap, requestDetails);
			List<Immunization> goldenList = null;

			for (IAnyResource golden: goldenList){
				i2 = toVaccDedupImmunization((Immunization) golden);
				comparison = comparer.compare(i1,i2);
				if (comparison.equals(ComparisonResult.EQUAL)) {
					mdmLinkDaoSvc.createOrUpdateLinkEntity(golden,immunization, MdmMatchOutcome.POSSIBLE_MATCH, MdmLinkSourceEnum.MANUAL,null);
				}
			}
		} catch (ClassCastException c) {
		}
		try {
			Patient patient = (Patient) theResource;
		} catch (ClassCastException c) {
		}
	}

	@Hook(Pointcut.MDM_AFTER_PERSISTED_RESOURCE_CHECKED)
	public void after() {

	}

	private org.immregistries.vaccination_deduplication.Immunization toVaccDedupImmunization(Immunization immunization){
		org.immregistries.vaccination_deduplication.Immunization i1 = new org.immregistries.vaccination_deduplication.Immunization();
		i1.setCVX(immunization.getVaccineCode().toString());
		try {
			i1.setDate(String.valueOf(immunization.getOccurrenceDateTimeType()));
		} catch (ParseException e) {
//				throw new RuntimeException(e);
		}
		i1.setLotNumber(immunization.getLotNumber());
		if (immunization.getPrimarySource()){
			i1.setSource(ImmunizationSource.SOURCE);
		}else {
			i1.setSource(ImmunizationSource.HISTORICAL);
		}
		return i1;
	}
}
