package org.example.browser.layout;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LengthTest {

    @Test
    public void parsesPixels() {
        Length l = Length.parse("10px");
        assertEquals(Length.Type.Px, l.type);
        assertEquals(10f, l.value, 0.0001f);
    }

    @Test
    public void parsesPlainNumberAsPixels() {
        Length l = Length.parse("24");
        assertEquals(Length.Type.Px, l.type);
        assertEquals(24f, l.value, 0.0001f);
    }

    @Test
    public void parsesPercent() {
        Length l = Length.parse("50%");
        assertEquals(Length.Type.Percent, l.type);
        assertEquals(50f, l.value, 0.0001f);
    }

    @Test
    public void parsesAuto() {
        assertEquals(Length.Type.Auto, Length.parse("auto").type);
        assertEquals(Length.Type.Auto, Length.parse("").type);
        assertEquals(Length.Type.Auto, Length.parse("  ").type);
        assertEquals(Length.Type.Auto, Length.parse(null).type);
        assertEquals(Length.Type.Auto, Length.AUTO.type);
    }

    @Test
    public void resolvesPixelsToValue() {
        assertEquals(42f, Length.parse("42px").resolve(1000f), 0.0001f);
    }

    @Test
    public void resolvesPercentAgainstBase() {
        assertEquals(50f, Length.parse("50%").resolve(100f), 0.0001f);
    }

    @Test
    public void autoResolvesToZero() {
        assertEquals(0f, Length.AUTO.resolve(100f), 0.0001f);
    }

    @Test
    public void resolveNonAutoReturnsZeroForAuto() {
        assertEquals(0f, Length.AUTO.resolveNonAuto(100f), 0.0001f);
        assertEquals(20f, Length.parse("20px").resolveNonAuto(100f), 0.0001f);
    }

    @Test
    public void toStringRendersTypes() {
        assertEquals("10.0px", Length.parse("10px").toString());
        assertEquals("50.0%", Length.parse("50%").toString());
        assertEquals("auto", Length.AUTO.toString());
    }
}
