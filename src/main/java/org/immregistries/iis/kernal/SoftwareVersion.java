package org.immregistries.iis.kernal;

import org.immregistries.iis.kernal.fhir.Application;

public class SoftwareVersion {
	public static final String VERSION = Application.class.getPackage().getImplementationVersion();
}
