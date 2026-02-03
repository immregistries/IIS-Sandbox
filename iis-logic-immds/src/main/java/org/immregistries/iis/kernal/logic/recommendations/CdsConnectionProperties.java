package org.immregistries.iis.kernal.logic.recommendations;

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

	private String default_connection = "";
	private String default_ice_connection = "";
	private List<CdsConnection> connections = new ArrayList<>();

	public List<CdsConnection> getConnections() {
		return connections;
	}

	public void setConnections(List<CdsConnection> connections) {
		this.connections = connections;
	}

	public String getDefault_connection() {
		return default_connection;
	}

	public void setDefault_connection(String default_connection) {
		this.default_connection = default_connection;
	}

	public String getDefault_ice_connection() {
		return default_ice_connection;
	}

	public void setDefault_ice_connection(String default_ice_connection) {
		this.default_ice_connection = default_ice_connection;
	}

	public static class CdsConnection {
		private String url = "";
		private String name = "";
		private Service service = Service.LSVF;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Service getService() {
			return service;
		}

		public void setService(Service service) {
			this.service = service;
		}

		@Override
		public String toString() {
			return "CdsConnection{" +
				"url='" + url + '\'' +
				", name='" + name + '\'' +
				", service=" + service +
				'}';
		}
	}


}
