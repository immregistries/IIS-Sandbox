package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.mapping.mappers.IisMapper;
import org.immregistries.iis.kernal.mapping.mappers.fields.IFieldMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.IisResourceMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.model.IisMappedToFhir;
import org.immregistries.iis.kernal.model.IisMappedToFhirField;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
public class MapperRegistry {

	@SuppressWarnings("rawtypes")
	@Autowired
	private List<IisResourceMasterReportedMapper> mapperMastersReported;
	@SuppressWarnings("rawtypes")
	@Autowired
	private List<IisResourceMapper> mapperMasters;
	@SuppressWarnings("rawtypes")
	@Autowired
	private List<IFieldMapper> fieldMappers;

	@SuppressWarnings("rawtypes")
	@Autowired
	private List<IisMapper> allMappers;


	@SuppressWarnings("rawtypes")
	public IisMapper mapper(IisMappedToFhir internal) {
		Class<? extends IisMappedToFhir> inteClass = internal.getClass();
		Optional<IisResourceMasterReportedMapper> reportedMapper = masterReportedMappersFiltered(inteClass).findFirst();
		if (reportedMapper.isPresent()) {
			return reportedMapper.get();
		}
		return mappersFiltered(inteClass)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(inteClass.getName()));
	}

	@SuppressWarnings("rawtypes")
	public IisMapper mapper(IBase iBase) {
		String fhirType = iBase.fhirType();
		return allMappersFiltered(fhirType)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(fhirType));
	}

	@SuppressWarnings("rawtypes")
	public IisResourceMapper resourceMapper(IisMappedToFhirResource internal) {
		Class<? extends IisMappedToFhirResource> inteClass = internal.getClass();
		Optional<IisResourceMasterReportedMapper> reportedMapper = masterReportedMappersFiltered(inteClass).findFirst();
		if (reportedMapper.isPresent()) {
			return reportedMapper.get();
		}
		return resourceMappersFiltered(inteClass)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(inteClass.getName()));
	}

	@SuppressWarnings("rawtypes")
	public IisResourceMapper resourceMapper(IAnyResource resource) {
		String fhirType = resource.fhirType();
		return resourceMappersFiltered(fhirType)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(fhirType));
	}

	@SuppressWarnings("rawtypes")
	public IFieldMapper fieldMapper(IBaseDatatype datatype) {
		String fhirType = datatype.fhirType();
		return fieldMappersFiltered(fhirType)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(fhirType));
	}

	@SuppressWarnings("rawtypes")
	public IFieldMapper fieldMapper(IisMappedToFhirField internal) {
		Class<? extends IisMappedToFhirField> aClass = internal.getClass();
		return fieldMappersFiltered(aClass)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(aClass.getName()));
	}

	@SuppressWarnings("rawtypes")
	public IisResourceMasterReportedMapper masterReportedMapper(IAnyResource resource) {
		String fhirType = resource.fhirType();
		return masterReportedMappersFiltered(fhirType)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(fhirType));
	}

	private @NotNull Supplier<RuntimeException> mapperNotFoundExceptionSupplier(String fhirType) {
		return () -> new RuntimeException("Mapper not found for " + fhirType);
	}

	private @NotNull Stream<IFieldMapper> fieldMappersFiltered(Class<? extends IisMappedToFhirField> inteClass) {
		return fieldMappers.stream().filter(mapper -> mapper.localType().equals(inteClass));
	}

	private @NotNull Stream<IFieldMapper> fieldMappersFiltered(String fhirType) {
		return fieldMappers.stream().filter(mapper -> fhirType.equals(mapper.fhirTypeName()));
	}

	private @NotNull Stream<IisResourceMasterReportedMapper> masterReportedMappersFiltered(String fhirType) {
		return mapperMastersReported.stream().filter(mapper -> mapper.fhirTypeName().equals(fhirType));
	}

	private @NotNull Stream<IisResourceMapper> resourceMappersFiltered(String fhirType) {
		return mapperMasters.stream().filter(mapper -> mapper.fhirTypeName().equals(fhirType));
	}

	private @NotNull Stream<IisMapper> allMappersFiltered(String fhirType) {
		return allMappers.stream().filter(mapper -> mapper.fhirTypeName().equals(fhirType));
	}

	private @NotNull Stream<IisResourceMasterReportedMapper> masterReportedMappersFiltered(Class<? extends IisMappedToFhir> inteClass) {
		return mapperMastersReported.stream().filter(mapper -> mapper.localReportedType().equals(inteClass) || mapper.localMasterType().equals(inteClass));
	}

	private @NotNull Stream<IisResourceMapper> resourceMappersFiltered(Class<? extends IisMappedToFhir> inteClass) {
		return mapperMasters.stream().filter(mapper -> mapper.localType().equals(inteClass));
	}

	private @NotNull Stream<IisResourceMapper> mappersFiltered(Class<? extends IisMappedToFhir> inteClass) {
		return mapperMasters.stream().filter(mapper -> mapper.localType().equals(inteClass));
	}

}
