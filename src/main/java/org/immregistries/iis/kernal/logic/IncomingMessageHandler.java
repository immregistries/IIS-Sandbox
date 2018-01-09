package org.immregistries.iis.kernal.logic;

import org.immregistries.dqa.hl7util.parser.HL7Reader;

public class IncomingMessageHandler {
  public String process(String message) {
    HL7Reader reader = new HL7Reader(message);
    String nameFirst = "";
    String nameLast = "";
    String address = "";
    String city = "";
    String state = "";
    String zip = "";
    String phone = "";
    String addressFrag = "";
    if (reader.advanceToSegment("PID"))
    {
      nameLast = reader.getValue(5, 1);
      nameFirst = reader.getValue(5, 2);
      address = reader.getValue(11, 1);
      city = reader.getValue(11, 3);
      state = reader.getValue(11, 4);
      zip = reader.getValue(11, 5);
      phone = reader.getValue(13, 7);
    }
    
    System.out.println("--> nameFirst = " + nameFirst);
    System.out.println("--> nameLast = " + nameLast);
    System.out.println("--> address = " + address);
    System.out.println("--> city = " + city);
    System.out.println("--> state = " + state);
    System.out.println("--> zip = " + zip);
    System.out.println("--> phone = " + phone);
    
    if (zip.length()>5)
    {
    	 zip = zip.substring(0, 5);
    }
    int spaceIndex = address.indexOf(" ");
    address = address.substring(0, spaceIndex);
    addressFrag = zip + ":" + address;
    
    System.out.println("--> addressFrag = " + addressFrag);
    
    
    String ndcCode = "";
    String adminDate = "";
    if (reader.advanceToSegment("RXA"))
    {
    		ndcCode = reader.getValue(5,1);
    		adminDate = reader.getValue(3,1);
    }
    System.out.println("--> ndcCode = " + ndcCode);
    System.out.println("--> adminDate = " + adminDate);
    
//    if (reader.advanceToSegment("OBX", "RXA"))
//    {
//    		if (reader.getValue(1, 1)=="1")
//    		{
//    			if (reader.advanceToSegment("OBX"))
//    		}
//    }
    
    
    
    // do stuff here to put it in the database
    return "MSH|^~\\&|DCS|MYIIS|MYIIS||20090604000020-0500||ACK^V04^ACK|1234567|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS\r"
        + "MSA|AA|9299381";
  }
}
