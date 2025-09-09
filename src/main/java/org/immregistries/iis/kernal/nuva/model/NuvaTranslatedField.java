package org.immregistries.iis.kernal.nuva.model;

import java.util.Objects;

public class NuvaTranslatedField {
	private String de;
	private String en;
	private String fr;
	private String nl;
	private String ru;
	private String ua;
	private String es;
	private String it;

	public NuvaTranslatedField() {
	}

	public NuvaTranslatedField(String de, String en, String fr, String nl, String ru, String ua, String es, String it) {
		this.de = de;
		this.en = en;
		this.fr = fr;
		this.nl = nl;
		this.ru = ru;
		this.ua = ua;
		this.es = es;
		this.it = it;
	}

	// Getters and Setters
	public String getDe() {
		return de;
	}

	public void setDe(String de) {
		this.de = de;
	}

	public String getEn() {
		return en;
	}

	public void setEn(String en) {
		this.en = en;
	}

	public String getFr() {
		return fr;
	}

	public void setFr(String fr) {
		this.fr = fr;
	}

	public String getNl() {
		return nl;
	}

	public void setNl(String nl) {
		this.nl = nl;
	}

	public String getRu() {
		return ru;
	}

	public void setRu(String ru) {
		this.ru = ru;
	}

	public String getUa() {
		return ua;
	}

	public void setUa(String ua) {
		this.ua = ua;
	}

	public String getEs() {
		return es;
	}

	public void setEs(String es) {
		this.es = es;
	}

	public String getIt() {
		return it;
	}

	public void setIt(String it) {
		this.it = it;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NuvaTranslatedField that = (NuvaTranslatedField) o;
		return Objects.equals(de, that.de) && Objects.equals(en, that.en) && Objects.equals(fr, that.fr) && Objects.equals(nl, that.nl) && Objects.equals(ru, that.ru) && Objects.equals(ua, that.ua) && Objects.equals(es, that.es) && Objects.equals(it, that.it);
	}

	@Override
	public int hashCode() {
		return Objects.hash(de, en, fr, nl, ru, ua, es, it);
	}
}