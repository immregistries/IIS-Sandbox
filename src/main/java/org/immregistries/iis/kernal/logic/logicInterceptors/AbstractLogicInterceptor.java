package org.immregistries.iis.kernal.logic.logicInterceptors;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractLogicInterceptor {

	public static final String IIS_REPORTABLE_LIST = "iisReportableList";

	public static List<IisReportable> iisReportableList(RequestDetails requestDetails) {
		if (requestDetails.getAttribute(IIS_REPORTABLE_LIST) == null) {
			requestDetails.setAttribute(IIS_REPORTABLE_LIST, new ArrayList<>(20));
		}
		return (List<IisReportable>) requestDetails.getAttribute(IIS_REPORTABLE_LIST);
	}

	public static @NotNull IisReportable err5IisReportable(String number, String text, List<@NotNull Hl7Location> hl7LocationList) {
		IisReportable iisReportable = new IisReportable();
		iisReportable.setHl7LocationList(hl7LocationList);
		CodedWithExceptions applicationErrorCode = new CodedWithExceptions("ERR-5");
		applicationErrorCode.setIdentifier(number);
		applicationErrorCode.setText(text);
		iisReportable.setApplicationErrorCode(applicationErrorCode);
		iisReportable.setHl7ErrorCode(applicationErrorCode);
		iisReportable.setSeverity(IisReportableSeverity.WARN);
		return iisReportable;
	}


}
