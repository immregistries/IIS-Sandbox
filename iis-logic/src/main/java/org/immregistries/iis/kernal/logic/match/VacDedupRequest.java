package org.immregistries.iis.kernal.logic.match;


import java.util.List;

public class VacDedupRequest {
	private String algorithm;
	private List<ImmunizationItem> immunizations;

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public List<ImmunizationItem> getImmunizations() {
		return immunizations;
	}

	public void setImmunizations(List<ImmunizationItem> immunizations) {
		this.immunizations = immunizations;
	}

	public static class ImmunizationItem {
		private String date;
		private String cvx;
		private String mvx;
		private String lot;
		private String org;
		private String source;

		public String getDate() {
			return date;
		}

		public void setDate(String date) {
			this.date = date;
		}

		public String getCvx() {
			return cvx;
		}

		public void setCvx(String cvx) {
			this.cvx = cvx;
		}

		public String getMvx() {
			return mvx;
		}

		public void setMvx(String mvx) {
			this.mvx = mvx;
		}

		public String getLot() {
			return lot;
		}

		public void setLot(String lot) {
			this.lot = lot;
		}

		public String getOrg() {
			return org;
		}

		public void setOrg(String org) {
			this.org = org;
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}
	}
}
