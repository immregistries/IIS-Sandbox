package org.immregistries.iis.kernal.logic.ack;

public enum ProcessingResult {
//
//	ILLOGICAL_DATE_ERROR("1", "Original", "Illogical Date error", "11/5/14", "Not Specific", "Low", "Not recommended for use. Use a more granular application error instead.", "Date conflicts with another date in the message.", "n/a", "n/a", "", "", "", "", "", "", "", "", "", "Existed before 2/18/2020", "", "", "", "", ""),
//	INVALID_DATE("2", "Original", "Invalid Date", "11/5/14", "Not Specific", "Low", "DT and DTM data types", " including formatting issues (not enough digit", " etc)", " bad characters and invalid values (eg. 20161342) but not conflicting dates (future dates", " etc) which will have more granular errors.", "Date is not valid or lacks required precision.", "Interface Analyst", "Review interface configuration or data base contents", "", "", "", "", "", "", "", "", "", "Existed before 2/18/2020", "", "", "", "", ""),
//	ILLOGICAL_VALUE_ERROR("3", "Original", "Illogical Value Error", "11/5/14", "Not Specific", "Low", "Not recommended for use. Use a more granular application error instead.", "The value conflicts with other data in the message", "n/a", "n/a", "", "", "", "", "", "", "", "", "", "Existed before 2/18/2020", "", "", "", "", ""),
//	INVALID_VALUE("4", "Original", "Invalid Value", "11/5/14", "Not Specific", "Low", "Number and text type data types", "The value is not valid. This applies for fields that are not associated with a table of values.", "Interface Analyst", "Review interface configuration or data base contents", "", "", "", "", "", "", "", "", "", "Existed before 2/18/2020", "", "", "", "", ""),
//	TABLE_VALUE_NOT_FOUND("5", "Original", "Table value not found", "11/5/14", "Not Specific", "Low", "CE", " CNE", " CWE", " IS", " ID and PT data types", " this is a code mapping error.", "Indicates that the coded value is not found in the associated table. ", "Interface Analyst", "Review interface configuration or mapping tables", "", "", "", "", "", "", "", "", "", "Existed before 2/18/2020", "", "", "", "", ""),
//	REQUIRED_OBSERVATION_MISSING("6", "Original", "Required observation missing", "11/5/14", "Not Specific", "Low", "Not recommended for use. Use a more granular application error instead.", "A required observation", " such as VFC eligibility status", " is missing.", "n/a", "n/a", "", "", "", "", "", "", "", "", "", "Existed before 2/18/2020", "", "", "", "", ""),
//	REQUIRED_DATA_MISSING("7", "Original", "Required Data Missing", "11/5/14", "Not Specific", "Low", "Any critical field", "This error would be applied at the application level and may be used when data is sent but could not be interpreted.", "Interface Analyst", "Review interface configuration or mapping tables", "", "", "", "", "", "", "", "", "", "Existed before 2/18/2020", "", "", "", "", ""),
//	CONFLICTING_START_AND_END_DATE_OF_ADMINISTRATION("2000", "Conflicting Data", "Conflicting Start and End Date of Administration", "10/23/17", "Active", "Low", "RXA-3;RXA-4", "Indicates that the dates in RXA-3 and RXA-4 are not identical.", "Interface Analyst", "Review interface configuration or data base contents", "", "", "", "MAY", "MAY", "", "MAY", "MAY", "MAY", "Existed before 2/18/2020", "", "", "", "", ""),
//	CONFLICTING_ADMINISTRATION_DATE_AND_EXPIRATION_DATE("2001", "Conflicting Data", "Conflicting Administration Date and Expiration Date", "10/23/17", "Active", "Medium", "RXA-3;RXA-16", "Indicates a conflict between the administration date in RXA-3 and the expiration date in RXA-16. In other words it indicates that an expired vaccine was administered.", "Clinical team", "Review patient record to confirm accuracy of dates (and recall patient if necessary)", "", "", "SHOULD", "SHOULD NOT", "SHOULD", "MAY", "SHOULD NOT", "MAY", "MAY", "Existed before 2/18/2020", "Added Business rule", "", "", "", "BR 118 - DQA2013 - BR118 Vaccination Encounter Date should not be after the lot number expiration date."),
//	String code; // Code
//	String classCode; // Class
//	String concept; // Concept
//	String published; // Published
//	String status; // Status
//	String implementationPriority; // Implementation Priority
//	String appliesTo; // Applies to
//	String description; // Description
//	String actor; // Actor
//	String actionByActor; // Action by Actor
//	String question; // Question
//	String dataQualityIncomingOngoingAssessment; // Data Quality Incoming/Ongoing Assessment
//	String iisDetectsIssue; // IIS Detects Issue
//	String iisActionCreateErrWithErrorEInErr4; // IIS Action - Create ERR with Error (E) in ERR-4
//	String iisActionCreateErrWithWarningWInErr4; // IIS Action - Create ERR with Warning (W) in ERR-4
//	String iisActionCreateErrWithNoticeNInErr4; // IIS Action - Create ERR with  Notice (N) in ERR-4
//	String iisActionCreateErrWithInformationalIInErr4; // IIS Action - Create ERR with  Informational (I) in ERR-4
//	String iisActionDetectOnly; // IIS Action - Detect Only
//	String iisActionDoNotDetect; // IIS Action - Do Not Detect
//	String codeSource; // Code Source
//	String groupNotes; // Group Notes
//	String newCodeCreatedByGroup; // New Code Created by Group
//	String crossReferencedCode; // Cross Referenced Code
//	String createdDate; // Created Date
//	String businessRule; // Business Rule
//
//	ProcessingResult(String code, String classCode, String concept, String published, String status, String implementationPriority, String appliesTo, String description, String actor, String actionByActor, String question, String dataQualityIncomingOngoingAssessment, String iisDetectsIssue, String iisActionCreateErrWithErrorEInErr4, String iisActionCreateErrWithWarningWInErr4, String iisActionCreateErrWithNoticeNInErr4, String iisActionCreateErrWithInformationalIInErr4, String iisActionDetectOnly, String iisActionDoNotDetect, String codeSource, String groupNotes, String newCodeCreatedByGroup, String crossReferencedCode, String createdDate, String businessRule) {
//		this.code = code;
//		this.classCode = classCode;
//		this.concept = concept;
//		this.published = published;
//		this.status = status;
//		this.implementationPriority = implementationPriority;
//		this.appliesTo = appliesTo;
//		this.description = description;
//		this.actor = actor;
//		this.actionByActor = actionByActor;
//		this.question = question;
//		this.dataQualityIncomingOngoingAssessment = dataQualityIncomingOngoingAssessment;
//		this.iisDetectsIssue = iisDetectsIssue;
//		this.iisActionCreateErrWithErrorEInErr4 = iisActionCreateErrWithErrorEInErr4;
//		this.iisActionCreateErrWithWarningWInErr4 = iisActionCreateErrWithWarningWInErr4;
//		this.iisActionCreateErrWithNoticeNInErr4 = iisActionCreateErrWithNoticeNInErr4;
//		this.iisActionCreateErrWithInformationalIInErr4 = iisActionCreateErrWithInformationalIInErr4;
//		this.iisActionDetectOnly = iisActionDetectOnly;
//		this.iisActionDoNotDetect = iisActionDoNotDetect;
//		this.codeSource = codeSource;
//		this.groupNotes = groupNotes;
//		this.newCodeCreatedByGroup = newCodeCreatedByGroup;
//		this.crossReferencedCode = crossReferencedCode;
//		this.createdDate = createdDate;
//		this.businessRule = businessRule;
//	}
}
