package org.immregistries.iis.kernal.logic.recommendations;

import org.immregistries.vfa.connect.model.Admin;
import org.immregistries.vfa.connect.model.ForecastActual;

import java.util.Date;

public enum VaccinePlanStatus implements IisEnum {
	COMPLETE("LA13421-5", "Complete - all required doses have been received to meet the requirements for a particular vaccine group."),
	ON_SCHEDULE("LA13422-3", "On schedule - person is not overdue for a given dose in the series. Includes a person too young to start the series."),
	OVERDUE("LA13423-1", "Overdue - person is late getting the next dose in the series."),
	TOO_OLD("LA13424-9", "Too old - cannot complete the series because the latest age for receiving dose has passed."), // Not returned by lonestar
	IMMUNE("LA27183-5", "Immune"),
	CONTRAINDICATED("LA4216-3", "Contraindicated"), // Not returned by lonestar
	NOT_RECOMMENDED("LA4695-8", "Not Recommended");

	private final String code;
	private final String label;

	VaccinePlanStatus(String code, String label) {
		this.code = code;
		this.label = label;
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * Right now all codes in enum belong to this list
	 *
	 * @return
	 */
	public String getTable() {
		return "LL940-8";
	}

	public org.hl7.fhir.r4.model.Coding toR4() {
		return new org.hl7.fhir.r4.model.Coding(this.getTable(), this.getCode(), this.getLabel());
	}

	public org.hl7.fhir.r5.model.Coding toR5() {
		return new org.hl7.fhir.r5.model.Coding(this.getTable(), this.getCode(), this.getLabel());
	}

	public static VaccinePlanStatus fromForecastActual(ForecastActual forecastActual) {
		Admin admin = forecastActual.getAdmin();
		return fromAdminCodeAndOverdueDate(admin, forecastActual.getOverdueDate());

	}

	public static VaccinePlanStatus fromAdminCodeAndOverdueDate(Admin admin, Date overdueDate) {
		if (admin != null) {
			switch (admin) {
				case NOT_COMPLETE: {
					if (overdueDate != null && new Date().after(overdueDate)) {
						return VaccinePlanStatus.OVERDUE;
					} else {
						return VaccinePlanStatus.ON_SCHEDULE;
					}
				}
				case OVERDUE:
					return VaccinePlanStatus.OVERDUE;
				case DUE:
				case DUE_LATER:
					return VaccinePlanStatus.ON_SCHEDULE;
				case NOT_RECOMMENDED:
					return VaccinePlanStatus.NOT_RECOMMENDED;
				case ASSUMED_COMPLETE_OR_IMMUNE:
				case FINISHED:
				case COMPLETE:
				case COMPLETE_FOR_SEASON:
				case IMMUNE:
					return VaccinePlanStatus.IMMUNE;
				case AGED_OUT:
					return VaccinePlanStatus.TOO_OLD;
				case UNKNOWN:
				case ERROR:
				case NO_RESULTS:
				case CONTRAINDICATED:
			}
		}
		return null;
	}
}

