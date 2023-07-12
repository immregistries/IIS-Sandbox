package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.batch2.jobs.imprt.BulkDataImportProvider;
import ca.uhn.fhir.batch2.jobs.reindex.ReindexProvider;
import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.config.ThreadPoolFactoryConfig;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.binary.interceptor.BinaryStorageInterceptor;
import ca.uhn.fhir.jpa.binary.provider.BinaryAccessProvider;
import ca.uhn.fhir.jpa.delete.ThreadSafeResourceDeleterSvc;
import ca.uhn.fhir.jpa.graphql.GraphQLProvider;
import ca.uhn.fhir.jpa.interceptor.CascadingDeleteInterceptor;
import ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingInterceptor;
import ca.uhn.fhir.jpa.packages.IPackageInstallerSvc;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.jpa.provider.*;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import org.immregistries.iis.kernal.fhir.BulkQuery.BulkQueryGroupProviderR4;
import org.immregistries.iis.kernal.fhir.BulkQuery.BulkQueryGroupProviderR5;
import org.immregistries.iis.kernal.fhir.BulkQuery.CustomBulkDataExportProvider;
import org.immregistries.iis.kernal.fhir.common.StarterJpaConfig;
import org.immregistries.iis.kernal.fhir.interceptors.IdentifierSolverInterceptor;
import org.immregistries.iis.kernal.fhir.interceptors.IdentifierSolverInterceptorR4;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import ca.uhn.fhir.jpa.subscription.util.SubscriptionDebugLogInterceptor;
import ca.uhn.fhir.mdm.provider.MdmProviderLoader;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative2.NullNarrativeGenerator;
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor;
import ca.uhn.fhir.rest.server.*;
import ca.uhn.fhir.rest.server.interceptor.*;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.tenant.UrlBaseTenantIdentificationStrategy;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import com.google.common.base.Strings;
import org.immregistries.iis.kernal.fhir.interceptors.SessionAuthorizationInterceptor;
import org.immregistries.iis.kernal.fhir.mdm.MdmCustomProvider;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Configuration
@Import(
	{ThreadPoolFactoryConfig.class,
		StarterJpaConfig.class
	}
)
public class ServerConfig {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ServerConfig.class);

	@Autowired(required = false)
	IFhirResourceDao<org.hl7.fhir.r4.model.Group> fhirResourceGroupDaoR4;
	@Autowired(required = false)
	IFhirResourceDao<org.hl7.fhir.r5.model.Group> fhirResourceGroupDaoR5;

	@Bean
	public RestfulServer restfulServer(IFhirSystemDao<?, ?> fhirSystemDao, AppProperties appProperties, DaoRegistry daoRegistry, Optional<MdmProviderLoader> mdmProviderProvider, IJpaSystemProvider jpaSystemProvider, ResourceProviderFactory resourceProviderFactory, DaoConfig daoConfig, ISearchParamRegistry searchParamRegistry, IValidationSupport theValidationSupport, DatabaseBackedPagingProvider databaseBackedPagingProvider, LoggingInterceptor loggingInterceptor, Optional<TerminologyUploaderProvider> terminologyUploaderProvider, Optional<SubscriptionTriggeringProvider> subscriptionTriggeringProvider, Optional<CorsInterceptor> corsInterceptor, IInterceptorBroadcaster interceptorBroadcaster, Optional<BinaryAccessProvider> binaryAccessProvider, BinaryStorageInterceptor binaryStorageInterceptor, IValidatorModule validatorModule, Optional<GraphQLProvider> graphQLProvider, CustomBulkDataExportProvider customBulkDataExportProvider, BulkDataImportProvider bulkDataImportProvider, ValueSetOperationProvider theValueSetOperationProvider, ReindexProvider reindexProvider, PartitionManagementProvider partitionManagementProvider, Optional<RepositoryValidatingInterceptor> repositoryValidatingInterceptor, IPackageInstallerSvc packageInstallerSvc, ThreadSafeResourceDeleterSvc theThreadSafeResourceDeleterSvc, ApplicationContext appContext,
												  PartitionCreationInterceptor partitionCreationInterceptor,
												  Optional<BulkQueryGroupProviderR5> bulkQueryGroupProviderR5,
												  Optional<BulkQueryGroupProviderR4> bulkQueryGroupProviderR4,
												  Optional<MdmCustomProvider> mdmCustomProvider,
												  Optional<IdentifierSolverInterceptor> identifierSolverInterceptor,
												  Optional<IdentifierSolverInterceptorR4> identifierSolverInterceptorR4,
												  SessionAuthorizationInterceptor sessionAuthorizationInterceptor) {
		RestfulServer fhirServer = new RestfulServer(fhirSystemDao.getContext());

		List<String> supportedResourceTypes = appProperties.getSupported_resource_types();

		if (!supportedResourceTypes.isEmpty()) {
			if (!supportedResourceTypes.contains("SearchParameter")) {
				supportedResourceTypes.add("SearchParameter");
			}
			daoRegistry.setSupportedResourceTypes(supportedResourceTypes);
		}

		if (appProperties.getNarrative_enabled()) {
			fhirSystemDao.getContext().setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		} else {
			fhirSystemDao.getContext().setNarrativeGenerator(new NullNarrativeGenerator());
		}

		if (appProperties.getMdm_enabled()) {
//			mdmProviderProvider.get().loadProvider();
			/**
			 * CUSTOM MDM PROVIDERS HERE
			 */
			resourceProviderFactory.addSupplier(mdmCustomProvider::get);
//			fhirServer.registerProvider(mdmCustomProvider.get());
			daoConfig.setAllowMdmExpansion(true);
		}
		/**
		 * CUSTOM PROVIDERS HERE
		 */
		fhirServer.registerProviders(resourceProviderFactory.createProviders());
		fhirServer.registerProvider(jpaSystemProvider);
		fhirServer.setServerConformanceProvider(calculateConformanceProvider(fhirSystemDao, fhirServer, daoConfig, searchParamRegistry, theValidationSupport));

		/*
		 * ETag Support
		 */

		if (!appProperties.getEtag_support_enabled()) fhirServer.setETagSupport(ETagSupportEnum.DISABLED);


		/*
		 * Default to JSON and pretty printing
		 */
		fhirServer.setDefaultPrettyPrint(appProperties.getDefault_pretty_print());

		/*
		 * Default encoding
		 */
		fhirServer.setDefaultResponseEncoding(appProperties.getDefault_encoding());

		/*
		 * This configures the server to page search results to and from
		 * the database, instead of only paging them to memory. This may mean
		 * a performance hit when performing searches that return lots of results,
		 * but makes the server much more scalable.
		 */

		fhirServer.setPagingProvider(databaseBackedPagingProvider);

		/*
		 * This interceptor formats the output using nice colourful
		 * HTML output when the request is detected to come from a
		 * browser.
		 */
		fhirServer.registerInterceptor(new ResponseHighlighterInterceptor());

		if (appProperties.getFhirpath_interceptor_enabled()) {
			fhirServer.registerInterceptor(new FhirPathFilterInterceptor());
		}

		fhirServer.registerInterceptor(loggingInterceptor);

		/*
		 * If you are hosting this server at a specific DNS name, the server will try to
		 * figure out the FHIR base URL based on what the web container tells it, but
		 * this doesn't always work. If you are setting links in your search bundles that
		 * just refer to "localhost", you might want to use a server address strategy:
		 */
		String serverAddress = appProperties.getServer_address();
		if (!Strings.isNullOrEmpty(serverAddress)) {
			fhirServer.setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
		} else if (appProperties.getUse_apache_address_strategy()) {
			boolean useHttps = appProperties.getUse_apache_address_strategy_https();
			fhirServer.setServerAddressStrategy(useHttps ? ApacheProxyAddressStrategy.forHttps() : ApacheProxyAddressStrategy.forHttp());
		} else {
			fhirServer.setServerAddressStrategy(new IncomingRequestAddressStrategy());
		}

		/*
		 * If you are using DSTU3+, you may want to add a terminology uploader, which allows
		 * uploading of external terminologies such as Snomed CT. Note that this uploader
		 * does not have any security attached (any anonymous user may use it by default)
		 * so it is a potential security vulnerability. Consider using an AuthorizationInterceptor
		 * with this feature.
		 */
		if (fhirSystemDao.getContext().getVersion().getVersion().isEqualOrNewerThan(FhirVersionEnum.DSTU3)) { // <-- ENABLED RIGHT NOW
			fhirServer.registerProvider(terminologyUploaderProvider.get());
		}

		// If you want to enable the $trigger-subscription operation to allow
		// manual triggering of a subscription delivery, enable this provider
		if (true) { // <-- ENABLED RIGHT NOW
			fhirServer.registerProvider(subscriptionTriggeringProvider.get());
		}

		corsInterceptor.ifPresent(fhirServer::registerInterceptor);

		if (daoConfig.getSupportedSubscriptionTypes().size() > 0) {
			// Subscription debug logging
			fhirServer.registerInterceptor(new SubscriptionDebugLogInterceptor());
		}

		if (appProperties.getAllow_cascading_deletes()) {
			CascadingDeleteInterceptor cascadingDeleteInterceptor = new CascadingDeleteInterceptor(fhirSystemDao.getContext(), daoRegistry, interceptorBroadcaster, theThreadSafeResourceDeleterSvc);
			fhirServer.registerInterceptor(cascadingDeleteInterceptor);
		}

		// Binary Storage
		if (appProperties.getBinary_storage_enabled() && binaryAccessProvider.isPresent()) {
			fhirServer.registerProvider(binaryAccessProvider.get());
			fhirServer.registerInterceptor(binaryStorageInterceptor);
		}

		// Validation

		if (validatorModule != null) {
			if (appProperties.getValidation().getRequests_enabled()) {
				RequestValidatingInterceptor interceptor = new RequestValidatingInterceptor();
				interceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
				interceptor.setValidatorModules(Collections.singletonList(validatorModule));
				fhirServer.registerInterceptor(interceptor);
			}
			if (appProperties.getValidation().getResponses_enabled()) {
				ResponseValidatingInterceptor interceptor = new ResponseValidatingInterceptor();
				interceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
				interceptor.setValidatorModules(Collections.singletonList(validatorModule));
				fhirServer.registerInterceptor(interceptor);
			}
		}

		// GraphQL
		if (appProperties.getGraphql_enabled()) {
			if (fhirSystemDao.getContext().getVersion().getVersion().isEqualOrNewerThan(FhirVersionEnum.DSTU3)) {
				fhirServer.registerProvider(graphQLProvider.get());
			}
		}

		if (appProperties.getOpenapi_enabled()) {
			fhirServer.registerInterceptor(new OpenApiInterceptor());
		}

		// Bulk Export
		if (appProperties.getBulk_export_enabled()) {
			/**
			 * Customized to support synch export
			 */
			fhirServer.registerProvider(customBulkDataExportProvider);
			if (bulkQueryGroupProviderR4.isPresent()) {
				fhirServer.registerProvider(bulkQueryGroupProviderR4.get());
				bulkQueryGroupProviderR4.get().setDao(fhirResourceGroupDaoR4);
			} else if (bulkQueryGroupProviderR5.isPresent()) {
				fhirServer.registerProvider(bulkQueryGroupProviderR5.get());
				bulkQueryGroupProviderR5.get().setDao(fhirResourceGroupDaoR5);
			}
		}

		//Bulk Import
		if (appProperties.getBulk_import_enabled()) {
			fhirServer.registerProvider(bulkDataImportProvider);
		}


		// valueSet Operations i.e $expand
		fhirServer.registerProvider(theValueSetOperationProvider);

		//reindex Provider $reindex
		fhirServer.registerProvider(reindexProvider);

		// Partitioning
		if (appProperties.getPartitioning() != null) {
//			fhirServer.registerInterceptor(new RequestTenantPartitionInterceptor());
			/**
			 * registered custom interceptor for automatic partition generation
			 */
			fhirServer.registerInterceptor(partitionCreationInterceptor);
			fhirServer.setTenantIdentificationStrategy(new UrlBaseTenantIdentificationStrategy());
			fhirServer.registerProviders(partitionManagementProvider);
		}
		repositoryValidatingInterceptor.ifPresent(fhirServer::registerInterceptor);

		// register custom interceptors
		fhirServer.registerInterceptor(sessionAuthorizationInterceptor);
		if (identifierSolverInterceptor.isPresent()) {
			fhirServer.registerInterceptor(identifierSolverInterceptor);
		} else if (identifierSolverInterceptorR4.isPresent()) {
			fhirServer.registerInterceptor(identifierSolverInterceptorR4);
		}
//		registerCustomInterceptors(fhirServer, appContext, appProperties.getCustomInterceptorClasses());

		return fhirServer;
	}

	/**
	 * check the properties for custom interceptor classes and registers them.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void registerCustomInterceptors(RestfulServer fhirServer, ApplicationContext theAppContext, List<String> customInterceptorClasses) {

		if (customInterceptorClasses == null) {
			return;
		}


		for (String className : customInterceptorClasses) {
			Class clazz;
			try {
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new ConfigurationException("Interceptor class was not found on classpath: " + className, e);
			}

			// first check if the class a Bean in the app context

			Object interceptor = null;
			try {
				interceptor = theAppContext.getBean(clazz);
				ourLog.info("className bean {}", className);
			} catch (NoSuchBeanDefinitionException ex) {
				// no op - if it's not a bean we'll try to create it
			}

			// if not a bean, instantiate the interceptor via reflection
			if (interceptor == null) {
				try {
					interceptor = clazz.getConstructor().newInstance();
				} catch (Exception e) {
					throw new ConfigurationException("Unable to instantiate interceptor class : " + className, e);
				}
			}

			fhirServer.registerInterceptor(interceptor);
		}
	}

	public static IServerConformanceProvider<?> calculateConformanceProvider(IFhirSystemDao fhirSystemDao, RestfulServer fhirServer, DaoConfig daoConfig, ISearchParamRegistry searchParamRegistry, IValidationSupport theValidationSupport) {
		FhirVersionEnum fhirVersion = fhirSystemDao.getContext().getVersion().getVersion();
		if (fhirVersion == FhirVersionEnum.DSTU2) {
			JpaConformanceProviderDstu2 confProvider = new JpaConformanceProviderDstu2(fhirServer, fhirSystemDao, daoConfig);
			confProvider.setImplementationDescription("HAPI FHIR DSTU2 Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.DSTU3) {

			JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(fhirServer, fhirSystemDao, daoConfig, searchParamRegistry);
			confProvider.setImplementationDescription("HAPI FHIR DSTU3 Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.R4) {

			JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(fhirServer, fhirSystemDao, daoConfig, searchParamRegistry, theValidationSupport);
			confProvider.setImplementationDescription("HAPI FHIR R4 Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.R4B) {

			JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(fhirServer, fhirSystemDao, daoConfig, searchParamRegistry, theValidationSupport);
			confProvider.setImplementationDescription("HAPI FHIR R4B Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.R5) {

			JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(fhirServer, fhirSystemDao, daoConfig, searchParamRegistry, theValidationSupport);
			confProvider.setImplementationDescription("HAPI FHIR R5 Server");
			return confProvider;
		} else {
			throw new IllegalStateException();
		}
	}
}
