package ca.uhn.fhir.jpa.starter.subscription;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(OnR5Condition.class)
public class SubscriptionTopicConfig {

	@Bean
	public SubscriptionTopicConfigurer subscriptionTopicConfigurer(DaoRegistry theDaoRegistry) {
		return new SubscriptionTopicConfigurer(theDaoRegistry);
	}

}
