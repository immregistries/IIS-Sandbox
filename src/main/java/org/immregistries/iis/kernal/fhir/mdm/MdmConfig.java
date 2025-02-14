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
@Import({MdmIisConsumerConfig.class, MdmSubmitterConfig.class, NicknameServiceConfig.class, SubscriptionTopicConfig.class})
public class MdmConfig {
	@Autowired
	AutowireCapableBeanFactory autowireCapableBeanFactory;

	@Primary
	@Bean
	MdmIisGoldenResourceHelper customGoldenResourceHelper(FhirContext theFhirContext) {
		MdmIisGoldenResourceHelper mdmIisGoldenResourceHelper = new MdmIisGoldenResourceHelper(theFhirContext);
		autowireCapableBeanFactory.autowireBean(mdmIisGoldenResourceHelper);
		return mdmIisGoldenResourceHelper;
	}

	@Primary
	@Bean
	IisSubscriptionCanonicalizer customSubscriptionCanonicalizer(FhirContext theFhirContext) {
		IisSubscriptionCanonicalizer iisSubscriptionCanonicalizer = new IisSubscriptionCanonicalizer(theFhirContext);
		autowireCapableBeanFactory.autowireBean(iisSubscriptionCanonicalizer);
		return iisSubscriptionCanonicalizer;
	}

	@Primary
	@Bean
	MdmIisProviderLoader customMdmProviderLoader() {
		MdmIisProviderLoader mdmIisProviderLoader = new MdmIisProviderLoader();
		autowireCapableBeanFactory.autowireBean(mdmIisProviderLoader);
		return mdmIisProviderLoader;
	}

	@Primary
	@Bean
	MdmIisSubscriptionLoader customMdmSubscriptionLoader() {
		MdmIisSubscriptionLoader mdmIisSubscriptionLoader = new MdmIisSubscriptionLoader();
		autowireCapableBeanFactory.autowireBean(mdmIisSubscriptionLoader);
		return mdmIisSubscriptionLoader;
	}


	@Primary
	@Bean
	IisSubscriptionValidatingInterceptor customSubscriptionValidatingInterceptor() {
		IisSubscriptionValidatingInterceptor iisSubscriptionValidatingInterceptor = new IisSubscriptionValidatingInterceptor();
		autowireCapableBeanFactory.autowireBean(iisSubscriptionValidatingInterceptor);
		return iisSubscriptionValidatingInterceptor;
	}

	@Bean
	IMdmSettings mdmSettings(@Autowired MdmRuleValidator theMdmRuleValidator, AppProperties appProperties) throws IOException {
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		Resource resource = resourceLoader.getResource("mdm-rules.json");
		String json = IOUtils.toString(resource.getInputStream(), Charsets.UTF_8);
		return new MdmSettings(theMdmRuleValidator).setEnabled(appProperties.getMdm_enabled()).setScriptText(json);
	}

//	@Primary
//	@Bean
//	MdmIisResourceFilteringSvc mdmCustomResourceFilteringSvc() {
//		MdmIisResourceFilteringSvc mdmCustomResourceFilteringSvc = new MdmIisResourceFilteringSvc();
//		autowireCapableBeanFactory.autowireBean(mdmCustomResourceFilteringSvc);
//		return mdmCustomResourceFilteringSvc;
//	}

}
