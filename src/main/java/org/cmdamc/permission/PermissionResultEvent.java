package org.cmdamc.permission;

public class PermissionResultEvent {
    public int requestCode;
    public String[] permissions;
    public int[] results;
    public PermissionResultEvent(int requestCode, String[] permissions, int[] results) {
        this.requestCode = requestCode;
        this.permissions = permissions;
        this.results = results;
    }

}
