package ca.uhn.fhir.jpa.starter.deprecated;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.deprecated.BaseJpaRestfulServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import javax.servlet.ServletException;

/**
 * Deprecated now dealt with in StarterJpaConfig
 */
@Import(AppProperties.class)
public class JpaRestfulServer extends BaseJpaRestfulServer {

	@Autowired
	AppProperties appProperties;

	private static final long serialVersionUID = 1L;

	public JpaRestfulServer() {
		super();
	}

	@Override
	protected void initialize() throws ServletException {
		super.initialize();

		// Add your own customization here

	}

}
