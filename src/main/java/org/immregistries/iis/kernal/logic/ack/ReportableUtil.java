package org.immregistries.iis.kernal.logic.ack;

import org.immregistries.iis.kernal.logic.ProcessingException;
import org.immregistries.mqe.hl7util.ReportableSource;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;

import java.util.List;

public final class ReportableUtil {

	public static IisReportable fromProcessingException(ProcessingException processingException) {
		IisReportable iisReportable = new IisReportable();

		Hl7Location location = new Hl7Location();
		location.setSegmentId(processingException.getSegmentId());
//		location.setComponentNumber(processingException.get);
		location.setFieldRepetition(processingException.getSegmentRepeat());
		location.setFieldPosition(processingException.getFieldPosition());
		iisReportable.setHl7LocationList(List.of(location));

		CodedWithExceptions hl7ErrorCode = new CodedWithExceptions();
		hl7ErrorCode.setIdentifier("101");
		hl7ErrorCode.setNameOfCodingSystem("HL70357");
		hl7ErrorCode.setText("Required field missing");
		iisReportable.setHl7ErrorCode(hl7ErrorCode);

		iisReportable.setSeverity(processingException.getErrorCode());
		iisReportable.setApplicationErrorCode(new CodedWithExceptions());
		iisReportable.setReportedMessage(processingException.getLocalizedMessage());
		iisReportable.setDiagnosticMessage(processingException.getMessage());
		iisReportable.setSource(ReportableSource.IIS);

		return iisReportable;
	}

}
