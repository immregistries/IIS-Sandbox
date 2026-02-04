package org.immregistries.iis.kernal;

import ca.uhn.fhir.rest.server.RestfulServer;
import org.immregistries.iis.kernal.controllers.filters.FilterRegistrationConfig;
import org.immregistries.iis.kernal.logic.config.CLVRConfig;
import org.immregistries.iis.kernal.logic.config.V2toFhirConfig;
import org.immregistries.iis.kernal.security.ServerSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.request.RequestContextListener;


@SpringBootApplication(exclude = { ElasticsearchRestClientAutoConfiguration.class, ThymeleafAutoConfiguration.class })
@Import({
	HapiFhirServerRegistrationConfig.class,
	FilterRegistrationConfig.class,
	ServerSecurityConfig.class,
	CLVRConfig.class,
	V2toFhirConfig.class,
})
@ServletComponentScan(basePackageClasses = {
	RestfulServer.class }, basePackages = {
	"org.immregistries.iis.kernal.controllers.rest",
	"org.immregistries.iis.kernal.controllers.servlet"
	// ,"org.immregistries.iis.kernal.repository"
})
@ComponentScan(basePackages = {
	"ca.uhn.fhir.jpa.starter",
	"org.immregistries.iis",
	"org.immregistries.iis.kernal"
})
public class Application extends SpringBootServletInitializer {

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
		// Server is now accessible at eg. http://localhost:8080/fhir/metadata
		// UI is now accessible at http://localhost:8080/
	}

	@Override
	protected SpringApplicationBuilder configure(
			SpringApplicationBuilder builder) {
		return builder.sources(Application.class);
	}


	/**
	 * Required to get access to httpRequest and session statically through spring,
	 * important to use the fhir client inside the servlets
	 *
	 * @return
	 */
	@Bean
	public RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}

}
