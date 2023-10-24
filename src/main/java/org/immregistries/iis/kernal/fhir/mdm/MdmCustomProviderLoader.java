package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.mdm.api.IMdmControllerSvc;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.api.IMdmSubmitSvc;
import ca.uhn.fhir.mdm.provider.*;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Overrides default Hapi MdmProviderLoader
 * Allows for Activation with FHIR R5
 */
public class MdmCustomProviderLoader extends MdmProviderLoader {
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
	AutowireCapableBeanFactory autowireCapableBeanFactory;

	private BaseMdmProvider myMdmProvider;
	@Override
	public void loadProvider() {
		switch (this.myFhirContext.getVersion().getVersion()) {
			case DSTU3:
			case R4:
			case R5:
				this.myResourceProviderFactory.addSupplier(() -> {
					MdmCustomProvider mdmCustomProvider = new MdmCustomProvider(this.myFhirContext, this.myMdmControllerSvc, this.myMdmControllerHelper, this.myMdmSubmitSvc, this.myMdmSettings);
					autowireCapableBeanFactory.autowireBean(mdmCustomProvider);
					return mdmCustomProvider ;
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
