package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.mdm.api.IMdmControllerSvc;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.api.IMdmSubmitSvc;
import ca.uhn.fhir.mdm.provider.BaseMdmProvider;
import ca.uhn.fhir.mdm.provider.MdmControllerHelper;
import ca.uhn.fhir.mdm.provider.MdmLinkHistoryProviderDstu3Plus;
import ca.uhn.fhir.mdm.provider.MdmProviderLoader;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Overrides default Hapi MdmProviderLoader
 * Allows for Activation with FHIR R5
 */
public class MdmIisProviderLoader extends MdmProviderLoader {
	@Autowired
	FhirContext myFhirContext;
	@Autowired
	ResourceProviderFactory myResourceProviderFactory;
	@Autowired
	MdmControllerHelper myMdmControllerHelper;
	@Autowired
	IMdmControllerSvc myMdmControllerSvc;
	@Autowired
	IMdmSubmitSvc myMdmSubmitSvc;
	@Autowired
	IMdmSettings myMdmSettings;
	@Autowired
	JpaStorageSettings myStorageSettings;

	@Autowired
	AutowireCapableBeanFactory autowireCapableBeanFactory;

	private BaseMdmProvider myMdmProvider;
	@Override
	public void loadProvider() {
		switch (this.myFhirContext.getVersion().getVersion()) {
			case DSTU3:
			case R4:
			case R5:
				this.myResourceProviderFactory.addSupplier(() -> {
					MdmIisProvider mdmIisProvider = new MdmIisProvider(this.myFhirContext, this.myMdmControllerSvc, this.myMdmControllerHelper, this.myMdmSubmitSvc, this.myMdmSettings);
					autowireCapableBeanFactory.autowireBean(mdmIisProvider);
					return mdmIisProvider;
				});
				if (this.myStorageSettings.isNonResourceDbHistoryEnabled()) {
					this.myResourceProviderFactory.addSupplier(() -> {
						return new MdmLinkHistoryProviderDstu3Plus(this.myFhirContext, this.myMdmControllerSvc);
					});
				}

				return;
			default:
				String var10002 = Msg.code(1497);
				throw new ConfigurationException(var10002 + "MDM not supported for FHIR version " + this.myFhirContext.getVersion().getVersion());
		}
	}

}
