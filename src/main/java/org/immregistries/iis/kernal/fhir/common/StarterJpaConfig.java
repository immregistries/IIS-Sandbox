package org.immregistries.iis.kernal.fhir.common;

import ca.uhn.fhir.batch2.coordinator.JobDefinitionRegistry;
import ca.uhn.fhir.batch2.jobs.reindex.ReindexJobParameters;
import ca.uhn.fhir.batch2.model.JobDefinition;
import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.IDaoRegistry;
import ca.uhn.fhir.jpa.api.config.ThreadPoolFactoryConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.config.util.HapiEntityManagerFactoryUtil;
import ca.uhn.fhir.jpa.config.util.ResourceCountCacheUtil;
import ca.uhn.fhir.jpa.config.util.ValidationSupportConfigUtil;
import ca.uhn.fhir.jpa.dao.FulltextSearchSvcImpl;
import ca.uhn.fhir.jpa.dao.IFulltextSearchSvc;
import ca.uhn.fhir.jpa.dao.search.HSearchSortHelperImpl;
import ca.uhn.fhir.jpa.dao.search.IHSearchSortHelper;
import ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingInterceptor;
import ca.uhn.fhir.jpa.packages.IPackageInstallerSvc;
import ca.uhn.fhir.jpa.packages.PackageInstallationSpec;
import ca.uhn.fhir.jpa.provider.*;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.search.IStaleSearchDeletingSvc;
import ca.uhn.fhir.jpa.search.StaleSearchDeletingSvcImpl;
import org.immregistries.iis.kernal.fhir.AppProperties;
import org.immregistries.iis.kernal.fhir.annotations.OnCorsPresent;
import org.immregistries.iis.kernal.fhir.annotations.OnImplementationGuidesPresent;
import org.immregistries.iis.kernal.fhir.common.validation.IRepositoryValidationInterceptorFactory;
import org.immregistries.iis.kernal.fhir.util.EnvironmentHelper;
import ca.uhn.fhir.jpa.util.ResourceCountCache;
import ca.uhn.fhir.jpa.validation.JpaValidationSupportChain;
import ca.uhn.fhir.rest.api.IResourceSupportedSvc;
import ca.uhn.fhir.rest.server.interceptor.*;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import com.google.common.collect.ImmutableList;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.web.cors.CorsConfiguration;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.*;

import static org.immregistries.iis.kernal.fhir.common.validation.IRepositoryValidationInterceptorFactory.ENABLE_REPOSITORY_VALIDATING_INTERCEPTOR;


@Configuration
@Import(
	ThreadPoolFactoryConfig.class
)
public class StarterJpaConfig {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(StarterJpaConfig.class);

	@Bean
	public IFulltextSearchSvc fullTextSearchSvc() {
		return new FulltextSearchSvcImpl();
	}

	@Bean
	public IStaleSearchDeletingSvc staleSearchDeletingSvc() {
		return new StaleSearchDeletingSvcImpl();
	}

	@Primary
	@Bean
	public CachingValidationSupport validationSupportChain(JpaValidationSupportChain theJpaValidationSupportChain) {
		return ValidationSupportConfigUtil.newCachingValidationSupport(theJpaValidationSupportChain);
	}


	@Autowired
	private ConfigurableEnvironment configurableEnvironment;

	/**
	 * Customize the default/max page sizes for search results. You can set these however
	 * you want, although very large page sizes will require a lot of RAM.
	 */
	@Bean
	public DatabaseBackedPagingProvider databaseBackedPagingProvider(AppProperties appProperties) {
		DatabaseBackedPagingProvider pagingProvider = new DatabaseBackedPagingProvider();
		pagingProvider.setDefaultPageSize(appProperties.getDefault_page_size());
		pagingProvider.setMaximumPageSize(appProperties.getMax_page_size());
		return pagingProvider;
	}

	@Bean
	public IResourceSupportedSvc resourceSupportedSvc(IDaoRegistry theDaoRegistry) {
		return new DaoRegistryResourceSupportedSvc(theDaoRegistry);
	}

	@Bean(name = "myResourceCountsCache")
	public ResourceCountCache resourceCountsCache(IFhirSystemDao<?, ?> theSystemDao) {
		return ResourceCountCacheUtil.newResourceCountCache(theSystemDao);
	}

