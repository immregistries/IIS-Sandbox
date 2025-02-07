package org.immregistries.iis.kernal.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObservationMaster extends AbstractMappedObject implements Serializable {
  private static final long serialVersionUID = 1L;

  private String observationId = "";
  private String identifierCode = "";
  private String valueCode = "";
	//  private PatientReported patientReported = null;
//  private VaccinationReported vaccinationReported = null;
	private String patientReportedId = "";
	private String vaccinationReportedId = "";
	private Date reportedDate = null;
	private Date updatedDate = null;
	private String valueType = "";
	private String identifierLabel = "";
	private String identifierTable = "";
	private String valueLabel = "";
	private String valueTable = "";
	private String unitsCode = "";
	private String unitsLabel = "";
	private String unitsTable = "";
	private String resultStatus = "";
	private Date observationDate = null;
	private String methodCode = "";
	private String methodLabel = "";
	private String methodTable = "";
	private List<BusinessIdentifier> businessIdentifiers = new ArrayList<>(2);

	public Date getReportedDate() {
		return reportedDate;
	}

	public void setReportedDate(Date reportedDate) {
		this.reportedDate = reportedDate;
	}

	public Date getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public String getIdentifierCode() {
		return identifierCode;
	}

	public void setIdentifierCode(String identifierCode) {
		this.identifierCode = identifierCode;
	}

	public String getIdentifierLabel() {
		return identifierLabel;
	}

	public void setIdentifierLabel(String identifierLabel) {
		this.identifierLabel = identifierLabel;
	}

	public String getIdentifierTable() {
		return identifierTable;
	}

	public void setIdentifierTable(String identifierTable) {
		this.identifierTable = identifierTable;
	}

	public String getValueCode() {
		return valueCode;
	}

	public void setValueCode(String valueCode) {
		this.valueCode = valueCode;
	}

	public String getValueLabel() {
		return valueLabel;
	}

	public void setValueLabel(String valueLabel) {
		this.valueLabel = valueLabel;
	}

	public String getValueTable() {
		return valueTable;
	}

	public void setValueTable(String valueTable) {
		this.valueTable = valueTable;
	}

	public String getUnitsCode() {
		return unitsCode;
	}

	public void setUnitsCode(String unitsCode) {
		this.unitsCode = unitsCode;
	}

	public String getUnitsLabel() {
		return unitsLabel;
	}

	public void setUnitsLabel(String unitsLabel) {
		this.unitsLabel = unitsLabel;
	}

	public String getUnitsTable() {
		return unitsTable;
	}

	public void setUnitsTable(String unitsTable) {
		this.unitsTable = unitsTable;
	}

	public Date getObservationDate() {
		return observationDate;
	}

	public void setObservationDate(Date observation_date) {
		this.observationDate = observation_date;
	}

	public String getMethodCode() {
		return methodCode;
	}

	public void setMethodCode(String methodCode) {
		this.methodCode = methodCode;
	}

	public String getMethodLabel() {
		return methodLabel;
	}

	public void setMethodLabel(String methodLabel) {
		this.methodLabel = methodLabel;
	}

	public String getMethodTable() {
		return methodTable;
	}

	public void setMethodTable(String methodTable) {
		this.methodTable = methodTable;
	}

	public String getResultStatus() {
		return resultStatus;
	}

	public void setResultStatus(String resultStatus) {
		this.resultStatus = resultStatus;
	}


	public String getPatientReportedId() {
		return patientReportedId;
	}

	public void setPatientReportedId(String patientReportedId) {
		this.patientReportedId = patientReportedId;
	}

	public String getVaccinationReportedId() {
		return vaccinationReportedId;
	}

	public void setVaccinationReportedId(String vaccinationReportedId) {
		this.vaccinationReportedId = vaccinationReportedId;
	}

	public String getObservationId() {
		return observationId;
	}

	public void setObservationId(String observationId) {
		this.observationId = observationId;
	}

	public List<BusinessIdentifier> getBusinessIdentifiers() {
		return businessIdentifiers;
	}

	public void setBusinessIdentifiers(List<BusinessIdentifier> businessIdentifiers) {
		this.businessIdentifiers = businessIdentifiers;
	}

	public void addBusinessIdentifier(BusinessIdentifier businessIdentifier) {
		if (this.businessIdentifiers == null) {
			this.businessIdentifiers = new ArrayList<>(3);
		}
		this.businessIdentifiers.add(businessIdentifier);
	}

	public BusinessIdentifier getFirstBusinessIdentifier() {
		if (businessIdentifiers.isEmpty()) {
			return null;
		}
		return this.businessIdentifiers.get(0);
	}

	@Override
	public String toString() {
		return "ObservationMaster{" +
			"observationId='" + observationId + '\'' +
			", identifierCode='" + identifierCode + '\'' +
			", valueCode='" + valueCode + '\'' +
			", patientReportedId='" + patientReportedId + '\'' +
			", vaccinationReportedId='" + vaccinationReportedId + '\'' +
			", reportedDate=" + reportedDate +
			", updatedDate=" + updatedDate +
			", valueType='" + valueType + '\'' +
			", identifierLabel='" + identifierLabel + '\'' +
			", identifierTable='" + identifierTable + '\'' +
			", valueLabel='" + valueLabel + '\'' +
			", valueTable='" + valueTable + '\'' +
			", unitsCode='" + unitsCode + '\'' +
			", unitsLabel='" + unitsLabel + '\'' +
			", unitsTable='" + unitsTable + '\'' +
			", resultStatus='" + resultStatus + '\'' +
			", observationDate=" + observationDate +
			", methodCode='" + methodCode + '\'' +
			", methodLabel='" + methodLabel + '\'' +
			", methodTable='" + methodTable + '\'' +
			'}';
	}
}
