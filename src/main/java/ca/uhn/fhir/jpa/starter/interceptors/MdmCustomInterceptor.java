package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.mdm.dao.MdmLinkDaoSvc;
import ca.uhn.fhir.jpa.mdm.svc.MdmLinkSvcImpl;
import ca.uhn.fhir.jpa.mdm.svc.MdmResourceDaoSvc;
import ca.uhn.fhir.mdm.api.*;
import ca.uhn.fhir.mdm.model.MdmTransactionContext;
import ca.uhn.fhir.mdm.provider.MdmControllerHelper;
import ca.uhn.fhir.mdm.provider.MdmProviderDstu3Plus;
import ca.uhn.fhir.mdm.util.GoldenResourceHelper;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.forR5.ImmunizationMapperR5;
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

import static org.immregistries.iis.kernal.repository.FhirRequestBase.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.repository.FhirRequestBase.GOLDEN_SYSTEM_TAG;

@Component
@Interceptor
public class MdmCustomInterceptor {
	Logger logger = LoggerFactory.getLogger(MdmCustomInterceptor.class);
	@Autowired
	IFhirResourceDao<Immunization> immunizationDao;
	@Autowired
	MdmLinkDaoSvc mdmLinkDaoSvc;

	@Autowired
	MdmResourceDaoSvc mdmResourceDaoSvc;
	@Autowired
	MdmLinkSvcImpl mdmLinkSvc;
//	MdmLinkDaoSvc mdmLinkDaoSvc;
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
	@Autowired
	private GoldenResourceHelper myGoldenResourceHelper;
	MdmProviderDstu3Plus mdmProvider;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;

	private void initialize() {
		mdmProvider = new MdmProviderDstu3Plus(this.myFhirContext, this.myMdmControllerSvc, this.myMdmControllerHelper, this.myMdmSubmitSvc, this.myMdmSettings);
	}

