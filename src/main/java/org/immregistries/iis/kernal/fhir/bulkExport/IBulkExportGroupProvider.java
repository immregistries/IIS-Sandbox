package org.immregistries.iis.kernal.fhir.bulkExport;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IBulkExportGroupProvider<Group extends IBaseResource> extends IResourceProvider {
	String ATR_EXTENSION_URI = "http://hl7.org/fhir/us/davinci-atr/StructureDefinition/atr-any-resource-extension";

	void setDao(IFhirResourceDao<Group> theDao);
}
