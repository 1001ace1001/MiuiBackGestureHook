package dev.codex.miuibackgesturehook.hooks.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MiuiHomePackagesTest {
    @Test
    public void pocoIsSupportedOnlyOnAndroid16() {
        assertTrue(MiuiHomePackages.isSupportedPackage(MiuiHomePackages.POCO, 36));
        for (int sdk : new int[]{35, 37, 38}) {
            assertFalse(MiuiHomePackages.isSupportedPackage(MiuiHomePackages.POCO, sdk));
            assertEquals(MiuiHomePackages.MIUI,
                    MiuiHomePackages.selectPackage(sdk, MiuiHomePackages.POCO, false, true));
        }
        assertTrue(MiuiHomePackages.isSupportedPackage(MiuiHomePackages.MIUI, 36));
        assertTrue(MiuiHomePackages.isSupportedPackage(MiuiHomePackages.MIUI, 37));
    }

    @Test
    public void defaultLauncherSelectsOneOwnerWhenBothPackagesExist() {
        assertEquals(MiuiHomePackages.POCO,
                MiuiHomePackages.selectPackage(36, MiuiHomePackages.POCO, true, true));
        assertEquals(MiuiHomePackages.MIUI,
                MiuiHomePackages.selectPackage(36, MiuiHomePackages.MIUI, true, true));
    }

    @Test
    public void thirdPartyHomeKeepsTheAvailableXiaomiRecentsOwner() {
        assertEquals(MiuiHomePackages.POCO,
                MiuiHomePackages.selectPackage(36, "third.party.launcher", false, true));
        assertEquals(MiuiHomePackages.MIUI,
                MiuiHomePackages.selectPackage(36, "third.party.launcher", true, true));
        assertEquals(MiuiHomePackages.MIUI,
                MiuiHomePackages.selectPackage(36, null, false, false));
        assertEquals(MiuiHomePackages.MIUI,
                MiuiHomePackages.selectPackage(36, MiuiHomePackages.POCO, true, false));
    }

    @Test
    public void broadcastRequiresTheSelectedPackageAndItsActualUidOwnership() {
        assertTrue(trusted(MiuiHomePackages.POCO, MiuiHomePackages.POCO,
                new String[]{MiuiHomePackages.POCO}, 36));
        assertFalse(trusted(MiuiHomePackages.POCO, MiuiHomePackages.POCO,
                new String[]{"android", "com.android.systemui"}, 36));
        assertFalse(trusted(MiuiHomePackages.POCO, MiuiHomePackages.MIUI,
                new String[]{MiuiHomePackages.MIUI, MiuiHomePackages.POCO}, 36));
        assertFalse(trusted(MiuiHomePackages.MIUI, MiuiHomePackages.POCO,
                new String[]{MiuiHomePackages.MIUI, MiuiHomePackages.POCO}, 36));
        assertFalse(trusted(MiuiHomePackages.POCO, MiuiHomePackages.POCO, null, 36));
        assertFalse(trusted(MiuiHomePackages.POCO, null,
                new String[]{MiuiHomePackages.POCO}, 36));
    }

    @Test
    public void android17CannotAcceptPocoStateEvenIfItsUidMatches() {
        assertFalse(trusted(MiuiHomePackages.POCO, MiuiHomePackages.POCO,
                new String[]{MiuiHomePackages.POCO}, 37));
        assertTrue(trusted(MiuiHomePackages.MIUI, MiuiHomePackages.MIUI,
                new String[]{MiuiHomePackages.MIUI}, 37));
    }

    @Test
    public void prefixLookalikesNeverGainPackageOrBroadcastTrust() {
        for (String candidate : new String[]{"com.mi.android.globallauncher.fake",
                "com.miui.home.fake", "com.mi.android.globallauncher:remote", "android", null}) {
            assertFalse(MiuiHomePackages.isKnownPackage(candidate));
            assertFalse(trusted(candidate, candidate, new String[]{candidate}, 36));
        }
    }

    @Test
    public void bothLauncherProcessFamiliesRemainProtectedDuringHotReload() {
        for (String candidate : new String[]{MiuiHomePackages.MIUI, MiuiHomePackages.POCO,
                MiuiHomePackages.MIUI + ":remote", MiuiHomePackages.POCO + ":safe_mode"}) {
            assertTrue(MiuiHomePackages.isLauncherProcess(candidate));
        }
        assertFalse(MiuiHomePackages.isLauncherProcess("com.mi.android.globallauncher.fake"));
        assertFalse(MiuiHomePackages.isLauncherProcess(null));
    }

    private static boolean trusted(String expected, String sender, String[] packages, int sdk) {
        return MiuiHomePackages.isTrustedSender(expected, sender, packages, sdk);
    }
}
