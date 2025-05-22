package org.immregistries.iis.kernal.logic;

import org.immregistries.iis.kernal.model.Tenant;

public interface IHl7MessageHandler {

	String process(String message, Tenant tenant, String facilityName);

}
