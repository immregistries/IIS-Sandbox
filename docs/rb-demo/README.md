# RB demo: risk-based CDS over HL7 v2 (QBP Z45 / RSP Z42)

Demonstrates the draft "Enabling Risk-Based CDS through HL7 V2 Query and Response" interface.
IIS Sandbox plays the IIS. smm-tester (branch `rb-demo`) plays the EHR side.
Everything here is **scripted demo behavior built on a draft spec**; see [OPEN-ITEMS.md](OPEN-ITEMS.md).

## What the IIS does with a Z45 query

* MSH-21 `Z45^CDCPHINVS`. Patient matching is the same as for Z44 (MRN first, then name and DOB).
* The OBX segments after the QPD are contextual conditions (`code^description^99CDS`, OBX-2 `ID` Y/N or `DT`).
  They are not used for matching.
* Match found: RSP profile **Z42** with a *scripted* forecast per condition. Per recommendation group
  (same OBX-4 sub-ID): `30956-7` vaccine type, `59783-1` status (LA code), optional
  `30981-5`/`30980-7`/`59778-1` dates, `59779-9` schedule used, a category OBX (placeholder LOINC
  `99999-9`, 99VRC value), and one `30982-3` reason-for-recommendation OBX per condition that caused it.
* A condition code the script does not know: one `ERR` (warning, `103^Table value not found^HL70357`,
  location `OBX^n^3`) per code, the query is **not** rejected, MSA stays `AA`.
* No match: Z33 (`NF`), same as other profiles. Conditions are not stored.
* The script is the table in `RiskBasedScript.java` (`org.immregistries.iis.kernal.logic.rb`).

| Scenario | Conditions | Scripted result |
|---|---|---|
| Pregnancy | `007=Y,170=20260701` | Tdap (115), On schedule, High-Risk; window 27 to 36 weeks after onset (20270106 to 20270310); reasons 007 and 170 |
| RSV infant | `278=Y` | RSV-mAb (nirsevimab) not needed (306), Immune, Routine |
| Healthcare worker | `055=Y` | HepB (45) High-Risk and Influenza (88) Routine, reason 055 on both |
| Unknown code | `055=Y,999=Y` | same as healthcare worker plus an ERR for 999 |

## Run the demo

1. IIS Sandbox (deployed from branch `rb-demo`): <http://localhost:8080/iis-sandbox/home>,
   SOAP endpoint <http://localhost:8080/iis-sandbox/soap> (WSDL: `/soap?wsdl=true`). Orgs and users are
   created on first use: any user id, password and facility id works (the demo uses `rbdemo`/`rbdemo`/`RBDEMO`).
2. smm-tester `rb-demo` (build with `mvn -DskipTests package`, deploy `smm-web/target/smm.war`).
   Create a patient in **Manual Query**, pick Query Type `QBP-Z45`, enter conditions such as
   `007=Y,170=20260701`, press Query, then Submit the generated message to a SOAP connection pointing at
   the IIS URL above. Send the patient's VXU first so the IIS knows the patient (**Update/Send VXU**).
3. Command line alternative: `run-scenarios.sh` sends each VXU in `vxu/` and then the Z45 query using the
   `SendHl7` class in smm-core. Output goes to `rsp/`. The generated queries are in `queries/`.

Files: `vxu/` patients, `queries/` Z45 requests, `rsp/` captured request and response per scenario.
