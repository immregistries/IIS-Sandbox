package org.immregistries.iis.kernal.logic.hl7v2.ack;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.logic.validation.ProcessingException;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.ack.IisReportableSeverity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
@Service
public class V2DateParseService {
	@Autowired
	IisReportableUtilService iisReportableUtilService;

	public Date parseDateWarn(String dateString, String errorMessage, String segmentId, int segmentRepeat, int fieldPosition, boolean strict, List<IisReportable> iisReportableList) {
		try {
			return parseDateInternal(dateString, strict);
		} catch (ParseException e) {
			if (errorMessage != null) {
				ProcessingException pe = new ProcessingException(errorMessage + ": " + e.getMessage(), segmentId, segmentRepeat, fieldPosition, IisReportableSeverity.WARN);
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
			}
		}
		return null;
	}

	public Date parseDateInternal(String dateString, boolean strict) throws ParseException {
		if (StringUtils.isBlank(dateString)) {
			return null;
		}
		Date date;
		if (dateString.length() > 8) {
			dateString = dateString.substring(0, 8);
		}
		SimpleDateFormat simpleDateFormat = generateSimpleDateFormat();
		simpleDateFormat.setLenient(!strict);
		date = simpleDateFormat.parse(dateString);
		return date;
	}

	public Date parseDateError(String dateString, String errorMessage, String segmentId, int segmentRepeat, int fieldPosition, boolean strict) throws ProcessingException {
		try {
			Date date = parseDateInternal(dateString, strict);
			if (date == null) {
				if (errorMessage != null) {
					throw new ProcessingException(errorMessage + ": No date was specified", segmentId, segmentRepeat, fieldPosition);
				}
			}
			return date;
		} catch (ParseException e) {
			if (errorMessage != null) {
				throw new ProcessingException(errorMessage + ": " + e.getMessage(), segmentId, segmentRepeat, fieldPosition);
			}
		}
		return null;
	}

	public SimpleDateFormat generateSimpleDateFormat() {
		return new SimpleDateFormat("yyyyMMdd");
	}

}
