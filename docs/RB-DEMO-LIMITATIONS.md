# RB demo: what is real and what is faked

This covers the risk-based CDS (RB) demo: IIS Sandbox branch `rb-demo` plus smm-tester branch `rb-demo`, as of November 17, 2026.
Every statement below is based on the code in those branches.

## The short answer

**No forecasting engine looks at the extra conditions.** When a risk-based query (QBP Z45) comes in, the IIS Sandbox does not call any forecaster, neither LoneStar nor ICE. It looks up each condition code in a small hard-coded table and returns the canned recommendation for that code. Nothing about the patient changes the answer: not age, not sex, not shot history.

What *is* real:
- The message plumbing. The demo receives a Z45 query, finds the patient with the IIS Sandbox's normal matching, reads the condition OBX segments, and builds a well-formed RSP (Z42 or Z33) with warnings for codes it does not know.
- The test patient and history setup in smm-tester. It uses the existing message generator and really sends a VXU first.

## Feature by feature

| Feature | Real or faked | How it actually works | Real work it would need |
|---|---|---|---|
| **Forecast engine** | **Faked** | For Z45 the IIS turns off its normal forecaster call (`sendBackForecast=false`). Recommendations come from a fixed table in `RiskBasedScript`. Each condition code maps to a set CVX, status and category, and sometimes a date rule. | Send the patient, history and conditions to a real CDS engine that understands risk conditions (for example a CDSi-based forecaster), then translate its answer. Or run the normal forecast and apply risk-based rules on top. |
| **Condition handling** | **Partly real** | Reading the conditions is real. The parser pulls each OBX after the QPD: code, code system (99CDS, also 99CDC), Y/N or date value, and sub-ID. What happens next is a table lookup: 13 codes are "known", but only 5 change the answer (007 pregnancy with 170 onset date, 278, 055, 045, 059). The other 8 known codes are silently accepted and do nothing. A Y/N value of N means "ignore". | A full value set and clinical rules for each condition, how conditions combine and conflict, and validation of values (for example a pregnancy onset date in the future). |
| **Age, sex and history** | **Faked (ignored)** | The function that builds the risk-based answer receives only the conditions and today's date. A man flagged pregnant gets Tdap. A 70-year-old with code 278 gets the infant RSV antibody answer. Prior doses never suppress a recommendation. | Feed the patient's DOB, sex and full history into the engine and let it decide eligibility, intervals and series status. |
| **Evaluation of past shots** | **Not done** | The patient's past shots are listed in the RSP as stored. The per-dose evaluation (dose valid or not) normally comes from the forecaster, and the forecaster is skipped for Z45, so no evaluation is returned. | Same as the forecast engine: the real engine supplies the evaluations. |
| **Dates in the recommendations** | **Faked (fixed offsets)** | Tdap: earliest and recommended 27 weeks after the onset date, latest 36 weeks. Influenza: today to today + 90 days. HepB, Yellow Fever, HepA, RSV: no dates at all. Pregnancy without an onset date: Tdap with no dates. | Dates computed by the engine from the schedule, the history and the condition. |
| **Codes and LOINCs** | **Placeholders in places** | The category OBX (Routine / SCDM / High-Risk) uses a made-up LOINC `99999-9` and made-up codes R, S and H, because the draft spec has none yet. The reason OBX uses LOINC 30982-3 with the draft's label text, not LOINC's official name. "Not needed" for RSV is shown as status Immune with a CVX label we wrote ("RSV-mAb (nirsevimab) - not needed"). The warning for an unknown condition (ERR 103, table value not found, severity W) is our choice, not the spec's. The vaccine type, status, date and schedule-used LOINCs and the LA status codes are the real, existing ones. | Swap in the assigned LOINC and value set when the spec publishes them, follow the final spec's example RSP, and agree on the error code with the workgroup. |
| **Z42 / Z33 responses** | **Real** | Patient matching is the IIS Sandbox's normal logic (the same as Z44). A match returns Z42 with the demographics, the history and the scripted forecast. No match returns Z33 (not found). Unknown conditions add ERR warnings without rejecting the query. Not covered: Z31 (multiple-match candidate list) is never returned for Z45, and the sandbox's special test modes (processing flavors) are ignored for Z45. | Decide the Z31 behavior for Z45 and add it. Add an end-to-end automated test (only unit tests exist today). |
| **smm RB Demo page inputs** | **Scripted test data** | The four buttons (Pregnancy/Tdap, RSV infant, Healthcare worker, Unknown code) each make up a random patient with the existing generator. They add a fixed, plausible history (for example MMR x2 plus an old Tdap) and prefill the condition string (for example `007=Y,170=<about 30 weeks ago>`). The page sends a real VXU, then a real Z45, over SOAP. The VXU and conditions are editable text boxes. Because the IIS ignores history, age and sex, the generated history is for realism only and does not affect the answer. | None for a demo. For real testing: pick conditions from a list (not free text), and add scenarios where the history *should* change the answer. |
| **VCI / SMART Health Card page** | **Cosmetic, unsigned** | `/vciDemo` turns an RSP into FHIR Patient and Immunization and then into an unsigned SMART Health Card payload. Its issuer URL (`iss`) is just a hard-coded string. The "Generate JWS" step says "not implemented": nothing is signed, there is no key, and nothing is published at the issuer URL. This page is separate from the RB demo. | Generate a signing key, sign the card (JWS), publish the public key at `<issuer>/.well-known/jwks.json`, and set the issuer URL to that real address. |
| **Logging and storage** | **Real (normal sandbox behavior)** | The conditions are not saved as patient data; they only shape the one answer. But like every query, the full Z45 message, conditions included, is saved in the sandbox's message log (`message_received`). The demo org and user (RBDEMO) are created automatically on first use. | Decide whether conditions may be kept in the message log. If not, redact the OBXs before logging. |

