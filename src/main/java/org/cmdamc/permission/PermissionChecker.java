package org.cmdamc.permission;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;


import java.util.ArrayList;

/**
 * Created by fengzhiping on 2018/10/21.
 */

public class PermissionChecker {
    public static final String PERMISSION_FRAGMENT = "PERMISSION_FRAGMENT";
    public static final int PERMISSION_REQUEST_CODE = 1001;
    Builder builder;
    int requestIndex = 0;

    private PermissionChecker(Builder builder) {
        this.builder = builder;
//        EventBus.getDefault().register(this);
 }

    public static PermissionChecker.Builder with(AppCompatActivity activity) {
        return new PermissionChecker.Builder(activity);
    }

    static class Config {
        public String permission;
        public String requestTitle;
        public String requestMessage;

        public Config(String permission, String title, String message) {
            this.permission = permission;
            this.requestTitle = title;
            this.requestMessage = message;
        }
    }

    public interface IBuilder {
        PermissionChecker build();
    }

    public static class BuilderMulti implements IBuilder {
        AppCompatActivity activity;
        String requestTitle = "";
        String requestMessage = "";
        String[] permissions;

        public BuilderMulti permissions(String[] permissions, String title, String message) {
            this.permissions = permissions;
            return this;
        }

        @Override
        public PermissionChecker build() {
            return null;
        }
    }

    public static class Builder implements IBuilder {
        AppCompatActivity activity;
        ArrayList<Config> configs;

        String requestOpenSettingMessage = "";
        Callback callback;
        AllowCallback allowCallback;
        DenyCallback denyCallback;

        private Builder(AppCompatActivity activity) {
            this.activity = activity;
            this.configs = new ArrayList<>();
        }

        public Builder permission(String permission, String title, String message) {
            this.configs.add(new Config(permission, title, message));
            return this;
        }



//        public Builder requestTitle(String title) {
//            this.requestTitle = title;
//            return this;
//        }
//
//        public Builder requestMessage(String message) {
//            this.requestMessage = message;
//            return this;
//        }

        public Builder requestOpenSettingMessage(String requestOpenSettingMessage) {
            this.requestOpenSettingMessage = requestOpenSettingMessage;
            return this;
        }

        public FullBuilder callback(Callback callback) {
            this.callback = callback;
            return new FullBuilder(this);
        }

        public SingleBuilder onAllow(AllowCallback callback) {
            allowCallback = callback;
            return new SingleBuilder(this);
        }

        public SingleBuilder onDeny(DenyCallback callback) {
            denyCallback = callback;
            return new SingleBuilder(this);
        }

        public PermissionChecker build() {
            return new PermissionChecker(this);
        }

        void notifyOnAllow(String permission) {
            if (allowCallback != null) {
                allowCallback.onAllow(permission);
            } else if (callback != null) {
                callback.onAllow(permission);
            }
        }

        void notifyOnDeny(String permission) {
            if (denyCallback != null) {
                denyCallback.onDeny(permission);
            } else if (callback != null) {
                callback.onDeny(permission);
            }
        }

    }

    public static class FullBuilder implements IBuilder {
        Builder builder;
        FullBuilder(Builder builder) {
            this.builder = builder;
        }


        @Override
        public PermissionChecker build() {
            return builder.build();
        }
    }

    public static class SingleBuilder implements IBuilder {
        Builder builder;

        SingleBuilder(Builder builder) {
            this.builder = builder;
        }

        @Override
        public PermissionChecker build() {
            return builder.build();
        }

        public SingleBuilder onAllow(AllowCallback callback) {
            builder.allowCallback = callback;
            return this;
        }

        public SingleBuilder onDeny(DenyCallback callback) {
            builder.denyCallback = callback;
            return this;
        }
    }

    public static final String[] PERMISSONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    public static abstract class Callback implements AllowCallback, DenyCallback {
//        void onAllow(String permission);
//        void onDeny(String permission);
    }

    public interface AllowCallback {
        void onAllow(String permission);
    }

