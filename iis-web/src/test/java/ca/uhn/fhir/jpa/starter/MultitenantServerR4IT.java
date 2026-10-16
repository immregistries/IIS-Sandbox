package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.client.interceptor.UrlTenantSelectionInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class}, properties =
	{
//		"spring.config.import: optional:file:.env.test[.properties]",
		"spring.datasource.url=jdbc:h2:mem:dbr4-mt",
		"spring.profile.active=dev",
		"spring.batch.job.enabled=false",
//		"spring.datasource.username=sa",
//		"spring.datasource.password=null",
//		"spring.datasource.driverClassName=org.h2.Driver",
		"hapi.fhir.fhir_version=r4",
		"hapi.fhir.subscription.websocket_enabled=true",
		"hapi.fhir.cr_enabled=false",
		"springdoc.swagger-ui.enabled=false",
		"hapi.fhir.mdm_enabled=true",
		"hapi.fhir.partitioning.partitioning_include_in_search_hashes=false",
		"hapi.fhir.partitioning.request_tenant_partitioning_mode=true",
		"IIS_MYSQL_URL=jdbc:h2:mem:usersTenants",
		"ENV_IIS_JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
		"jpa.properties.hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect",
		"hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect",

		"server.servlet.context-path=/iis",
	})
class MultitenantServerR4IT {


	private static final String FHIR_USER = "multitenant-it";
	private static final String FHIR_PASSWORD = "MultitenantItPass1";

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(MultitenantServerR4IT.class);
	private static UrlTenantSelectionInterceptor ourClientTenantInterceptor;
	private IGenericClient ourClient;
	private FhirContext ourCtx;
	@LocalServerPort
	private int port;


	@Test
	void testCreateAndReadInTenantA() {


		/*
		 * No explicit $partition-management-create-partition call: this server does not
		 * register HAPI's PartitionManagementProvider (see PartitionModeConfigurer).
		 * PartitionTenantCreationInterceptor creates the partition on first use from the
		 * tenant in the URL instead.
		 */
		ourClientTenantInterceptor.setTenantId("TENANT-A");
		Patient pt = new Patient();
		pt.addName().setFamily("Family A");
		ourClient.create().resource(pt).execute().getId();

		Bundle searchResult = ourClient.search().forResource(Patient.class).returnBundle(Bundle.class).cacheControl(new CacheControlDirective().setNoCache(true)).execute();
		assertEquals(1, searchResult.getEntry().size());
		Patient pt2 = (Patient) searchResult.getEntry().get(0).getResource();
		assertEquals("Family A", pt2.getName().get(0).getFamily());
	}

	@Test
	void testCreateAndReadInTenantB() {


		// The partition is created on first use, see testCreateAndReadInTenantA.
		ourClientTenantInterceptor.setTenantId("TENANT-B");
		Patient pt = new Patient();
		pt.addName().setFamily("Family B");
		ourClient.create().resource(pt).execute().getId();

		Bundle searchResult = ourClient.search().forResource(Patient.class).returnBundle(Bundle.class).cacheControl(new CacheControlDirective().setNoCache(true)).execute();
		assertEquals(1, searchResult.getEntry().size());
		Patient pt2 = (Patient) searchResult.getEntry().get(0).getResource();
		assertEquals("Family B", pt2.getName().get(0).getFamily());
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
		/*
		 * IisAuthorizationInterceptor guards the FHIR endpoint. Self registration is on by
		 * default, so the first call with these credentials creates the account.
		 */
		ourClient.registerInterceptor(new BasicAuthInterceptor(FHIR_USER, FHIR_PASSWORD));
	}


}
