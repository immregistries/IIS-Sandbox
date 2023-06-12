package org.immregistries.iis.kernal.fhir.BulkQuery;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProviderPatient;
import ca.uhn.fhir.jpa.rp.r5.GroupResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
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
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@Controller
@Conditional(OnR5Condition.class)
public class BulkQueryGroupProviderR5 extends GroupResourceProvider {
	Logger logger = LoggerFactory.getLogger(BulkQueryGroupProviderR5.class);

	@Autowired
	BaseJpaResourceProviderPatient<Patient> patientProvider;

	@Autowired
	IFhirSystemDao fhirSystemDao;
	@Autowired
	IFhirResourceDao<Group> fhirResourceGroupDao;

	@Autowired
	private IFhirResourceDao<Binary> binaryDao;

	public BulkQueryGroupProviderR5() {
		super();
		setDao(fhirResourceGroupDao);
	}

	/**
	 * Group/123/$everything
	 */
	@Operation(name = JpaConstants.OPERATION_EVERYTHING, idempotent = true, bundleType = BundleTypeEnum.SEARCHSET)
	public Bundle groupInstanceEverything(

		@IdParam
		IdType theId,

		@Description(shortDefinition = "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
		@OperationParam(name = Constants.PARAM_COUNT, typeName = "unsignedInt")
		IPrimitiveType<Integer> theCount,

		@Description(shortDefinition = "Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
		@OperationParam(name = Constants.PARAM_OFFSET, typeName = "unsignedInt")
		IPrimitiveType<Integer> theOffset,

		@Description(shortDefinition = "Only return resources which were last updated as specified by the given range")
		@OperationParam(name = Constants.PARAM_LASTUPDATED, min = 0, max = 1)
		DateRangeParam theLastUpdated,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _content filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_CONTENT, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
		List<IPrimitiveType<String>> theContent,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _text filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TEXT, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
		List<IPrimitiveType<String>> theNarrative,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _filter filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_FILTER, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
		List<IPrimitiveType<String>> theFilter,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _type filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TYPE, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
		List<IPrimitiveType<String>> theTypes,

		@Sort
		SortSpec theSortSpec,

		ServletRequestDetails theRequestDetails
	) throws IOException {
		try {
			Bundle bundle = new Bundle();
			Group group = read(theRequestDetails.getServletRequest(), theId, theRequestDetails);
			for (Group.GroupMemberComponent member : group.getMember()) {
				if (member.getEntity().getReference().split("/")[0].equals("Patient")) {
					Bundle patientBundle = new Bundle();
					IBundleProvider bundleProvider = patientProvider.patientInstanceEverything(theRequestDetails.getServletRequest(), new IdType(member.getEntity().getReference()), theCount, theOffset, theLastUpdated, theContent, theNarrative, theFilter, theTypes, theSortSpec, theRequestDetails);
					for (IBaseResource resource : bundleProvider.getAllResources()) {
						patientBundle.addEntry().setResource((Resource) resource);
					}
					bundle.addEntry().setResource(patientBundle);
				}
			}
			return bundle;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

//	public void groupInstanceSynchExport(
//
//		@IdParam
//		IdType theId,
//
//		@Description(formalDefinition = "The format for the requested Bulk Data files to be generated as per FHIR Asynchronous Request Pattern. Defaults to application/fhir+ndjson. The server SHALL support Newline Delimited JSON, but MAY choose to support additional output formats. The server SHALL accept the full content type of application/fhir+ndjson as well as the abbreviated representations application/ndjson and ndjson.")
//		@OperationParam(name = "_outputFormat")
//		IPrimitiveType<String> theOutputFormat,
//
//		@Description(shortDefinition = "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
//		@OperationParam(name = Constants.PARAM_COUNT, typeName = "unsignedInt")
//		IPrimitiveType<Integer> theCount,
//
//		@Description(shortDefinition = "Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
//		@OperationParam(name = Constants.PARAM_OFFSET, typeName = "unsignedInt")
//		IPrimitiveType<Integer> theOffset,
//
//		@Description(shortDefinition = "Only return resources which were last updated as specified by the given range")
//		@OperationParam(name = Constants.PARAM_LASTUPDATED, min = 0, max = 1)
//		DateRangeParam theLastUpdated,
//
//		@Description(shortDefinition = "Filter the resources to return only resources matching the given _content filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
//		@OperationParam(name = Constants.PARAM_CONTENT, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
//		List<IPrimitiveType<String>> theContent,
//
//		@Description(shortDefinition = "Filter the resources to return only resources matching the given _text filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
//		@OperationParam(name = Constants.PARAM_TEXT, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
//		List<IPrimitiveType<String>> theNarrative,
//
//		@Description(shortDefinition = "Filter the resources to return only resources matching the given _filter filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
//		@OperationParam(name = Constants.PARAM_FILTER, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
//		List<IPrimitiveType<String>> theFilter,
//
//		@Description(shortDefinition = "Filter the resources to return only resources matching the given _type filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
//		@OperationParam(name = Constants.PARAM_TYPE, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
//		List<IPrimitiveType<String>> theTypes,
//
//		@Sort
//		SortSpec theSortSpec,
//
//		ServletRequestDetails theRequestDetails
//	) throws IOException {
//		Session dataSession = PopServlet.getDataSession();
//		javax.servlet.http.HttpServletRequest theServletRequest = theRequestDetails.getServletRequest();
//		logger.info("Parameters {}", (Object) theRequestDetails.getParameters().get("_elements"));
//		try {
//
//			if (theOutputFormat == null) {
////			theRequestDetails.se
//			}
//			BulkExportResponseJson bulkResponseDocument = new BulkExportResponseJson();
//
//			String serverBase = StringUtils.removeEnd(theRequestDetails.getServerBaseForRequest(), "/");
//			Map<String, Bundle> bundleMap = new HashMap<>();
//			Group group = read(theServletRequest, theId, theRequestDetails);
//
//			Bundle errorsBundle = new Bundle();
//			for (Group.GroupMemberComponent member : group.getMember()) {
//				if (member.getEntity().getReference().split("/")[0].equals("Patient")) {
//					Bundle memberBundle = new Bundle();
//					// TODO add normal filter for type filter
//					try {
//						IBundleProvider bundleProvider = patientProvider.patientInstanceEverything(theServletRequest, new IdType(member.getEntity().getReference()), theCount, theOffset, theLastUpdated, theContent, theNarrative, theFilter, theTypes, theSortSpec, theRequestDetails);
//						for (IBaseResource resource : bundleProvider.getAllResources()) {
//							bundleMap.putIfAbsent(resource.fhirType(), new Bundle());
//							bundleMap.get(resource.fhirType()).addEntry().setResource((Resource) resource);
//						}
//					} catch (Exception e) {
//						/**
//						 * Caught Exceptions are exported in a ndJson Binary file
//						 */
//						e.printStackTrace();
//						OperationOutcome operationOutcome = new OperationOutcome();
//						operationOutcome.addIssue()
//							.setDetails(new CodeableConcept(new Coding().setDisplay(e.getMessage())));
//						errorsBundle.addEntry().setResource(operationOutcome); // TODO Add informations
//					}
//				}
//			}
//
//			IParser parser = fhirResourceGroupDao.getContext().newNDJsonParser();
//			RequestDetails detailsCopy = new SystemRequestDetails();
//			detailsCopy.setTenantId(theRequestDetails.getTenantId());
//			for (Map.Entry<String, Bundle> entry : bundleMap.entrySet()) {
//				Binary binary = new Binary();
//				binary.setContentType("Bulk");
//				binary.setContent(parser.encodeResourceToString(entry.getValue()).getBytes(StandardCharsets.UTF_8));
//				DaoMethodOutcome outcome = binaryDao.create(binary, detailsCopy);
//				IIdType newIId;
//				String nextUrl;
//				if (outcome.getResource() != null) {
//					newIId = outcome.getResource().getIdElement();
//					nextUrl = serverBase + "/" + newIId.toUnqualifiedVersionless().getValue();
//				} else if (outcome.getId() != null) {
//					newIId = outcome.getId();
//					nextUrl = serverBase + "/" + newIId.toUnqualifiedVersionless().getValue();
//				} else {
//					nextUrl = "ERROR";
//				}
//				bulkResponseDocument.addOutput()
//					.setType(entry.getKey())
//					.setUrl(nextUrl);
//			}
//
//			if (!errorsBundle.getEntry().isEmpty()) { // If exceptions were caught
//				Binary binary = new Binary();
//				binary.setContentType("Bulk-Error");
//				binary.setContent(parser.encodeResourceToString(errorsBundle).getBytes(StandardCharsets.UTF_8));
//				DaoMethodOutcome outcome = binaryDao.create(binary, detailsCopy);
//
//				IIdType newIId;
//				String nextUrl;
//				if (outcome.getResource() != null) {
//					newIId = outcome.getResource().getIdElement();
//					nextUrl = serverBase + "/" + newIId.toUnqualifiedVersionless().getValue();
//				} else if (outcome.getId() != null) {
//					newIId = outcome.getId();
//					nextUrl = serverBase + "/" + newIId.toUnqualifiedVersionless().getValue();
//				} else {
//					nextUrl = "ERROR";
//				}
//				BulkExportResponseJson.Output errorOutput = new BulkExportResponseJson.Output();
//				errorOutput.setType("OperationOutcome");
//				errorOutput.setUrl(nextUrl);
//				bulkResponseDocument.getError().add(errorOutput);
//			}
//
//
//			bulkResponseDocument.setTransactionTime(new Date(System.currentTimeMillis()));
//			bulkResponseDocument.setRequiresAccessToken(true);
//			bulkResponseDocument.setRequest(theRequestDetails.getCompleteUrl());
//
//			HttpServletResponse response = theRequestDetails.getServletResponse();
//			JsonUtil.serialize(bulkResponseDocument, response.getWriter());
//			response.getWriter().close();
//		} catch (Exception e) {
//			throw e;
//		} finally {
//			dataSession.close();
//		}
//	}

	/**
	 * Group/123/$member-add
	 */
	@Operation(name = "$member-add", idempotent = true)
	public Group groupInstanceMemberAdd(

		@IdParam
		IdType theId,

		@Description(shortDefinition = "The MemberId of the member to be added to the Group.")
		@OperationParam(name = "memberId", typeName = "Identifier")
		Identifier memberId,

		@Description(shortDefinition = "The Provider to whom the member is being attributed to.")
		@OperationParam(name = "providerNpi", typeName = "Identifier")
		Identifier providerNpi,

		@Description(shortDefinition = "The reference of the member to be added to the Group.")
		@OperationParam(name = "patientReference", typeName = "Reference")
		Reference patientReference,

		@Description(shortDefinition = "The reference to the Provider to whom the member is being attributed to.")
		@OperationParam(name = "providerReference", typeName = "Reference")
		Reference providerReference,

		@Description(shortDefinition = "The period over which the patient is being attributed to the provider.")
		@OperationParam(name = "attributionPeriod", typeName = "Period")
		Period attributionPeriod,

		ServletRequestDetails theRequestDetails
	) {
		Group group = read(theRequestDetails.getServletRequest(), theId, theRequestDetails);
		;
		Group.GroupMemberComponent memberComponent;
		if (memberId != null && providerNpi != null) {
			if (!group.getManagingEntity().getIdentifier().getValue().equals(providerNpi.getValue()) || !group.getManagingEntity().getIdentifier().getSystem().equals(providerNpi.getSystem())) {
				throw new InvalidRequestException("Not the right provider for this group" + providerNpi.getValue() + " " + group.getManagingEntity().getIdentifier().getSystem().equals(providerNpi.getSystem()) + " " + group.getManagingEntity().getIdentifier().getValue().equals(providerNpi.getValue()));
			}
			memberComponent = group.getMember().stream()
				.filter(member -> memberId.getValue().equals(member.getEntity().getIdentifier().getValue()) && memberId.getSystem().equals(member.getEntity().getIdentifier().getSystem())) //TODO better conditions
				.findFirst()
				.orElse(group.addMember());
			memberComponent.setEntity(new Reference().setIdentifier(memberId)); //TODO solve reference with interceptor ?
		} else if (patientReference != null && providerReference != null) {
			if (!group.getManagingEntity().equals(providerReference)) {
				throw new InvalidRequestException("Not the right provider for this group");
			}
			memberComponent = group.getMember().stream()
				.filter(member -> patientReference.equals(member.getEntity()))
				.findFirst()
				.orElse(group.addMember());
			memberComponent.setEntity(patientReference); //TODO solve reference with interceptor ?
		} else {
			throw new InvalidRequestException("parameters combination not supported");
		}
		memberComponent.setPeriod(attributionPeriod);
		return (Group) update(theRequestDetails.getServletRequest(), group, theId, "", theRequestDetails).getResource();
	}


	/**
	 * Group/123/$member-remove
	 */
	@Operation(name = "$member-remove", idempotent = true, bundleType = BundleTypeEnum.SEARCHSET)
	public Group groupInstanceMemberRemove(

		@IdParam
		IdType theId,

		@Description(shortDefinition = "The MemberId of the member to be added to the Group.")
		@OperationParam(name = "memberId", typeName = "Identifier")
		Identifier memberId,

		@Description(shortDefinition = "The Provider to whom the member is being attributed to.")
		@OperationParam(name = "providerNpi", typeName = "Identifier")
		Identifier providerNpi,

		@Description(shortDefinition = "The reference of the member to be added to the Group.")
		@OperationParam(name = "patientReference", typeName = "Reference")
		Reference patientReference,

		@Description(shortDefinition = "The reference to the Provider to whom the member is being attributed to.")
		@OperationParam(name = "providerReference", typeName = "Reference")
		Reference providerReference,

		@Description(shortDefinition = "The reference to the coverage based on which the attribution has to be removed.")
		@OperationParam(name = "coverageReference", typeName = "Reference")
		Reference coverageReference,

		ServletRequestDetails theRequestDetails
	) throws IOException {
		Group group = this.fhirResourceGroupDao.read(theId, theRequestDetails);
		Group.GroupMemberComponent memberComponent;
		if (memberId != null && providerNpi != null) {
			if (!group.getManagingEntity().getIdentifier().getValue().equals(providerNpi.getValue()) || !group.getManagingEntity().getIdentifier().getSystem().equals(providerNpi.getSystem())) {
				throw new InvalidRequestException("Not the right provider for this group");
			}
			group.getMember()
				.remove(group.getMember().stream()
					.filter((member) -> {
						return memberId.getValue().equals(member.getEntity().getIdentifier().getValue())
							&& (
							(memberId.getSystem() == null && member.getEntity().getIdentifier().getSystem() == null)
								|| memberId.getSystem().equals(member.getEntity().getIdentifier().getSystem())
						); //TODO better conditions
					})
					.findFirst()
					.orElse(null));

		} else if (patientReference != null && providerReference != null) {
			if (!group.getManagingEntity().equals(providerReference)) {
				throw new InvalidRequestException("Not the right provider for this group");
			}
			group.getMember()
				.remove(group.getMember().stream()
					.filter(member -> patientReference.equals(member.getEntity()))
					.findFirst()
					.orElse(null));

		} else {
			throw new InvalidRequestException("parameters combination not supported");
		}
		return (Group) update(theRequestDetails.getServletRequest(), group, theId, "", theRequestDetails).getResource();
	}
}