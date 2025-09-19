package org.immregistries.iis.kernal.logic.shlink.evc;

import com.syadem.nuva.NUVA;
import com.syadem.nuva.Vaccine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NuvaService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private NUVA nuva;

	public NUVA getNuva() {
		Vaccine vaccine = nuva.getQueries().lookupVaccineByCode(519);
		Vaccine vaccine1 = nuva.getQueries().lookupVaccineByCode(1032);
		logger.info("NUVA Vaccines test 1032 {} 519 {}", vaccine1, vaccine);
		return nuva;
	}


	public NuvaService() {
	}
}
