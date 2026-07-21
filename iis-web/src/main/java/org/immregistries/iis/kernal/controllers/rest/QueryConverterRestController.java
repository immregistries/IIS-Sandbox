package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.smm.tester.manager.query.QueryConverter;
import org.immregistries.smm.tester.manager.query.QueryType;
import org.immregistries.smm.transform.ScenarioManager;
import org.immregistries.smm.transform.TestCaseMessage;
import org.immregistries.smm.transform.Transformer;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.QUERY_CONVERTER_PATH;

@RestController
@RequestMapping(IisRestPath.BasePath.REST_PATH + QUERY_CONVERTER_PATH)
public class QueryConverterRestController {

	public static class ConvertRequest {
		private String message;
		private String queryType;

		public ConvertRequest() {
		}

		public ConvertRequest(String message, String queryType) {
			this.message = message;
			this.queryType = queryType;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getQueryType() {
			return queryType;
		}

		public void setQueryType(String queryType) {
			this.queryType = queryType;
		}
	}

	@GetMapping(value = "/sample", produces = "text/plain")
	public String getSample() {
		TestCaseMessage testCaseMessage = ScenarioManager
			.createTestCaseMessage(ScenarioManager.SCENARIO_1_R_ADMIN_CHILD);
		Transformer transformer = new Transformer();
		transformer.transform(testCaseMessage);
		return testCaseMessage.getMessageText();
	}

	@PostMapping(produces = "text/plain")
	public String convert(@RequestBody ConvertRequest request) {
		String message = request.getMessage();
		if (message == null || message.isEmpty()) {
			return getSample();
		}
		if (request.getQueryType() != null && !request.getQueryType().isEmpty()) {
			QueryConverter queryConverter = QueryConverter.getQueryConverter(
				QueryType.getValue(request.getQueryType()));
			return queryConverter.convert(message);
		}
		return message;
	}
}
