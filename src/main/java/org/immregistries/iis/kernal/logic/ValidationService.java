package org.immregistries.iis.kernal.logic;

import gov.nist.validation.report.Entry;
import gov.nist.validation.report.Report;
import hl7.v2.profile.Profile;
import hl7.v2.profile.XMLDeserializer;
import hl7.v2.validation.SyncHL7Validator;
import hl7.v2.validation.content.ConformanceContext;
import hl7.v2.validation.content.DefaultConformanceContext;
import hl7.v2.validation.vs.ValueSetLibrary;
import hl7.v2.validation.vs.ValueSetLibraryImpl;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.mqe.hl7util.ReportableSource;
import org.immregistries.mqe.hl7util.SeverityLevel;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;
import org.immregistries.mqe.validator.MqeMessageService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ValidationService {
	public MqeMessageService getMqeMessageService() {
		return mqeMessageService;
	}

	/**
	 * DYNAMIC VALUE SETS for validation
	 */
	public MqeMessageService mqeMessageService;

	SyncHL7Validator syncHL7ValidatorVxuZ22;
	SyncHL7Validator syncHL7ValidatorQbpZ34;
	SyncHL7Validator syncHL7ValidatorQbpZ44;


	public ValidationService() {

		mqeMessageService = MqeMessageService.INSTANCE;

		{
			InputStream profileXML = ValidationService.class.getResourceAsStream("/export/VXU-Z22_Profile.xml");
			InputStream constraintsXML = ValidationService.class.getResourceAsStream("/export/VXU-Z22_Constraints.xml");
			InputStream vsLibraryXML = ValidationService.class.getResourceAsStream("/export/VXU-Z22_ValueSetLibrary.xml");

			Profile profile = XMLDeserializer.deserialize(profileXML).get();
			ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibraryXML).get();
			ConformanceContext conformanceContext = DefaultConformanceContext.apply(Collections.singletonList(constraintsXML)).get();
			syncHL7ValidatorVxuZ22 = new SyncHL7Validator(profile, valueSetLibrary, conformanceContext);
		}

		{
			InputStream profileXML = ValidationService.class.getResourceAsStream("/export/QBP-Z34_Profile.xml");
			InputStream constraintsXML = ValidationService.class.getResourceAsStream("/export/QBP-Z34_Constraints.xml");
			InputStream vsLibraryXML = ValidationService.class.getResourceAsStream("/export/QBP-Z34_ValueSetLibrary.xml");

			Profile profile = XMLDeserializer.deserialize(profileXML).get();
			ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibraryXML).get();
			ConformanceContext conformanceContext = DefaultConformanceContext.apply(Collections.singletonList(constraintsXML)).get();
			syncHL7ValidatorQbpZ34 = new SyncHL7Validator(profile, valueSetLibrary, conformanceContext);
		}

		{
			InputStream profileXML = ValidationService.class.getResourceAsStream("/export/QBP-Z44_Profile.xml");
			InputStream constraintsXML = ValidationService.class.getResourceAsStream("/export/QBP-Z44_Constraints.xml");
			InputStream vsLibraryXML = ValidationService.class.getResourceAsStream("/export/QBP-Z44_ValueSetLibrary.xml");

			Profile profile = XMLDeserializer.deserialize(profileXML).get();
			ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibraryXML).get();
			ConformanceContext conformanceContext = DefaultConformanceContext.apply(Collections.singletonList(constraintsXML)).get();
			syncHL7ValidatorQbpZ44 = new SyncHL7Validator(profile, valueSetLibrary, conformanceContext);
		}
	}

	public List<IisReportable> nistValidation(String message, String profileId) throws Exception {
		String id;
		SyncHL7Validator syncHL7Validator;
		if ("Z34".equals(profileId)) {
			id = "89df2062-96c4-4cbc-9ef2-817e4b4bc4f1";
			syncHL7Validator = syncHL7ValidatorQbpZ34;
		} else if ("Z44".equals(profileId)) {
			id = "b760d322-9afd-439e-96f5-43db66937c4e";
			syncHL7Validator = syncHL7ValidatorQbpZ44;
		} else if ("Z22".equals(profileId)) {
			id = "aa72383a-7b48-46e5-a74a-82e019591fe7";
			syncHL7Validator = syncHL7ValidatorVxuZ22;
		} else {
			id = "aa72383a-7b48-46e5-a74a-82e019591fe7";
			syncHL7Validator = syncHL7ValidatorVxuZ22;
		}
		Report report = syncHL7Validator.check(message, id);
		List<IisReportable> reportableList = new ArrayList<>();
		for (Map.Entry<String, List<Entry>> mapEntry : report.getEntries().entrySet()) {
			for (Entry assertion : mapEntry.getValue()) {
//				logger.info("entry {}", assertion.toText());
				String severity = assertion.getClassification();
				SeverityLevel severityLevel = SeverityLevel.ACCEPT;
				if (severity.equalsIgnoreCase("error")) {
					severityLevel = SeverityLevel.WARN;
				}

				if (severityLevel != SeverityLevel.ACCEPT) {
					IisReportable reportable = new IisReportable();
					reportable.setSource(ReportableSource.NIST);
					reportable.setSeverity(IisReportableSeverity.WARN);
					reportableList.add(reportable);
					reportable.setReportedMessage(assertion.getDescription());
//					reportable.setSeverity(severityLevel);
					reportable.getHl7ErrorCode().setIdentifier("0");
					CodedWithExceptions cwe = new CodedWithExceptions();
					cwe.setAlternateIdentifier(assertion.getCategory());
					cwe.setAlternateText(assertion.getDescription());
					cwe.setNameOfAlternateCodingSystem("L");
					reportable.setApplicationErrorCode(cwe);
					String path = assertion.getPath();
					reportable.setDiagnosticMessage(path);
					fillErrorLocationFromPath(reportable, path);
				}
			}
		}
//		ValidationReport validationReport = new ValidationReport(report.toText());

		return reportableList;
	}

	private void fillErrorLocationFromPath(IisReportable reportable, String path) {
		if (path != null && path.length() >= 3) {
			String segmentid = path.substring(0, 3);
			if (path.length() > 3) {
				path = path.substring(4);
			} else {
				path = "";
			}

			Hl7Location errorLocation = IisReportable.readErrorLocation(path, segmentid);
			if (errorLocation != null) {
				reportable.getHl7LocationList().add(errorLocation);
			}
		}
	}

}