## What would make it "legitimate"

1. A real engine that takes the conditions as input together with age, sex and history.
2. The final spec's codes and example response.
3. An end-to-end test of the Z45 path.
4. A decision on what the IIS may store.

The message format work, the parsing, and the EHR-side query building are already done and are reusable.

## Appendix: where to look in the code

IIS Sandbox (`rb-demo`):
- `src/main/java/org/immregistries/iis/kernal/logic/rb/RiskBasedScript.java`: the hard-coded table of codes and recommendations, plus the placeholder category codes.
- `src/main/java/org/immregistries/iis/kernal/logic/rb/RiskBasedForecast.java`:
  - `build(conditions, today)`: no patient data goes in.
  - `applyDates`: the fixed offsets.
  - `writeForecast`: the OBX layout and the placeholder LOINC `99999-9`.
  - `writeErrors`: the ERR choice.
  - `isCodeSystemOk`: 99CDS, 99CDC, or empty.
- `src/main/java/org/immregistries/iis/kernal/logic/rb/ContextualConditionParser.java`: reads the OBX segments after the QPD.
- `src/main/java/org/immregistries/iis/kernal/logic/IncomingMessageHandler.java`, `buildRSP`:
  - `riskBased` turns off `sendBackForecast`, so `doForecast` (LoneStar/ICE) and the dose evaluation are skipped.
  - Z42/Z33 selection.
  - `recordMessageReceived` logs the full query.
- `src/main/java/org/immregistries/iis/kernal/servlet/VciServlet.java`: the hard-coded `iss` and "Generate JWS (not implemented)".
- `src/test/java/org/immregistries/iis/kernal/logic/rb/RiskBasedForecastTest.java`: 9 unit tests (parser, table, writer). There is no end-to-end Z45 test.
- `docs/rb-demo/OPEN-ITEMS.md`: the spec gaps and what the demo assumed for each.

smm-tester (`rb-demo`):
- `smm-web/src/main/java/org/immregistries/smm/tester/RbDemoServlet.java`: the four scenarios, random patients, the fixed histories, and the prefilled conditions.
- `smm-core/src/main/java/org/immregistries/smm/tester/manager/query/QueryConverterQBPZ45.java`: builds the Z45 and its condition OBXs from a VXU.
