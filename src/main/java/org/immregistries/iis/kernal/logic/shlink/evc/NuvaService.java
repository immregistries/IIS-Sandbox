package org.immregistries.iis.kernal.logic.shlink.evc;

import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class NuvaService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	public NuvaService() {
		Model model = ModelFactory.createDefaultModel();

		// Define the path to your RDF file
		String filePath = "/terminologie-nuva-1.0.992.rdf";

		try (InputStream in = this.getClass().getResourceAsStream(filePath)) {
			// Read the RDF file into the model
			// The "RDF/XML" parameter specifies the language. Other options include "TTL" for Turtle, "N3", etc.
			model.read(in, null, "RDF/XML");

			// Now the model contains the data from the RDF file
			System.out.println("Model loaded successfully. Number of statements: " + model.size());

			// You can now query or iterate over the model
			// For example, list all statements
			StmtIterator iter = model.listStatements();
			while (iter.hasNext()) {
				Statement stmt = iter.nextStatement();
				Resource subject = stmt.getSubject();
				Property predicate = stmt.getPredicate();
				RDFNode object = stmt.getObject();


				logger.info("RDF read\n subject: {}\n predicate: {}\n object: {}\n", subject.toString(), predicate.toString(), object.toString());
				break;
			}

			NodeIterator nodes = model.listObjects();
			while (nodes.hasNext()) {
				RDFNode node = nodes.nextNode();
				logger.info("RDF read node \n {}", node);
				break;
			}
			ResIterator subjects = model.listSubjects();
			while (subjects.hasNext()) {
				Resource resource = subjects.nextResource();
				logger.info("RDF read resource \n {}", resource);
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
