package org.immregistries.iis.kernal.controllers;

public final class IisRestPath {

	/*
	 * Patterns used many times throughout the webapp
	 */
//	Tenant based paths
	public static final String REST_TENANT_PATH = BasePath.REST_PATH + BasePath.TENANT_PATH
		+ IisPathVariable.PlaceHolder.TENANT_NAME_PLACEHOLDER;
	//	Patient based path
	public static final String REST_PATIENT_PATH = REST_TENANT_PATH + BasePath.PATIENT_PATH
		+ IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER;

	public static final String PATIENT_MANIFEST_FULL_PATH = REST_TENANT_PATH + BasePath.PATIENT_MANIFEST_PATH;

	public static final String FITS_EXAMPLE_PATH = "/fits/example";

	public static final String SH_LINK_CONTENT_PATH = BasePath.REST_PATH + "/" + Key.SH_LINK_FILES_KEY;
	public final static String SH_LINKS_STORED_MANIFEST_FULL_PATH = BasePath.REST_PATH + BasePath.STORED_MANIFEST_PATH;



	/**
	 * pure keys
	 */
	public static final class Key {
		public static final String REST_KEY = "rest";
		public static final String TENANT_KEY = "tenant";
		public static final String PATIENT_KEY = "patient";
		public static final String CODE_MAPS_KEY = "code-maps";
		public static final String AUTHENTICATION_KEY = "authentication";
		public static final String FHIR_MESSAGING_KEY = "fhir-messaging";
		public static final String GROUP_KEY = "group";
		public static final String RECOMMENDATION_KEY = "recommendation";
		public static final String VAC_DEDUP_KEY = "vacDedup";
		public static final String PATIENT_SH_LINK_KEY = "patient-sh-link";
		public static final String V2_TO_FHIR_KEY = "v2-to-fhir";
		public static final String TENANT_COMPARE_KEY = "tenantCompare";
		public static final String CREATE_KEY = "$create";
		public static final String MAPPING_KEY = "mapping";
		public static final String MESSAGE_KEY = "message";
		public static final String PATIENT_MANIFEST_KEY = "patient-manifest";
		public static final String VACCINATION_KEY = "vaccination";
		public static final String POP_KEY = "pop";
		public static final String IIS_KEYS_KEY = "iis-keys";
		public static final String OBSERVATIONS_KEY = "observations";
		public static final String RELATED_KEY = "related";
		public static final String SH_LINK_KEY = "sh-link";
		public static final String SH_LINK_PAYLOAD_KEY = "sh-link-payload";
		public static final String FHIR_RESOURCE_KEY = "fhir-resource";
		public static final String CLVR_KEY = "clvr";
		public static final String SH_LINK_FILES_KEY = "sh-link-files";
		public static final String STORED_MANIFEST_KEY = "stored-manifest";
		public static final String SUBSCRIPTION_TOPIC_KEY = "SubscriptionTopic";
		public static final String FLAVORS_KEY = "flavors";
	}

	public enum RestKey {
		REST(Key.REST_KEY),
		TENANT(Key.TENANT_KEY),
		PATIENT(Key.PATIENT_KEY),
		CODE_MAPS(Key.CODE_MAPS_KEY),
		AUTHENTICATION(Key.AUTHENTICATION_KEY),
		FHIR_MESSAGING(Key.FHIR_MESSAGING_KEY),
		GROUP(Key.GROUP_KEY),
		RECOMMENDATION(Key.RECOMMENDATION_KEY),
		VAC_DEDUP(Key.VAC_DEDUP_KEY),
		PATIENT_SH_LINK(Key.PATIENT_SH_LINK_KEY),
		V2_TO_FHIR(Key.V2_TO_FHIR_KEY),
		TENANT_COMPARE(Key.TENANT_COMPARE_KEY),
		CREATE(Key.CREATE_KEY),
		MAPPING(Key.MAPPING_KEY),
		MESSAGE(Key.MESSAGE_KEY),
		PATIENT_MANIFEST(Key.PATIENT_MANIFEST_KEY),
		VACCINATION(Key.VACCINATION_KEY),
		POP(Key.POP_KEY),
		IIS_KEYS(Key.IIS_KEYS_KEY),
		OBSERVATIONS(Key.OBSERVATIONS_KEY),
		RELATED(Key.RELATED_KEY),
		SH_LINK_PAYLOAD(Key.SH_LINK_PAYLOAD_KEY),
		FHIR_RESOURCE(Key.FHIR_RESOURCE_KEY),
		CLVR(Key.CLVR_KEY),
		SH_LINK(Key.SH_LINK_KEY),
		STORED_MANIFEST(Key.STORED_MANIFEST_KEY),
		SUBSCRIPTION_TOPIC(Key.SUBSCRIPTION_TOPIC_KEY),
		SH_LINK_FILES(Key.SH_LINK_FILES_KEY),
		FLAVORS(Key.FLAVORS_KEY);

		private final String key;

		RestKey(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}
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
		public static final String PATIENT_MANIFEST_PATH = "/" + Key.PATIENT_MANIFEST_KEY;
		public static final String VACCINATION_PATH = "/" + Key.VACCINATION_KEY;
		public static final String PATIENT_SH_LINK_PATH = "/" + Key.PATIENT_SH_LINK_KEY;
		public static final String POP_PATH = "/" + Key.POP_KEY;
		public static final String IIS_KEYS_PATH = "/" + Key.IIS_KEYS_KEY;
		public static final String OBSERVATIONS_PATH = "/" + Key.OBSERVATIONS_KEY;
		public static final String RELATED_PATH = "/" + Key.RELATED_KEY;
		public static final String SH_LINK_PAYLOAD_PATH = "/" + Key.SH_LINK_PAYLOAD_KEY;
		public static final String FHIR_RESOURCE_PATH = "/" + Key.FHIR_RESOURCE_KEY;
		public static final String CLVR_PATH = "/" + Key.CLVR_KEY;
		public static final String SH_LINK_PATH = "/" + Key.SH_LINK_KEY;
		public static final String STORED_MANIFEST_PATH = "/" + Key.STORED_MANIFEST_KEY;
		public static final String SUBSCRIPTION_TOPIC_PATH = "/" + Key.SUBSCRIPTION_TOPIC_KEY;
		public static final String FLAVORS_PATH = "/" + Key.FLAVORS_KEY;

	}
}
