package org.immregistries.iis.kernal.controllers;


public final class RestConstants {



	public static final class PathVariable {
		/**
		 * To be used in @PathVariable annotations
		 */
		public static final class Key {
			public static final String TENANT_ID = "tenantId";
			public static final String PATIENT_ID = "patientId";
			public static final String CONTENT_ID = "contentId";
			public static final String MANIFEST_ID = "manifestId";
			public static final String VACCINATION_ID = "vaccinationId";
			public static final String KEY_ID = "keyId";
		}

		/**
		 * To be used in @RequestMapping annotations
		 */
		public static final class PlaceHolder {
			public static final String TENANT_ID_PLACEHOLDER = "/{" + Key.TENANT_ID + "}";
			public static final String PATIENT_ID_PLACEHOLDER = "/{" + Key.PATIENT_ID + "}";
			public static final String CONTENT_ID_PLACEHOLDER = "/{" + Key.CONTENT_ID + "}";
			public static final String MANIFEST_ID_PLACEHOLDER = "/{" + Key.MANIFEST_ID + "}";
			public static final String VACCINATION_ID_PLACEHOLDER = "/{" + Key.VACCINATION_ID + "}";
			public static final String KEY_ID_PLACEHOLDER = "/{" + Key.KEY_ID + "}";
		}
	}

	public static final class Path {

		/**
		 * pure keys
		 */
		public static final class Key {
			public static final String REST_KEY = "rest";
			public static final String TENANT_KEY = "tenant";
			public static final String PATIENT_KEY = "patient";
			public static final String CODE_MAPS_KEY = "codeMaps";
			public static final String AUTHENTICATION_KEY = "authentication";
			public static final String FHIR_MESSAGING_KEY = "fhirMessaging";
			public static final String GROUP_KEY = "group";
			public static final String RECOMMENDATION_KEY = "recommendation";
			public static final String VAC_DEDUP_KEY = "vacDedup";
			public static final String PATIENT_SH_LINK_KEY = "patientShLink";
			public static final String V2_TO_FHIR_KEY = "v2ToFhir";
			public static final String TENANT_COMPARE_KEY = "tenantCompare";
			public static final String CREATE_KEY = "$create";
			public static final String MAPPING_KEY = "mapping";
			public static final String MESSAGE_KEY = "message";
			public static final String MANIFEST_KEY = "manifest";
			public static final String VACCINATION_KEY = "vaccination";
			public static final String POP_KEY = "pop";
			public static final String IIS_KEYS = "iisKeys";
		}

		/**
		 * Single modular path
		 */
		public static final String REST_PATH = "/" + Key.REST_KEY;
		public static final String TENANT_PATH = "/" + Key.TENANT_KEY;
		public static final String PATIENT_PATH = "/" + Key.PATIENT_KEY;
		public static final String CODE_MAPS_PATH = "/" + Key.CODE_MAPS_KEY;
		public static final String AUTHENTICATION_PATH = "/" + Key.AUTHENTICATION_KEY;
		public static final String FHIR_MESSAGING_PATH = "/" + Key.FHIR_MESSAGING_KEY;
		public static final String GROUP_PATH = "/" + Key.GROUP_KEY;
		public static final String RECOMMENDATION_PATH = "/" + Key.RECOMMENDATION_KEY;
		public static final String VAC_DEDUP_PATH = "/" + Key.VAC_DEDUP_KEY;
		public static final String V2_TO_FHIR_PATH = "/" + Key.V2_TO_FHIR_KEY;
		public static final String TENANT_COMPARE_BASE_PATH = "/" + Key.TENANT_COMPARE_KEY;
		public static final String $_CREATE_PATH = "/" + Key.CREATE_KEY;
		public static final String MAPPING_KEY_PATH = "/" + Key.MAPPING_KEY;
		public static final String MESSAGE_PATH = "/" + Key.MESSAGE_KEY;
		public static final String MANIFEST_PATH_SUFFIX = "/" + Key.MANIFEST_KEY;
		public static final String VACCINATION_PATH = "/" + Key.VACCINATION_KEY;
		public static final String PATIENT_SH_LINK_PATH = "/" + Key.PATIENT_SH_LINK_KEY;
		public static final String POP_PATH = "/" + Key.POP_KEY;
		public static final String IIS_KEYS_PATH = "/" + Key.IIS_KEYS;

		public static final String FITS_EXAMPLE_PATH = "/fits/example";

		public static final String SHLINK_FILES = "shlink/files";

		public static final String SHLINK_CONTENT_PATH = REST_PATH + "/" + SHLINK_FILES;

		public static final String REST_TENANT_PATH = REST_PATH + TENANT_PATH + PathVariable.PlaceHolder.TENANT_ID_PLACEHOLDER;
		public static final String REST_PATIENT_PATH = REST_TENANT_PATH + PATIENT_PATH + PathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER;
		public static final String MANIFEST_FULL_PATH = REST_TENANT_PATH + MANIFEST_PATH_SUFFIX;

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
		public static final String PARAM_EXP = "exp";

	}

}
