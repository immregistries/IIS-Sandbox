# IIS Sandbox on FHIR Walkthrough

You can find examples of resources in the xml files provided in the same directory

Any testing API platform such  as Postman or Insomnia can be used

## Multitenancy

### Authentication

The server uses a tenant id specified in the Url as in
 - http://localhost:8080/iis-sandbox/fhir/tenantId/
   
along with a http basic authentication password

As an example, if you are using __POSTMAN__, the "AUTHORIZATION" section with type "BASIC AUTH" needs to be filled.

### Creating a new user

Introducing a new tenant id in a request, along with a http basic authentication header, will automatically create a new user in the system

## Requests

### Basic requests with patient resources
You can use examplePatient.xml if you don't have any other example :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Patient xmlns="http://hl7.org/fhir">
  <id value="example"/>
  <text>
    <status value="generated"/>
    <div xmlns="http://www.w3.org/1999/xhtml">
      <p> Henry Levin the 7th</p>
    </div>
  </text>
  <identifier>
    <use value="usual"/>
    <type>
      <coding>
        <system value="http://terminology.hl7.org/CodeSystem/v2-0203"/>
        <code value="MR"/>
      </coding>
    </type>
    <system value="urn:oid:2.16.840.1.113883.19.5"/>
    <value value="example"/>
  </identifier>
  <active value="true"/>
  <name>
    <family value="Levin"/>
    <given value="Henry"/>
  </name>
  <gender value="male"/>
  <birthDate value="1932-09-23"/>
  <managingOrganization>
    <reference value="Organization/2.16.840.1.113883.19.5"/>
    <display value="Good Health Clinic"/>
  </managingOrganization>
