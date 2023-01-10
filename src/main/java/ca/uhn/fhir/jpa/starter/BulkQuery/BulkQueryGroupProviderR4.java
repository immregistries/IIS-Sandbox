package ca.uhn.fhir.jpa.starter.BulkQuery;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.bulk.export.model.BulkExportResponseJson;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProviderPatient;
import ca.uhn.fhir.jpa.rp.r4.GroupResourceProvider;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.model.MessageReceived;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@Conditional(OnR4Condition.class)
public class BulkQueryGroupProviderR4 extends GroupResourceProvider {
	Logger logger = LoggerFactory.getLogger(BulkQueryGroupProviderR4.class);

	@Autowired
	BaseJpaResourceProviderPatient<Patient> patientProvider;

	@Autowired
	IFhirSystemDao fhirSystemDao;
	@Autowired
	IFhirResourceDao<Group> fhirResourceGroupDao;

	@Autowired
	private IFhirResourceDao<Binary> binaryDao;

	public BulkQueryGroupProviderR4() {
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
		Session dataSession = PopServlet.getDataSession();
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
			throw e;
		} finally {
			dataSession.close();
		}
	}

	public void groupInstanceSynchExport(

		javax.servlet.http.HttpServletRequest theServletRequest,

		@IdParam
		IdType theId,

		@Description(formalDefinition = "The format for the requested Bulk Data files to be generated as per FHIR Asynchronous Request Pattern. Defaults to application/fhir+ndjson. The server SHALL support Newline Delimited JSON, but MAY choose to support additional output formats. The server SHALL accept the full content type of application/fhir+ndjson as well as the abbreviated representations application/ndjson and ndjson.")
		@OperationParam(name = "_outputFormat")
		IPrimitiveType<String> theOutputFormat,

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
		Session dataSession = PopServlet.getDataSession();
		try {

			if (theOutputFormat == null) {
//			theRequestDetails.se
			}
			BulkExportResponseJson bulkResponseDocument = new BulkExportResponseJson();

			String serverBase = StringUtils.removeEnd(theRequestDetails.getServerBaseForRequest(), "/");
			Map<String, Bundle> bundleMap = new HashMap<>();
			Group group = read(theServletRequest, theId, theRequestDetails);
			for (Group.GroupMemberComponent member : group.getMember()) {
				if (member.getEntity().getReference().split("/")[0].equals("Patient")) {
					Bundle memberBundle = new Bundle();
					// TODO add normal filter for type filter
					IBundleProvider bundleProvider = patientProvider.patientInstanceEverything(theServletRequest, new IdType(member.getEntity().getReference()), theCount, theOffset, theLastUpdated, theContent, theNarrative, theFilter, theTypes, theSortSpec, theRequestDetails);
					for (IBaseResource resource : bundleProvider.getAllResources()) {
						bundleMap.putIfAbsent(resource.fhirType(), new Bundle());
						bundleMap.get(resource.fhirType()).addEntry().setResource((Resource) resource);
					}
				}
			}

			IParser parser = fhirResourceGroupDao.getContext().newNDJsonParser();
			RequestDetails detailsCopy = new SystemRequestDetails();
			detailsCopy.setTenantId(theRequestDetails.getTenantId());
			for (Map.Entry<String, Bundle> entry :
				bundleMap.entrySet()) {
				Binary binary = new Binary();
				binary.setContentType("Bulk");
				binary.setContent(parser.encodeResourceToString(entry.getValue()).getBytes(StandardCharsets.UTF_8));
				DaoMethodOutcome outcome = binaryDao.create(binary, detailsCopy);
				IIdType newIId;
				String nextUrl;
				if (outcome.getResource() != null) {
					newIId = outcome.getResource().getIdElement();
					nextUrl = serverBase + "/" + newIId.toUnqualifiedVersionless().getValue();
				} else if (outcome.getId() != null) {
					newIId = outcome.getId();
					nextUrl = serverBase + "/" + newIId.toUnqualifiedVersionless().getValue();
				} else {
//					throw RuntimeException();
					nextUrl = "ERROR";

				}
				bulkResponseDocument.addOutput()
					.setType(entry.getKey())
					.setUrl(nextUrl);
//
//				MessageReceived ndJson = new MessageReceived();
//				OrgAccess orgAccess = (OrgAccess) theRequestDetails.getAttribute("orgAccess");
//				ndJson.setOrgMaster(orgAccess.getOrg());
//				ndJson.setMessageRequest(theRequestDetails.getCompleteUrl());
//				ndJson.setMessageResponse(parser.encodeResourceToString(entry.getValue()));
//				ndJson.setReportedDate(new Date());
//				int id = (int) dataSession.save(ndJson);
//				logger.info("{}/ndjson?tenantId={}&ndJsonId={}", theRequestDetails.getCompleteUrl().split("/fhir")[0], theRequestDetails.getTenantId(), id);
//				bulkResponseDocument.addOutput()
//					.setType(entry.getKey())
//					.setUrl(theRequestDetails.getCompleteUrl().split("/fhir")[0]
//						+ "/ndjson?tenantId=" + theRequestDetails.getTenantId() + "&ndJsonId=" + id);
			}

			bulkResponseDocument.setTransactionTime(new Date(System.currentTimeMillis()));
			bulkResponseDocument.setRequiresAccessToken(true);
			bulkResponseDocument.setRequest(theRequestDetails.getServletRequest().getRequestURI());

			HttpServletResponse response = theRequestDetails.getServletResponse();
			JsonUtil.serialize(bulkResponseDocument, response.getWriter());
			response.getWriter().close();
		} catch (Exception e) {
			throw e;
		} finally {
			dataSession.close();
		}
	}
}