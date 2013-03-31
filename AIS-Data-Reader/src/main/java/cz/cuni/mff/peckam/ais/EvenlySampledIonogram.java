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
package cz.cuni.mff.peckam.ais;

import java.util.Arrays;

/**
 * A ionogram with the frequency (columns) evenly sampled.
 * <p>
 * The ionograms read directly from the .LBL files have uneven frequency sampling depending on which frequency table was
 * used for acquisition. This ionogram interpolates those data and generates an even sampling from them.
 * 
 * @author Martin Pecka
 */
public class EvenlySampledIonogram extends Ionogram
{

    // frequency table at http://www-pw.physics.uiowa.edu/plasma-wave/marsx/restricted/super/DOCUMENT/AIS_FREQ_TABLE.TXT

    /** The maximum frequency. */
    private static float    MIN_FREQUENCY   = 99500f;

    /** The maximum frequency. */
    private static float    MAX_FREQUENCY   = 5501800f;

    /** Range of the allowed frequencies. */
    private static float    FREQUENCY_RANGE = MAX_FREQUENCY - MIN_FREQUENCY;

    /** The maximum number of samples. */
    private static int      MAX_SAMPLES   = 1000;

    /** The ionogram's data. */
    private final Float[][] data;

    /** The columnKeys - frequencies. */
    private final Float[]   columnKeys;

    /** The original ionogram. */
    private final Ionogram  original;

    /**
     * @param original The original ionogram.
     */
    public EvenlySampledIonogram(Ionogram original)
    {
        super(original.getColumns(), original.getOrbitNumber(), original.getPositionInSeries());

        this.original = original;

        final AISProduct[] columns = original.getColumns();
        float minFreqDiff = Float.MAX_VALUE;
        final float SMALLEST_VALUE = 0.00001f;

        for (int i = 0; i < columns.length - 1; i++) {
            final float freqDiff = columns[i + 1].getFrequency() - columns[i].getFrequency();
            if (freqDiff < minFreqDiff && freqDiff > SMALLEST_VALUE)
                minFreqDiff = freqDiff;
        }

        final int numSamplesFromFreq = (int) Math.ceil(FREQUENCY_RANGE / minFreqDiff);
        final int numSamples = Math.min(numSamplesFromFreq, MAX_SAMPLES);

        this.data = new Float[numSamples][columns[0].getHeight()];
        for (int i = 0; i < data.length; i++) {
            final Float[] column = new Float[columns[0].getHeight()];
            Arrays.fill(column, 0f);
            data[i] = column;
        }

        this.columnKeys = new Float[numSamples];

        for (int sample = 0, col = 0; sample < data.length; sample++) {
            final float sampleFreq = MIN_FREQUENCY + sample / (float) numSamples * FREQUENCY_RANGE;
            this.columnKeys[sample] = sampleFreq / 1E6f;

            while (col < columns.length - 1 && sampleFreq > columns[col + 1].getFrequency())
                col++;

            AISProduct prevColumn = null, nextColumn = null;
            float prevFreq = 0f, nextFreq = 0f, freqDiff = 0f;

            while (Math.abs(freqDiff) < SMALLEST_VALUE && col < columns.length - 1) {
                prevColumn = columns[col];
                nextColumn = columns[col + 1];

                prevFreq = prevColumn.getFrequency();
                nextFreq = nextColumn.getFrequency();
                freqDiff = nextFreq - prevFreq;

                if (Math.abs(freqDiff) < SMALLEST_VALUE)
                    col++;
            }

            if (prevColumn == null || nextColumn == null)
                continue;

            final Float[] prevData = prevColumn.getData()[0];
            final Float[] nextData = nextColumn.getData()[0];

            final float sampleRatio = (sampleFreq - prevFreq) / freqDiff;
            for (int j = 0; j < prevColumn.getHeight(); j++) {
                this.data[sample][j] = sampleRatio * prevData[j] + (1 - sampleRatio) * nextData[j];
            }

            for (ProductOverlay<?, Float, Float, ? extends Product<Float, Float, Float>> overlay : original
                    .getOverlays()) {
                addOverlay(overlay);
            }
        }
    }

    @Override
    public Float[][] getData()
    {
        return data;
    }

    @Override
    public int getWidth()
    {
        return data.length;
    }

    @Override
    public int getHeight()
    {
        return data[0].length;
    }

    /**
     * @return The original unevenly sampled ionogram.
     */
    public Ionogram getOriginal()
    {
        return original;
    }

    @Override
    public Float[] getColumnKeys()
    {
        return this.columnKeys;
    }
}
