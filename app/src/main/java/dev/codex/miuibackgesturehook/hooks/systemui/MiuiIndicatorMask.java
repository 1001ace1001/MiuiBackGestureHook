// SPDX-License-Identifier: Apache-2.0
package dev.codex.miuibackgesturehook.hooks.systemui;

/** Converts resource opacity into edge coverage without a binary silhouette threshold. */
final class MiuiIndicatorMask {
    private MiuiIndicatorMask() {}

    static void normalizeAlpha(int[] pixels) {
        int peak = 0;
        for (int pixel : pixels) {
            peak = Math.max(peak, pixel >>> 24);
        }
        if (peak == 0) {
            throw new IllegalArgumentException("Indicator mask has no visible pixels");
        }
        for (int i = 0; i < pixels.length; i++) {
            int alpha = pixels[i] >>> 24;
            int coverage = (alpha * 255 + peak / 2) / peak;
            pixels[i] = (coverage << 24) | 0x00FFFFFF;
        }
    }
}
