package org.immregistries.iis.kernal.model;

/**
 * Created by Eric on 12/20/17.
 */
public class OrgAccess {
    private int orgAccessId = 0;
    private int orgId = 0;
    private String accessName = "";
    private String accessKey = "";

    public int getOrgAccessId() {
        return orgAccessId;
    }

    public void setOrgAccessId(int orgAccessId) {
        this.orgAccessId = orgAccessId;
    }

    public int getOrgId() {
        return orgId;
    }

    public void setOrgId(int orgId) {
        this.orgId = orgId;
    }

    public String getAccessName() {
        return accessName;
    }

    public void setAccessName(String accessName) {
        this.accessName = accessName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }


}
