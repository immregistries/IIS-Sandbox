package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.mapping.fieldsMappers.IFieldMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMasterMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.model.IisDiffableObject;
import org.immregistries.iis.kernal.model.IisMappedToFhir;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
public class AllMappingService {
	@Autowired
	private FhirContext fhirContext;

	@Autowired
	MapperRegistry mapperRegistry;


	@SuppressWarnings("unchecked")
	public IAnyResource fhirResource(IisMappedToFhirResource internal) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterMapper mapper = mapperRegistry.selectMapper(internal);
		return mapper.fhirResource(internal);
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
		IisResourceMasterMapper mapper = mapperRegistry.selectMapper(resource);
		return mapper.localObject(resource);
	}

	@SuppressWarnings("unchecked")
	public IisDiffableObject localObject(IBaseDatatype datatype) {
		@SuppressWarnings("rawtypes")
		IFieldMapper mapper = mapperRegistry.selectMapper(datatype);
		return mapper.localObject(datatype);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectReportedWithMaster(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = mapperRegistry.selectMapperReported(resource);
		return mapper.localObjectReportedWithMaster(resource);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectReported(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = mapperRegistry.selectMapperReported(resource);
		return mapper.localObjectReported(resource);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectMaster(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = mapperRegistry.selectMapperReported(resource);
		return mapper.localObjectMaster(resource);
	}

}
