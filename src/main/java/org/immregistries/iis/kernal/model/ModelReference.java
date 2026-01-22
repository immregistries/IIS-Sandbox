package org.immregistries.iis.kernal.model;

public class ModelReference extends IisMappedToFhir {
	private String reference = "";
	private BusinessIdentifier identifier;

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public BusinessIdentifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(BusinessIdentifier identifier) {
		this.identifier = identifier;
	}
}
