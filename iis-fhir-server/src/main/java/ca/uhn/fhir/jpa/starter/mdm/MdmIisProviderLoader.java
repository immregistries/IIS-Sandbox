package ca.uhn.fhir.jpa.starter.mdm;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.mdm.api.IMdmControllerSvc;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.api.IMdmSubmitSvc;
import ca.uhn.fhir.mdm.provider.*;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Supplier;

/**
 * Overrides default Hapi MdmProviderLoader
 * Allows for Activation with FHIR R5
 */
public class MdmIisProviderLoader extends MdmProviderLoader {
	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private ResourceProviderFactory myResourceProviderFactory;

	@Autowired
	private MdmControllerHelper myMdmControllerHelper;

	@Autowired
	private IMdmControllerSvc myMdmControllerSvc;

	@Autowired
	private IMdmSubmitSvc myMdmSubmitSvc;

	@Autowired
	private IMdmSettings myMdmSettings;

	@Autowired
	private JpaStorageSettings myStorageSettings;

	@Autowired
	private IInterceptorBroadcaster myInterceptorBroadcaster;

	private Supplier<Object> myMdmProviderSupplier;
	private Supplier<Object> myPatientMatchProviderSupplier;
	private Supplier<Object> myMdmHistoryProviderSupplier;

	public void loadPatientMatchProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
			case R4:
			case R5:
				// We store the supplier so that removeSupplier works properly
				myPatientMatchProviderSupplier = () -> new PatientMatchProvider(myMdmControllerHelper);
				myResourceProviderFactory.addSupplier(myPatientMatchProviderSupplier);
				break;
			default:
				throw new ConfigurationException(Msg.code(2574) + "Patient/$match not supported for FHIR version "
						+ myFhirContext.getVersion().getVersion());
		}
	}

	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
			case R4:
			case R5:
				// We store the supplier so that removeSupplier works properly
				myMdmProviderSupplier = () -> new MdmProviderDstu3Plus(
						myFhirContext,
						myMdmControllerSvc,
						myMdmControllerHelper,
						myMdmSubmitSvc,
						myInterceptorBroadcaster,
						myMdmSettings);
				myResourceProviderFactory.addSupplier(myMdmProviderSupplier);
				if (myStorageSettings.isNonResourceDbHistoryEnabled()) {
					myMdmHistoryProviderSupplier = () -> new MdmLinkHistoryProviderDstu3Plus(
							myFhirContext, myMdmControllerSvc, myInterceptorBroadcaster);
					myResourceProviderFactory.addSupplier(myMdmHistoryProviderSupplier);
				}
				break;
			default:
				throw new ConfigurationException(Msg.code(1497) + "MDM not supported for FHIR version "
						+ myFhirContext.getVersion().getVersion());
		}
	}

	@PreDestroy
	public void unloadProvider() {
		if (myMdmProviderSupplier != null) {
			myResourceProviderFactory.removeSupplier(myMdmProviderSupplier);
		}
		if (myMdmHistoryProviderSupplier != null) {
			myResourceProviderFactory.removeSupplier(myMdmHistoryProviderSupplier);
		}
		if (myPatientMatchProviderSupplier != null) {
			myResourceProviderFactory.removeSupplier(myPatientMatchProviderSupplier);
		}
	}

}
