package org.immregistries.iis.kernal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;


@EnableConfigurationProperties
@ConfigurationProperties
@Configuration
public class IisConfigService {
	private static final String TOOL_DEPLOYED_URL = "iis.deployed.url";
	private static final String ADMIN_USER = "iis.deployed.admin.username";
	private static final String ADMIN_PASSWORD = "iis.deployed.admin.password";
	private static final String TOOL_DEPLOYED_HOST = "iis.deployed.host";
	private static final String DEFAULT_DEPLOYMENT_URL = "localhost:8080";
	private static final String PROPERTIES_CONTEXT_PATH = "server.servlet.contextPath";


	private String HOST;

	public String getCONTEXT_PATH() {
		return CONTEXT_PATH;
	}

	private String CONTEXT_PATH;
	private String USER;
	private String PASSWORD;

	public void configure(Properties properties) {
		this.HOST = properties.getProperty(TOOL_DEPLOYED_HOST);
		this.CONTEXT_PATH = properties.getProperty(PROPERTIES_CONTEXT_PATH);
	}

	public String getHOST() {
		return HOST;
	}
}