    public interface DenyCallback {
        void onDeny(String permission);
    }

    public void check() {
        checkNext();
    }

    private void checkNext() {
        int current = requestIndex++;
        if (current >= builder.configs.size()) {
            // 检查完了
//            EventBus.getDefault().unregister(PermissionChecker.this);
            return;
        }
        Config config = builder.configs.get(current);
        if (!hasPermission(config)) {
            showDialogTipUserRequestPermission(config);
        } else {
            builder.notifyOnAllow(config.permission);
            checkNext();
        }
    }

    private boolean hasPermission(Config config){
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            int i = ContextCompat.checkSelfPermission(builder.activity, config.permission);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (i != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                return false;
            }
        }
        return true;
    }

    private void showDialogTipUserRequestPermission(final Config config) {
        Dialog dialog = new AlertDialog.Builder(builder.activity)
                .setTitle(config.requestTitle)
                .setMessage(config.requestMessage)
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRequestPermission(config);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(builder.activity, "用户取消授权", Toast.LENGTH_LONG).show();
                        builder.notifyOnDeny(config.permission);
                        checkNext();
                        //EventBus.getDefault().unregister(PermissionChecker.this);
//                        finish();
                    }
                }).setCancelable(false).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // 开始提交请求权限
    private void startRequestPermission(Config config) {
        FragmentManager fm = builder.activity.getSupportFragmentManager();
        PermissionFragment permissionFragment = (PermissionFragment)fm.findFragmentByTag(PERMISSION_FRAGMENT);
        if(permissionFragment == null) {
            permissionFragment = new PermissionFragment();
            permissionFragment.setConfig(config, this);
            fm.beginTransaction().add(permissionFragment, PERMISSION_FRAGMENT).commitNow();
            permissionFragment.requestPermissions(new String[] {config.permission}, PERMISSION_REQUEST_CODE);
        } else {
            permissionFragment.setConfig(config, this);
            permissionFragment.requestPermissions(new String[] {config.permission}, PERMISSION_REQUEST_CODE);
        }
        //ActivityCompat.requestPermissions(builder.activity, new String[] {config.permission}, 321);
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
    void onPermissionResult(PermissionResultEvent event) {

        Config config = builder.configs.get(requestIndex - 1);
        if (config == null) {
            return;
        }
        if (event.requestCode == PERMISSION_REQUEST_CODE && event.permissions[0].equals(config.permission)) {
//            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                // 检查该权限是否已经获取
//                int i = ContextCompat.checkSelfPermission(builder.activity, builder.permission);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (event.results[0] != PackageManager.PERMISSION_GRANTED) {
                    // 提示用户应该去应用设置界面手动开启权限
                    //showDialogTipUserGoToAppSetting(config);
                    if (mAlertDialog != null && mAlertDialog.isShowing()) {
                        mAlertDialog.dismiss();
                    }
                    builder.notifyOnDeny(config.permission);
                    checkNext();
                } else {
                    if (mAlertDialog != null && mAlertDialog.isShowing()) {
                        mAlertDialog.dismiss();
                    }
                    Toast.makeText(builder.activity, "权限获取成功", Toast.LENGTH_SHORT).show();
                    builder.notifyOnAllow(config.permission);
                    checkNext();

//                    EventBus.getDefault().unregister(this);
                }
            }
//        }
    }

    AlertDialog mAlertDialog;
    // 提示用户去应用设置界面手动开启权限
    private void showDialogTipUserGoToAppSetting(Config config){
        mAlertDialog = new AlertDialog.Builder(builder.activity)
                .setTitle(config.requestTitle).setMessage(builder.requestOpenSettingMessage)
                .setPositiveButton("立即开启",new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 跳转到应用设置界面
                        goToAppSetting();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        builder.activity.finish();
                    }
                }).setCancelable(false).show();

    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", builder.activity.getPackageName(), null);
        intent.setData(uri);
        builder.activity.startActivityForResult(intent, 123);
    }
}
