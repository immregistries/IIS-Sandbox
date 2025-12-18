package org.immregistries.iis.kernal.mapping;

import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.mapping.interfaces.*;
import org.immregistries.iis.kernal.model.AbstractMappedObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AllMappingService {

    @Autowired
    private Set<IisFhirMapperMaster> mapperMasters;

    public IBaseResource fhirResource(AbstractMappedObject internal) {
        @SuppressWarnings("rawtypes")
        IisFhirMapperMaster mapper = selectMapper(internal);
        if (mapper == null) {
            throw new IllegalArgumentException("No mapper found for " + internal.getClass().getName());
        }
        return mapper.fhirResource(internal);
    }

    public AbstractMappedObject localObject(IBaseResource resource) {
        @SuppressWarnings("rawtypes")
        IisFhirMapperMaster mapper = selectMapper(resource);
        if (mapper == null) {
            throw new IllegalArgumentException("No mapper found for " + resource.fhirType());
        }
        return mapper.localObject(resource);
    }

    @SuppressWarnings("rawtypes")
    public IisFhirMapperMaster selectMapper(AbstractMappedObject internal) {
        Class inteClass = internal.getClass();
        return mapperMasters.stream().filter(mapper -> mapper.localMasterType().equals(inteClass)).findFirst()
                .orElseThrow(() -> new RuntimeException("Mapper not found for " + inteClass.getName()));
    }

    @SuppressWarnings("rawtypes")
    public IisFhirMapperMaster selectMapper(IBaseResource resource) {
        String fhirType = resource.fhirType();
        return mapperMasters.stream().filter(mapper -> mapper.fhirType().equals(fhirType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Mapper not found for " + fhirType));
    }
}
