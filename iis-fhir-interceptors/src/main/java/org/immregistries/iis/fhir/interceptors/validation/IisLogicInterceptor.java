package org.immregistries.iis.fhir.interceptors.validation;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.DiffResult;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.mapping.mappers.resources.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.model.ITenantTiedObject;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.ack.IisReportableSeverity;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class IisLogicInterceptor implements IisValidationInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String IIS_REPORTABLE_LIST = "iisReportableList";

	public List<IisReportable> iisReportableList(RequestDetails requestDetails) {
		if (requestDetails.getAttribute(IIS_REPORTABLE_LIST) == null) {
			requestDetails.setAttribute(IIS_REPORTABLE_LIST, new ArrayList<>(20));
		}
		return (List<IisReportable>) requestDetails.getAttribute(IIS_REPORTABLE_LIST);
	}

	public @NotNull IisReportable err5IisReportable(String number, String text, List<@NotNull Hl7Location> hl7LocationList) {
		IisReportable iisReportable = new IisReportable();
		iisReportable.setHl7LocationList(hl7LocationList);
		CodedWithExceptions applicationErrorCode = new CodedWithExceptions("ERR-5");
		applicationErrorCode.setIdentifier(number);
		applicationErrorCode.setText(text);
		iisReportable.setApplicationErrorCode(applicationErrorCode);
		iisReportable.setHl7ErrorCode(applicationErrorCode);
		iisReportable.setSeverity(IisReportableSeverity.WARN);
		return iisReportable;
	}

	public boolean testMapping(IisResourceMasterReportedMapper<IisMappedToFhirResource, IisMappedToFhirResource, IisMappedToFhirResource, IAnyResource> mapper, IisMappedToFhirResource iisMappedToResourceObject) {
		IAnyResource resource = mapper.fhirObject(iisMappedToResourceObject);
		IisMappedToFhirResource iisMappedToResourceObject1 = mapper.localObjectReported(resource);
		if (iisMappedToResourceObject1 == null) {
			iisMappedToResourceObject1 = mapper.localObject(resource);
		}
		boolean res = iisMappedToResourceObject.toString().equals(iisMappedToResourceObject1.toString());
		if (!res) {
			logger.info("Object Mapping check failed\n{}\n\n{}\n", iisMappedToResourceObject, iisMappedToResourceObject1);
		}
		if (iisMappedToResourceObject1 instanceof ITenantTiedObject) {
			((ITenantTiedObject) iisMappedToResourceObject1).setTenant(((ITenantTiedObject) iisMappedToResourceObject).getTenant());
		}
		DiffResult<IisMappedToFhirResource> diffResult = iisMappedToResourceObject.diff(iisMappedToResourceObject1);
		if (diffResult.getNumberOfDiffs() > 0) {
			logger.info("Object Mapping check FAILED");
			printDiff(logger, diffResult);
		}
		return res;
	}

	public boolean testMappingFhir(IisResourceMasterReportedMapper<IisMappedToFhirResource, IisMappedToFhirResource, IisMappedToFhirResource, IAnyResource> mapper, IAnyResource resource, IParser parser) {
		IisMappedToFhirResource iisMappedToResourceObject1 = mapper.localObjectReported(resource);
		IAnyResource resource1 = mapper.fhirObject(iisMappedToResourceObject1);
		String s1 = parser.encodeResourceToString(resource);
		String s2 = parser.encodeResourceToString(resource1);
		boolean res = s1.equals(s2);
//		if (!res) {
//			logger.info("FHIR Mapping check failed {}\n\n{}", s1, s2);
//		}
		return res;
	}

	public static void printDiff(Logger logger, DiffResult<IisMappedToFhirResource> diffResult) {
//		logger.info("Object Mapping check DIFF: \n{}", JsonFormatter.prettyPrint(diffResult.toString(ToStringStyle.JSON_STYLE)));
//		logger.info("Object Mapping check DIFF: {}", diffResult.toString(ToStringStyle.SHORT_PREFIX_STYLE));
		diffResult.getDiffs().stream().forEach((dif) -> {
			/*
			 * Temp fix for issue of false negative, probably due to pointer issue
			 */
			if (!StringUtils.equals(String.valueOf(dif.getRight()), String.valueOf(dif.getLeft()))) {
				logger.info("Object Mapping check DIFF: {}\n{}\n{}\n", dif.getFieldName(), dif.getRight(), dif.getLeft());
			}
		});
	}

}
