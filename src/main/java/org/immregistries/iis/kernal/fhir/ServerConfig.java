package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.batch2.jobs.imprt.BulkDataImportProvider;
import ca.uhn.fhir.batch2.jobs.reindex.ReindexProvider;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.config.ThreadPoolFactoryConfig;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.binary.interceptor.BinaryStorageInterceptor;
import ca.uhn.fhir.jpa.binary.provider.BinaryAccessProvider;
import ca.uhn.fhir.jpa.delete.ThreadSafeResourceDeleterSvc;
import ca.uhn.fhir.jpa.graphql.GraphQLProvider;
import ca.uhn.fhir.jpa.interceptor.CascadingDeleteInterceptor;
import ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingInterceptor;
import ca.uhn.fhir.jpa.ips.provider.IpsOperationProvider;
import ca.uhn.fhir.jpa.packages.IPackageInstallerSvc;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.jpa.provider.*;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
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
import org.immregistries.iis.kernal.fhir.bulkExport.CustomBulkDataExportProvider;
import org.immregistries.iis.kernal.fhir.bulkExport.IBulkExportGroupProvider;
import org.immregistries.iis.kernal.fhir.common.AppProperties;
import org.immregistries.iis.kernal.fhir.common.StarterJpaConfig;
import org.immregistries.iis.kernal.fhir.immdsForecast.IRecommendationForecastProvider;
import org.immregistries.iis.kernal.fhir.interceptors.CustomAuthorizationInterceptor;
import org.immregistries.iis.kernal.fhir.interceptors.GroupAuthorityInterceptor;
import org.immregistries.iis.kernal.fhir.interceptors.IIdentifierSolverInterceptor;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.fhir.ips.IpsConfig;
import org.immregistries.iis.kernal.logic.logicInterceptors.ImmunizationProcessingInterceptor;
import org.immregistries.iis.kernal.logic.logicInterceptors.ObservationProcessingInterceptor;
import org.immregistries.iis.kernal.logic.logicInterceptors.PatientProcessingInterceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Class responsible for executing the HAPIFHIR Server config, registering providers and interceptors, including custom ones
 */
