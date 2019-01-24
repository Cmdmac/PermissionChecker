# PermissionChecker
Android动态权限申请库

## 使用示例
PermissionChecker.with(this)
        .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "存储权限不可用", "XXX需要存储权限以正常工作，是否允许？")
        .permission(Manifest.permission.ACCESS_COARSE_LOCATION, "定位权限不可用", "XXX需要定位权限以正常工作，是否允许？")
        .onAllow(new PermissionChecker.AllowCallback() {
            @Override
            public void onAllow(String permission) {
                Toast.makeText(MainActivity.this, "allow", Toast.LENGTH_SHORT).show();
            }
        }).onDeny(new PermissionChecker.DenyCallback() {
    @Override
    public void onDeny(String permission) {
        Toast.makeText(MainActivity.this, "deny", Toast.LENGTH_SHORT).show();
    }
}).build().check();
