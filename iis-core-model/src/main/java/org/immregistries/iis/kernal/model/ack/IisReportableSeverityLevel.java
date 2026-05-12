package org.immregistries.iis.kernal.model.ack;


/**
 * Based on MQE Reportable severity
 */
public enum IisReportableSeverityLevel {

	/**
	 * Data needs to be fixed, and the message should be resubmitted.
	 */
	ERROR("E", "Error", "Rejected with Errors")
	/**
	 * Data needs to be fixed, but we can deal with it in this message. No need
	 * to resubmit.
	 */
	,
	WARN("W", "Warn", "Accepted with Warnings")
	/**
	 * This means an issue is acceptable, but we're not going to tell anyone
	 * about it, except maybe on a report.
	 */
	,
	ACCEPT("A", "Accept", "Accepted")
	/**
	 * This means an issues is acceptable, and we do want to tell people.
	 */
	,
	INFO("I", "Info", "Informational"),
	NOTICE("N", "Notice", "Notice");

	private String severityCode = "";
	private String severityLabel = "";
	private String severityDescription = "";

	private IisReportableSeverityLevel(String actionCode, String actionLabel,
												  String actionDesc) {
		this.severityCode = actionCode;
		this.severityLabel = actionLabel;
		this.severityDescription = actionDesc;
	}

	public static IisReportableSeverityLevel findByCode(String code) {
		for (IisReportableSeverityLevel s : IisReportableSeverityLevel.values()) {
			if (s.getCode() == code) {
				return s;
			}
		}
		return null;
	}

	public static IisReportableSeverityLevel findByLabel(String label) {
		for (IisReportableSeverityLevel s : IisReportableSeverityLevel.values()) {
			if (s.getLabel().equalsIgnoreCase(label)) {
				return s;
			}
		}
		return null;
	}

	public String getLabel() {
		return this.severityLabel;
	}

	public String getCode() {
		return severityCode;
	}

	public String getDescription() {
		return this.severityDescription;
	}

}
