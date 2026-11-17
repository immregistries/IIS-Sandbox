# RB demo: open items and assumptions

Built on the draft "Enabling Risk-Based CDS through HL7 V2 Query and Response" as it stands on 2026-10-29.
This list is what the draft does not settle, and what the demo had to guess. Each item says what the demo does today,
so it is easy to change when the spec firms up. Items marked (verify) are from my reading of the draft and should be
checked against the current text before anyone relies on them.

## Gaps in the draft spec

| # | Gap | What the demo does | Decided (2026-11-02) |
|---|---|---|---|
| 1 | **99CDS vs 99CDC.** The local code system for contextual conditions is written both ways. | Sends and echoes `99CDS`. The IIS accepts `99CDC` on input as well. Any other system gets an ERR and the condition is ignored. | **Decided:** `99CDC` in the draft is a typo; `99CDS` is correct. (IIS still tolerantly accepts `99CDC`.) |
| 2 | **Disposition section.** The draft has a Disposition section that does not say yet how a recommendation not being given (or being not needed) is reported (verify). | Not implemented. A "not needed" case (RSV, scenario 278) is shown as a recommendation with status `LA27183-5 Immune`. | **Decided:** Disposition is being cut from the spec. Nothing to implement. |
| 3 | **No LOINC for the Vaccine Recommendation Category.** The draft lists the values (Routine, SCDM, High-Risk, system 99VRC) but no identifier and no codes. | Placeholder OBX-3 `99999-9^Vaccine Recommendation Category^LN` and placeholder codes `R`, `S`, `H` with the labels `Routine`, `SCDM`, `High-Risk` in 99VRC. Constants are in `RiskBasedForecast` and `RiskBasedScript`. | **In progress:** LOINC assignment is underway. Placeholders stay until assigned. |
| 4 | **30982-3 label.** The draft shows `Reason for Recommendation`; check the official LOINC long and short names and the capitalization (verify). | Sends `30982-3^Reason for Recommendation^LN`, as in the draft example. | **Open.** Per loinc.org, long name is `Reason applied by forecast logic to project this vaccine`, short name `Reason applied to project this vaccine` (not `Reason for Recommendation`). |
| 5 | **QPD-1.2 text mismatch.** The conformance table text is `Request Evaluated Immunization History and Risk-Based Forecast`, the example message says `Request Evaluated History and Risk-Based Forecast`. | The query (smm-tester) and the RSP use the conformance table text. The IIS does not look at the text, only at MSH-21 (and echoes the QPD it received unchanged). | **Partly decided:** Z45 is ours to name; assume Z45 wherever Z44 appears in the draft. QPD-1.2 text is still open. |
| 6 | **Persistence rule.** The draft does not say whether the IIS may or must store the contextual conditions, or discard them after answering. | Not stored. Conditions only shape the one response. | **Decided:** Do not persist conditions (matches the demo). |
| 7 | **ERR codes.** No error code is defined for an unknown contextual condition (or a bad value) and the draft says the query must not be rejected. | `ERR||OBX^n^3|103^Table value not found^HL70357|W||||message`: severity warning, one ERR per unknown code, MSA stays `AA`. Chosen by me. | **Open** for discussion; not decided. |
| 8 | **No example RSP in the draft.** Only the query side has an example. Order of OBXs inside a recommendation group, where the reason OBX goes, and how multiple reasons repeat are not shown. | Per group (sub-ID): vaccine type, status, dates, schedule used, category, then one reason per condition. Reasons carry only `code^description^99CDS` (no value). | **Decided:** An example RSP is coming in a future spec version. |

## Assumptions the demo made (scripted, not clinical advice)

* **The forecast is scripted.** `RiskBasedScript` maps condition codes to fixed recommendations. The IIS Sandbox does not call
  LoneStar/ICE for Z45. A hybrid (real baseline forecast plus risk-based changes) is not done.
* Pregnancy (007 with onset date 170): Tdap, earliest/recommended 27 weeks and latest 36 weeks after the onset date. Whether
  "onset of pregnancy" means LMP or conception is not defined (verify), 27 to 36 weeks is only the usual Tdap window.
* RSV infant (278): shows the "nirsevimab not needed" case as CVX `306` (verify CVX code and label), status Immune, category Routine.
* Healthcare personnel (055): HepB (High-Risk) and influenza (Routine). Rows for 045 (Yellow Fever) and 059 (HepA) exist to show
  the table is data driven. Other known codes (016, 023, 024, 053, 056, 057, 058) are accepted, echo nothing, and have no rule.
* A Y/N (`ID`) condition with `N` is accepted and has no effect. A date (`DT`) condition is used when the value is present.
* Status in series uses the LA codes from the draft (LA13421-5 Complete, LA13422-3 On schedule, LA27183-5 Immune,
  LA4216-3 Contraindicated). `59779-9` schedule used is `VXC16^ACIP^CDCPHINVS`.
* The IIS answers Z45 with Z42 for a match and Z33 for no match. Z31 (candidate list) and Z32 are not used for Z45. Processing
  flavors (ORANGE, COCONUT) are ignored for Z45.
* OBX-4 in the query is the sub-ID that smm-tester numbers 1..n. The IIS does not interpret it.

## Not done yet / for later

* Done 2026-11-02: Z45 conditions are a form field on smm-tester Manual Query (with scenario quick-picks), and an RB Demo page (Send > RB Demo) generates a patient, sends a VXU, then sends the Z45 query.
* No Z45 scenario for a candidate list (Z31).
* IIS Sandbox `dev` (Spring/FHIR rewrite) was not touched. The demo lives on the older `master` code.
