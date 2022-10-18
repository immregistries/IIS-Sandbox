package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.mdm.dao.MdmLinkDaoSvc;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.mdm.api.IMdmControllerSvc;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.api.IMdmSubmitSvc;
import ca.uhn.fhir.mdm.provider.MdmControllerHelper;
import ca.uhn.fhir.mdm.provider.MdmProviderDstu3Plus;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.immregistries.vaccination_deduplication.computation_classes.Deterministic;
import org.immregistries.vaccination_deduplication.reference.ComparisonResult;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.interceptor.Interceptor;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;

import static org.immregistries.iis.kernal.repository.FhirRequests.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.repository.FhirRequests.GOLDEN_SYSTEM_TAG;

@Component
@Interceptor
public class MdmCustomInterceptor {
	Logger log = LoggerFactory.getLogger(MdmCustomInterceptor.class);
	@Autowired
	IFhirResourceDao<Immunization> immunizationDao;
	@Autowired
	MdmLinkDaoSvc mdmLinkDaoSvc;

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

	MdmProviderDstu3Plus mdmProvider;

	@Autowired
	RepositoryClientFactory repositoryClientFactory;

	private void initialize() {
		mdmProvider = new MdmProviderDstu3Plus(this.myFhirContext, this.myMdmControllerSvc, this.myMdmControllerHelper, this.myMdmSubmitSvc, this.myMdmSettings);
	}


	@Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
	public void invoke(IBaseResource theResource, RequestDetails theRequestDetails) {
		initialize();
//		mdmProvider = new MdmProviderDstu3Plus(this.myFhirContext, this.myMdmControllerSvc, this.myMdmControllerHelper, this.myMdmSubmitSvc, this.myMdmSettings);
		//TODO find a better way to figure type out
		try {
			Immunization immunization = (Immunization) theResource;
			if (immunization.getPatient() == null){
				throw new InvalidRequestException("No patient specified");
			}
			Deterministic comparer = new Deterministic();
			ComparisonResult comparison;
			org.immregistries.vaccination_deduplication.Immunization i1 = toVaccDedupImmunization(immunization);
			org.immregistries.vaccination_deduplication.Immunization i2;

			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			ServletRequestDetails servletRequestDetails = new ServletRequestDetails();
//			servletRequestDetails.setServer((RestfulServer) theRequestDetails.getServer());
//			servletRequestDetails.setServletRequest(request);
			servletRequestDetails.setTenantId(theRequestDetails.getTenantId());

			SearchParameterMap searchParameterMap = new SearchParameterMap()
				.add("_tag", new TokenParam(GOLDEN_SYSTEM_TAG,GOLDEN_RECORD));
			Bundle bundle;
			IGenericClient client = repositoryClientFactory.newGenericClientForPartition(theRequestDetails.getTenantId());

			if (immunization.getPatient().getReference() != null) {
				StringParam param =  new StringParam();
				param.setValueAsQueryToken(myFhirContext,"patient","mdm",immunization.getPatient().getReference());
				searchParameterMap.add("patient:mdm", param);
				bundle = (Bundle) client.search().byUrl("Immunization?_tag="+ GOLDEN_SYSTEM_TAG + "|"+ GOLDEN_RECORD +"&patient:mdm=" + immunization.getPatient().getReference()).execute();
//				mdmProvider.queryLinks(
//					new StringType(immunization.getPatient().getReference()),
//					new StringType(immunization.getPatient().getReference()),
//					new StringType("MATCH"),
//					new StringType(""),
//					new UnsignedIntType(0),
//					new UnsignedIntType(100),
//					servletRequestDetails);
			} else if (immunization.getPatient().getIdentifier() != null){
//				searchParameterMap.add("patient.identifier", new TokenParam(immunization.getPatient().getIdentifier().getSystem(),immunization.getPatient().getIdentifier().getValue()));
				bundle = (Bundle) client.search().byUrl(
					"Immunization?_tag="+ GOLDEN_SYSTEM_TAG + "|"+ GOLDEN_RECORD
					+ "&patient.identifier=" + immunization.getPatient().getIdentifier().getSystem()
						+ "|" + immunization.getPatient().getIdentifier().getValue()
				);
			} else {
				throw new InvalidRequestException("No patient specified");
			}
//			for (IBaseResource golden: bun){
			for (Bundle.BundleEntryComponent entry: bundle.getEntry()){

				Immunization golden_i = (Immunization) entry.getResource();
				log.info("{}", golden_i.getIdentifierFirstRep().getValue());

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
		initialize();
		log.info("MDM_AFTER_PERSISTED_RESOURCE_CHECKED");
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

	public void setMdmProvider(MdmProviderDstu3Plus mdmProvider) {
		this.mdmProvider = mdmProvider;
	}
}
