package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.entity.PartitionEntity;
import ca.uhn.fhir.jpa.partition.IPartitionLookupSvc;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.util.ITestingUiClientFactory;
import ca.uhn.fhir.to.FhirTesterMvcConfig;
import ca.uhn.fhir.to.TesterConfig;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.servlet.http.HttpServletRequest;

//@formatter:off
/**
 * This spring config file configures the web testing module. It serves two
 * purposes:
 * 1. It imports FhirTesterMvcConfig, which is the spring config for the
 *    tester itself
 * 2. It tells the tester which server(s) to talk to, via the testerConfig()
 *    method below
 */
@Configuration
@Import(FhirTesterMvcConfig.class)
public class FhirTesterConfig {

	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	IPartitionLookupSvc partitionLookupSvc;
	@Autowired
	PartitionManagementProvider partitionManagementProvider;

	/**
	 * This bean tells the testing webpage which servers it should configure itself
	 * to communicate with. In this example we configure it to talk to the local
	 * server, as well as one public server. If you are creating a project to
	 * deploy somewhere else, you might choose to only put your own server's
	 * address here.
	 *
	 * Note the use of the ${serverBase} variable below. This will be replaced with
	 * the base URL as reported by the server itself. Often for a simple Tomcat
	 * (or other container) installation, this will end up being something
	 * like "http://localhost:8080/hapi-fhir-jpaserver-starter". If you are
	 * deploying your server to a place with a fully qualified domain name,
	 * you might want to use that instead of using the variable.
	 */
  @Bean
  public TesterConfig testerConfig(AppProperties appProperties) {
    TesterConfig retVal = new TesterConfig();
//    appProperties.getTester().entrySet().stream().forEach(t -> {
//      retVal
//        .addServer()
//        .withId(t.getKey())
//        .withFhirVersion(t.getValue().getFhir_version())
//        .withBaseUrl(t.getValue().getServer_address())
//        .withName(t.getValue().getName());
//      retVal.setRefuseToFetchThirdPartyUrls(
//        t.getValue().getRefuse_to_fetch_third_party_urls());
//
//    });
//	  for (PartitionEntity partitionEntity: partitionLookupSvc.listPartitions()) {
//		  retVal.addServer()
//			  .withId(String.valueOf(partitionEntity.getId()))
//			  .withFhirVersion(appProperties.getFhir_version())
//			  .withBaseUrl(appProperties.getServer_address() + "" + partitionEntity.getName())
//			  .withName(partitionEntity.getName());
//	  }

//	  retVal.setClientFactory(repositoryClientFactory);
    return retVal;
  }

}
//@formatter:on
