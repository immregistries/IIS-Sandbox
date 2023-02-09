package org.immregistries.iis.kernal.fhir.cql;

import ca.uhn.fhir.cql.config.CqlR4Config;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

@Conditional({OnR4Condition.class, CqlConfigCondition.class})
@Import({CqlR4Config.class})
public class StarterCqlR4Config {
}
