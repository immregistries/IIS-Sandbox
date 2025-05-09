package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.jpa.mdm.config.MdmSubmitterConfig;
import ca.uhn.fhir.jpa.searchparam.config.NicknameServiceConfig;
import ca.uhn.fhir.jpa.topic.SubscriptionTopicConfig;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.rules.config.MdmRuleValidator;
import ca.uhn.fhir.mdm.rules.config.MdmSettings;
import org.apache.commons.io.IOUtils;
import org.immregistries.iis.kernal.fhir.common.AppProperties;
import org.immregistries.iis.kernal.fhir.mdm.match.MdmIisConsumerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Conditional(MdmConfigCondition.class)
@Import({MdmIisConsumerConfig.class, MdmSubmitterConfig.class, NicknameServiceConfig.class, SubscriptionTopicConfig.class})
public class MdmConfig {
	@Autowired
	AutowireCapableBeanFactory autowireCapableBeanFactory;

//	@Primary
//	@Bean
//	GoldenResourceHelper customGoldenResourceHelper(FhirContext theFhirContext,
//																	IMdmSettings theMdmSettings,
//																	EIDHelper theEIDHelper,
//																	MdmPartitionHelper theMdmPartitionHelper) {
//		MdmIisGoldenResourceHelper mdmIisGoldenResourceHelper = new MdmIisGoldenResourceHelper(theFhirContext, theMdmSettings, theEIDHelper, theMdmPartitionHelper);
//		autowireCapableBeanFactory.autowireBean(mdmIisGoldenResourceHelper);
//		return mdmIisGoldenResourceHelper;
//	}

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
	IisSubscriptionValidatingInterceptor iisSubscriptionValidatingInterceptor() {
		IisSubscriptionValidatingInterceptor iisSubscriptionValidatingInterceptor = new IisSubscriptionValidatingInterceptor();
		autowireCapableBeanFactory.autowireBean(iisSubscriptionValidatingInterceptor);
		return iisSubscriptionValidatingInterceptor;
	}

//	@Primary
//	@Bean
//	MdmIisResourceFilteringSvc mdmCustomResourceFilteringSvc() {
//		MdmIisResourceFilteringSvc mdmCustomResourceFilteringSvc = new MdmIisResourceFilteringSvc();
//		autowireCapableBeanFactory.autowireBean(mdmCustomResourceFilteringSvc);
//		return mdmCustomResourceFilteringSvc;
//	}

}
