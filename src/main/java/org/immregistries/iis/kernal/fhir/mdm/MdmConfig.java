package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.mdm.config.MdmSubmitterConfig;
import ca.uhn.fhir.jpa.searchparam.config.NicknameServiceConfig;
import ca.uhn.fhir.jpa.topic.SubscriptionTopicConfig;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.rules.config.MdmRuleValidator;
import ca.uhn.fhir.mdm.rules.config.MdmSettings;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.immregistries.iis.kernal.fhir.common.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
@Conditional(MdmConfigCondition.class)
@Import({MdmCustomConsumerConfig.class, MdmSubmitterConfig.class, NicknameServiceConfig.class, SubscriptionTopicConfig.class})
public class MdmConfig {
	@Autowired
	AutowireCapableBeanFactory autowireCapableBeanFactory;

	@Primary
	@Bean
	CustomGoldenResourceHelper customGoldenResourceHelper(FhirContext theFhirContext) {
		CustomGoldenResourceHelper customGoldenResourceHelper = new CustomGoldenResourceHelper(theFhirContext);
		autowireCapableBeanFactory.autowireBean(customGoldenResourceHelper);
		return customGoldenResourceHelper;
	}

	@Primary
	@Bean
	CustomSubscriptionCanonicalizer customSubscriptionCanonicalizer(FhirContext theFhirContext) {
		CustomSubscriptionCanonicalizer customSubscriptionCanonicalizer = new CustomSubscriptionCanonicalizer(theFhirContext);
		autowireCapableBeanFactory.autowireBean(customSubscriptionCanonicalizer);
		return customSubscriptionCanonicalizer;
	}

	@Primary
	@Bean
	MdmCustomProviderLoader customMdmProviderLoader() {
		MdmCustomProviderLoader mdmCustomProviderLoader = new MdmCustomProviderLoader();
		autowireCapableBeanFactory.autowireBean(mdmCustomProviderLoader);
		return mdmCustomProviderLoader;
	}

	@Primary
	@Bean
	MdmCustomSubscriptionLoader customMdmSubscriptionLoader() {
		MdmCustomSubscriptionLoader mdmCustomSubscriptionLoader = new MdmCustomSubscriptionLoader();
		autowireCapableBeanFactory.autowireBean(mdmCustomSubscriptionLoader);
		return mdmCustomSubscriptionLoader;
	}


	@Primary
	@Bean
	CustomSubscriptionValidatingInterceptor customSubscriptionValidatingInterceptor() {
		CustomSubscriptionValidatingInterceptor customSubscriptionValidatingInterceptor = new CustomSubscriptionValidatingInterceptor();
		autowireCapableBeanFactory.autowireBean(customSubscriptionValidatingInterceptor);
		return customSubscriptionValidatingInterceptor;
	}

	@Bean
	IMdmSettings mdmSettings(@Autowired MdmRuleValidator theMdmRuleValidator, AppProperties appProperties) throws IOException {
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		Resource resource = resourceLoader.getResource("mdm-rules.json");
		String json = IOUtils.toString(resource.getInputStream(), Charsets.UTF_8);
		return new MdmSettings(theMdmRuleValidator).setEnabled(appProperties.getMdm_enabled()).setScriptText(json);
	}
}
