package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import org.immregistries.iis.kernal.fhir.annotations.OnEitherVersion;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.mdm.MdmConfig;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.immregistries.iis.kernal.fhir.security.ServerSecurityConfig;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.servlet.HomeServlet;
import org.immregistries.iis.kernal.servlet.LoginServlet;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.iis.kernal.servlet.PopServletR4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.request.RequestContextListener;

import javax.servlet.http.HttpServlet;

@ServletComponentScan(basePackageClasses = {
	RestfulServer.class}, basePackages = {
	"org.immregistries.iis.kernal.servlet"
//	,"org.immregistries.iis.kernal.repository"
})
@SpringBootApplication(exclude = {ElasticsearchRestClientAutoConfiguration.class})
@Import({
	SubscriptionSubmitterConfig.class,
	SubscriptionProcessorConfig.class,
	SubscriptionChannelConfig.class,
	WebsocketDispatcherConfig.class,
	MdmConfig.class,
	JpaBatch2Config.class,
	Batch2JobsConfig.class,
	ServletRegistrationConfig.class,
	ServerConfig.class,
	ServerSecurityConfig.class,
})
@ComponentScan(basePackages = {
	"ca.uhn.fhir.jpa.starter",
	"org.immregistries.iis.kernal"
})
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {
	  CodeMapManager.getCodeMap(); // Initializes codemaps
    SpringApplication.run(Application.class, args);

    //Server is now accessible at eg. http://localhost:8080/fhir/metadata
    //UI is now accessible at http://localhost:8080/
  }

  @Override
  protected SpringApplicationBuilder configure(
    SpringApplicationBuilder builder) {
    return builder.sources(Application.class);
  }

  @Autowired
  AutowireCapableBeanFactory beanFactory;

	@Bean
	@Conditional(OnEitherVersion.class)
	public ServletRegistrationBean hapiServletRegistration(RestfulServer restfulServer) {
		ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
		beanFactory.autowireBean(restfulServer);
		servletRegistrationBean.setServlet(restfulServer);
		servletRegistrationBean.addUrlMappings("/fhir/*");
		servletRegistrationBean.setLoadOnStartup(1);

		return servletRegistrationBean;
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean loginServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new LoginServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/loginForm");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean popServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new PopServletR4();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/pop");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public ServletRegistrationBean homeServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HomeServlet servlet = new HomeServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/home");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	/**
	 * Required to get access to httpRequest qnd session through spring, important to use the fhir client inside the servlets
	 *
	 * @return
	 */
	@Bean
	public RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}


}
