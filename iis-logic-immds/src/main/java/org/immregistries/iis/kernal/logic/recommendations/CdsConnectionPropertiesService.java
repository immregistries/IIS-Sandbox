package org.immregistries.iis.kernal.logic.recommendations;

import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CdsConnectionPropertiesService {

	@Autowired()
	private CdsConnectionProperties cdsConnectionProperties;

	public CdsConnectionProperties.CdsConnection getConnectionByName(String name) {
		return cdsConnectionProperties.getConnections().stream()
			.filter(cdsConnection -> Strings.CI.equals(name, cdsConnection.getName()))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Cds Configuration not found for name " + name + " in application.yml"));
	}

	public CdsConnectionProperties.CdsConnection getConnectionByService(org.immregistries.vfa.connect.model.Service service) {
		return cdsConnectionProperties.getConnections().stream()
			.filter(cdsConnection -> cdsConnection.getService().equals(service))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Cds Configuration not found for service " + service + " in application.yml"));
	}


	public CdsConnectionProperties.CdsConnection getDefaultConnection() {
		return getConnectionByName(cdsConnectionProperties.getDefault_connection());
	}

	public CdsConnectionProperties.CdsConnection getDefaultIceConnection() {
		return getConnectionByName(cdsConnectionProperties.getDefault_ice_connection());
	}
}
