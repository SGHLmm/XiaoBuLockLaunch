package com.xiaobulocklaunch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class MainActivity extends Activity {
    private EditText mappings;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView title = new TextView(this);
        title.setText("小布锁屏启动");
        title.setTextSize(23);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("内置映射：微信、哔哩哔哩、QQ、支付宝、抖音、淘宝、高德地图、百度地图、网易云音乐、小红书。\n\n自定义格式：语音应用名称=应用包名，每行一条。只有小布助手生成应用启动指令时映射才会生效。");
        info.setTextSize(14);
        info.setPadding(0, dp(12), 0, dp(12));
        root.addView(info);

        mappings = new EditText(this);
        mappings.setGravity(Gravity.TOP | Gravity.START);
        mappings.setMinLines(6);
        mappings.setTextSize(14);
        mappings.setHint("例如：\n我的工具箱=com.tencent.mm\n家庭相册=com.example.album");
        mappings.setText(getSharedPreferences("mappings", MODE_PRIVATE)
                .getString("custom_mappings", ""));
        root.addView(mappings, new LinearLayout.LayoutParams(-1, 0, 1));

        Button pick = new Button(this);
        pick.setText("从已安装应用中查看包名");
        pick.setOnClickListener(v -> showApps());
        root.addView(pick);

        Button save = new Button(this);
        save.setText("保存映射");
        save.setOnClickListener(v -> saveMappings());
        root.addView(save);

        TextView note = new TextView(this);
        note.setText("使用步骤：安装本应用 → 在 LSPosed 中启用模块并勾选“小布助手”作用域 → 强制停止并重新打开小布助手。锁屏时是否显示目标应用，仍受 ColorOS/Android 后台启动和锁屏策略限制；本模块不会解锁手机或跳过验证。");
        note.setTextSize(12);
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note);

        setContentView(root);
    }

    private void saveMappings() {
        String value = mappings.getText().toString();
        getSharedPreferences("mappings", MODE_PRIVATE).edit()
                .putString("custom_mappings", value).commit();
        File file = new File(getApplicationInfo().dataDir + "/shared_prefs/mappings.xml");
        // XSharedPreferences needs a readable preference file on supported Xposed setups.
        // LSPosed may still enforce platform-specific access restrictions.
        file.setReadable(true, false);
        new AlertDialog.Builder(this).setMessage("映射已保存。若 LSPosed 尚未加载模块，请重新勾选作用域并重启小布助手。")
                .setPositiveButton("确定", null).show();
    }

    private void showApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> launchable = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null) launchable.add(app);
        }
        Collections.sort(launchable, new Comparator<ApplicationInfo>() {
            @Override public int compare(ApplicationInfo a, ApplicationInfo b) {
                return pm.getApplicationLabel(a).toString()
                        .compareToIgnoreCase(pm.getApplicationLabel(b).toString());
            }
        });
        String[] labels = new String[launchable.size()];
        for (int i = 0; i < launchable.size(); i++) {
            ApplicationInfo app = launchable.get(i);
            labels[i] = pm.getApplicationLabel(app) + "  (" + app.packageName + ")";
        }
        new AlertDialog.Builder(this).setTitle("已安装应用包名")
                .setItems(labels, (dialog, which) -> {
                    ApplicationInfo app = launchable.get(which);
                    String current = mappings.getText().toString();
                    if (!current.isEmpty() && !current.endsWith("\n")) current += "\n";
                    mappings.setText(current + "我的应用=" + app.packageName);
                    mappings.setSelection(mappings.getText().length());
                }).setNegativeButton("关闭", null).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