	@Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED)
	public void invoke(IBaseResource theResource, RequestDetails theRequestDetails) {
		initialize();
//		mdmProvider = new MdmProviderDstu3Plus(this.myFhirContext, this.myMdmControllerSvc, this.myMdmControllerHelper, this.myMdmSubmitSvc, this.myMdmSettings);
		//TODO find a better way to figure type out
		try {
			Immunization immunization = (Immunization) theResource;
//			if(immunization.hasMeta() && immunization.getMeta().getTag(GOLDEN_SYSTEM_TAG,GOLDEN_RECORD) != null) { // if is golden record
//				return;
//			}
			if (immunization.getPatient() == null){
				throw new InvalidRequestException("No patient specified");
			}
			Deterministic comparer = new Deterministic();
			ComparisonResult comparison;
			org.immregistries.vaccination_deduplication.Immunization i1 = toVaccDedupImmunization(immunization, theRequestDetails);
			org.immregistries.vaccination_deduplication.Immunization i2;

			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			ServletRequestDetails servletRequestDetails = new ServletRequestDetails();
//			servletRequestDetails.setServer((RestfulServer) theRequestDetails.getServer());
			servletRequestDetails.setServletRequest(request);
			servletRequestDetails.setTenantId(theRequestDetails.getTenantId());
			MdmTransactionContext mdmTransactionContext = new MdmTransactionContext(MdmTransactionContext.OperationType.CREATE_RESOURCE);
			mdmTransactionContext.setResourceType("Immunization");
			Bundle bundle;
			IGenericClient client = repositoryClientFactory.newGenericClientForPartition(theRequestDetails.getTenantId());

			if (immunization.getPatient().getReference() != null) {
				bundle = (Bundle) client.search().byUrl("Immunization?_tag="+ GOLDEN_SYSTEM_TAG + "|"+ GOLDEN_RECORD +"&patient:mdm=" + immunization.getPatient().getReference()).execute();
			}
			else if (immunization.getPatient().getIdentifier() != null) {
				bundle = (Bundle) client.search().byUrl(
					"1/Immunization?_tag="+ GOLDEN_SYSTEM_TAG + "|"+ GOLDEN_RECORD
					+ "&patient.identifier=" + immunization.getPatient().getIdentifier().getSystem()
						+ "|" + immunization.getPatient().getIdentifier().getValue()
				);
			} else {
				throw new InvalidRequestException("No patient specified");
			}
			boolean hasMatch = false;
			for (Bundle.BundleEntryComponent entry: bundle.getEntry()){
				Immunization golden_i = (Immunization) entry.getResource();
				i2 = toVaccDedupImmunization(golden_i, theRequestDetails);
				comparison = comparer.compare(i1,i2);
				String matching_level = (golden_i.getPatient().equals(immunization.getPatient()))? "MATCH" : "POSSIBLE_MATCH";
				// TODO scan mdm links to check match level
				if (comparison.equals(ComparisonResult.EQUAL)) {
					mdmProvider.createLink(
						new StringType("Immunization/" + golden_i.getId().split("Immunization/")[1]),
						new StringType("Immunization/" + immunization.getId().split("Immunization/")[1]),
						new StringType(matching_level),
						servletRequestDetails
					);
					hasMatch = true;
					break;
				}
			}
			if (!hasMatch){
//				Create golden resource, currently made by mdm itself
//				IAnyResource golden = myGoldenResourceHelper.createGoldenResourceFromMdmSourceResource(immunization,mdmTransactionContext);
//				golden.setUserData(Constants.RESOURCE_PARTITION_ID, RequestPartitionId.fromPartitionName(theRequestDetails.getTenantId()));
//				mdmLinkSvc.updateLink(golden,immunization,MdmMatchOutcome.NEW_GOLDEN_RESOURCE_MATCH,MdmLinkSourceEnum.MANUAL,mdmTransactionContext);
			}
		} catch (ClassCastException c) {
		}
	}

	private org.immregistries.vaccination_deduplication.Immunization toVaccDedupImmunization(Immunization immunization, RequestDetails theRequestDetails){
		org.immregistries.vaccination_deduplication.Immunization i1 = new org.immregistries.vaccination_deduplication.Immunization();
		i1.setCVX(immunization.getVaccineCode().getCode(ImmunizationMapperR5.CVX));
		if(immunization.hasManufacturer()){
			i1.setMVX(immunization.getManufacturer().getIdentifier().getValue());
		}
		try {
			if (immunization.hasOccurrenceStringType()){
				i1.setDate(immunization.getOccurrenceStringType().getValue()); // TODO parse correctly
			} else if (immunization.hasOccurrenceDateTimeType()) {
				i1.setDate(immunization.getOccurrenceDateTimeType().getValue());
			}
		} catch (ParseException e) {
//				throw new RuntimeException(e);

		}


		i1.setLotNumber(immunization.getLotNumber());
		if (immunization.getPrimarySource()){
			i1.setSource(ImmunizationSource.SOURCE);
		} else {
			if (immunization.hasInformationSource()) {
				if (immunization.hasInformationSourceCodeableConcept()){
					if(immunization.getInformationSourceCodeableConcept().getCode(ImmunizationMapperR5.INFORMATION_SOURCE).equals("00")){
						i1.setSource(ImmunizationSource.SOURCE);
					} else {
						i1.setSource(ImmunizationSource.HISTORICAL);
					}
				}
			}
		}

		if (immunization.hasInformationSource()){ // TODO improve organisation naming and designation among tenancy or in resource info
			if (immunization.hasInformationSourceReference()) {
				if(immunization.getInformationSourceReference().getIdentifier() != null){
					i1.setOrganisationID(immunization.getInformationSourceReference().getIdentifier().getValue());
				} else if (immunization.getInformationSourceReference().getReference() != null
					&& immunization.getInformationSourceReference().getReference().startsWith("Organisation/")) {
					i1.setOrganisationID(immunization.getInformationSourceReference().getReference()); // TODO get organisation name from db
				}
			}
		}
		if ( i1.getOrganisationID().isBlank() && theRequestDetails != null){
			i1.setOrganisationID(theRequestDetails.getTenantId());
		}
		logger.info("Organisation id {}", i1);
		return i1;
	}

}
