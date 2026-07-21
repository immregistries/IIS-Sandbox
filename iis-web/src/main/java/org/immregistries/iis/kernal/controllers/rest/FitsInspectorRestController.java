package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.vfa.connect.IISConnector;
import org.immregistries.vfa.connect.IISConnector.ParseDebugLine;
import org.immregistries.vfa.connect.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(IisRestPath.BasePath.REST_PATH + "/fits")
public class FitsInspectorRestController {

	@Autowired
	private CodeMapRestController codeMapRestController;

	public static class InspectRequest {
		private String rspMessage;
		private String messageName;

		public InspectRequest() {
		}

		public InspectRequest(String rspMessage, String messageName) {
			this.rspMessage = rspMessage;
			this.messageName = messageName;
		}

		public String getRspMessage() {
			return rspMessage;
		}

		public void setRspMessage(String rspMessage) {
			this.rspMessage = rspMessage;
		}

		public String getMessageName() {
			return messageName;
		}

		public void setMessageName(String messageName) {
			this.messageName = messageName;
		}
	}

	public static class ForecastActualDto {
		private String vaccineGroup;
		private String adminStatus;
		private String validDate;
		private String dueDate;
		private String overdueDate;
		private String vaccineCvx;

		public ForecastActualDto() {
		}

		public ForecastActualDto(String vaccineGroup, String adminStatus, String validDate,
		                         String dueDate, String overdueDate, String vaccineCvx) {
			this.vaccineGroup = vaccineGroup;
			this.adminStatus = adminStatus;
			this.validDate = validDate;
			this.dueDate = dueDate;
			this.overdueDate = overdueDate;
			this.vaccineCvx = vaccineCvx;
		}

		public String getVaccineGroup() {
			return vaccineGroup;
		}

		public String getAdminStatus() {
			return adminStatus;
		}

		public String getValidDate() {
			return validDate;
		}

		public String getDueDate() {
			return dueDate;
		}

		public String getOverdueDate() {
			return overdueDate;
		}

		public String getVaccineCvx() {
			return vaccineCvx;
		}
	}

	public static class ParseDebugLineDto {
		private String lineStatus;
		private String line;
		private String lineStatusReason;

		public ParseDebugLineDto() {
		}

		public ParseDebugLineDto(String lineStatus, String line, String lineStatusReason) {
			this.lineStatus = lineStatus;
			this.line = line;
			this.lineStatusReason = lineStatusReason;
		}

		public String getLineStatus() {
			return lineStatus;
		}

		public String getLine() {
			return line;
		}

		public String getLineStatusReason() {
			return lineStatusReason;
		}
	}

	public static class VaccineGroupCount {
		private String label;
		private int count;

		public VaccineGroupCount() {
		}

		public VaccineGroupCount(String label, int count) {
			this.label = label;
			this.count = count;
		}

		public String getLabel() {
			return label;
		}

		public int getCount() {
			return count;
		}
	}

	public static class FitsInspectResult {
		private List<ForecastActualDto> forecastActuals;
		private List<ParseDebugLineDto> parseDebugLines;
		private Map<String, List<FamilyMappingEntry>> familyMapping;
		private List<VaccineGroupCount> vaccineGroupCounts;
		private String junitCode;

		public FitsInspectResult() {
		}

		public FitsInspectResult(List<ForecastActualDto> forecastActuals, List<ParseDebugLineDto> parseDebugLines,
		                         Map<String, List<FamilyMappingEntry>> familyMapping,
		                         List<VaccineGroupCount> vaccineGroupCounts, String junitCode) {
			this.forecastActuals = forecastActuals;
			this.parseDebugLines = parseDebugLines;
			this.familyMapping = familyMapping;
			this.vaccineGroupCounts = vaccineGroupCounts;
			this.junitCode = junitCode;
		}

		public List<ForecastActualDto> getForecastActuals() {
			return forecastActuals;
		}

		public List<ParseDebugLineDto> getParseDebugLines() {
			return parseDebugLines;
		}

		public Map<String, List<FamilyMappingEntry>> getFamilyMapping() {
			return familyMapping;
		}

