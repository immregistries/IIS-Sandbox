package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.starter.annotations.OnEitherVersion;
import ca.uhn.fhir.jpa.starter.mdm.MdmConfig;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import org.immregistries.iis.kernal.servlet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.http.HttpServlet;

@ServletComponentScan(basePackageClasses = {
  JpaRestfulServer.class}, basePackages = {
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
	Batch2JobsConfig.class
})
@ComponentScan(basePackages = {
	"ca.uhn.fhir.jpa.starter",
	"org.immregistries.iis.kernal"
})
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {

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
  public ServletRegistrationBean hapiServletRegistration() {
    ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
    JpaRestfulServer jpaRestfulServer = new JpaRestfulServer();
    beanFactory.autowireBean(jpaRestfulServer);
    servletRegistrationBean.setServlet(jpaRestfulServer);
    servletRegistrationBean.addUrlMappings("/fhir/*");
    servletRegistrationBean.setLoadOnStartup(1);

    return servletRegistrationBean;
  }

  @Bean
  public ServletRegistrationBean overlayRegistrationBean() {

    AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
    annotationConfigWebApplicationContext.register(FhirTesterConfig.class);

    DispatcherServlet dispatcherServlet = new DispatcherServlet(
      annotationConfigWebApplicationContext);
    dispatcherServlet.setContextClass(AnnotationConfigWebApplicationContext.class);
    dispatcherServlet.setContextConfigLocation(FhirTesterConfig.class.getName());

    ServletRegistrationBean registrationBean = new ServletRegistrationBean();
    registrationBean.setServlet(dispatcherServlet);
    registrationBean.addUrlMappings("/overlay/*");
    registrationBean.setLoadOnStartup(1);
    return registrationBean;

  }

  @Bean
  public ServletRegistrationBean homeServletRegistrationBean() {
	  ServletRegistrationBean registrationBean = new ServletRegistrationBean();
	  HomeServlet servlet = new HomeServlet();
	  beanFactory.autowireBean(servlet);
	  registrationBean.setServlet(servlet);
	  registrationBean.addUrlMappings( "/home");
	  registrationBean.setLoadOnStartup(1);
	  return registrationBean;
  }


	@Bean
	public ServletRegistrationBean popServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new PopServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings( "/pop");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean messageServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new MessageServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings( "/message");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean soapServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new SoapServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings( "/soap");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
	@Bean
	public ServletRegistrationBean patientServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new PatientServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings( "/patient");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
	@Bean
	public ServletRegistrationBean vaccinationServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new VaccinationServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings( "/vaccination");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
	@Bean
	public ServletRegistrationBean subscriptionServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new SubscriptionServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings( "/subscription");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
	@Bean
	public ServletRegistrationBean subscriptionTopicServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new SubscriptionTopicServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings( "/SubscriptionTopic");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}
}
