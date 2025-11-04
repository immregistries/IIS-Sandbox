package org.immregistries.iis.kernal.logic.shlink;

import com.syadem.nuva.NUVA;
import com.syadem.nuva.SupportedLocale;
import org.immregitries.clvr.*;
import org.springframework.context.annotation.Bean;
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
	public SigningService signingService()  {
		return  new SigningService();
	}
	@Bean
	public CborService cborService() {
		return  new CborService();
	}

	@Bean
	public QrCodeService qrCodeService()  {
		return  new QrCodeService();
	}

	@Bean
	public FhirConversionUtil fhirConversionUtil(NUVAService nuvaService) {
		return  new FhirConversionUtil(nuvaService);
	}

	@Bean
	public CLVRService clvrService(SigningService signingService,
											 CborService cborService,
											 QrCodeService qrCodeService) {
		return  new CLVRService(signingService,cborService,qrCodeService);
	}




}
