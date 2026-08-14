package org.example.browser.layout;

public class MarginCollapser {
    private MarginCollapser() {
    }

    public static float collapse(float a, float b) {
        return largestPositiveMinusLargestNegative(a, b);
    }

    private static float largestPositiveMinusLargestNegative(float... values) {
        float maxPos = 0;
        float maxNeg = 0;
        for (float v : values) {
            if (v >= 0)
                maxPos = Math.max(maxPos, v);
            else
                maxNeg = Math.min(maxNeg, v);
        }
        return maxPos + maxNeg; // maxNeg <= 0, so this subtracts
    }
}
