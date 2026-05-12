package org.immregistries.iis.kernal.logic.shlink;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IIdType;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.persisted.entities.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShLinkManifestGenerator {


	@Autowired
	FhirContext fhirContext;

	public ShLinkManifest generateExamplePatientManifest(Tenant tenant, PatientMaster patientMaster) {
		String patientLocation = "/fhir/" + tenant.getOrganizationName() + "/Patient/" + patientMaster.getPatientId();
		return generateManifest(tenant, patientLocation);
	}

	public ShLinkManifest generateExamplePatientManifest(Tenant tenant, IIdType iIdType) {
		String patientLocation = "/fhir/" + tenant.getOrganizationName() + "/Patient/" + iIdType.getIdPart();
		return generateManifest(tenant, patientLocation);
	}

	public ShLinkManifest generateExamplePatientIpsManifest(Tenant tenant, IIdType iIdType) {
		String patientLocation = "/fhir/" + tenant.getOrganizationName() + "/Patient/" + iIdType.getIdPart() + "/$summary";
		return generateManifest(tenant, patientLocation);
	}

	public ShLinkManifest generateManifest(Tenant tenant, String fhirLocation) {
		ShLinkManifest shLinkManifest = generateManifest(tenant);
		ShLinkManifest.FileManifest fileManifest = generateFhirFileManifest();
		fileManifest.setLocation(fhirLocation);
		shLinkManifest.addFiles(fileManifest);
		return shLinkManifest;
	}

	public ShLinkManifest generateManifest(Tenant tenant) {
		ShLinkManifest shLinkManifest = new ShLinkManifest();
		shLinkManifest.setTenant(tenant);
		shLinkManifest.setStatus("finalized");
		return shLinkManifest;
	}

	private ShLinkManifest.@NotNull FileManifest generateFhirFileManifest() {
		ShLinkManifest.FileManifest fileManifest = new ShLinkManifest.FileManifest();
		String fhirVersion = fhirContext.getVersion().getVersion().getFhirVersionString();
		fileManifest.setContentType("application/fhir+json;fhirVersion=" + fhirVersion);
		return fileManifest;
	}

}