@Configuration
@Import(
	{
		ThreadPoolFactoryConfig.class,
		StarterJpaConfig.class,
		IpsConfig.class
	}
)
public class ServerConfig {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ServerConfig.class);

	/**
	 * FHIR Server Configuration method, registering providers and interceptors, including customization
	 *
	 * @param fhirSystemDao                     fhirSystemDao
	 * @param appProperties                     appProperties
	 * @param daoRegistry                       daoRegistry
	 * @param jpaSystemProvider                 jpaSystemProvider
	 * @param resourceProviderFactory           resourceProviderFactory
	 * @param jpaStorageSettings                jpaStorageSettings
	 * @param searchParamRegistry               searchParamRegistry
	 * @param theValidationSupport              theValidationSupport
	 * @param databaseBackedPagingProvider      databaseBackedPagingProvider
	 * @param loggingInterceptor                loggingInterceptor
	 * @param terminologyUploaderProvider       terminologyUploaderProvider
	 * @param subscriptionTriggeringProvider    subscriptionTriggeringProvider
	 * @param corsInterceptor                   corsInterceptor
	 * @param interceptorBroadcaster            interceptorBroadcaster
	 * @param binaryAccessProvider              binaryAccessProvider
	 * @param binaryStorageInterceptor          binaryStorageInterceptor
	 * @param validatorModule                   validatorModule
	 * @param graphQLProvider                   graphQLProvider
	 * @param bulkDataExportProvider            bulkDataExportProvider
	 * @param bulkDataImportProvider            bulkDataImportProvider
	 * @param theValueSetOperationProvider      theValueSetOperationProvider
	 * @param reindexProvider                   reindexProvider
	 * @param partitionManagementProvider       partitionManagementProvider
	 * @param repositoryValidatingInterceptor   repositoryValidatingInterceptor
	 * @param packageInstallerSvc               packageInstallerSvc
	 * @param theThreadSafeResourceDeleterSvc   theThreadSafeResourceDeleterSvc
	 * @param appContext                        appContext
	 * @param theIpsOperationProvider           IPS Provider
	 * @param mdmProviderLoader                 mdmProviderLoader
	 * @param partitionCreationInterceptor      partitionCreationInterceptor
	 * @param bulkQueryGroupProvider            bulkQueryGroupProvider
	 * @param identifierSolverInterceptor       identifierSolverInterceptor
	 * @param groupAuthorityInterceptor         groupAuthorityInterceptor
	 * @param recommendationForecastProvider    IMMDS Forecast operations provider
	 * @param customAuthorizationInterceptor   sessionAuthorizationInterceptor
	 * @param patientProcessingInterceptor      custom patientProcessingInterceptor
	 * @param observationProcessingInterceptor  custom observationProcessingInterceptor
	 * @param immunizationProcessingInterceptor custom immunizationProcessingInterceptor
	 * @return Restful Server
	 */
	@Bean
	public RestfulServer restfulServer(IFhirSystemDao<?, ?> fhirSystemDao, AppProperties appProperties, DaoRegistry daoRegistry, IJpaSystemProvider jpaSystemProvider, ResourceProviderFactory resourceProviderFactory, JpaStorageSettings jpaStorageSettings, ISearchParamRegistry searchParamRegistry, IValidationSupport theValidationSupport, DatabaseBackedPagingProvider databaseBackedPagingProvider, LoggingInterceptor loggingInterceptor, Optional<TerminologyUploaderProvider> terminologyUploaderProvider, Optional<SubscriptionTriggeringProvider> subscriptionTriggeringProvider, Optional<CorsInterceptor> corsInterceptor, IInterceptorBroadcaster interceptorBroadcaster, Optional<BinaryAccessProvider> binaryAccessProvider, BinaryStorageInterceptor binaryStorageInterceptor, IValidatorModule validatorModule, Optional<GraphQLProvider> graphQLProvider, CustomBulkDataExportProvider bulkDataExportProvider, BulkDataImportProvider bulkDataImportProvider, ValueSetOperationProvider theValueSetOperationProvider, ReindexProvider reindexProvider, PartitionManagementProvider partitionManagementProvider, Optional<RepositoryValidatingInterceptor> repositoryValidatingInterceptor, IPackageInstallerSvc packageInstallerSvc, ThreadSafeResourceDeleterSvc theThreadSafeResourceDeleterSvc, ApplicationContext appContext,
												  Optional<IpsOperationProvider> theIpsOperationProvider,
												  Optional<MdmProviderLoader> mdmProviderLoader,
												  Optional<DiffProvider> diffProvider,
												  PartitionCreationInterceptor partitionCreationInterceptor,
												  Optional<IBulkExportGroupProvider> bulkQueryGroupProvider,
												  Optional<IIdentifierSolverInterceptor> identifierSolverInterceptor,
												  Optional<GroupAuthorityInterceptor> groupAuthorityInterceptor,
												  Optional<IRecommendationForecastProvider> recommendationForecastProvider,
												  CustomAuthorizationInterceptor customAuthorizationInterceptor,
												  PatientProcessingInterceptor patientProcessingInterceptor,
												  ObservationProcessingInterceptor observationProcessingInterceptor,
												  ImmunizationProcessingInterceptor immunizationProcessingInterceptor) {
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
			mdmProviderLoader.get().loadProvider();
			jpaStorageSettings.setAllowMdmExpansion(true);
		}
		/*
		 * CUSTOM PROVIDERS HERE
		 */
		fhirServer.registerProviders(resourceProviderFactory.createProviders());
		fhirServer.registerProvider(jpaSystemProvider);
		fhirServer.setServerConformanceProvider(calculateConformanceProvider(fhirSystemDao, fhirServer, jpaStorageSettings, searchParamRegistry, theValidationSupport));

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

		// If you want to enable the $trigger-subscription operation to allow
		// manual triggering of a subscription delivery, enable this provider
		if (true) { // <-- ENABLED RIGHT NOW
			fhirServer.registerProvider(subscriptionTriggeringProvider.get());
		}

		corsInterceptor.ifPresent(fhirServer::registerInterceptor);

		if (!jpaStorageSettings.getSupportedSubscriptionTypes().isEmpty()) {
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
			/*
			 * Customized
			 */
			fhirServer.registerProvider(bulkDataExportProvider);
			if (bulkQueryGroupProvider.isPresent()) {
				bulkQueryGroupProvider.get().setDao(daoRegistry.getResourceDao("Group"));
				fhirServer.registerProvider(bulkQueryGroupProvider.get());
			}
		}

		// Bulk Import
		if (appProperties.getBulk_import_enabled()) {
			fhirServer.registerProvider(bulkDataImportProvider);
		}

		if (diffProvider.isPresent()) {
			fhirServer.registerProvider(diffProvider.get());
		}

		// valueSet Operations i.e $expand
		fhirServer.registerProvider(theValueSetOperationProvider);

		//reindex Provider $reindex
		fhirServer.registerProvider(reindexProvider);

		// Partitioning
		if (appProperties.getPartitioning() != null) {
			/*
			 * Registered custom interceptor for automatic partition generation
			 */
			fhirServer.registerInterceptor(partitionCreationInterceptor);
			fhirServer.setTenantIdentificationStrategy(new UrlBaseTenantIdentificationStrategy());
			fhirServer.registerProviders(partitionManagementProvider);
		}
		repositoryValidatingInterceptor.ifPresent(fhirServer::registerInterceptor);

		//register the IPS Provider
		theIpsOperationProvider.ifPresent(fhirServer::registerProvider);
		recommendationForecastProvider.ifPresent(fhirServer::registerProvider);

		/*
		 * CUSTOM INTERCEPTORS HERE
		 */
		fhirServer.registerInterceptor(customAuthorizationInterceptor);
		identifierSolverInterceptor.ifPresent(fhirServer::registerInterceptor);
		groupAuthorityInterceptor.ifPresent(fhirServer::registerInterceptor);
		/*
		 * Processing and validating interceptors, adding part of inherited V2 validation logic with Flavors.
		 */
		fhirServer.registerInterceptor(patientProcessingInterceptor);
		fhirServer.registerInterceptor(immunizationProcessingInterceptor);
		fhirServer.registerInterceptor(observationProcessingInterceptor);
		return fhirServer;
	}

	/**
	 * Inherited from HAPI FHIR Skeleton
	 * @param fhirSystemDao system Dao
	 * @param fhirServer server
	 * @param jpaStorageSettings storage settings
	 * @param searchParamRegistry searchParamRegistry
	 * @param theValidationSupport validation support
	 * @return Conformance provider
	 */
	public static IServerConformanceProvider<?> calculateConformanceProvider(IFhirSystemDao fhirSystemDao, RestfulServer fhirServer, JpaStorageSettings jpaStorageSettings, ISearchParamRegistry searchParamRegistry, IValidationSupport theValidationSupport) {
		FhirVersionEnum fhirVersion = fhirSystemDao.getContext().getVersion().getVersion();
		if (fhirVersion == FhirVersionEnum.R4) {

			JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(fhirServer, fhirSystemDao, jpaStorageSettings, searchParamRegistry, theValidationSupport);
			confProvider.setImplementationDescription("HAPI FHIR R4 Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.R4B) {

			JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(fhirServer, fhirSystemDao, jpaStorageSettings, searchParamRegistry, theValidationSupport);
			confProvider.setImplementationDescription("HAPI FHIR R4B Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.R5) {

			JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(fhirServer, fhirSystemDao, jpaStorageSettings, searchParamRegistry, theValidationSupport);
			confProvider.setImplementationDescription("HAPI FHIR R5 Server");
			return confProvider;
		} else {
			throw new IllegalStateException();
		}
	}
}
