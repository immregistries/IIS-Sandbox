package org.immregistries.iis.kernal.controllers;


public final class IisRestPath {

	public static final String FITS_EXAMPLE_PATH = "/fits/example";

	public static final String SHLINK_FILES = "shlink/files";

	public static final String SHLINK_CONTENT_PATH = BasePath.REST_PATH + "/" + SHLINK_FILES;

	public static final String REST_TENANT_PATH = BasePath.REST_PATH + BasePath.TENANT_PATH + IisPathVariable.PlaceHolder.TENANT_ID_PLACEHOLDER;
	public static final String REST_PATIENT_PATH = REST_TENANT_PATH + BasePath.PATIENT_PATH + IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER;
	public static final String MANIFEST_FULL_PATH = REST_TENANT_PATH + BasePath.MANIFEST_PATH;

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
		public static final String IIS_KEYS_KEY = "iisKeys";
		public static final String OBSERVATIONS_KEY = "observations";
		public static final String RELATED_KEY = "related";
		public static final String SH_LINK_PAYLOAD_KEY = "shLinkPayload";
		public static final String FHIR_RESOURCE_KEY = "fhirResource";
		public static final String CLVR_KEY = "clvr";
	}

	/**
	 * Single modular path
	 */
	public static final class BasePath {
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
		public static final String MANIFEST_PATH = "/" + Key.MANIFEST_KEY;
		public static final String VACCINATION_PATH = "/" + Key.VACCINATION_KEY;
		public static final String PATIENT_SH_LINK_PATH = "/" + Key.PATIENT_SH_LINK_KEY;
		public static final String POP_PATH = "/" + Key.POP_KEY;
		public static final String IIS_KEYS_PATH = "/" + Key.IIS_KEYS_KEY;
		public static final String OBSERVATIONS_PATH = "/" + Key.OBSERVATIONS_KEY;
		public static final String RELATED_PATH =  "/" + Key.RELATED_KEY;
		public static final String SH_LINK_PAYLOAD_PATH =  "/" + Key.SH_LINK_PAYLOAD_KEY;
		public static final String FHIR_RESOURCE_PATH = "/" + Key.FHIR_RESOURCE_KEY;
		public static final String CLVR_PATH = "/" + Key.CLVR_KEY;
	}
}
