package org.immregistries.iis.kernal.mapping;

import java.util.List;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMasterMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.model.AbstractMappedObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AllMappingService {

    @SuppressWarnings("rawtypes")
    @Autowired
    private List<IisResourceMasterReportedMapper> mapperMastersReported;

    @SuppressWarnings("rawtypes")
    @Autowired
    private List<IisResourceMasterMapper> mapperMasters;

    @SuppressWarnings("unchecked")
    public IBaseResource fhirResource(AbstractMappedObject internal) {
        @SuppressWarnings("rawtypes")
		  IisResourceMasterMapper mapper = selectMapper(internal);
        return mapper.fhirResource(internal);
    }

    @SuppressWarnings("unchecked")
    public AbstractMappedObject localObject(IBaseResource resource) {
        @SuppressWarnings("rawtypes")
		  IisResourceMasterMapper mapper = selectMapper(resource);
        return mapper.localObject(resource);
    }

    @SuppressWarnings("unchecked")
    public AbstractMappedObject localObjectReportedWithMaster(IBaseResource resource) {
        @SuppressWarnings("rawtypes")
		  IisResourceMasterReportedMapper mapper = selectMapperReported(resource);
        return mapper.localObjectReportedWithMaster(resource);
    }

    @SuppressWarnings("unchecked")
    public AbstractMappedObject localObjectReported(IBaseResource resource) {
        @SuppressWarnings("rawtypes")
		  IisResourceMasterReportedMapper mapper = selectMapperReported(resource);
        return mapper.localObjectReported(resource);
    }

    @SuppressWarnings("rawtypes")
    public IisResourceMasterMapper selectMapper(AbstractMappedObject internal) {
        Class inteClass = internal.getClass();
        if (mapperMastersReported.stream().anyMatch(mapper -> mapper.localReportedType().equals(inteClass))) {
            return mapperMastersReported.stream().filter(mapper -> mapper.localReportedType().equals(inteClass))
                    .findFirst().orElseThrow(() -> new RuntimeException("Mapper not found for " + inteClass.getName()));
        }
        return mapperMasters.stream().filter(mapper -> mapper.localMasterType().equals(inteClass)).findFirst()
                .orElseThrow(() -> new RuntimeException("Mapper not found for " + inteClass.getName()));
    }

    @SuppressWarnings("rawtypes")
    public IisResourceMasterMapper selectMapper(IBaseResource resource) {
        String fhirType = resource.fhirType();
        return mapperMasters.stream().filter(mapper -> mapper.fhirType().equals(fhirType))
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
