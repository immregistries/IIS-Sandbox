package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.parser.IParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.DiffResult;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.mapping.mappers.resources.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.model.ITenantTiedObject;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MappingConsistencyTestService { // TODO Generate controller
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

	public void printDiff(Logger logger, DiffResult<IisMappedToFhirResource> diffResult) {
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
