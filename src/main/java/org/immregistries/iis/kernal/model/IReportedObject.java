package org.immregistries.iis.kernal.model;

/**
 * Local equivalent of non golden record
 *
 * @param <Master>
 */
public interface IReportedObject<Master> {

	default boolean isGoldenRecord() {
		return false;
	}

	Master getMasterRecord();

	void setMasterRecord(Master master);
}
