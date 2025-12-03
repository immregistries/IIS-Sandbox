package org.immregistries.iis.kernal.servlet.shlink;

import org.immregistries.iis.kernal.servlet.TenantController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(TenantController.TENANT_PATH + CLVRController.CLVR_PATH_SUFFIX)
public class CLVRController {
	public static final String CLVR_PATH_SUFFIX = "/clvr";
	Logger logger = LoggerFactory.getLogger(this.getClass());
//
//	@Autowired
//	KeyStoreService keyStoreService;
//
//	@Autowired
//	ShLinkUtilService shLinkUtilService;
//
//	@Autowired
//	CLVRService clvrService;
//
//	@Autowired
//	IpsGeneratorSvcIIS ipsGeneratorSvcIIS;
//
//	@Autowired
//	IPartitionLookupSvc partitionLookupSvc;
//
//	@Autowired
//	FhirConversionUtil fhirConversionUtil;
//
//
//	@GetMapping("/{patientId}")
//	protected void doGetPatientCLVR(
//		HttpServletRequest req,
//		HttpServletResponse resp,
//		@PathVariable("patientId") String patientId,
//		@RequestParam(value = "pdf", required = false) boolean pdf
//	) throws IOException, ServletException, DataFormatException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, WriterException, COSEException {
//		Tenant tenant = CurrentTenantUtil.getTenantRedirectIfNone(req, resp);
//		UserAccess userAccess = UserAccessUtil.getUserAccess();
//		OutputStream outputStream = resp.getOutputStream();
//
//		IisKey iisSigningKey = keyStoreService.getIisSigningKeyOrCreate("", userAccess, tenant);
//
//		IBaseBundle ipsToBeEncoded = ipsGeneratorSvcIIS.generateIps(TenantUtil.requestDetailsWithPartitionName(partitionLookupSvc), new IdType(patientId), "");
//		CLVRPayload clvrPayload = fhirConversionUtil.toCLVRPayloadFromBundle((Bundle) ipsToBeEncoded);
//
//		String qrCode = clvrService.encodeCLVRtoQrCode(clvrPayload, iisSigningKey.keyPair());
//		logger.info("qrCode {}", qrCode);
//
//		if (!pdf) {
//			resp.setContentType("image/png"); // Set content type for PNG image
//			shLinkUtilService.printQrCodeAsImage(outputStream, qrCode);
//		} else {
//			PDDocument pdDocument = CLVRPdfUtil.createPdf(clvrPayload, qrCode.getBytes(), "IIS SANDBOX");
//			printPdf(req, resp, pdDocument, "testCLVR");
//		}
//	}
//
//
//	protected void printPdf(
//		HttpServletRequest req,
//		HttpServletResponse resp,
//		PDDocument pdDocument,
//		String name
//	) throws IOException {
//		resp.setContentType("application/pdf");
//		resp.setHeader("Content-Disposition", "attachment; filename=" + name);
//		pdDocument.save(resp.getOutputStream());
//		pdDocument.close();
//		resp.getOutputStream().flush();
//		resp.getOutputStream().close();
//	}

}
