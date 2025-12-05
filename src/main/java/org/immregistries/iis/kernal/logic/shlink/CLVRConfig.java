package org.immregistries.iis.kernal.logic.shlink;

import com.syadem.nuva.NUVA;
import com.syadem.nuva.SupportedLocale;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregitries.clvr.*;
import org.immregitries.clvr.impl.*;
import org.immregitries.clvr.mapping.FhirConversionUtil;
import org.immregitries.clvr.mapping.FhirConversionUtilR4;
import org.immregitries.clvr.mapping.FhirConversionUtilR5;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class CLVRConfig {


	@Bean
	public NUVA nuva() throws IOException {
		return NUVA.load(SupportedLocale.English);
	}


	@Bean
	public NUVAService nuvaService(NUVA nuva) throws IOException {
		return new NUVAService(nuva);
	}

	@Bean
	public CLVRPdfService clvrPdfService(NUVAService nuvaService) {
		return new CLVRPdfServiceImpl(nuvaService);
	}

	@Bean
	public SigningService signingService() {
		return new SigningServiceImpl();
	}

	@Bean
	public CborService cborService() {
		return new CborServiceImpl();
	}

	@Bean
	public QrCodeService qrCodeService() {
		return new QrCodeServiceImpl();
	}

	@Bean
	@Conditional(OnR4Condition.class)
	public FhirConversionUtilR4 fhirConversionUtilR4(NUVAService nuvaService) {
		return new FhirConversionUtilR4(nuvaService);
	}

	@Bean
	@Conditional(OnR5Condition.class)
	public FhirConversionUtilR5 fhirConversionUtilR5(NUVAService nuvaService) {
		return new FhirConversionUtilR5(nuvaService);
	}

	@Bean
	public CLVRService clvrService(SigningService signingService,
											 CborService cborService,
											 QrCodeService qrCodeService) {
		return new CLVRServiceImpl(signingService, cborService, qrCodeService);
	}


}
