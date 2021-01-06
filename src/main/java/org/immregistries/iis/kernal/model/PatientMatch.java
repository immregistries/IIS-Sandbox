package org.immregistries.iis.kernal.model;

import java.io.Serializable;

/**
 * Created by Eric on 12/20/17.
 */
public class PatientMatch implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private int matchId = 0;
  private PatientReported reportedPatientA = null;
  private PatientReported reportedPatientB = null;
  private MatchStatus matchStatus = null;

  public int getMatchId() {
    return matchId;
  }

  public void setMatchId(int matchId) {
    this.matchId = matchId;
  }

  public PatientReported getReportedPatientA() {
    return reportedPatientA;
  }

  public void setReportedPatientA(PatientReported reportedPatientA) {
    this.reportedPatientA = reportedPatientA;
  }

  public PatientReported getReportedPatientB() {
    return reportedPatientB;
  }

  public void setReportedPatientB(PatientReported reportedPatientB) {
    this.reportedPatientB = reportedPatientB;
  }

  public MatchStatus getMatchStatus() {
    return matchStatus;
  }

  public void setMatchStatus(MatchStatus matchStatus) {
    this.matchStatus = matchStatus;
  }
}
