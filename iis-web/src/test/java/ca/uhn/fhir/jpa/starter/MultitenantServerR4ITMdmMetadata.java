package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.client.interceptor.UrlTenantSelectionInterceptor;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.immregistries.iis.kernal.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class}, properties =
	{
		"spring.datasource.url=jdbc:h2:mem:dbr4-mt",
		"hapi.fhir.fhir_version=r4",
		"hapi.fhir.subscription.websocket_enabled=true",
		"hapi.fhir.cr_enabled=false",
		"hapi.fhir.partitioning.partitioning_include_in_search_hashes=false",
		"hapi.fhir.partitioning.request_tenant_partitioning_mode=true",
		"hapi.fhir.mdm_enabled=true",
		"IIS_MYSQL_URL=jdbc:h2:mem/usersTenants",
		"jpa.properties.hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect",
		"hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect",

		"server.servlet.context-path=/iis",
	})
class MultitenantServerR4ITMdmMetadata {


	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(MultitenantServerR4ITMdmMetadata.class);
	private static UrlTenantSelectionInterceptor ourClientTenantInterceptor;
	private IGenericClient ourClient;
	private FhirContext ourCtx;
	@LocalServerPort
	private int port;

	@Autowired
	protected JpaStorageSettings myStorageSettings;

	@Test
	void testReadMetadataNoExpansion() {
		myStorageSettings.setAllowMdmExpansion(false);
		createTenantAndReadMetadata(1, "TENANT-A");
	}

	@Test
	void testReadMetadataExpansion() {
		myStorageSettings.setAllowMdmExpansion(true);
		createTenantAndReadMetadata(2, "TENANT-B");
	}

	@Test
	void testReadMetadataNoExpansion2() {
		myStorageSettings.setAllowMdmExpansion(false);
		createTenantAndReadMetadata(3, "TENANT-C");
	}


	private void createTenantAndReadMetadata(int tenantId, String tenantName) {
		// Create tenant
		ourClientTenantInterceptor.setTenantId("DEFAULT");
		ourClient
			.operation()
			.onServer()
			.named(ProviderConstants.PARTITION_MANAGEMENT_CREATE_PARTITION)
			.withParameter(Parameters.class, ProviderConstants.PARTITION_MANAGEMENT_PARTITION_ID, new IntegerType(tenantId))
			.andParameter(ProviderConstants.PARTITION_MANAGEMENT_PARTITION_NAME, new CodeType(tenantName))
			.execute();

		ourClientTenantInterceptor.setTenantId(tenantName);
		ourClient.search().byUrl("metadata").execute();
	}

	@BeforeEach
	void beforeEach() {


		ourClientTenantInterceptor = new UrlTenantSelectionInterceptor();
		ourCtx = FhirContext.forR4();
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/iis/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
		ourClient.registerInterceptor(new LoggingInterceptor(true));
		ourClient.registerInterceptor(ourClientTenantInterceptor);
	}
}
