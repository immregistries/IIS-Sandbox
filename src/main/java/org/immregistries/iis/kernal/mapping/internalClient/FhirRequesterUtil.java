package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ICriterionInternal;
import org.hl7.fhir.instance.model.api.IBaseResource;

import static org.immregistries.iis.kernal.mapping.internalClient.FhirSaveRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.FhirSaveRequester.GOLDEN_SYSTEM_TAG;

public class FhirRequesterUtil {

	/**
	 * Converts HAPI ICriterion Object to HTTP URI parameter substring
	 *
	 * @param iCriterion HAPIFHIR criterion
	 * @return HTTP parameter String equivalent
	 */
	public static String stringCriterion(FhirContext fhirContext, ICriterion iCriterion) {
		ICriterionInternal iCriterionInternal = (ICriterionInternal) iCriterion;
		return iCriterionInternal.getParameterName() + "=" + iCriterionInternal.getParameterValue(fhirContext);
	}



	/**
	 * Converts list HAPI ICriterion to a complete HTTP URI parameter suffix
	 *
	 * @param criteria HAPIFHIR criteria list
	 * @return Complete HTTP URI suffix
	 */
	public  static  String stringCriterionList(FhirContext fhirContext, ICriterion... criteria) {
		int size = criteria.length;
		StringBuilder params = new StringBuilder();
		if (size > 0) {
			params = new StringBuilder(stringCriterion(fhirContext, criteria[0]));
			int i = 1;
			while (i < size) {
				params.append("&").append(stringCriterion(fhirContext,criteria[i]));
				i++;
			}
		}
		return params.toString();
	}

	/**
	 * Checks Meta and Tags
	 *
	 * @param iBaseResource FHIR resource
	 * @return if resource is golden record
	 */
	public static boolean isGoldenRecord(IBaseResource iBaseResource) {
		if (iBaseResource != null && iBaseResource.getMeta() != null && !iBaseResource.getMeta().isEmpty()) {
			return iBaseResource.getMeta().getTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD) != null;
		}
		return false;
	}

	public static boolean isNotGoldenRecord(IBaseResource iBaseResource) {
		return !isGoldenRecord(iBaseResource);
	}

}
