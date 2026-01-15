package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.mapping.fieldsMappers.IFieldMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMasterMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.model.IisDiffableObject;
import org.immregistries.iis.kernal.model.IisMappedObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AllMappingService {

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
	 public IBaseResource fhirResource(IisMappedObject internal) {
        @SuppressWarnings("rawtypes")
        IisResourceMasterMapper mapper = selectMapper(internal);
        return mapper.fhirResource(internal);
    }

    @SuppressWarnings("unchecked")
	 public IisMappedObject localObject(IBaseResource resource) {
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
	 public IisMappedObject localObjectReportedWithMaster(IBaseResource resource) {
        @SuppressWarnings("rawtypes")
        IisResourceMasterReportedMapper mapper = selectMapperReported(resource);
        return mapper.localObjectReportedWithMaster(resource);
    }

    @SuppressWarnings("unchecked")
	 public IisMappedObject localObjectReported(IBaseResource resource) {
        @SuppressWarnings("rawtypes")
        IisResourceMasterReportedMapper mapper = selectMapperReported(resource);
        return mapper.localObjectReported(resource);
    }

    @SuppressWarnings("rawtypes")
	 public IisResourceMasterMapper selectMapper(IisMappedObject internal) {
		 Class inteClass = internal.getClass();
		 Optional<IisResourceMasterReportedMapper> reportedMapper = mapperMastersReported.stream().filter(mapper -> mapper.localReportedType().equals(inteClass)).findFirst();
		 if (reportedMapper.isPresent()) {
			 return reportedMapper.get();
        }
		 Optional<IisResourceMasterMapper> masterMapper = mapperMasters.stream().filter(mapper -> mapper.localMasterType().equals(inteClass)).findFirst();
		 return masterMapper.orElseThrow(() -> new RuntimeException("Mapper not found for " + inteClass.getName()));
    }

    @SuppressWarnings("rawtypes")
    public IisResourceMasterMapper selectMapper(IBaseResource resource) {
        String fhirType = resource.fhirType();
        return mapperMasters.stream().filter(mapper -> mapper.fhirType().equals(fhirType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Mapper not found for " + fhirType));
    }

	@SuppressWarnings("rawtypes")
	public IFieldMapper selectMapper(IBaseDatatype datatype) {
		String fhirType = datatype.fhirType();
		return fieldMappers.stream().filter(mapper -> mapper.r4Type().equals(fhirType) || mapper.r5Type().equals(fhirType))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Mapper not found for " + fhirType));
	}

    @SuppressWarnings("rawtypes")
    public IisResourceMasterReportedMapper selectMapperReported(IBaseResource resource) {
        String fhirType = resource.fhirType();
        return mapperMastersReported.stream().filter(mapper -> mapper.fhirType().equals(fhirType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Mapper not found for " + fhirType));
    }
}
