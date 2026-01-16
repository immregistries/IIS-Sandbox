package org.immregistries.iis.kernal.fhir.bulk;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IAnyResource;

/**
 * Custom group provider to extend support for Bulk operation and member-add & member-remove
 *
 * @param <Group> FHIR Group class
 */
public interface IBulkExportGroupProvider<Group extends IAnyResource> extends IResourceProvider {
	String ATR_EXTENSION_URI = "http://hl7.org/fhir/us/davinci-atr/StructureDefinition/atr-any-resource-extension";

	void setDao(IFhirResourceDao<Group> theDao);
}
