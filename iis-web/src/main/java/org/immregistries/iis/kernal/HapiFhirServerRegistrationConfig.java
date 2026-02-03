package org.immregistries.iis.kernal;

import ca.uhn.fhir.jpa.starter.annotations.OnEitherVersion;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HapiFhirServerRegistrationConfig {
	public static final String FHIR_SERVER_PATH_EXTENSION = "/fhir";

	@Bean
	@Conditional(OnEitherVersion.class)
	public ServletRegistrationBean hapiServletRegistration(RestfulServer restfulServer) {
		ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
		servletRegistrationBean.setServlet(restfulServer);
		servletRegistrationBean.addUrlMappings(FHIR_SERVER_PATH_EXTENSION + "/*");
		servletRegistrationBean.setLoadOnStartup(1);
		return servletRegistrationBean;
	}

}
