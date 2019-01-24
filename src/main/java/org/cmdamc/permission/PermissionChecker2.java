package org.cmdamc.permission;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;

public abstract class PermissionChecker2 implements IPermissionChecker {
    public static final String PERMISSION_FRAGMENT = "PERMISSION_FRAGMENT";
    public static final int PERMISSION_REQUEST_CODE = 1001;

    public static Builder with(AppCompatActivity activity) {
        return new Builder(activity);
    }

//    interface IOneBuilder {
//        IOneBuilder permission(String permission, String title, String message);
//    }
//
//    interface IMultiBuilder {
//        IMultiBuilder permissions(String[] permission, String title, String message);
//    }

    public static class Builder {
        AppCompatActivity activity;
        OneBuilder oneBuilder;
        MultiBuilder multiBuilder;

        Builder(AppCompatActivity activity) {
            this.activity = activity;
        }

        public OneBuilder permission(String permission, String title, String message) {
            if (oneBuilder == null) {
                oneBuilder = new OneBuilder(this.activity);
            }
            oneBuilder.permission(permission, title, message);
            return oneBuilder;
        }

        public MultiBuilder permissions(String[] permissions, String title, String message) {
            if (multiBuilder == null) {
                multiBuilder = new MultiBuilder(this.activity);
            }
            multiBuilder.permissions(permissions, title, message);
            return multiBuilder;
        }
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

    static abstract class BaseSubBuilder {
        AlertDialog mAlertDialog;
        AppCompatActivity activity;
        Callback callback;
        AllowCallback allowCallback;
        DenyCallback denyCallback;
        BaseSubBuilder(AppCompatActivity activity) {
            this.activity = activity;
        }

        public FullCallbackBuilder callback(Callback callback) {
            this.callback = callback;
            return new FullCallbackBuilder(this);
        }

        public SingleCallbackBuilder onAllow(AllowCallback callback) {
            allowCallback = callback;
            return new SingleCallbackBuilder(this);
        }

        public SingleCallbackBuilder onDeny(DenyCallback callback) {
            denyCallback = callback;
            return new SingleCallbackBuilder(this);
        }

        public PermissionCheckerImpl build() {
            return new PermissionCheckerImpl(this);
        }

        protected boolean hasPermission(String permission){
            // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 检查该权限是否已经获取
                int i = ContextCompat.checkSelfPermission(activity, permission);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (i != PackageManager.PERMISSION_GRANTED) {
                    // 如果没有授予该权限，就去提示用户请求
                    return false;
                }
            }
            return true;
        }

        // 开始提交请求权限
        protected void startRequestPermission(String permission) {
            FragmentManager fm = activity.getSupportFragmentManager();
            PermissionFragment2 permissionFragment = (PermissionFragment2)fm.findFragmentByTag(PERMISSION_FRAGMENT);
            if(permissionFragment == null) {
                permissionFragment = new PermissionFragment2();
                permissionFragment.setChecker(this);
                fm.beginTransaction().add(permissionFragment, PERMISSION_FRAGMENT).commitNow();
                permissionFragment.requestPermissions(new String[] {permission}, PERMISSION_REQUEST_CODE);
            } else {
                permissionFragment.setChecker(this);
                permissionFragment.requestPermissions(new String[] {permission}, PERMISSION_REQUEST_CODE);
            }
            //ActivityCompat.requestPermissions(builder.activity, new String[] {config.permission}, 321);
        }

