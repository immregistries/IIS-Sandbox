package org.immregistries.iis.kernal.model;

import org.immregistries.vfa.connect.model.TestEvent;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class VaccinationReported implements Serializable {
  private static final long serialVersionUID = 1L;
	
  private String vaccinationReportedId = "";
  private PatientReported patientReported = null;
  private String patientReportedId = "";
  private String vaccinationReportedExternalLink = "";
  private VaccinationMaster vaccination = null;
  private Date reportedDate = null;
  private Date updatedDate = null;

  private Date administeredDate = null;
  private String vaccineCvxCode = "";
  private String vaccineNdcCode = "";
  private String vaccineMvxCode = "";
  private String administeredAmount = "";
  private String informationSource = "";
  private String lotnumber = "";
  private Date expirationDate = null;
  private String completionStatus = "";
  private String actionCode = "";
  private String refusalReasonCode = "";
  private String bodySite = "";
  private String bodyRoute = "";
  private String fundingSource = "";
  private String fundingEligibility = "";
  private TestEvent testEvent = null;


	private String orgLocationId = "";
  private OrgLocation orgLocation = null;
  private ModelPerson enteredBy = null;
  private ModelPerson orderingProvider = null;
  private ModelPerson administeringProvider = null;


  public ModelPerson getEnteredBy() {
    return enteredBy;
  }

  public void setEnteredBy(ModelPerson enteredBy) {
    this.enteredBy = enteredBy;
  }

  public ModelPerson getOrderingProvider() {
    return orderingProvider;
  }

  public void setOrderingProvider(ModelPerson orderingProvider) {
    this.orderingProvider = orderingProvider;
  }

  public ModelPerson getAdministeringProvider() {
    return administeringProvider;
  }

  public void setAdministeringProvider(ModelPerson administeringProvider) {
    this.administeringProvider = administeringProvider;
  }

  public TestEvent getTestEvent() {
    return testEvent;
  }

  public void setTestEvent(TestEvent testEvent) {
    this.testEvent = testEvent;
  }

  public Date getAdministeredDate() {
    return administeredDate;
  }

  public void setAdministeredDate(Date administeredDate) {
    this.administeredDate = administeredDate;
  }

  public String getVaccineCvxCode() {
    return vaccineCvxCode;
  }

  public void setVaccineCvxCode(String vaccineCvxCode) {
    this.vaccineCvxCode = vaccineCvxCode;
  }

  public String getVaccineNdcCode() {
    return vaccineNdcCode;
  }

  public void setVaccineNdcCode(String vaccineNdcCode) {
    this.vaccineNdcCode = vaccineNdcCode;
  }

  public String getVaccineMvxCode() {
    return vaccineMvxCode;
  }

  public void setVaccineMvxCode(String vaccineMvxCode) {
    this.vaccineMvxCode = vaccineMvxCode;
  }

  public String getAdministeredAmount() {
    return administeredAmount;
  }

  public void setAdministeredAmount(String administeredAmount) {
    this.administeredAmount = administeredAmount;
  }

  public String getInformationSource() {
    return informationSource;
  }

  public void setInformationSource(String informationSource) {
    this.informationSource = informationSource;
  }

  public String getLotnumber() {
    return lotnumber;
  }

  public void setLotnumber(String lotnumber) {
    this.lotnumber = lotnumber;
  }

  public Date getExpirationDate() {
    return expirationDate;
  }

  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }

  public String getCompletionStatus() {
    return completionStatus;
  }

  public void setCompletionStatus(String completionStatus) {
    this.completionStatus = completionStatus;
  }

  public String getActionCode() {
    return actionCode;
  }

  public void setActionCode(String actionCode) {
    this.actionCode = actionCode;
  }

  public String getRefusalReasonCode() {
    return refusalReasonCode;
  }

  public void setRefusalReasonCode(String refusalReasonCode) {
    this.refusalReasonCode = refusalReasonCode;
  }

  public String getBodySite() {
    return bodySite;
  }

  public void setBodySite(String bodySite) {
    this.bodySite = bodySite;
  }

  public String getBodyRoute() {
    return bodyRoute;
  }

  public void setBodyRoute(String bodyRoute) {
    this.bodyRoute = bodyRoute;
  }

  public String getFundingSource() {
    return fundingSource;
  }

  public void setFundingSource(String fundingSource) {
    this.fundingSource = fundingSource;
  }

  public String getFundingEligibility() {
    return fundingEligibility;
  }

  public void setFundingEligibility(String fundingEligibility) {
    this.fundingEligibility = fundingEligibility;
  }

  public String getVaccinationReportedId() {
    return vaccinationReportedId;
  }

  public void setVaccinationReportedId(String reportedVaccinationId) {
    this.vaccinationReportedId = reportedVaccinationId;
  }

  public PatientReported getPatientReported() {
	  if (patientReported == null && patientReportedId != null && !patientReportedId.equals("")) {
		  //
	  }
    return patientReported;
  }

  public void setPatientReported(PatientReported reportedPatient) {
	  if (reportedPatient != null && reportedPatient.getPatientReportedId() != null) {
		  setPatientReportedId(reportedPatient.getPatientReportedId());
	  }
	  this.patientReported = reportedPatient;
  }

  public String getVaccinationReportedExternalLink() {
//	  return vaccinationReportedId;
	  return vaccinationReportedExternalLink;
  }

  public void setVaccinationReportedExternalLink(String reportedOrderId) {
//    this.vaccinationReportedId = reportedOrderId;
    this.vaccinationReportedExternalLink = reportedOrderId;
  }

  public VaccinationMaster getVaccination() {
    return vaccination;
  }

  public void setVaccination(VaccinationMaster vaccination) {
    this.vaccination = vaccination;
  }

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

  public OrgLocation getOrgLocation() {
    return orgLocation;
  }

  public void setOrgLocation(OrgLocation orgLocation) {
    this.orgLocation = orgLocation;
	 if (orgLocation != null) {
		 this.orgLocationId = orgLocation.getOrgLocationId();
	 }
  }

	public String getPatientReportedId() {
		return patientReportedId;
	}

	public void setPatientReportedId(String patientReportedId) {
		this.patientReportedId = patientReportedId;
	}

	public String getOrgLocationId() {
		return orgLocationId;
	}

	public void setOrgLocationId(String orgLocationId) {
		this.orgLocationId = orgLocationId;
	}

}
