package org.cmdamc.permission;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

public class PermissionFragment2 extends Fragment {

    PermissionChecker2.BaseSubBuilder permissionChecker;
    public void setChecker(PermissionChecker2.BaseSubBuilder permissionChecker) {
        this.permissionChecker = permissionChecker;
    }
//    public PermissionFragment(PermissionChecker.Config config) {
//
//    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        EventBus.getDefault().post(new PermissionResultEvent(requestCode, permissions, grantResults));
        if (permissionChecker != null) {
            permissionChecker.onPermissionResult(new PermissionResultEvent(requestCode, permissions, grantResults));
        }
    }

}