</Patient>
```

 - CREATE A PATIENT : 
    - **POST** http://localhost:8080/iis-sandbox/fhir/tenantId/Patient/
    
 - READ A PATIENT :
    - **GET** http://localhost:8080/iis-sandbox/fhir/tenantId/Patient/example

 - UPDATE A PATIENT :
    - **PUT** http://localhost:8080/iis-sandbox/fhir/tenantId/Patient/example

 - DELETE A PATIENT :
    - **DELETE** http://localhost:8080/iis-sandbox/fhir/tenantId/Patient/example
    


### Basic requests with Immunization resources
You can use testImmunization if you have no Immunization resource example :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Immunization xmlns="http://hl7.org/fhir">
    <id value="test"/>
    <text> <status value="generated"/> <div xmlns="http://www.w3.org/1999/xhtml"><p> <b> Generated Narrative with Details</b> </p> <p> <b> id</b> : example</p> <p> <b> identifier</b> : test</p> <p> <b> status</b> : completed</p> <p> <b> vaccineCode</b> : Fluvax (Influenza) <span> (Details : {urn:oid:1.2.36.1.2001.1005.17 code 'FLUVAX' = 'Fluvax)</span> </p> <p> <b> patient</b> : <a> Patient/example</a> </p> <p> <b> encounter</b> : <a> Encounter/example</a> </p> <p> <b> occurrence</b> : 10/01/2013</p> <p> <b> primarySource</b> : true</p> <p> <b> location</b> : <a> Location/1</a> </p> <p> <b> manufacturer</b> : <a> Organization/hl7</a> </p> <p> <b> lotNumber</b> : TESTTEST</p> <p> <b> expirationDate</b> : 15/02/2015</p> <p> <b> site</b> : left arm <span> (Details : {http://terminology.hl7.org/CodeSystem/v3-ActSite code 'LA' = 'left arm', given
        as 'left arm'})</span> </p> <p> <b> route</b> : Injection, intramuscular <span> (Details : {http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration code 'IM' =
        'Injection, intramuscular', given as 'Injection, intramuscular'})</span> </p> <p> <b> doseQuantity</b> : 5 mg<span>  (Details: UCUM code mg = 'mg')</span> </p> <blockquote> <p> <b> performer</b> </p> <p> <b> function</b> : Ordering Provider <span> (Details : {http://terminology.hl7.org/CodeSystem/v2-0443 code 'OP' = 'Ordering Provider)</span> </p> <p> <b> actor</b> : <a> Practitioner/example</a> </p> </blockquote> <blockquote> <p> <b> performer</b> </p> <p> <b> function</b> : Administering Provider <span> (Details : {http://terminology.hl7.org/CodeSystem/v2-0443 code 'AP' = 'Administering Provider)</span> </p> <p> <b> actor</b> : <a> Practitioner/example</a> </p> </blockquote> <p> <b> note</b> : Notes on adminstration of vaccine</p> <p> <b> reasonCode</b> : Procedure to meet occupational requirement <span> (Details : {SNOMED CT code '429060002' = 'Procedure to meet occupational requirement)</span> </p> <p> <b> isSubpotent</b> : true</p> <h3> Educations</h3> <table> <tr> <td> -</td> <td> <b> DocumentType</b> </td> <td> <b> PublicationDate</b> </td> <td> <b> PresentationDate</b> </td> </tr> <tr> <td> *</td> <td> 253088698300010311120702</td> <td> 02/07/2012</td> <td> 10/01/2013</td> </tr> </table> <p> <b> programEligibility</b> : Not Eligible <span> (Details : {http://terminology.hl7.org/CodeSystem/immunization-program-eligibility code
        'ineligible' = 'Not Eligible)</span> </p> <p> <b> fundingSource</b> : Private <span> (Details : {http://terminology.hl7.org/CodeSystem/immunization-funding-source code 'private'
        = 'Private)</span> </p> </div> </text> <identifier>
    <system value="urn:ietf:rfc:3986"/>
    <value value="test"/>
</identifier>
    <status value="completed"/>
    <vaccineCode>
        <coding>
            <system value="urn:oid:1.2.36.1.2001.1005.17"/>
            <code value="FLUVAX"/>
        </coding>
        <text value="Fluvax (Influenza)"/>
    </vaccineCode>
    <patient>
        <reference value="Patient/example"/>
    </patient>
    <encounter>
        <reference value="Encounter/example"/>
    </encounter>
    <occurrenceDateTime value="2013-01-10"/>
    <primarySource value="true"/>
    <location>
        <reference value="Location/1"/>
    </location>
    <manufacturer>
        <reference value="Organization/hl7"/>
    </manufacturer>
    <lotNumber value="changedlot"/>
    <expirationDate value="2015-02-15"/>
    <site>
        <coding>
            <system value="http://terminology.hl7.org/CodeSystem/v3-ActSite"/>
            <code value="LA"/>
            <display value="left arm"/>
        </coding>
    </site>
    <route>
        <coding>
            <system value="http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration"/>
            <code value="IM"/>
            <display value="Injection, intramuscular"/>
        </coding>
    </route>
    <doseQuantity>
        <value value="5"/>
        <system value="http://unitsofmeasure.org"/>
        <code value="mg"/>
    </doseQuantity>
    <performer>
        <function>
            <coding>
                <system value="http://terminology.hl7.org/CodeSystem/v2-0443"/>
                <code value="OP"/>
            </coding>
        </function>
        <actor>
            <reference value="Practitioner/example"/>
        </actor>
    </performer>
    <performer>
        <function>
            <coding>
                <system value="http://terminology.hl7.org/CodeSystem/v2-0443"/>
                <code value="AP"/>
            </coding>
        </function>
        <actor>
            <reference value="Practitioner/example"/>
        </actor>
    </performer>
    <note>
        <text value="Notes on adminstration of vaccine"/>
    </note>
    <reasonCode>
        <coding>
            <system value="http://snomed.info/sct"/>
            <code value="429060002"/>
        </coding>
    </reasonCode>
    <isSubpotent value="true"/>
    <education>
        <documentType value="253088698300010311120702"/>
        <publicationDate value="2012-07-02"/>
        <presentationDate value="2013-01-10"/>
    </education>
    <programEligibility>
        <coding>
            <system value="http://terminology.hl7.org/CodeSystem/immunization-program-eligibility"/>
            <code value="ineligible"/>
        </coding>
    </programEligibility>
    <fundingSource>
        <coding>
            <system value="http://terminology.hl7.org/CodeSystem/immunization-funding-source"/>
            <code value="private"/>
        </coding>
    </fundingSource>
</Immunization>
```

