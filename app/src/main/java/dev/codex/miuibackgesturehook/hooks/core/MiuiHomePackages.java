package dev.codex.miuibackgesturehook.hooks.core;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.Arrays;

/**
 * Package identity differs from the com.miui.home class namespace on POCO.
 */
public final class MiuiHomePackages {
    public static final String MIUI = "com.miui.home";
    public static final String POCO = "com.mi.android.globallauncher";

    private MiuiHomePackages() {
    }

    public static boolean isKnownPackage(String packageName) {
        return MIUI.equals(packageName) || POCO.equals(packageName);
    }

    public static boolean isSupportedPackage(String packageName, int sdk) {
        return MIUI.equals(packageName) || (sdk == Build.VERSION_CODES.BAKLAVA && POCO.equals(packageName));
    }

    public static boolean isLauncherProcess(String processName) {
        return isKnownPackage(processName) || (processName != null
                && (processName.startsWith(MIUI + ":") || processName.startsWith(POCO + ":")));
    }

    public static boolean isTrustedSender(String expectedPackage, String senderPackage,
                                          String[] uidPackages, int sdk) {
        return isSupportedPackage(expectedPackage, sdk)
                && expectedPackage.equals(senderPackage)
                && uidPackages != null && Arrays.asList(uidPackages).contains(senderPackage);
    }

    static String selectPackage(int sdk, String homePackage, boolean miuiEnabled,
                                boolean pocoEnabled) {
        if (sdk != Build.VERSION_CODES.BAKLAVA) {
            return MIUI;
        }
        if (POCO.equals(homePackage) && pocoEnabled) {
            return POCO;
        }
        if (miuiEnabled) {
            return MIUI;
        }
        return pocoEnabled ? POCO : MIUI;
    }

    public static String resolve(Context context) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.BAKLAVA) {
            return MIUI;
        }
        String ownPackage = context.getPackageName();
        if (isKnownPackage(ownPackage)) {
            return ownPackage;
        }
        PackageManager pm = context.getPackageManager();
        ResolveInfo home = pm.resolveActivity(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME),
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY));
        String homePackage = home == null || home.activityInfo == null
                ? null : home.activityInfo.packageName;
        return selectPackage(Build.VERSION.SDK_INT, homePackage,
                isEnabled(pm, MIUI), isEnabled(pm, POCO));
    }

    private static boolean isEnabled(PackageManager pm, String packageName) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName,
                    PackageManager.ApplicationInfoFlags.of(0));
            return info.enabled;
        } catch (PackageManager.NameNotFoundException missing) {
            return false;
        }
    }
}