		public List<VaccineGroupCount> getVaccineGroupCounts() {
			return vaccineGroupCounts;
		}

		public String getJunitCode() {
			return junitCode;
		}
	}

	public static class FamilyMappingEntry {
		private String label;
		private int count;

		public FamilyMappingEntry() {
		}

		public FamilyMappingEntry(String label, int count) {
			this.label = label;
			this.count = count;
		}

		public String getLabel() {
			return label;
		}

		public int getCount() {
			return count;
		}
	}

	@PostMapping("/inspect")
	public FitsInspectResult inspect(@RequestBody InspectRequest request) throws IOException, ParseException {
		String rsp = request.getRspMessage();
		String messageName = request.getMessageName() != null
			? request.getMessageName().replaceAll("\\s", "_")
			: "testCase";

		Software software = new Software();
		SoftwareResult softwareResult = new SoftwareResult();
		IISConnector c = new IISConnector(software, VaccineGroup.getForecastItemList());
		List<ForecastActual> forecastActualList = new ArrayList<>();
		TestCase testCase = IISConnector.recreateTestCase(rsp);
		List<ParseDebugLine> parseDebugLineList = new ArrayList<>();
		c.readRSP(forecastActualList, testCase, softwareResult, rsp, parseDebugLineList);

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

		List<ForecastActualDto> forecastDtos = forecastActualList.stream().map(fa ->
			new ForecastActualDto(
				fa.getVaccineGroup().getLabel(),
				fa.getAdminStatus(),
				fa.getValidDate() != null ? sdf.format(fa.getValidDate()) : null,
				fa.getDueDate() != null ? sdf.format(fa.getDueDate()) : null,
				fa.getOverdueDate() != null ? sdf.format(fa.getOverdueDate()) : null,
				fa.getVaccineCvx()
			)
		).collect(Collectors.toList());

		List<ParseDebugLineDto> debugDtos = parseDebugLineList.stream().map(pdl ->
			new ParseDebugLineDto(
				pdl.getLineStatus().toString(),
				pdl.getLine(),
				pdl.getLineStatusReason()
			)
		).collect(Collectors.toList());

		Map<String, List<VaccineGroup>> rawFamilyMapping = c.getFamilyMapping();
		CodeMap codeMap = codeMapRestController.getCodeMaps(null);
		Map<String, List<FamilyMappingEntry>> familyMapping = new LinkedHashMap<>();
		for (Map.Entry<String, List<VaccineGroup>> entry : rawFamilyMapping.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				int count = (int) forecastActualList.stream()
					.filter(fa -> fa.getVaccineCvx().equals(entry.getKey()))
					.count();
				List<FamilyMappingEntry> entries = entry.getValue().stream()
					.map(vg -> new FamilyMappingEntry(vg.getLabel(), count))
					.collect(Collectors.toList());
				familyMapping.put(entry.getKey(), entries);
			}
		}

		List<VaccineGroup> vaccineGroupList = new ArrayList<>(VaccineGroup.getForecastItemList());
		vaccineGroupList.sort(Comparator.comparing(VaccineGroup::getLabel));
		List<VaccineGroupCount> vaccineGroupCounts = vaccineGroupList.stream().map(vg -> {
			int count = (int) forecastActualList.stream()
				.filter(fa -> fa.getVaccineGroup().equals(vg))
				.count();
			return new VaccineGroupCount(vg.getLabel(), count);
		}).filter(vgc -> vgc.getCount() > 0).collect(Collectors.toList());

		String junitCode = buildJunitCode(rsp, messageName, forecastActualList, testCase);

