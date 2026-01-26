package org.immregistries.iis.kernal.controllers.rest.util;

import static org.immregistries.iis.kernal.controllers.rest.util.RestConstants.Path.Variables.*;

public final class RestConstants {

	public static final class Path {

		public static final String TENANT_ID_PLACEHOLDER = "/{" + TENANT_ID + "}";
		public static final String PATIENT_ID_PLACEHOLDER = "/{" + PATIENT_ID + "}";
		public static final String CONTENT_ID_PLACEHOLDER = "/{" + CONTENT_ID + "}";
		public static final String MANIFEST_ID_PLACEHOLDER = "/{" + MANIFEST_ID + "}";

		public static final class Variables {
			public static final String TENANT_ID = "tenantId";
			public static final String PATIENT_ID = "patientId";
			public static final String CONTENT_ID = "contentId";
			public static final String MANIFEST_ID = "manifestId";

		}


		public static final String REST = "rest";
		public static final String REST_PATH = "/" + REST;

		public static final String TENANT = "tenant";
		public static final String TENANT_PATH = "/" + TENANT;
		public static final String REST_TENANT_PATH = REST_PATH + TENANT_PATH + TENANT_ID_PLACEHOLDER;

		public static final String PATIENT = "patient";
		public static final String PATIENT_PATH = "/" + PATIENT;
		public static final String PATIENT_BASE_PATH = PATIENT_PATH + PATIENT_ID_PLACEHOLDER;
		public static final String REST_PATIENT_PATH = REST_PATH + TENANT_PATH + TENANT_ID_PLACEHOLDER
				+ PATIENT_BASE_PATH;

		public static final String CODEMAPS_PATH_KEY = "codemaps";
		public static final String CODEMAPS_PATH_SUFFIX = "/" + CODEMAPS_PATH_KEY;

		public static final String AUTHENTICATION_KEY_PATH = "/authentication";
		public static final String FITS_EXAMPLE_PATH = "/fits/example";
		public static final String FHIR_MESSAGING_KEY_PATH = "/fhirMessaging";
		public static final String GROUP_PATH_KEY = "/group";
		public static final String RECOMMENDATION_PATH = "/recommendation";
		public static final String VAC_DEDUP_PATH = "/vacDedup";
		public static final String SHLINK_FILES = "shlink/files";
		public static final String SHLINK_CONTENT_PATH = REST_PATH + "/" + SHLINK_FILES;
		public static final String SHLINK_QR_CODE_PATH_SUFFIX = "/qr";
		public static final String V2_TO_FHIR_PATH = "/v2ToFhir";
		public static final String TENANT_COMPARE_BASE_PATH = "/tenantCompare";
		public static final String $_CREATE_PATH_KEY = "/$create";
		public static final String MAPPING_KEY_PATH = "/mapping";
		public static final String MESSAGE_PATH_KEY = "/message";
		public static final String MANIFEST_PATH_SUFFIX = "/manifest";
		public static final String MANIFEST_FULL_PATH = REST_TENANT_PATH + MANIFEST_PATH_SUFFIX;
		public static final String VACCINATION = "/vaccination";


	}

	public static final class Param {
		public static final String MDM_EXPAND_REST_PARAM = "isGolden";
		public static final String RECOMMENDATION_ID = "recommendationId";
		public static final String RECOMMENDATION_IDENTIFIER = "recommendationIdentifier";
		public static final String SECRET_KEY = "secretKey";
		public static final String KEY_ID = "keyId";
		public static final String PATIENT_ID = "patientId";
		public static final String FLAG = "flag";
		public static final String FACILITY_NAME = "facilityName";
		public static final String INCLUDE_GOLDEN = "includeGolden";
		public static final String TENANT_IDS = "tenantIds";
		public static final String RECIPIENT = "recipient";
		public static final String PASSCODE = "passcode";
		public static final String EMBEDDED_LENGTH_MAX = "embeddedLengthMax";
	}

}
