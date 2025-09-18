package org.immregistries.iis.kernal.logic.shlink.evc;

import com.syadem.nuva.NUVA;
import com.syadem.nuva.SupportedLocale;
import com.syadem.nuva.Vaccine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class NuvaService {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	//	@Autowired
	private NUVA nuva;

	public NUVA getNuva() {
		if (nuva == null) {
			logger.info("NUVA LOADING");
			try {
				this.nuva = NUVA.load(SupportedLocale.English);
				Vaccine vaccine = nuva.getQueries().lookupVaccineByCode(519);
				Vaccine vaccine1 = nuva.getQueries().lookupVaccineByCode(1032);
				logger.info("NUVA Vaccines test {} {}", vaccine1, vaccine);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return nuva;
	}


	public NuvaService() {
	}
}
