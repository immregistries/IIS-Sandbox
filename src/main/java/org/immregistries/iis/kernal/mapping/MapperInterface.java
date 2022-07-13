package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.model.api.IResource;

import java.io.Serializable;

public interface MapperInterface {
	public Serializable getReported(IResource resource);
	public Serializable getMaster(IResource resource);
	public IResource getFhirResource(Serializable master,Serializable reported);
}
