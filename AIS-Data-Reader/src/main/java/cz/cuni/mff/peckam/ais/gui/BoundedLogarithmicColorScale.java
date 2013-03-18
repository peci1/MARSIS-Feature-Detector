/**
 * Copyright (c) 2013, Martin Pecka (peci1@seznam.cz)
 * All rights reserved.
 * Licensed under the following BSD License.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Martin Pecka nor the
 * names of contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cz.cuni.mff.peckam.ais.gui;

import java.awt.Color;

/**
 * Color scale bounded by a pair of numbers, giving all "outside" values a single color, and giving the "inside" values
 * colors according to a logarithmic rule.
 * 
 * @author Martin Pecka
 * @param <N> The numeric type of the value.
 */
public class BoundedLogarithmicColorScale<N extends Number> implements ColorScale<N>
{

    /** Log10 of the minimum value. */
    private final float        logMin;

    /** Log10 of the maximum value. */
    private final float        logMax;

    /** Range between {@link #logMin} and {@link #logMax}. */
    private final float        logRange;

    /** The maximum value of hue used in this scale. */
    private final static float HUE_MAX = 240f / 360f;

    /**
     * @param min The minimum value.
     * @param max The maximum value.
     */
    public BoundedLogarithmicColorScale(N min, N max)
    {
        if (min.doubleValue() > max.doubleValue())
            throw new IllegalArgumentException("min > max");

        this.logMin = (float) Math.log10(min.doubleValue());
        this.logMax = (float) Math.log10(max.doubleValue());

        this.logRange = logMax - logMin;
    }

    @Override
    public Color getColor(N value)
    {
        final float logValue = (float) Math.log10(value.doubleValue());

        if (logValue < logMin)
            return Color.black;

        if (logValue > logMax)
            return new Color(Color.HSBtoRGB(HUE_MAX, 1f, 1f));

        final float hue = (1 - (logValue - logMin) / logRange) * HUE_MAX;
        return new Color(Color.HSBtoRGB(hue, 1f, 1f));
    }

}
