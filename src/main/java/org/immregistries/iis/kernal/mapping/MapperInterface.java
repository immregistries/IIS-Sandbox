package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.model.api.IResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.DomainResource;

import java.io.Serializable;

public interface MapperInterface {
	public Serializable getReported(DomainResource resource);
	public Serializable getMaster(DomainResource resource);
	public DomainResource getFhirResource(Serializable master,Serializable reported);
}
