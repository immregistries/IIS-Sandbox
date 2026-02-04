package org.immregistries.iis.kernal.controllers;

public class IisPathVariable {
	/**
	 * To be used in @PathVariable annotations
	 */
	public static final class Key {
		public static final String TENANT_ID = "tenantId";
		public static final String TENANT_NAME = "tenantName";
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
		public static final String TENANT_NAME_PLACEHOLDER = "/{" + Key.TENANT_NAME + "}";
		public static final String PATIENT_ID_PLACEHOLDER = "/{" + Key.PATIENT_ID + "}";
		public static final String CONTENT_ID_PLACEHOLDER = "/{" + Key.CONTENT_ID + "}";
		public static final String MANIFEST_ID_PLACEHOLDER = "/{" + Key.MANIFEST_ID + "}";
		public static final String VACCINATION_ID_PLACEHOLDER = "/{" + Key.VACCINATION_ID + "}";
		public static final String KEY_ID_PLACEHOLDER = "/{" + Key.KEY_ID + "}";
	}
}
