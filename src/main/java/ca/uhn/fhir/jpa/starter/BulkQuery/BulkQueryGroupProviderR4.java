package ca.uhn.fhir.jpa.starter.BulkQuery;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProvider;
import ca.uhn.fhir.jpa.provider.r4.BaseJpaResourceProviderPatientR4;
import ca.uhn.fhir.jpa.rp.r4.GroupResourceProvider;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;

import java.util.List;


@Controller
@Conditional(OnR4Condition.class)
public class BulkQueryGroupProviderR4 extends GroupResourceProvider {
	Logger logger = LoggerFactory.getLogger(BulkQueryGroupProviderR4.class);

	@Autowired
	FhirContext fhirContext;
	@Autowired
	BaseJpaResourceProvider<Patient> patientProvider;

	@Autowired
	IFhirSystemDao fhirSystemDao;
	@Autowired
	IFhirResourceDao<Group> fhirResourceGroupDao;

	public BulkQueryGroupProviderR4() {
		super();
		setDao(fhirResourceGroupDao);
	}

	/**
	 * Group/123/$export
	 */
	@Operation(name = JpaConstants.OPERATION_EVERYTHING, idempotent = true, bundleType = BundleTypeEnum.SEARCHSET)
	public Bundle groupInstanceExport(

		javax.servlet.http.HttpServletRequest theServletRequest,

		@IdParam
			IdType theId,

		@Description(formalDefinition = "The format for the requested Bulk Data files to be generated as per FHIR Asynchronous Request Pattern. Defaults to application/fhir+ndjson. The server SHALL support Newline Delimited JSON, but MAY choose to support additional output formats. The server SHALL accept the full content type of application/fhir+ndjson as well as the abbreviated representations application/ndjson and ndjson.")
		@OperationParam(name = "_outputFormat")
			UnsignedIntType theOutputFormat,

		@Description(formalDefinition = "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
		@OperationParam(name = Constants.PARAM_COUNT)
			UnsignedIntType theCount,

		@Description(formalDefinition = "Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
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
		Bundle bundle = new Bundle();
		Group group = read(theServletRequest, theId, theRequestDetails);
		for (Group.GroupMemberComponent member : group.getMember()) {
			if (member.getEntity().getReference().split("/")[0].equals("Patient")) {
				Bundle patientBundle = new Bundle();
				IBundleProvider bundleProvider = ((BaseJpaResourceProviderPatientR4) patientProvider).patientInstanceEverything(theServletRequest, new IdType(member.getEntity().getReference()), theCount, theOffset, theLastUpdated, theContent, theNarrative, theFilter, theTypes, theSortSpec, theRequestDetails);
				for (IBaseResource resource : bundleProvider.getAllResources()) {
					patientBundle.addEntry().setResource((Resource) resource);
				}
				bundle.addEntry().setResource(patientBundle);
			}
		}

		if (theOutputFormat == null) {
//			theRequestDetails.se
		}

//		IParser parser = fhirContext.newNDJsonParser().setPrettyPrint(true);
//		return parser.encodeResourceToString(bundle);
		return bundle;
	}
}