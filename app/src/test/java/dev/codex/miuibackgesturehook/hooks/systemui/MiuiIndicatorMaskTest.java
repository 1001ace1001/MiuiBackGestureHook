package dev.codex.miuibackgesturehook.hooks.systemui;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public final class MiuiIndicatorMaskTest {
    @Test
    public void translucentResourceKeepsPartialEdgeCoverageAndOpaqueInterior() {
        int[] pixels = {0x00123456, 0x10000000, 0x20808080, 0x40FFFFFF, 0x80000000};
        MiuiIndicatorMask.normalizeAlpha(pixels);
        assertArrayEquals(new int[] {
                0x00FFFFFF, 0x20FFFFFF, 0x40FFFFFF, 0x80FFFFFF, 0xFFFFFFFF
        }, pixels);
    }

    @Test
    public void opaqueResourcePreservesEveryAntialiasedEdgeAlphaRegardlessOfColor() {
        int[] pixels = {0x00123456, 0x01000000, 0x7F010203, 0xFEFFFFFF, 0xFF202124};
        MiuiIndicatorMask.normalizeAlpha(pixels);
        assertArrayEquals(new int[] {
                0x00FFFFFF, 0x01FFFFFF, 0x7FFFFFFF, 0xFEFFFFFF, 0xFFFFFFFF
        }, pixels);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fullyTransparentResourceCannotBecomeAnOpaqueRectangle() {
        MiuiIndicatorMask.normalizeAlpha(new int[] {0x00123456, 0x00FFFFFF});
    }
}
