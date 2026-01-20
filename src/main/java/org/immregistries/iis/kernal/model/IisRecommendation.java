package org.immregistries.iis.kernal.model;

import org.immregistries.vfa.connect.model.ForecastActual;

import java.util.Date;
import java.util.List;

public class IisRecommendation extends IisMappedToFhirResource {
	private String id;
	private IisPatient iisPatient;
	private List<BusinessIdentifier> businessIdentifierList;
	private List<ForecastActual> forecastActualList;
	private Date date;
	private BusinessIdentifier authority;

	public IisRecommendation() {
	}

	public IisRecommendation(IisPatient iisPatient, List<ForecastActual> forecastActualList, Date date) {
		this.iisPatient = iisPatient;
		this.forecastActualList = forecastActualList;
		this.date = date;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public IisPatient getIisPatient() {
		return iisPatient;
	}

	public void setIisPatient(IisPatient iisPatient) {
		this.iisPatient = iisPatient;
	}

	public List<ForecastActual> getForecastActualList() {
		return forecastActualList;
	}

	public void setForecastActualList(List<ForecastActual> forecastActualList) {
		this.forecastActualList = forecastActualList;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<BusinessIdentifier> getBusinessIdentifierList() {
		return businessIdentifierList;
	}

	public void setBusinessIdentifierList(List<BusinessIdentifier> businessIdentifierList) {
		this.businessIdentifierList = businessIdentifierList;
	}

	public BusinessIdentifier getAuthority() {
		return authority;
	}

	public void setAuthority(BusinessIdentifier authority) {
		this.authority = authority;
	}
}