		return new FitsInspectResult(forecastDtos, debugDtos, familyMapping, vaccineGroupCounts, junitCode);
	}

	private String buildJunitCode(String rsp, String messageName,
	                              List<ForecastActual> forecastActualList, TestCase testCase) throws IOException {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

		sb.append("exampleMap.put(\"").append(messageName).append("\", RSP_")
			.append(messageName.toUpperCase()).append(");\n\n");
		sb.append("private static final String RSP_").append(messageName.toUpperCase()).append(" = \"\" \n");

		BufferedReader in = new BufferedReader(new StringReader(rsp));
		String line;
		while ((line = in.readLine()) != null) {
			sb.append("    + \"");
			int pos = line.indexOf('\\');
			while (pos >= 0) {
				sb.append(line, 0, pos);
				sb.append("\\\\");
				line = line.substring(pos + 1);
				pos = line.indexOf('\\');
			}
			sb.append(line).append("\\r\"\n");
		}
		in.close();
		sb.append("  ;\n");

		sb.append("  @Test\n");
		sb.append("  public void testRSP_").append(messageName).append("() throws Exception {\n");
		sb.append("    List<ForecastActual> forecastActualList = new ArrayList<ForecastActual>();\n");
		sb.append("    TestCase testCase = run(forecastActualList, RSP_")
			.append(messageName.toUpperCase()).append(");\n");
		sb.append("    SimpleDateFormat sdf = new SimpleDateFormat(\"MM/dd/yyyy\");\n");
		sb.append("    assertEquals(\"Not all test events read\", ")
			.append(testCase.getTestEventList().size())
			.append(", testCase.getTestEventList().size()); \n");

		int posA = 0;
		for (TestEvent testEvent : testCase.getTestEventList()) {
			if (testEvent.getEvaluationActualList() != null && !testEvent.getEvaluationActualList().isEmpty()) {
				sb.append("    assertEquals(\"Wrong number of evaluations\", ")
					.append(testEvent.getEvaluationActualList().size())
					.append(", testCase.getTestEventList().get(").append(posA)
					.append(").getEvaluationActualList().size()); \n");
				int posB = 0;
				for (EvaluationActual ea : testEvent.getEvaluationActualList()) {
					if (ea.getVaccineCvx() != null) {
						sb.append("    assertEquals(\"Wrong CVX found\", \"")
							.append(ea.getVaccineCvx()).append("\", testCase.getTestEventList().get(")
							.append(posA).append(").getEvaluationActualList().get(").append(posB)
							.append(").getVaccineCvx()); \n");
						sb.append("    assertEquals(\"Wrong validity found\", \"")
							.append(ea.getDoseValid()).append("\", testCase.getTestEventList().get(")
							.append(posA).append(").getEvaluationActualList().get(").append(posB)
							.append(").getDoseValid()); \n");
					}
					posB++;
				}
			}
			posA++;
		}

		sb.append("    assertEquals(\"Not all forecasts read\", ")
			.append(forecastActualList.size()).append(",forecastActualList.size()); \n");

		posA = 0;
		for (ForecastActual fa : forecastActualList) {
			sb.append("    assertEquals(\"Forecast not found\", \"")
				.append(fa.getVaccineGroup().getLabel()).append("\", forecastActualList.get(")
				.append(posA).append(").getVaccineGroup().getLabel()); \n");
			sb.append("    assertEquals(\"Wrong status found\", \"")
				.append(fa.getAdminStatus()).append("\", forecastActualList.get(")
				.append(posA).append(").getAdminStatus()); \n");
			appendDateAssertion(sb, "Valid", "getValidDate", fa.getValidDate(), sdf, posA);
			appendDateAssertion(sb, "Due", "getDueDate", fa.getDueDate(), sdf, posA);
			appendDateAssertion(sb, "Overdue", "getOverdueDate", fa.getOverdueDate(), sdf, posA);
			posA++;
		}

		sb.append("  }\n");
		return sb.toString();
	}

	private void appendDateAssertion(StringBuilder sb, String label, String getter,
	                                 Date date, SimpleDateFormat sdf, int pos) {
		if (date == null) {
			sb.append("    assertNull(\"").append(label)
				.append(" date should be null\", forecastActualList.get(")
				.append(pos).append(").").append(getter).append("()); \n");
		} else {
			sb.append("    assertNotNull(\"").append(label)
				.append(" date should not be null\", forecastActualList.get(")
				.append(pos).append(").").append(getter).append("()); \n");
			sb.append("    assertEquals(\"Wrong ").append(label.toLowerCase())
				.append(" date found\", \"").append(sdf.format(date))
				.append("\", sdf.format(forecastActualList.get(")
				.append(pos).append(").").append(getter).append("())); \n");
		}
	}
}
