package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.controllers.servlet.TenantController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

//@RestController
@RequestMapping(TenantController.TENANT_PATH + "/fhir/*")
public class FhirController {

	@Autowired
	private RestfulServer restfulServer;

	@RequestMapping()
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendRedirect("/fhir");
	}

//	@GetMapping()
//	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		this.handleRequest(RequestTypeEnum.GET, request, response);
//	}
//
//	@RequestMapping(method = RequestMethod.OPTIONS)
//	protected void doOptions(HttpServletRequest theReq, HttpServletResponse theResp) throws ServletException, IOException {
//		this.handleRequest(RequestTypeEnum.OPTIONS, theReq, theResp);
//	}
//
//	@PostMapping()
//	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		this.handleRequest(RequestTypeEnum.POST, request, response);
//	}
//
//	@PutMapping()
//	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		this.handleRequest(RequestTypeEnum.PUT, request, response);
//	}
}
