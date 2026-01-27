package org.immregistries.iis.kernal.controllers.filters;

import org.immregistries.iis.kernal.controllers.rest.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
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
	private static final String REST_TENANT_URL_FILTER = "restTenantUrlFilter";

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	@Bean(name = "tenantUrlFilter")
	public TenantUrlFilter tenantUrlFilter() {
		TenantUrlFilter tenantUrlFilter = new TenantUrlFilter();
		return tenantUrlFilter;
	}

	@Bean
	public FilterRegistrationBean tenantUrlFilterRegistrationBean(TenantUrlFilter tenantUrlFilter) {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(tenantUrlFilter);
		registration.addUrlPatterns(TenantController.TENANT_BASE_PATH + "/*");
		registration.setName("tenantUrlFilter");
		registration.setOrder(1);
		return registration;
	}

	@Bean(name = REST_TENANT_URL_FILTER)
	public RestTenantUrlFilter restTenantUrlFilter() {
		return new RestTenantUrlFilter();
	}

	@Bean
	public FilterRegistrationBean restTenantUrlFilterRegistrationBean(
		RestTenantUrlFilter restTenantUrlFilter) {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(restTenantUrlFilter);
		registration.addUrlPatterns(IisRestPath.BasePath.REST_PATH + TenantController.TENANT_BASE_PATH + "/*");
		registration.setName(REST_TENANT_URL_FILTER);
		registration.setOrder(1);
		return registration;
	}

}
