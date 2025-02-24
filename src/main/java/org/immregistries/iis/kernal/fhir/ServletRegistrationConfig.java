package org.immregistries.iis.kernal.fhir;

import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.servlet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Registers  Servlets and Controllers for UI and inherited IIS Sandbox V2 functionalities
 */
@Configuration
public class ServletRegistrationConfig {
	@Autowired
	AutowireCapableBeanFactory beanFactory;

//	@Bean
//	@Conditional(OnR5Condition.class)
//	public ServletRegistrationBean<GroupServlet> groupServletRegistrationBean() {
//		ServletRegistrationBean<GroupServlet> registrationBean = new ServletRegistrationBean<>();
//		GroupServlet servlet = new GroupServlet();
//		beanFactory.autowireBean(servlet);
//		registrationBean.setServlet(servlet);
//		registrationBean.addUrlMappings("/group");
//		registrationBean.setLoadOnStartup(1);
//		return registrationBean;
//	}

	@Bean
	public ServletRegistrationBean<MessageServlet> messageServletRegistrationBean() {
		ServletRegistrationBean<MessageServlet> registrationBean = new ServletRegistrationBean<MessageServlet>();
		MessageServlet servlet = new MessageServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/message");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	@Conditional(OnR5Condition.class)
	public ServletRegistrationBean<SubscriptionServlet> subscriptionServletRegistrationBean() {
		ServletRegistrationBean<SubscriptionServlet> registrationBean = new ServletRegistrationBean<SubscriptionServlet>();
		SubscriptionServlet servlet = new SubscriptionServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/subscription");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<LocationServlet> locationServletRegistrationBean() {
		ServletRegistrationBean<LocationServlet> registrationBean = new ServletRegistrationBean<LocationServlet>();
		LocationServlet servlet = new LocationServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/location");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	@Conditional(OnR5Condition.class)
	public ServletRegistrationBean<CovidServlet> covidServletRegistrationBean() {
		ServletRegistrationBean<CovidServlet> registrationBean = new ServletRegistrationBean<CovidServlet>();
		CovidServlet servlet = new CovidServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/covid");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<FitsServlet> fitsServletRegistrationBean() {
		ServletRegistrationBean<FitsServlet> registrationBean = new ServletRegistrationBean();
		FitsServlet servlet = new FitsServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/fits");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

//	@Bean
//	@Conditional(OnR5Condition.class)
//	public ServletRegistrationBean<FhirTestServlet> fhirTestServletRegistrationBean() {
//		ServletRegistrationBean registrationBean = new ServletRegistrationBean();
//		HttpServlet servlet = new FhirTestServlet();
//		beanFactory.autowireBean(servlet);
//		registrationBean.setServlet(servlet);
//		registrationBean.addUrlMappings("/fhirTest");

	/// /		registrationBean.setLoadOnStartup(1);
//		return registrationBean;
//	}

	@Bean
	public ServletRegistrationBean<LabServlet> labServletRegistrationBean() {
		ServletRegistrationBean<LabServlet> registrationBean = new ServletRegistrationBean<>();
		LabServlet servlet = new LabServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/lab");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<QueryConverterServlet> queryConverterServletRegistrationBean() {
		ServletRegistrationBean<QueryConverterServlet> registrationBean = new ServletRegistrationBean<>();
		QueryConverterServlet servlet = new QueryConverterServlet();
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
	public ServletRegistrationBean<VacDedupServlet> vacDedupServletRegistrationBean() {
		ServletRegistrationBean<VacDedupServlet> registrationBean = new ServletRegistrationBean<>();
		VacDedupServlet servlet = new VacDedupServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/vacDedup");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<VciServlet> vciServletRegistrationBean() {
		ServletRegistrationBean<VciServlet> registrationBean = new ServletRegistrationBean<>();
		VciServlet servlet = new VciServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/vciDemo");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<VXUDownloadServlet> vXUDownloadServletRegistrationBean() {
		ServletRegistrationBean<VXUDownloadServlet> registrationBean = new ServletRegistrationBean<>();
		VXUDownloadServlet servlet = new VXUDownloadServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/VXUDownload");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<VXUDownloadFormServlet> vXUDownloadFormServletRegistrationBean() {
		ServletRegistrationBean<VXUDownloadFormServlet> registrationBean = new ServletRegistrationBean<>();
		VXUDownloadFormServlet servlet = new VXUDownloadFormServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/VXUDownloadForm");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<CovidGenerateServlet> covidGenerateServletRegistrationBean() {
		ServletRegistrationBean<CovidGenerateServlet> registrationBean = new ServletRegistrationBean<>();
		CovidGenerateServlet servlet = new CovidGenerateServlet();
		beanFactory.autowireBean(servlet);
		registrationBean.setServlet(servlet);
		registrationBean.addUrlMappings("/covidGenerate");
//		registrationBean.setLoadOnStartup(1);
		return registrationBean;
	}

//	@Bean
//	@Conditional(OnR5Condition.class)
//	public ServletRegistrationBean<TestMapping> testMappingRegistrationBean() {
//		ServletRegistrationBean<TestMapping> registrationBean = new ServletRegistrationBean<>();
//		TestMapping servlet = new TestMapping();
//		beanFactory.autowireBean(servlet);
//		registrationBean.setServlet(servlet);
//		registrationBean.addUrlMappings("/utest");
////		registrationBean.setLoadOnStartup(1);
//		return registrationBean;
//	}
@Bean
public ServletRegistrationBean<TenantCompareServlet> tenantCompareServletServletRegistrationBean() {
	ServletRegistrationBean<TenantCompareServlet> registrationBean = new ServletRegistrationBean<>();
	TenantCompareServlet servlet = new TenantCompareServlet();
	beanFactory.autowireBean(servlet);
	registrationBean.setServlet(servlet);
	registrationBean.addUrlMappings("/tenantCompare");
//		registrationBean.setLoadOnStartup(1);
	return registrationBean;
}

}
