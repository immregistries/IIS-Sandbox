package org.immregistries.iis.kernal.logic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.dqa.hl7util.parser.HL7Reader;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

public class IncomingMessageHandler {
  // TODO: 
  //   Organize logic classes, need to have access classes for every object, maybe a new Access package? 
  //   Look at names of database fields, make more consistent

  private Session dataSession = null;

  public IncomingMessageHandler(Session dataSession) {
    this.dataSession = dataSession;
  }

  public String process(String message, OrgAccess orgAccess) throws ProcessingException {
    HL7Reader reader = new HL7Reader(message);
    String nameFirst = "";
    String nameLast = "";
    String nameMiddle = "";
    String address = "";
    String city = "";
    String state = "";
    String zip = "";
    String phone = "";
    String addressFrag = "";
    String patientReportedExternalLink = "";
    Date birthDate = null;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    if (reader.advanceToSegment("PID")) {
    	  patientReportedExternalLink = reader.getValueBySearchingRepeats(3, 1, "MR", 5);
      nameLast = reader.getValue(5, 1);
      nameFirst = reader.getValue(5, 2);
      nameMiddle = reader.getValue(5, 3);
      address = reader.getValue(11, 1);
      city = reader.getValue(11, 3);
      state = reader.getValue(11, 4);
      zip = reader.getValue(11, 5);
      phone = reader.getValue(13, 7);
      if (zip.length() > 5) {
          zip = zip.substring(0, 5);
        }
        int spaceIndex = address.indexOf(" ");
        address = address.substring(0, spaceIndex);
        addressFrag = zip + ":" + address;
      try {
        birthDate = simpleDateFormat.parse(reader.getValue(7));
      } catch (ParseException e) {
        throw new ProcessingException("Could not read date of birth");
      }
    }

    if (patientReportedExternalLink.equals("")) {
      throw new ProcessingException("MRN was not found, required for accepting vaccination report");
    }



    System.out.println("--> nameFirst = " + nameFirst);
    System.out.println("--> nameLast = " + nameLast);
    System.out.println("--> address = " + address);
    System.out.println("--> city = " + city);
    System.out.println("--> state = " + state);
    System.out.println("--> zip = " + zip);
    System.out.println("--> phone = " + phone);

    PatientReported patientReported = null;
    PatientMaster patient = null;
    {
      Query query =
          dataSession.createQuery("from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
      query.setParameter(0, orgAccess.getOrg());
      query.setParameter(1, patientReportedExternalLink);
      List<PatientReported> patientReportedList = query.list();
      if (patientReportedList.size() > 0) {
        patientReported = patientReportedList.get(0);
        patient = patientReported.getPatient();
      }
    }

    if (patientReported == null) {
      patient = new PatientMaster();
      patient.setPatientExternalLink(generatePatientExternalLink());
      patientReported = new PatientReported();
      patientReported.setOrgReported(orgAccess.getOrg());
      patientReported.setPatientReportedExternalLink(patientReportedExternalLink);
      patientReported.setPatient(patient);
      patientReported.setReportedDate(new Date());
    }
    patient.setPatientNameLast(nameLast);
    patient.setPatientNameFirst(nameFirst);
    patient.setPatientNameMiddle(nameMiddle);
    patient.setPatientAddressFrag(addressFrag);
    patient.setPatientPhoneFrag(phone);
    patient.setPatientBirthDate(birthDate);
    patient.setPatientSoundexFirst("Blah"); // TODO, later
    patient.setPatientSoundexLast("Blah");  // TODO, later
    patientReported.setUpdatedDate(new Date());
    String patientData = "";
    reader.resetPostion();
    while(!(reader.getSegmentName().equals("ORC"))) {
    	  patientData += reader.getOriginalSegment() + "/r";
    	  reader.advance();
    }
    patientReported.setPatientData(patientData);
    {
      Transaction transaction = dataSession.beginTransaction();
      dataSession.saveOrUpdate(patient);
      dataSession.saveOrUpdate(patientReported);
      transaction.commit();
    }
    while (reader.getSegmentName().equals("ORC")) {
    	  String vaccineData = "";
    	  VaccinationReported vaccinationReported = null;
      VaccinationMaster vaccination = null;
    	  String ndcCode = "";
      Date adminDate = null;
      String vaccinationReportedExternalLink = reader.getValue(3);
      if (vaccinationReportedExternalLink.equals("")) {
        throw new ProcessingException("Vaccination order id was not found, unable to process");
      }
      vaccineData += reader.getOriginalSegment() + "/r";
      if (reader.advanceToSegment("RXA")) {
    	  	ndcCode = reader.getValue(5, 1);
        try {
            adminDate = simpleDateFormat.parse(reader.getValue(3,1));
          } catch (ParseException e) {
            throw new ProcessingException("Could not read administered date");
          }
//        System.out.println("--> ndcCode = " + ndcCode);
//        System.out.println("--> adminDate = " + adminDate);
        {
          Query query = dataSession.createQuery(
              "from VaccinationReported where patientReported = ? and vaccinationReportedExternalLink = ?");
          query.setParameter(0, patientReported);
          query.setParameter(1, vaccinationReportedExternalLink);
          List<VaccinationReported> vaccinationReportedList = query.list();
          if (vaccinationReportedList.size() > 0) {
        	  	vaccinationReported = vaccinationReportedList.get(0);
        	  	vaccination = vaccinationReported.getVaccination();
          }
        }
        if (vaccinationReported == null) {
        		vaccination = new VaccinationMaster();
        		vaccinationReported = new VaccinationReported();
        		vaccinationReported.setVaccination(vaccination);
        		vaccination.setVaccinationReported(null);
        		vaccinationReported.setReportedDate(new Date());
        		vaccinationReported.setVaccinationReportedExternalLink(vaccinationReportedExternalLink);
        }
        vaccinationReported.setPatientReported(patientReported);
        vaccination.setPatient(patient);
        vaccination.setVaccineCvxCode(ndcCode); // TODO: need to change to cvx
        vaccination.setAdministeredDate(adminDate);
        vaccinationReported.setUpdatedDate(new Date());
        while (!(reader.getSegmentName().equals("ORC")) ) {
        	  vaccineData += reader.getOriginalSegment() + "/r";
        	  if (!(reader.advance())) {
        		  break;
        	  }
        }
        vaccinationReported.setVaccinationData(vaccineData);
        {
          Transaction transaction = dataSession.beginTransaction();
          dataSession.saveOrUpdate(vaccination);
          dataSession.saveOrUpdate(vaccinationReported);
          vaccination.setVaccinationReported(vaccinationReported);
          dataSession.saveOrUpdate(vaccination);
          transaction.commit();
        }
      }
    }



    

    System.out.println("--> addressFrag = " + addressFrag);


//    String ndcCode = "";
//    String adminDate = "";
//    if (reader.advanceToSegment("RXA")) {
//      ndcCode = reader.getValue(5, 1);
//      adminDate = reader.getValue(3, 1);
//    }
//    System.out.println("--> ndcCode = " + ndcCode);
//    System.out.println("--> adminDate = " + adminDate);

    



    // do stuff here to put it in the database
    return "MSH|^~\\&|DCS|MYIIS|MYIIS||20090604000020-0500||ACK^V04^ACK|1234567|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS\r"
        + "MSA|AA|9299381";
  }

  private static final Random random = new Random();
  private static final char[] ID_CHARS =
      {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T',
          'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

  public String generatePatientExternalLink() {
    boolean keepLooking = true;
    int count = 0;
    while (keepLooking) {
      count++;
      if (count > 1000) {
        throw new RuntimeException("Unable to get a new id, tried 1000 times!");
      }
      String patientExternalLink = generateId();
      Query query = dataSession.createQuery("from PatientMaster where patientExternalLink = ?");
      query.setParameter(0, patientExternalLink);
      if (query.list().size() == 0) {
        return patientExternalLink;
        // we found a unique id!
      }
    }
    return null;
  }

  public String generateId() {
    String patientRegistryId = "";
    for (int i = 0; i < 12; i++) {
      patientRegistryId += ID_CHARS[random.nextInt(ID_CHARS.length)];
    }
    return patientRegistryId;
  }
}
