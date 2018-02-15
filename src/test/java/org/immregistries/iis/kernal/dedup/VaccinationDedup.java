package org.immregistries.iis.kernal.dedup;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.immregistries.vaccination_deduplication.Immunization;
import org.immregistries.vaccination_deduplication.ImmunizationSource;
import org.immregistries.vaccination_deduplication.LinkedImmunization;
import org.immregistries.vaccination_deduplication.VaccinationDeduplication;

import junit.framework.TestCase;

public class VaccinationDedup extends TestCase {
  public void testDeduplicateDeterministicPatient1() throws ParseException {

    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    LinkedImmunization patientRecords = new LinkedImmunization();
    {
      Immunization immunization = new Immunization();
      immunization.setCVX("10"); // IPV
      immunization.setDate(sdf.parse("10/01/2016"));
      immunization.setSource(ImmunizationSource.SOURCE);
      immunization.setOrganisationID("1");
      patientRecords.add(immunization);
    }
    {
      Immunization immunization = new Immunization();
      immunization.setCVX("03"); // MMR
      immunization.setDate(sdf.parse("10/01/2016"));
      immunization.setSource(ImmunizationSource.SOURCE);
      immunization.setOrganisationID("1");
      patientRecords.add(immunization);
    }
    {
      Immunization immunization = new Immunization();
      immunization.setCVX("89"); // Polio
      immunization.setDate(sdf.parse("10/01/2016"));
      immunization.setSource(ImmunizationSource.HISTORICAL);
      immunization.setOrganisationID("2");
      patientRecords.add(immunization);
    }

    VaccinationDeduplication vaccinationDeduplication = VaccinationDeduplication.getInstance();
    vaccinationDeduplication.initialize();

    ArrayList<LinkedImmunization> result =
        vaccinationDeduplication.deduplicateDeterministic(patientRecords);

    int i = 0;
    for (LinkedImmunization li : result) 
    {
      i++;
      System.out.println("Immunization " + i + ": " + li.getType());
      for (Immunization immunization: li)
      {
        System.out.println("  + " + immunization);
      }
    }
    


  }
}
