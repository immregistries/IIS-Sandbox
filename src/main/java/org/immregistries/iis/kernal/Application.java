package org.immregistries.iis.kernal;

import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.immregistries.iis.kernal.controllers.filters.FilterRegistrationConfig;
import org.immregistries.iis.kernal.fhir.ServerConfig;
import org.immregistries.iis.kernal.fhir.common.annotations.OnEitherVersion;
import org.immregistries.iis.kernal.fhir.mdm.MdmConfig;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.iis.kernal.security.ServerSecurityConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.request.RequestContextListener;


@SpringBootApplication(exclude = { ElasticsearchRestClientAutoConfiguration.class, ThymeleafAutoConfiguration.class })
@Import({
		SubscriptionSubmitterConfig.class,
		SubscriptionProcessorConfig.class,
		SubscriptionChannelConfig.class,
		WebsocketDispatcherConfig.class,
		MdmConfig.class,
		JpaBatch2Config.class,
		Batch2JobsConfig.class,
		FilterRegistrationConfig.class,
		ServerConfig.class,
		ServerSecurityConfig.class,
})
@ServletComponentScan(basePackageClasses = {
	RestfulServer.class }, basePackages = {
	"org.immregistries.iis.kernal.servlet"
	// ,"org.immregistries.iis.kernal.repository"
})
@ComponentScan(basePackages = {
		"ca.uhn.fhir.jpa.starter",
		"org.immregistries.iis.kernal"
})
public class Application extends SpringBootServletInitializer {

	/**
	 * TODO get from Configuration
	 */
	public static final String IIS_PATH_BASE = "/iis";
	public static final String FHIR_PATH_EXTENSION = "/fhir";
	public static @NotNull String fhirServerBasePath(Tenant tenant) {
		return Application.IIS_PATH_BASE + FHIR_PATH_EXTENSION + "/" + tenant.getOrganizationName();
	}


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

	@Bean
	@Conditional(OnEitherVersion.class)
	public ServletRegistrationBean hapiServletRegistration(RestfulServer restfulServer) {
		ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
		beanFactory.autowireBean(restfulServer);
		servletRegistrationBean.setServlet(restfulServer);
		servletRegistrationBean.addUrlMappings(FHIR_PATH_EXTENSION + "/*");
		servletRegistrationBean.setLoadOnStartup(1);
		return servletRegistrationBean;
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
