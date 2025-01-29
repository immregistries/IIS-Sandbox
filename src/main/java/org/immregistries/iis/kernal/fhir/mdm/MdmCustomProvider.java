package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.mdm.api.IMdmControllerSvc;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.api.IMdmSubmitSvc;
import ca.uhn.fhir.mdm.provider.MdmControllerHelper;
import ca.uhn.fhir.mdm.provider.MdmProviderDstu3Plus;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MdmCustomProvider extends MdmProviderDstu3Plus {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());


	@Autowired
	FhirContext myFhirContext;
	@Autowired
	ResourceProviderFactory myResourceProviderFactory;
	@Autowired
	MdmControllerHelper myMdmControllerHelper;
	@Autowired
	private IMdmControllerSvc myMdmControllerSvc;
	@Autowired
	private IMdmSubmitSvc myMdmSubmitSvc;
	@Autowired
	private IMdmSettings myMdmSettings;

	public MdmCustomProvider(FhirContext theFhirContext, IMdmControllerSvc theMdmControllerSvc, MdmControllerHelper theMdmHelper, IMdmSubmitSvc theMdmSubmitSvc, IMdmSettings theIMdmSettings) {
		super(theFhirContext, theMdmControllerSvc, theMdmHelper, theMdmSubmitSvc, theIMdmSettings);
	}

	@Operation(name = ProviderConstants.EMPI_MATCH, typeName = "Immunization")
	public IBaseBundle immunizationMatch(@OperationParam(name = ProviderConstants.MDM_MATCH_RESOURCE, min = 1, max = 1, typeName = "Immunization") IAnyResource theImmunization,
									 RequestDetails theRequestDetails) {
		if (theImmunization == null) {
			throw new InvalidRequestException(Msg.code(1498) + "resource may not be null");
		}
		return myMdmControllerHelper.getMatchesAndPossibleMatchesForResource(theImmunization, "Immunization", theRequestDetails);
	}
}
