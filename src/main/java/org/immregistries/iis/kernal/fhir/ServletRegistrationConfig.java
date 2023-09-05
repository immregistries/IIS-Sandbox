package org.immregistries.iis.kernal.fhir;

import org.immregistries.iis.kernal.OAuthLoginServlet;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.servlet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServlet;

@Configuration
@Conditional(OnR5Condition.class)
public class ServletRegistrationConfig {
	@Autowired
	AutowireCapableBeanFactory beanFactory;

	@Bean
	public ServletRegistrationBean<GroupServlet> groupServletRegistrationBean() {
		ServletRegistrationBean<GroupServlet> registrationBean = new ServletRegistrationBean<>();
		GroupServlet servlet = new GroupServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/group");
		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean messageServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new MessageServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/message");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	@Conditional(OnR5Condition.class)
	public ServletRegistrationBean subscriptionServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new SubscriptionServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/subscription");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean subscriptionTopicServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new SubscriptionTopicServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/SubscriptionTopic");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean locationServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new LocationServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/location");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	@Conditional(OnR5Condition.class)
	public ServletRegistrationBean covidServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new CovidServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/covid");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean fitsServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new FitsServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/fits");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean fhirTestServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new FhirTestServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/fhirTest");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean labServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new LabServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/lab");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean queryConverterServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new QueryConverterServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/queryConverter");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

//	@Bean
//	public ServletRegistrationBean v2ToFhirServletRegistrationBean() {
//		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
//		HttpServlet servlet = new V2ToFhirServlet();
//		beanFactory.autowireBean(servlet);
//		registrationBean.setServlet(servlet);
//		registrationBean.addUrlMappings("/v2ToFhir");
////		registrationBean.setLoadOnStartup(1);
//		return registrationBean;
//	}

	@Bean
	public ServletRegistrationBean vacDedupServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new VacDedupServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/vacDedup");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean vciServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new VciServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/vciDemo");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean vXUDownloadServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new VXUDownloadServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/VXUDownload");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean vXUDownloadFormServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new VXUDownloadFormServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/VXUDownloadForm");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean covidGenerateServletRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new CovidGenerateServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/covidGenerate");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	@Conditional(OnR5Condition.class)
	public ServletRegistrationBean testMappingRegistrationBean() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
		HttpServlet servlet = new TestMapping();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/utest");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

}
