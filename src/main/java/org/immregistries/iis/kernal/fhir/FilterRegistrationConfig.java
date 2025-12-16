package org.immregistries.iis.kernal.fhir;

import org.immregistries.iis.kernal.controllers.filters.TenantRequestLoggingFilter;
import org.immregistries.iis.kernal.controllers.filters.TenantUrlFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Servlets and Controllers for UI and inherited IIS Sandbox V2
 * functionalities
 */
@Configuration
public class FilterRegistrationConfig {
	@Autowired
	AutowireCapableBeanFactory beanFactory;

	@Bean(name = "tenantUrlFilter")
	public TenantUrlFilter tenantUrlFilter() {
		TenantUrlFilter tenantUrlFilter = new TenantUrlFilter();
		return tenantUrlFilter;
	}

	@Bean
	public FilterRegistrationBean tenantUrlFilterRegistrationBean(TenantUrlFilter tenantUrlFilter) {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(tenantUrlFilter);
		registration.addUrlPatterns("/tenant/*");
		registration.setName("tenantUrlFilter");
		registration.setOrder(1);
		return registration;
	}

	@Bean(name = "tenantRequestLoggingFilter")
	public TenantRequestLoggingFilter tenantRequestLoggingFilter() {
		return new TenantRequestLoggingFilter();
	}

	@Bean
	public FilterRegistrationBean tenantRequestLoggingFilterRegistrationBean(
			TenantRequestLoggingFilter tenantRequestLoggingFilter) {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(tenantRequestLoggingFilter);
		registration.addUrlPatterns("/rest/tenant/*");
		registration.setName("tenantRequestLoggingFilter");
		registration.setOrder(1);
		return registration;
	}

}
