package org.immregistries.iis.fhir.mdm;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.api.IMdmSurvivorshipService;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.mdm.model.MdmTransactionContext;
import ca.uhn.fhir.mdm.util.EIDHelper;
import ca.uhn.fhir.mdm.util.GoldenResourceHelper;
import ca.uhn.fhir.mdm.util.MdmPartitionHelper;
import jakarta.annotation.Nonnull;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


/**
 * adding all of the source's identifier to goldenResource identifiers
 */
public class MdmIisGoldenResourceHelper extends GoldenResourceHelper {
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	static final String FIELD_NAME_IDENTIFIER = "identifier";

	private final FhirContext myFhirContext;

	@Autowired
	public MdmIisGoldenResourceHelper(
		FhirContext theFhirContext,
		IMdmSettings theMdmSettings,
		EIDHelper theEIDHelper,
		MdmPartitionHelper theMdmPartitionHelper) {
		super(theFhirContext, theMdmSettings, theEIDHelper, theMdmPartitionHelper);
		myFhirContext = theFhirContext;
	}

	/**
	 * Creates a copy of the specified resource. This method will carry over resource EID if it exists. If it does not exist,
	 * a randomly generated UUID EID will be created.
	 *
	 * @param <T>                      Supported MDM resource type (e.g. Patient, Practitioner)
	 * @param theIncomingResource     The resource to build the golden resource off of.
	 *                                 Could be the source resource or another golden resource.
	 *                                 If a golden resource, do not provide an IMdmSurvivorshipService
	 * @param theMdmTransactionContext The mdm transaction context
	 * @param theMdmSurvivorshipService IMdmSurvivorshipSvc. Provide only if survivorshipskills are desired
	 *                                  to be applied. Provide null otherwise.
	 */
	@Nonnull
	@Override
	public <T extends IAnyResource> T createGoldenResourceFromMdmSourceResource(
		T theIncomingResource,
		MdmTransactionContext theMdmTransactionContext,
		IMdmSurvivorshipService theMdmSurvivorshipService) {
		T newGoldenResource = super.createGoldenResourceFromMdmSourceResource(theIncomingResource, theMdmTransactionContext, theMdmSurvivorshipService);
		// get a ref to the actual ID Field
		RuntimeResourceDefinition resourceDefinition = myFhirContext.getResourceDefinition(theIncomingResource);
		// hapi has 2 metamodels: for children and types
		BaseRuntimeChildDefinition goldenResourceIdentifier = resourceDefinition.getChildByName(FIELD_NAME_IDENTIFIER);

		addAllEid(goldenResourceIdentifier, theIncomingResource, newGoldenResource);
		return newGoldenResource;
	}

	/**
	 * Custom method for IIS Sandbox, add all identifiers to Golden resource
	 */
	private <T extends IAnyResource> void addAllEid(
		BaseRuntimeChildDefinition theGoldenResourceIdentifier,
		IAnyResource theIncomingResource,
		IAnyResource theNewGoldenResource) {


		List<IBase> incomingResourceIdentifiers = theGoldenResourceIdentifier.getAccessor().getValues(theIncomingResource);

		ourLog.debug("Adding Identifier to ");

		for (IBase incomingResourceIdentifier : incomingResourceIdentifiers) {
			ca.uhn.fhir.util.TerserUtil.cloneIdentifierIntoResource(
				myFhirContext,
				theGoldenResourceIdentifier,
				incomingResourceIdentifier,
				theNewGoldenResource);
		}
	}


}
