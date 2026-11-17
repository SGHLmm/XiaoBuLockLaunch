package com.xiaobulocklaunch;

import android.text.TextUtils;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class ModuleEntry implements IXposedHookLoadPackage {
    private static final String XIAOBU_PACKAGE = "com.heytap.speechassist";
    private static final String LAUNCH_APP_CLASS =
            "com.heytap.speech.engine.protocol.directive.app.LaunchApp";
    private static XSharedPreferences preferences;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!XIAOBU_PACKAGE.equals(lpparam.packageName)) return;

        try {
            Class<?> operation = XposedBridge.class.getClassLoader() == null
                    ? Class.forName("com.heytap.speechassist.skill.openapp.operation.OpenAppOperation",
                            false, lpparam.classLoader)
                    : Class.forName("com.heytap.speechassist.skill.openapp.operation.OpenAppOperation",
                            false, lpparam.classLoader);

            XposedBridge.hookAllMethods(operation, "tryLaunchApp", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length == 0 || param.args[0] == null) return;
                    Object directive = param.args[0];
                    try {
                        Method getName = directive.getClass().getMethod("getAppName");
                        Method getPackage = directive.getClass().getMethod("getPackageName");
                        Method setPackage = directive.getClass().getMethod("setPackageName", String.class);

                        String appName = (String) getName.invoke(directive);
                        if (TextUtils.isEmpty(appName)) return;

                        String targetPackage = resolvePackage(appName);
                        if (TextUtils.isEmpty(targetPackage)) return;

                        String currentPackage = (String) getPackage.invoke(directive);
                        if (!targetPackage.equals(currentPackage)) {
                            setPackage.invoke(directive, targetPackage);
                            XposedBridge.log("XiaoBuLockLaunch: " + appName + " -> " + targetPackage);
                        }
                    } catch (Throwable t) {
                        XposedBridge.log("XiaoBuLockLaunch mapping error: " + t);
                    }
                }
            });
            XposedBridge.log("XiaoBuLockLaunch: hooked OpenAppOperation.tryLaunchApp");
        } catch (Throwable t) {
            XposedBridge.log("XiaoBuLockLaunch hook failed: " + t);
        }
    }

    private static String resolvePackage(String appName) {
        String key = normalize(appName);
        if (key.isEmpty()) return null;

        Map<String, String> mappings = builtInMappings();
        try {
            if (preferences == null) {
                preferences = new XSharedPreferences("com.xiaobulocklaunch", "mappings");
            }
            preferences.reload();
            String custom = preferences.getString("custom_mappings", "");
            for (String line : custom.split("\\r?\\n")) {
                int split = line.indexOf('=');
                if (split <= 0) continue;
                String alias = normalize(line.substring(0, split));
                String packageName = line.substring(split + 1).trim();
                if (!alias.isEmpty() && !packageName.isEmpty()) mappings.put(alias, packageName);
            }
        } catch (Throwable t) {
            XposedBridge.log("XiaoBuLockLaunch preferences unavailable: " + t);
        }
        return mappings.get(key);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "").replace("打开", "").replace("启动", "");
    }

    private static Map<String, String> builtInMappings() {
        Map<String, String> map = new HashMap<>();
        add(map, "微信,微信聊天", "com.tencent.mm");
        add(map, "哔哩哔哩,b站,哔哩, bilibili", "tv.danmaku.bili");
        add(map, "qq,腾讯qq", "com.tencent.mobileqq");
        add(map, "支付宝", "com.eg.android.AlipayGphone");
        add(map, "抖音", "com.ss.android.ugc.aweme");
        add(map, "淘宝", "com.taobao.taobao");
        add(map, "高德地图,高德", "com.autonavi.minimap");
        add(map, "百度地图", "com.baidu.BaiduMap");
        add(map, "网易云音乐,网易云", "com.netease.cloudmusic");
        add(map, "小红书", "com.xingin.xhs");
        return map;
    }

    private static void add(Map<String, String> map, String aliases, String packageName) {
        for (String alias : aliases.split(",")) map.put(normalize(alias), packageName);
    }
}
