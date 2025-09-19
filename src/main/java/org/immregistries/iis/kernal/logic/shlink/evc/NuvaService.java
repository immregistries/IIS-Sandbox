package org.immregistries.iis.kernal.logic.shlink.evc;

import com.syadem.nuva.Code;
import com.syadem.nuva.NUVA;
import com.syadem.nuva.Vaccine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

	public Optional<Vaccine> findByCvx(String cvxCode) {
		logger.info("list nomenclature {}", nuva.getQueries().getAllNomenclatures());

		Stream<Vaccine> vaccineStream = StreamSupport.stream(nuva.getVaccineRepository().spliterator(), true);

		Optional<Vaccine> first = vaccineStream.filter(vaccine -> {
			Optional<Code> cvx = vaccine.getCodesList().stream()
				.filter(code -> "CVX".equals(code.getNomenclature()) && cvxCode.equals(code.getValue())).findFirst();
			return cvx.isPresent();
		}).findFirst();
//
//		logger.info("Nuva found ? {}", first.isPresent());
//		if (first.isPresent()) {
//			logger.info("Nuva found \n {} ", first.get());
//		}
		return first;
	}
}