- CREATE AN IMMUNIZATION :
    - **POST** http://localhost:8080/iis-sandbox/fhir/tenantId/Immunization

- READ AN IMMUNIZATION :
    - **GET** http://localhost:8080/iis-sandbox/fhir/tenantId/Immunization/test

- UPDATE AN IMMUNIZATION :
    - **PUT** http://localhost:8080/iis-sandbox/fhir/tenantId/Immunization/test
    
- DELETE AN IMMUNIZATION :
    - **DELETE** http://localhost:8080/iis-sandbox/fhir/tenantId/Immunization/test

## Linked Ressources and possible duplicates

The system imitates the **EMPI** system from Hapi Fhir and links  Patient resources with pre-existing resources if there are suspicions that they are related or duplicates

 - Patients are linked with Person Resources
 - Immunization are linked with MedicationAdministration Resources, using FHIR Extensions
### Creating a Patient with links :
- Initiating an example of a situation of linking
    - As an example you can make POST each of these resources
        - matchPatient.xml
        - match2Patient.xml
        - match1Patient.xml
    

- Visualize the Person Ressource and each link with its levels of assurance
    - **GET**  http://localhost:8080/iis-sandbox/fhir/tenantId/Person/patientWithLinks

### Creating an Immunization with links :
you can use dedupImmunization.xml :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Immunization xmlns="http://hl7.org/fhir">
  <id value="vaccinaction1"/>
  <text> <status value="generated"/> <div xmlns="http://www.w3.org/1999/xhtml"><p> <b> Generated Narrative with Details</b> </p> <p> <b> id</b> : example</p> <p> <b> identifier</b> : urn:oid:1.3.6.1.4.1.21367.2005.3.7.1234</p> <p> <b> status</b> : completed</p> <p> <b> vaccineCode</b> : Fluvax (Influenza) <span> (Details : {urn:oid:1.2.36.1.2001.1005.17 code 'FLUVAX' = 'Fluvax)</span> </p> <p> <b> patient</b> : <a> Patient/example</a> </p> <p> <b> encounter</b> : <a> Encounter/example</a> </p> <p> <b> occurrence</b> : 10/01/2013</p> <p> <b> primarySource</b> : true</p> <p> <b> location</b> : <a> Location/1</a> </p> <p> <b> manufacturer</b> : <a> Organization/hl7</a> </p> <p> <b> lotNumber</b> : AAJN11K</p> <p> <b> expirationDate</b> : 15/02/2015</p> <p> <b> site</b> : left arm <span> (Details : {http://terminology.hl7.org/CodeSystem/v3-ActSite code 'LA' = 'left arm', given
    as 'left arm'})</span> </p> <p> <b> route</b> : Injection, intramuscular <span> (Details : {http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration code 'IM' =
    'Injection, intramuscular', given as 'Injection, intramuscular'})</span> </p> <p> <b> doseQuantity</b> : 5 mg<span>  (Details: UCUM code mg = 'mg')</span> </p> <blockquote> <p> <b> performer</b> </p> <p> <b> function</b> : Ordering Provider <span> (Details : {http://terminology.hl7.org/CodeSystem/v2-0443 code 'OP' = 'Ordering Provider)</span> </p> <p> <b> actor</b> : <a> Practitioner/example</a> </p> </blockquote> <blockquote> <p> <b> performer</b> </p> <p> <b> function</b> : Administering Provider <span> (Details : {http://terminology.hl7.org/CodeSystem/v2-0443 code 'AP' = 'Administering Provider)</span> </p> <p> <b> actor</b> : <a> Practitioner/example</a> </p> </blockquote> <p> <b> note</b> : Notes on adminstration of vaccine</p> <p> <b> reasonCode</b> : Procedure to meet occupational requirement <span> (Details : {SNOMED CT code '429060002' = 'Procedure to meet occupational requirement)</span> </p> <p> <b> isSubpotent</b> : true</p> <h3> Educations</h3> <table> <tr> <td> -</td> <td> <b> DocumentType</b> </td> <td> <b> PublicationDate</b> </td> <td> <b> PresentationDate</b> </td> </tr> <tr> <td> *</td> <td> 253088698300010311120702</td> <td> 02/07/2012</td> <td> 10/01/2013</td> </tr> </table> <p> <b> programEligibility</b> : Not Eligible <span> (Details : {http://terminology.hl7.org/CodeSystem/immunization-program-eligibility code
    'ineligible' = 'Not Eligible)</span> </p> <p> <b> fundingSource</b> : Private <span> (Details : {http://terminology.hl7.org/CodeSystem/immunization-funding-source code 'private'
    = 'Private)</span> </p> </div> </text> <identifier>
  <system value="urn:ietf:rfc:3986"/>
  <value value="vaccination1"/>
</identifier>
  <status value="completed"/>
  <vaccineCode>
    <coding>
      <system value="urn:oid:1.2.36.1.2001.1005.17"/>
      <code value="10"/>
    </coding>
    <text value="Fluvax (Influenza)"/>
  </vaccineCode>
  <patient>
    <reference value="Patient/example"/>
  </patient>
  <encounter>
    <reference value="Encounter/example"/>
  </encounter>
  <occurrenceDateTime value="2019-02-10"/>
  <primarySource value="true"/>
  <location>
    <reference value="Location/1"/>
  </location>
  <manufacturer>
    <reference value="Organization/hl7"/>
  </manufacturer>
  <lotNumber value="AAJN11K"/>
  <expirationDate value="2015-02-15"/>
  <site>
    <coding>
      <system value="http://terminology.hl7.org/CodeSystem/v3-ActSite"/>
      <code value="LA"/>
      <display value="left arm"/>
    </coding>
  </site>
  <route>
    <coding>
      <system value="http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration"/>
      <code value="IM"/>
      <display value="Injection, intramuscular"/>
    </coding>
  </route>
  <doseQuantity>
    <value value="5"/>
    <system value="http://unitsofmeasure.org"/>
    <code value="mg"/>
  </doseQuantity>
  <performer>
    <function>
      <coding>
        <system value="http://terminology.hl7.org/CodeSystem/v2-0443"/>
        <code value="OP"/>
      </coding>
    </function>
    <actor>
      <reference value="Practitioner/example"/>
    </actor>
  </performer>
  <performer>
    <function>
      <coding>
        <system value="http://terminology.hl7.org/CodeSystem/v2-0443"/>
        <code value="AP"/>
      </coding>
    </function>
    <actor>
      <reference value="Practitioner/example"/>
    </actor>
  </performer>
  <note>
    <text value="Notes on adminstration of vaccine"/>
  </note>
  <reasonCode>
    <coding>
      <system value="http://snomed.info/sct"/>
      <code value="429060002"/>
    </coding>
  </reasonCode>
  <isSubpotent value="true"/>
  <education>
    <documentType value="253088698300010311120702"/>
    <publicationDate value="2012-07-02"/>
    <presentationDate value="2013-01-10"/>
  </education>
  <programEligibility>
    <coding>
      <system value="http://terminology.hl7.org/CodeSystem/immunization-program-eligibility"/>
      <code value="ineligible"/>
    </coding>
  </programEligibility>
  <fundingSource>
    <coding>
      <system value="http://terminology.hl7.org/CodeSystem/immunization-funding-source"/>
      <code value="private"/>
    </coding>
  </fundingSource>
</Immunization>
```
- Create two possible Duplicates 
    - **POST** http://localhost:8080/iis-sandbox/fhir/tenantId/Immunization
    - Then change the dates and repost

- Visualize the MedicationAdministration Resource 
    - **GET** http://localhost:8080/iis-sandbox/fhir/tenantId/MedicationAdministration/vaccineCODE







