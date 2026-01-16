package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.mapping.fieldsMappers.IFieldMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMasterReportedMapper;
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
public class MapperRegistry {

	@Autowired
	private FhirContext fhirContext;


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
	public IisResourceMapper mapper(IisMappedToFhirResource internal) {
		Class inteClass = internal.getClass();
		Optional<IisResourceMasterReportedMapper> reportedMapper = masterReportedMappersFiltered(inteClass).findFirst();
		if (reportedMapper.isPresent()) {
			return reportedMapper.get();
		}
		Optional<IisResourceMapper> masterMapper = masterMappersFiltered(inteClass).findFirst();
		return masterMapper.orElseThrow(mapperNotFoundExceptionSupplier(inteClass.getName()));
	}

	@SuppressWarnings("rawtypes")
	public IisResourceMapper mapper(IAnyResource resource) {
		String fhirType = resource.fhirType();
		return masterMappersFiltered(fhirType)
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
	public IFieldMapper fieldMapper(IisMappedToFhir internal) {
		Class<? extends IisMappedToFhir> aClass = internal.getClass();
		return fieldMappersFiltered(aClass)
			.findFirst()
			.orElseThrow(mapperNotFoundExceptionSupplier(aClass.getName()));
	}

	@SuppressWarnings("rawtypes")
	public IisResourceMasterReportedMapper mapperReported(IAnyResource resource) {
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
		return fieldMappers.stream().filter(mapper -> fhirType.equals(mapper.fhirTypeName()));
	}

	private @NotNull Stream<IisResourceMasterReportedMapper> masterReportedMappersFiltered(String fhirType) {
		return mapperMastersReported.stream().filter(mapper -> mapper.fhirResourceName().equals(fhirType));
	}

	private @NotNull Stream<IisResourceMapper> masterMappersFiltered(String fhirType) {
		return mapperMasters.stream().filter(mapper -> mapper.fhirResourceName().equals(fhirType));
	}

	private @NotNull Stream<IisResourceMasterReportedMapper> masterReportedMappersFiltered(Class inteClass) {
		return mapperMastersReported.stream().filter(mapper -> mapper.localReportedType().equals(inteClass) || mapper.localMasterType().equals(inteClass));
	}

	private @NotNull Stream<IisResourceMapper> masterMappersFiltered(Class inteClass) {
		return mapperMasters.stream().filter(mapper -> mapper.localType().equals(inteClass));
	}

}
