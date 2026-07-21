package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.smm.tester.TestCovidReporting;
import org.springframework.web.bind.annotation.*;

import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.COVID_GENERATE_PATH;

@RestController
@RequestMapping(IisRestPath.BasePath.REST_PATH + COVID_GENERATE_PATH)
public class CovidGenerateRestController {

	public static class GenerateRequest {
		private int messageCount;
		private boolean includeAdmin;
		private boolean includeRefusal;
		private boolean includeComorbidity;
		private boolean includeMissed;
		private boolean includeSerology;

		public GenerateRequest() {
		}

		public GenerateRequest(int messageCount, boolean includeAdmin, boolean includeRefusal,
		                       boolean includeComorbidity, boolean includeMissed, boolean includeSerology) {
			this.messageCount = messageCount;
			this.includeAdmin = includeAdmin;
			this.includeRefusal = includeRefusal;
			this.includeComorbidity = includeComorbidity;
			this.includeMissed = includeMissed;
			this.includeSerology = includeSerology;
		}

		public int getMessageCount() {
			return messageCount;
		}

		public void setMessageCount(int messageCount) {
			this.messageCount = messageCount;
		}

		public boolean isIncludeAdmin() {
			return includeAdmin;
		}

		public void setIncludeAdmin(boolean includeAdmin) {
			this.includeAdmin = includeAdmin;
		}

		public boolean isIncludeRefusal() {
			return includeRefusal;
		}

		public void setIncludeRefusal(boolean includeRefusal) {
			this.includeRefusal = includeRefusal;
		}

		public boolean isIncludeComorbidity() {
			return includeComorbidity;
		}

		public void setIncludeComorbidity(boolean includeComorbidity) {
			this.includeComorbidity = includeComorbidity;
		}

		public boolean isIncludeMissed() {
			return includeMissed;
		}

		public void setIncludeMissed(boolean includeMissed) {
			this.includeMissed = includeMissed;
		}

		public boolean isIncludeSerology() {
			return includeSerology;
		}

		public void setIncludeSerology(boolean includeSerology) {
			this.includeSerology = includeSerology;
		}
	}

	@PostMapping(produces = "text/plain")
	public String generate(@RequestBody GenerateRequest request) {
		TestCovidReporting.Options options = new TestCovidReporting.Options();
		boolean includeDummy = request.isIncludeComorbidity() || request.isIncludeMissed() || request.isIncludeSerology();

		if (request.isIncludeAdmin() && request.isIncludeRefusal() && !includeDummy) {
			options.setAdministeredPercentage(0.75);
			options.setRefusedPercentage(1.0);
		} else if (request.isIncludeAdmin() && !request.isIncludeRefusal() && includeDummy) {
			options.setAdministeredPercentage(0.75);
			options.setRefusedPercentage(0.0);
		} else if (request.isIncludeAdmin() && !request.isIncludeRefusal() && !includeDummy) {
			options.setAdministeredPercentage(1.0);
		} else if (!request.isIncludeAdmin() && request.isIncludeRefusal() && includeDummy) {
			options.setAdministeredPercentage(0.0);
			options.setRefusedPercentage(0.5);
		} else if (!request.isIncludeAdmin() && request.isIncludeRefusal() && !includeDummy) {
			options.setAdministeredPercentage(0.0);
			options.setRefusedPercentage(1.0);
		} else if (!request.isIncludeAdmin() && !request.isIncludeRefusal() && includeDummy) {
			options.setAdministeredPercentage(0.0);
			options.setRefusedPercentage(0.0);
		}

		options.setComorbidityPercentage(request.isIncludeComorbidity() ? 0.4 : 0);
		options.setMissedPercentage(request.isIncludeMissed() ? 0.4 : 0);
		options.setSerologyPercentage(request.isIncludeSerology() ? 0.4 : 0);

		int count = Math.min(request.getMessageCount(), 1000);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			sb.append(TestCovidReporting.createHL7Message(options));
		}
		return sb.toString();
	}
}
