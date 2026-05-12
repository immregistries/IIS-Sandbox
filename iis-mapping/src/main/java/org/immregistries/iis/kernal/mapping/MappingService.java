package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.model.api.IElement;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.mapping.mappers.IisMapper;
import org.immregistries.iis.kernal.mapping.mappers.fields.IFieldMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.IisResourceMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.model.IisDiffable;
import org.immregistries.iis.kernal.model.IisMappedToFhir;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO Simplify
 */
@Service
public class MappingService {

	@Autowired
	MapperRegistry mapperRegistry;


	@SuppressWarnings("unchecked")
	public IAnyResource fhirResource(IisMappedToFhirResource internal) {
		@SuppressWarnings("rawtypes")
		IisResourceMapper mapper = mapperRegistry.resourceMapper(internal);
		return mapper.fhirObject(internal);
	}

	@SuppressWarnings("unchecked")
	public IElement fhirObject(IisMappedToFhir internal) {
		@SuppressWarnings("rawtypes")
		IisMapper mapper = mapperRegistry.mapper(internal);
		return mapper.fhirObject(internal);
	}

//    @SuppressWarnings("unchecked")
//	 public IAnyResource fhir(IisDiffableObject internal) {
//        @SuppressWarnings("rawtypes")
//        IisResourceMasterMapper mapper = selectMapper();
//        return mapper.fhirResource(internal);
//    }

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObject(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMapper mapper = mapperRegistry.resourceMapper(resource);
		return mapper.localObject(resource);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhir localObject(IElement iElement) {
		@SuppressWarnings("rawtypes")
		IisMapper mapper = mapperRegistry.mapper(iElement);
		return mapper.localObject(iElement);
	}

	@SuppressWarnings("unchecked")
	public IisDiffable localField(IBaseDatatype datatype) {
		@SuppressWarnings("rawtypes")
		IFieldMapper mapper = mapperRegistry.fieldMapper(datatype);
		return mapper.localObject(datatype);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectReportedWithMaster(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = mapperRegistry.masterReportedMapper(resource);
		return mapper.localObjectReportedWithMaster(resource);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectReported(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = mapperRegistry.masterReportedMapper(resource);
		return mapper.localObjectReported(resource);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectMaster(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = mapperRegistry.masterReportedMapper(resource);
		return mapper.localObjectMaster(resource);
	}

}