	@Primary
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource myDataSource, ConfigurableListableBeanFactory myConfigurableListableBeanFactory, FhirContext theFhirContext) {
		LocalContainerEntityManagerFactoryBean retVal = HapiEntityManagerFactoryUtil.newEntityManagerFactory(myConfigurableListableBeanFactory, theFhirContext);
		retVal.setPersistenceUnitName("HAPI_PU");

		try {
			retVal.setDataSource(myDataSource);
		} catch (Exception e) {
			throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
		}
		retVal.setJpaProperties(EnvironmentHelper.getHibernateProperties(configurableEnvironment, myConfigurableListableBeanFactory));
		return retVal;
	}

	@Bean
	@Primary
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager retVal = new JpaTransactionManager();
		retVal.setEntityManagerFactory(entityManagerFactory);
		return retVal;
	}

	@Bean
	public IHSearchSortHelper hSearchSortHelper(ISearchParamRegistry mySearchParamRegistry) {
		return new HSearchSortHelperImpl(mySearchParamRegistry);
	}


	@Bean
	@ConditionalOnProperty(prefix = "hapi.fhir", name = ENABLE_REPOSITORY_VALIDATING_INTERCEPTOR, havingValue = "true")
	public RepositoryValidatingInterceptor repositoryValidatingInterceptor(IRepositoryValidationInterceptorFactory factory) {
		return factory.buildUsingStoredStructureDefinitions();
	}

	@Bean
	public LoggingInterceptor loggingInterceptor(AppProperties appProperties) {

		/*
		 * Add some logging for each request
		 */

		LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
		loggingInterceptor.setLoggerName(appProperties.getLogger().getName());
		loggingInterceptor.setMessageFormat(appProperties.getLogger().getFormat());
		loggingInterceptor.setErrorMessageFormat(appProperties.getLogger().getError_format());
		loggingInterceptor.setLogExceptions(appProperties.getLogger().getLog_exceptions());
		return loggingInterceptor;
	}

	@Bean("packageInstaller")
	@Primary
	@Conditional(OnImplementationGuidesPresent.class)
	public IPackageInstallerSvc packageInstaller(AppProperties appProperties, JobDefinition<ReindexJobParameters> reindexJobParametersJobDefinition, JobDefinitionRegistry jobDefinitionRegistry, IPackageInstallerSvc packageInstallerSvc) {
		jobDefinitionRegistry.addJobDefinitionIfNotRegistered(reindexJobParametersJobDefinition);

		if (appProperties.getImplementationGuides() != null) {
			Map<String, AppProperties.ImplementationGuide> guides = appProperties.getImplementationGuides();
			for (Map.Entry<String, AppProperties.ImplementationGuide> guide : guides.entrySet()) {
				PackageInstallationSpec packageInstallationSpec = new PackageInstallationSpec().setPackageUrl(guide.getValue().getUrl()).setName(guide.getValue().getName()).setVersion(guide.getValue().getVersion()).setInstallMode(PackageInstallationSpec.InstallModeEnum.STORE_AND_INSTALL);
				if (appProperties.getInstall_transitive_ig_dependencies()) {
					packageInstallationSpec.setFetchDependencies(true);
					packageInstallationSpec.setDependencyExcludes(ImmutableList.of("hl7.fhir.r2.core", "hl7.fhir.r3.core", "hl7.fhir.r4.core", "hl7.fhir.r5.core"));
				}
				packageInstallerSvc.install(packageInstallationSpec);
			}
		}
		return packageInstallerSvc;
	}

	@Bean
	@Conditional(OnCorsPresent.class)
	public CorsInterceptor corsInterceptor(AppProperties appProperties) {
		// Define your CORS configuration. This is an example
		// showing a typical setup. You should customize this
		// to your specific needs
		ourLog.info("CORS is enabled on this server");
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedHeader(HttpHeaders.ORIGIN);
		config.addAllowedHeader(HttpHeaders.ACCEPT);
		config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
		config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
		config.addAllowedHeader(HttpHeaders.CACHE_CONTROL);
		config.addAllowedHeader("x-fhir-starter");
		config.addAllowedHeader("X-Requested-With");
		config.addAllowedHeader("Prefer");

		List<String> allAllowedCORSOrigins = appProperties.getCors().getAllowed_origin();
		allAllowedCORSOrigins.forEach(config::addAllowedOriginPattern);
		ourLog.info("CORS allows the following origins: " + String.join(", ", allAllowedCORSOrigins));

		config.addExposedHeader("Location");
		config.addExposedHeader("Content-Location");
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
		config.setAllowCredentials(appProperties.getCors().getAllow_Credentials());

		// Create the interceptor and register it
		return new CorsInterceptor(config);

	}

}
