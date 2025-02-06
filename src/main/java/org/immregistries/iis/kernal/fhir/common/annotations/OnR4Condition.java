package org.immregistries.iis.kernal.fhir.common.annotations;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnR4Condition implements Condition {
  @Override
  public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata metadata) {
	  FhirVersionEnum version;
	  String envVersion = System.getenv("FHIR_VERSION");
	  if (StringUtils.isNotBlank(envVersion)) {
		  version = FhirVersionEnum.forVersionString(envVersion.toUpperCase());
	  } else {
		  version = FhirVersionEnum.forVersionString(conditionContext.
			  getEnvironment()
			  .getProperty("hapi.fhir.fhir_version")
			  .toUpperCase());
	  }

    return version == FhirVersionEnum.R4;

  }
}