        protected void showDialogTipUserRequestPermission(final String requestTitle, final String requestMessage, final String... permission) {
            Dialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(requestTitle)
                    .setMessage(requestMessage)
                    .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onOk(permission);
//                            startRequestPermission(config);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Toast.makeText(activity, "用户取消授权", Toast.LENGTH_LONG).show();
                            onCancel(permission);
//                            checkNext();
                            //EventBus.getDefault().unregister(PermissionChecker.this);
//                        finish();
                        }
                    }).setCancelable(false).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        void notifyOnAllow(String... permission) {
            if (allowCallback != null) {
                allowCallback.onAllow(permission);
            } else if (callback != null) {
                callback.onAllow(permission);
            }
        }

        void notifyOnDeny(String... permission) {
            if (denyCallback != null) {
                denyCallback.onDeny(permission);
            } else if (callback != null) {
                callback.onDeny(permission);
            }
        }

        abstract void onPermissionResult(PermissionResultEvent event);
        abstract void onOk(String... permissions);
        abstract void onCancel(String... permissions);
        abstract void check();
    }

    public static class OneBuilder extends BaseSubBuilder {
        ArrayList<Config> configs;
        int requestIndex = 0;

        OneBuilder(AppCompatActivity activity) {
            super(activity);
            this.configs = new ArrayList<>();
        }

        @Override
        void onPermissionResult(PermissionResultEvent event) {
            Config config = configs.get(requestIndex - 1);
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
                    notifyOnDeny(config.permission);
                    checkNext();
                } else {
                    if (mAlertDialog != null && mAlertDialog.isShowing()) {
                        mAlertDialog.dismiss();
                    }
                    Toast.makeText(activity, "权限获取成功", Toast.LENGTH_SHORT).show();
                    notifyOnAllow(config.permission);
                    checkNext();

//                    EventBus.getDefault().unregister(this);
                }
            }
        }

        @Override
        void onOk(String... permissions) {
            startRequestPermission(permissions[0]);
        }

        @Override
        void onCancel(String... permissions) {
            notifyOnDeny(permissions);
            checkNext();
        }

        @Override
        void check() {
            checkNext();
        }

        private void checkNext() {
            int current = requestIndex++;
            if (current >= configs.size()) {
                // 检查完了
//            EventBus.getDefault().unregister(PermissionChecker.this);
                return;
            }
            Config config = configs.get(current);
            if (!hasPermission(config.permission)) {
                showDialogTipUserRequestPermission(config.requestTitle, config.requestMessage, config.permission);
            } else {
                notifyOnAllow(config.permission);
                checkNext();
            }
        }


        public OneBuilder permission(String permission, String title, String message) {
            this.configs.add(new Config(permission, title, message));
            return this;
        }

    }

    public static class MultiBuilder extends BaseSubBuilder {

        String[] permissions;
        String title;
        String message;
        MultiBuilder(AppCompatActivity activity) {
            super(activity);
        }

        @Override
        void onPermissionResult(PermissionResultEvent event) {
            if (event.requestCode == PERMISSION_REQUEST_CODE) {
//            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                // 检查该权限是否已经获取
//                int i = ContextCompat.checkSelfPermission(builder.activity, builder.permission);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                }

                ArrayList<String> allow = new ArrayList<>();
                ArrayList<String> deny = new ArrayList<>();
                for (int i = 0; i < event.results.length; i++) {
                    if (event.results[i] == PackageManager.PERMISSION_GRANTED) {
                        allow.add(event.permissions[i]);
                    } else {
                        deny.add(event.permissions[i]);
                    }
                }

                if (allow.size() > 0) {
                    notifyOnAllow(allow.toArray(new String[0]));
                }

                if (deny.size() > 0) {
                    notifyOnDeny(deny.toArray(new String[0]));
                }
//                if (event.results[0] != PackageManager.PERMISSION_GRANTED) {
//                    // 提示用户应该去应用设置界面手动开启权限
//                    //showDialogTipUserGoToAppSetting(config);
//
//                    notifyOnDeny(event.permissions);
//                } else {
//                    Toast.makeText(activity, "权限获取成功", Toast.LENGTH_SHORT).show();
//                    notifyOnAllow(event.permissions);
////                    checkNext();
//                }
            }
        }

        @Override
        void onOk(String... permissions) {
            FragmentManager fm = activity.getSupportFragmentManager();
            PermissionFragment2 permissionFragment = (PermissionFragment2)fm.findFragmentByTag(PERMISSION_FRAGMENT);
            if(permissionFragment == null) {
                permissionFragment = new PermissionFragment2();
                permissionFragment.setChecker(this);
                fm.beginTransaction().add(permissionFragment, PERMISSION_FRAGMENT).commitNow();
                permissionFragment.requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            } else {
                permissionFragment.setChecker(this);
                permissionFragment.requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }
        }

        @Override
        void onCancel(String... permissions) {
            notifyOnDeny(permissions);
        }

        @Override
        void check() {
            ArrayList<String> list = new ArrayList<>();
            for(String permission : permissions) {
                if (!hasPermission(permission)) {
                    list.add(permission);
                }
            }

            if (list.size() > 0) {
                showDialogTipUserRequestPermission(title, message, permissions);
            } else {
                notifyOnAllow(permissions);
            }
        }

        public MultiBuilder permissions(String[] permissions, String title, String message) {
//            ArrayList<String> list = new ArrayList<>();
//            for (String permission : permissions) {
//                boolean hasPermission = hasPermission(permission);
//                if (!hasPermission) {
//                    list.add(permission);
//                }
//            }
//            this.permissions = list.toArray(new String[0]);
            this.permissions = permissions;
            this.title = title;
            this.message = message;
            return this;
        }

    }

    public static class FullCallbackBuilder implements IBuilder {
        BaseSubBuilder builder;
        FullCallbackBuilder(BaseSubBuilder builder) {
            this.builder = builder;
        }

        @Override
        public PermissionCheckerImpl build() {
            return builder.build();
        }
    }

    public static class SingleCallbackBuilder implements IBuilder {
        BaseSubBuilder builder;

        SingleCallbackBuilder(BaseSubBuilder builder) {
            this.builder = builder;
        }

        @Override
        public PermissionCheckerImpl build() {
            return builder.build();
        }

        public SingleCallbackBuilder onAllow(AllowCallback callback) {
            builder.allowCallback = callback;
            return this;
        }

        public SingleCallbackBuilder onDeny(DenyCallback callback) {
            builder.denyCallback = callback;
            return this;
        }
    }

    public interface IBuilder {
        PermissionCheckerImpl build();
    }

    public static abstract class Callback implements AllowCallback, DenyCallback {
//        void onAllow(String permission);
//        void onDeny(String permission);
    }

    public interface AllowCallback {
        void onAllow(String... permission);
    }

    public interface DenyCallback {
        void onDeny(String... permission);
    }

    public static class PermissionCheckerImpl {
        BaseSubBuilder builder;
        PermissionCheckerImpl(BaseSubBuilder builder) {
            this.builder = builder;
        }

        public void check() {
            builder.check();
        }
    }
}
