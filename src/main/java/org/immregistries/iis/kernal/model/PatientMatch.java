package org.immregistries.iis.kernal.model;

/**
 * Created by Eric on 12/20/17.
 */
public class PatientMatch {
    private int matchId = 0;
    private int reportedPatientAId = 0;
    private int reportedPatientBId = 0;
    private MatchStatus matchStatus = null;

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public int getReportedPatientAId() {
        return reportedPatientAId;
    }

    public void setReportedPatientAId(int reportedPatientAId) {
        this.reportedPatientAId = reportedPatientAId;
    }

    public int getReportedPatientBId() {
        return reportedPatientBId;
    }

    public void setReportedPatientBId(int reportedPatientBId) {
        this.reportedPatientBId = reportedPatientBId;
    }

    public MatchStatus getMatchStatus() { return matchStatus; }

    public void setMatchStatus(MatchStatus matchStatus) { this.matchStatus = matchStatus; }
}
