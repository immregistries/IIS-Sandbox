package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.mapping.fieldsMappers.IFieldMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.model.IisDiffableObject;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MappingService {

	@Autowired
	MapperRegistry mapperRegistry;


	@SuppressWarnings("unchecked")
	public IAnyResource fhirResource(IisMappedToFhirResource internal) {
		@SuppressWarnings("rawtypes")
		IisResourceMapper mapper = mapperRegistry.mapper(internal);
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
		IisResourceMapper mapper = mapperRegistry.mapper(resource);
		return mapper.localObject(resource);
	}

	@SuppressWarnings("unchecked")
	public IisDiffableObject localField(IBaseDatatype datatype) {
		@SuppressWarnings("rawtypes")
		IFieldMapper mapper = mapperRegistry.fieldMapper(datatype);
		return mapper.localObject(datatype);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectReportedWithMaster(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = mapperRegistry.mapperReported(resource);
		return mapper.localObjectReportedWithMaster(resource);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectReported(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = mapperRegistry.mapperReported(resource);
		return mapper.localObjectReported(resource);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectMaster(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = mapperRegistry.mapperReported(resource);
		return mapper.localObjectMaster(resource);
	}

}
