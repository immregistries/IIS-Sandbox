package org.immregistries.iis.kernal.model;

public interface IReportedObject<Master> {
	Master getMasterRecord();

	void setMasterRecord(Master master);
}
