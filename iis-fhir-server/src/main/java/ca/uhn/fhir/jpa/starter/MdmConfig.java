package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.mdm.config.MdmSubmitterConfig;
import ca.uhn.fhir.jpa.model.config.SubscriptionSettings;
import ca.uhn.fhir.jpa.partition.IRequestPartitionHelperSvc;
import ca.uhn.fhir.jpa.searchparam.config.NicknameServiceConfig;
import ca.uhn.fhir.jpa.starter.match.MdmIisConsumerConfig;
import ca.uhn.fhir.jpa.subscription.match.matcher.matching.SubscriptionStrategyEvaluator;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionCanonicalizer;
import ca.uhn.fhir.jpa.subscription.submit.interceptor.validator.SubscriptionChannelTypeValidatorFactory;
import ca.uhn.fhir.jpa.subscription.submit.interceptor.validator.SubscriptionQueryValidator;
import ca.uhn.fhir.jpa.topic.SubscriptionTopicConfig;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.interceptor.MdmSearchExpandingInterceptor;
import ca.uhn.fhir.mdm.rules.config.MdmRuleValidator;
import ca.uhn.fhir.mdm.rules.config.MdmSettings;
import ca.uhn.fhir.mdm.util.EIDHelper;
import ca.uhn.fhir.mdm.util.GoldenResourceHelper;
import ca.uhn.fhir.mdm.util.MdmPartitionHelper;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Conditional(MdmConfigCondition.class)
@Import({MdmIisConsumerConfig.class, MdmSubmitterConfig.class, NicknameServiceConfig.class, SubscriptionTopicConfig.class})
public class MdmConfig {

	@Primary
	@Bean
	GoldenResourceHelper customGoldenResourceHelper(FhirContext theFhirContext,
																	IMdmSettings theMdmSettings,
																	EIDHelper theEIDHelper,
																	MdmPartitionHelper theMdmPartitionHelper) {
		return new MdmIisGoldenResourceHelper(theFhirContext, theMdmSettings, theEIDHelper, theMdmPartitionHelper);
	}

//	@Primary
//	@Bean
//	MdmIisProviderLoader customMdmProviderLoader() {
//		MdmIisProviderLoader mdmProviderLoader = new MdmIisProviderLoader();
//		autowireCapableBeanFactory.autowireBean(mdmProviderLoader);
//		return mdmProviderLoader;
//	}
//

	@Bean
	IMdmSettings mdmSettings(@Autowired MdmRuleValidator theMdmRuleValidator, AppProperties appProperties)
		throws IOException {
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		Resource resource = resourceLoader.getResource(appProperties.getMdm_rules_json_location());
		String json = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
		return new MdmSettings(theMdmRuleValidator)
			.setEnabled(appProperties.getMdm_enabled())
			.setScriptText(json);
	}

	@Primary
	@Bean
	IisSubscriptionValidatingInterceptor iisSubscriptionValidatingInterceptor(DaoRegistry myDaoRegistry, SubscriptionSettings mySubscriptionSettings, SubscriptionStrategyEvaluator mySubscriptionStrategyEvaluator, SubscriptionCanonicalizer mySubscriptionCanonicalizer, FhirContext myFhirContext, IRequestPartitionHelperSvc myRequestPartitionHelperSvc, SubscriptionQueryValidator mySubscriptionQueryValidator, SubscriptionChannelTypeValidatorFactory mySubscriptionChannelTypeValidatorFactory) {
		IisSubscriptionValidatingInterceptor iisSubscriptionValidatingInterceptor = new IisSubscriptionValidatingInterceptor(myDaoRegistry,mySubscriptionSettings,mySubscriptionStrategyEvaluator,mySubscriptionCanonicalizer,myFhirContext,myRequestPartitionHelperSvc,mySubscriptionQueryValidator,mySubscriptionChannelTypeValidatorFactory);
		return iisSubscriptionValidatingInterceptor;
	}

	/**
	 * Overriding MdmSearchExpandingInterceptor for metadata search support with multitenancy
	 *
	 * @return
	 */
	@Primary
	@Bean
	@Lazy
	MdmSearchExpandingInterceptor mdmIisSearchExpandingInterceptor() {
		return new MdmIisSearchExpandingInterceptor();
	}

}
