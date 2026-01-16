package org.immregistries.iis.kernal.mapping.mappers.resources;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public abstract class ObservationMapper<Observation extends IAnyResource>
		implements
		IisResourceMasterReportedMapper<ObservationMaster, ObservationReported, ObservationMaster, Observation> {

	public String fhirTypeName() {
		return "Observation";
	}

	public static final String OBSERVATION = "Observation";

	public Class<ObservationMaster> localType() {
		return ObservationMaster.class;
	}

	public Class<ObservationMaster> localMasterType() {
		return ObservationMaster.class;
	}

	public Class<ObservationReported> localReportedType() {
		return ObservationReported.class;
	}

	public static final String IDENTIFIER_CODE = "identifierCode";
	public static final String OBSERVATION_DATE = "observationDate";
	public static final String RESULT_STATUS = "resultStatus";
	public static final String SUBIDENTIFIER_EXTENSION = "http://hl7.org/fhir/uv/v2mappings/ConceptMap/datatype-og-subidentifier-to-extension";
	public static final String V_2_STATUS_EXTENSION = "v2Status";
	public static final String OBS_TYPE_OBX_2 = "ObsType-OBX-2";

	public abstract ObservationReported localObjectReported(Observation i);

	public abstract ObservationReported localObjectReportedWithMaster(Observation observation);
}
