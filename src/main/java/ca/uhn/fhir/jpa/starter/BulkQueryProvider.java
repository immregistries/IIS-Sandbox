package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProvider;
import ca.uhn.fhir.jpa.provider.r5.BaseJpaResourceProviderPatientR5;
import ca.uhn.fhir.jpa.provider.r5.JpaResourceProviderR5;
import ca.uhn.fhir.jpa.rp.r5.GroupResourceProvider;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.bulk.BulkDataExportOptions;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.BundleProviders;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Controller
public class BulkQueryProvider extends GroupResourceProvider {
	Logger logger = LoggerFactory.getLogger(BulkQueryProvider.class);

	@Autowired
	FhirContext fhirContext;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
//	@Autowired
//	IFhirResourceDao<Patient> myPatientDao;
	@Autowired
	BaseJpaResourceProvider<Patient> patientProvider;

	/**
	 * Group/123/$everything
	 */
	@Operation(name = JpaConstants.OPERATION_EVERYTHING, idempotent = true, bundleType = BundleTypeEnum.SEARCHSET)
	public Bundle groupInstanceEverything(

		javax.servlet.http.HttpServletRequest theServletRequest,

		@IdParam
			IdType theId,

		@Description(formalDefinition = "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
		@OperationParam(name = Constants.PARAM_COUNT)
			UnsignedIntType theCount,

		@Description(formalDefinition="Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
		@OperationParam(name = Constants.PARAM_OFFSET)
			UnsignedIntType theOffset,

		@Description(shortDefinition = "Only return resources which were last updated as specified by the given range")
		@OperationParam(name = Constants.PARAM_LASTUPDATED, min = 0, max = 1)
			DateRangeParam theLastUpdated,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _content filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_CONTENT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theContent,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _text filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TEXT, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theNarrative,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _filter filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_FILTER, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theFilter,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _type filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TYPE, min = 0, max = OperationParam.MAX_UNLIMITED)
			List<StringType> theTypes,

		@Sort
			SortSpec theSortSpec,

		RequestDetails theRequestDetails
	) {
		IGenericClient client = repositoryClientFactory.newGenericClientForPartition(theRequestDetails.getTenantId());
		Bundle bundle = new Bundle().setIdentifier(new Identifier().setValue("test-bulk"));

//		Parameters inParams = new Parameters();
//		for (Map.Entry<String,String[]> entry : theRequestDetails.getParameters().entrySet()) {
//			inParams.addParameter(entry.getKey(),entry.getValue()[0]);
//		}

		Group group = client.read().resource(Group.class).withId(theId).execute();
		for (Group.GroupMemberComponent member: group.getMember()) {
			if (member.getEntity().getReference().split("/")[0].equals("Patient")) {
				Bundle patientBundle = new Bundle();
//				patientBundle = client.operation().onInstance(member.getEntity().getReference()).named("$everything").withParameters(inParams).returnResourceType(Bundle.class).execute();
				IBundleProvider bundleProvider = ((BaseJpaResourceProviderPatientR5) patientProvider).patientInstanceEverything(theServletRequest, new IdType( member.getEntity().getReference()), theCount, theOffset, theLastUpdated, theContent, theNarrative, theFilter, theTypes, theSortSpec, theRequestDetails);
				for (IBaseResource resource: bundleProvider.getAllResources()) {
					patientBundle.addEntry().setResource( (Resource) resource);
				}
				bundle.addEntry().setResource(patientBundle);
			}
		}
//		logger.info("ND {}", fhirContext.newNDJsonParser().encodeResourceToString(bundle));
		return bundle;
	}
}