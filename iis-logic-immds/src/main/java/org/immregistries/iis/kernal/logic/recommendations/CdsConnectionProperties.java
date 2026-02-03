package org.immregistries.iis.kernal.logic.recommendations;

import org.apache.commons.lang3.Strings;
import org.immregistries.vfa.connect.model.Service;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "iis.cds")
@Configuration
@EnableConfigurationProperties
public class CdsConnectionProperties {

	private List<CdsConnection> connections = new ArrayList<>();

	public List<CdsConnection> getConnections() {
		return connections;
	}

	public void setConnections(List<CdsConnection> connections) {
		this.connections = connections;
	}

	public CdsConnection getConnectionByName(String name) {
		return connections.stream()
			.filter(cdsConnection -> Strings.CI.equals(name, cdsConnection.getName()))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Cds Configuration not found for name " + name + " in application.yml"));
	}


	public CdsConnection getConnectionByService(org.immregistries.vfa.connect.model.Service service) {
		return connections.stream()
			.filter(cdsConnection -> cdsConnection.getService().equals(service))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Cds Configuration not found for service " + service + " in application.yml"));
	}


	public static class CdsConnection {
		private String url = "";
		private String name = "";
		private org.immregistries.vfa.connect.model.Service service = Service.LSVF;

		public String getUrl() {
			return url;
		}

		public String getName() {
			return name;
		}

		public Service getService() {
			return service;
		}
	}


}
