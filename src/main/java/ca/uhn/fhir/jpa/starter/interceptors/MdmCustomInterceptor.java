package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.mdm.dao.MdmLinkDaoSvc;
import ca.uhn.fhir.jpa.mdm.svc.MdmResourceDaoSvc;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.mdm.api.*;
import ca.uhn.fhir.mdm.provider.BaseMdmProvider;
import ca.uhn.fhir.mdm.provider.MdmControllerHelper;
import ca.uhn.fhir.mdm.provider.MdmProviderDstu3Plus;
import ca.uhn.fhir.mdm.provider.MdmProviderLoader;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.StringType;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.vaccination_deduplication.computation_classes.Deterministic;
import org.immregistries.vaccination_deduplication.reference.ComparisonResult;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.text.ParseException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.immregistries.iis.kernal.repository.FhirRequests.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.repository.FhirRequests.GOLDEN_SYSTEM_TAG;

@Service
public class MdmCustomInterceptor {
	Logger log = LoggerFactory.getLogger(MdmCustomInterceptor.class);
	@Autowired
	IFhirResourceDao<Immunization> immunizationDao;
	@Autowired
	MdmLinkDaoSvc mdmLinkDaoSvc;
	MdmProviderDstu3Plus mdmProvider;

	@Autowired
	private FhirContext myFhirContext;
	@Autowired
	private ResourceProviderFactory myResourceProviderFactory;
	@Autowired
	private MdmControllerHelper myMdmControllerHelper;
	@Autowired
	private IMdmControllerSvc myMdmControllerSvc;
	@Autowired
	private IMdmSubmitSvc myMdmSubmitSvc;
	@Autowired
	private IMdmSettings myMdmSettings;
//	@Autowired
//	RepositoryClientFactory repositoryClientFactory;

	@Hook(Pointcut.MDM_BEFORE_PERSISTED_RESOURCE_CHECKED)
	public void before(IBaseResource theResource, RequestDetails theRequestDetails) {
		log.info("MDM_BEFORE_PERSISTED_RESOURCE_CHECKED");
		mdmProvider = new MdmProviderDstu3Plus(this.myFhirContext, this.myMdmControllerSvc, this.myMdmControllerHelper, this.myMdmSubmitSvc, this.myMdmSettings);
		//TODO find a better way to figure type out
		try {
			Immunization immunization = (Immunization) theResource;
			if (immunization.getPatient()==null){
				throw new InvalidRequestException("No patient specified");
			}
			Deterministic comparer = new Deterministic();
			ComparisonResult comparison;
			org.immregistries.vaccination_deduplication.Immunization i1 = toVaccDedupImmunization(immunization);
			org.immregistries.vaccination_deduplication.Immunization i2;

			OrgAccess orgAccess = (OrgAccess) ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession(false).getAttribute("orgAccess");
			ServletRequestDetails servletRequestDetails = new ServletRequestDetails();
			servletRequestDetails.setTenantId(orgAccess.getAccessName());
			SearchParameterMap searchParameterMap = new SearchParameterMap()
				.add("_tag", new TokenParam(GOLDEN_SYSTEM_TAG,GOLDEN_RECORD));
			if (immunization.getPatient().getId() != null) {
				searchParameterMap.add("patient:mdm", new StringParam(immunization.getPatient().getId()));
			} else if (immunization.getPatient().getIdentifier() != null){
				searchParameterMap.add("patient.identifier", new TokenParam(immunization.getPatient().getIdentifier().getSystem(),immunization.getPatient().getIdentifier().getValue()));
			}

			IBundleProvider bundleProvider = immunizationDao.search(searchParameterMap, servletRequestDetails);

			for (IBaseResource golden: bundleProvider.getAllResources()){
				Immunization golden_i = (Immunization) golden;
				i2 = toVaccDedupImmunization(golden_i);
				comparison = comparer.compare(i1,i2);
				if (comparison.equals(ComparisonResult.EQUAL)) {
					log.info("MATCH");

					mdmProvider.createLink(new StringType(golden_i.getId()), new StringType(immunization.getId()),new StringType("MATCH"),servletRequestDetails);
//					mdmLinkDaoSvc.createOrUpdateLinkEntity(golden_i,immunization, MdmMatchOutcome.POSSIBLE_MATCH, MdmLinkSourceEnum.MANUAL,null);
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
