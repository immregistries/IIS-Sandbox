package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.export.Exporter;
import org.mitre.synthea.helpers.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController()
@RequestMapping("Synthea")
public class SyntheaController {
	@Autowired
	FhirContext fhirContext;
	@Autowired
	IFhirResourceDao<Patient> patientIFhirResourceDao;

	@GetMapping
	public String get() {
		Generator.GeneratorOptions options = new Generator.GeneratorOptions();
		options.population = 1;
		Config.set("exporter.fhir.export", "false");
		Config.set("exporter.hospital.fhir.export", "false");
		Config.set("exporter.practitioner.fhir.export", "false");
		options.singlePersonSeed = 1220L;
		options.enabledModules = new ArrayList<>(1);
		options.enabledModules.add("Immunization");
		Exporter.ExporterRuntimeOptions ero = new Exporter.ExporterRuntimeOptions();
		ero.enableQueue(Exporter.SupportedFhirVersion.R4);

		// Create and start generator
		Generator generator = new Generator(options, ero);
		ExecutorService generatorService = Executors.newFixedThreadPool(1);
		generatorService.submit(() -> generator.run());


//		IParser parser  = fhirContext.newJsonParser().setPrettyPrint(true);
		String result = "";
		try {
			int recordCount = 0;
			while(recordCount < options.population) {
				try {
					String jsonRecord = ero.getNextRecord();
					result = jsonRecord;
					recordCount++;
					patientIFhirResourceDao.create(fhirContext.newJsonParser().parseResource(Patient.class,jsonRecord), ServletHelper.requestDetailsWithPartitionName());
				} catch (InterruptedException ex) {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


		return result;
	}
}
