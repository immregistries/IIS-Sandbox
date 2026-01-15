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

	@SuppressWarnings("rawtypes")
	@Autowired
	private List<IisResourceMasterReportedMapper> mapperMastersReported;
	@SuppressWarnings("rawtypes")
	@Autowired
	private List<IisResourceMasterMapper> mapperMasters;
	@SuppressWarnings("rawtypes")
	@Autowired
	private List<IFieldMapper> fieldMappers;

	@SuppressWarnings("unchecked")
	public IAnyResource fhirResource(IisMappedToFhirResource internal) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterMapper mapper = selectMapper(internal);
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
		IisResourceMasterMapper mapper = selectMapper(resource);
		return mapper.localObject(resource);
	}

	@SuppressWarnings("unchecked")
	public IisDiffableObject localObject(IBaseDatatype datatype) {
		@SuppressWarnings("rawtypes")
		IFieldMapper mapper = selectMapper(datatype);
		return mapper.localObject(datatype);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectReportedWithMaster(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = selectMapperReported(resource);
		return mapper.localObjectReportedWithMaster(resource);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectReported(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = selectMapperReported(resource);
		return mapper.localObjectReported(resource);
	}

	@SuppressWarnings("unchecked")
	public IisMappedToFhirResource localObjectMaster(IAnyResource resource) {
		@SuppressWarnings("rawtypes")
		IisResourceMasterReportedMapper mapper = selectMapperReported(resource);
		return mapper.localObjectMaster(resource);
	}

	@SuppressWarnings("rawtypes")
	public IisResourceMasterMapper selectMapper(IisMappedToFhirResource internal) {
		Class inteClass = internal.getClass();
		Optional<IisResourceMasterReportedMapper> reportedMapper = masterReportedMappersFiltered(inteClass).findFirst();
		if (reportedMapper.isPresent()) {
			return reportedMapper.get();
		}
		Optional<IisResourceMasterMapper> masterMapper = masterMappersFiltered(inteClass).findFirst();
		return masterMapper.orElseThrow(mapperNotFoundExceptionSupplier(inteClass.getName()));
	}

	@SuppressWarnings("rawtypes")
	public IisResourceMasterMapper selectMapper(IAnyResource resource) {
		String fhirType = resource.fhirType();
		return masterMappersFiltered(fhirType)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(fhirType));
	}

	@SuppressWarnings("rawtypes")
	public IFieldMapper selectMapper(IBaseDatatype datatype) {
		String fhirType = datatype.fhirType();
		return fieldMappersFiltered(fhirType)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(fhirType));
	}

	@SuppressWarnings("rawtypes")
	public IFieldMapper selectFieldMapper(IisMappedToFhir internal) {
		Class<? extends IisMappedToFhir> aClass = internal.getClass();
		return fieldMappersFiltered(aClass)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(aClass.getName()));
	}

	@SuppressWarnings("rawtypes")
	public IisResourceMasterReportedMapper selectMapperReported(IAnyResource resource) {
		String fhirType = resource.fhirType();
		return masterReportedMappersFiltered(fhirType)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(fhirType));
	}

	private @NotNull Supplier<RuntimeException> mapperNotFoundExceptionSupplier(String fhirType) {
		return () -> new RuntimeException("Mapper not found for " + fhirType);
	}

	private @NotNull Stream<IFieldMapper> fieldMappersFiltered(Class inteClass) {
		return fieldMappers.stream().filter(mapper -> mapper.localType().equals(inteClass));
	}

	private @NotNull Stream<IFieldMapper> fieldMappersFiltered(String fhirType) {
		return fieldMappers.stream().filter(mapper -> fhirType.equals(mapper.fhirType()));
	}

	private @NotNull Stream<IisResourceMasterReportedMapper> masterReportedMappersFiltered(String fhirType) {
		return mapperMastersReported.stream().filter(mapper -> mapper.fhirType().equals(fhirType));
	}

	private @NotNull Stream<IisResourceMasterMapper> masterMappersFiltered(String fhirType) {
		return mapperMasters.stream().filter(mapper -> mapper.fhirType().equals(fhirType));
	}

	private @NotNull Stream<IisResourceMasterReportedMapper> masterReportedMappersFiltered(Class inteClass) {
		return mapperMastersReported.stream().filter(mapper -> mapper.localReportedType().equals(inteClass) || mapper.localMasterType().equals(inteClass));
	}

	private @NotNull Stream<IisResourceMasterMapper> masterMappersFiltered(Class inteClass) {
		return mapperMasters.stream().filter(mapper -> mapper.localType().equals(inteClass));
	}
}
