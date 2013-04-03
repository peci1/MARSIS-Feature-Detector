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

import java.awt.Point;

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

    /** The maximum number of samples. */
    private static int      MAX_SAMPLES   = 1000;

    /** The columnKeys - frequencies. */
    private final Float[]   columnKeys;

    /**
     * @param original The original ionogram.
     */
    public EvenlySampledIonogram(Ionogram original)
    {
        super(original.getColumns(), original.getOrbitNumber(), original.getPositionInSeries());

        final int width = computeIdealNumOfFreqSamples(original);
        final int height = width / 2; // to maintain aspect ratio from original

        this.columnKeys = createColumnKeys(width);
        resample(original, width, height);

        for (ProductOverlay<?, Float, Float, ? extends Product<Float, Float, Float>> overlay : original.getOverlays()) {
            addOverlay(overlay);
        }
        setReferenceDetectionResult(original.getReferenceDetectionResult());
    }

    /**
     * Return the number of frequency samples needed for full-resolution resampling. The number has an upper limit in
     * order not to go too high.
     * 
     * @param original The ionogram to compute this value for.
     * @return The desired number of frequency samples.
     */
    private int computeIdealNumOfFreqSamples(Ionogram original)
    {
        final AISProduct[] columns = original.getColumns();
        float minFreqDiff = Float.MAX_VALUE;

        // we just wanna find the lowest frequency diff among all the given frequencies
        for (int i = 0; i < columns.length - 1; i++) {
            final float freqDiff = columns[i + 1].getFrequency() - columns[i].getFrequency();
            if (freqDiff < minFreqDiff)
                minFreqDiff = freqDiff;
        }

        final int numSamplesFromFreq = (int) Math.ceil(FREQUENCY_RANGE / minFreqDiff);
        return Math.min(numSamplesFromFreq, MAX_SAMPLES);
    }

    /**
     * Resample the original ionogram into this ionogram to a size given by the parameters.
     * 
     * @param original The (possibly unevenly scaled) original ionogram.
     * @param width The desired width.
     * @param height The desired height.
     */
    private void resample(Ionogram original, int width, int height)
    {
        setData(null); // to allow garbage collection
        System.gc();

        final float[][] data = new float[width][height];
        final float[][] weights = new float[width][height];
        final Float[][] origData = original.getData();

        final AISProduct[] cols = original.getColumns();
        // take the best interpolated positions of the old pixels to new bins and copy values; save the number of
        // original values in a new bin in the array weights
        for (int f = 0; f < origData.length; f++) {
            final int fBin = getFreqBin(cols[f].getFrequency(), width);
            for (int t = 0; t < origData[f].length; t++) {
                final int tBin = getTimeBin(t, height);
                data[fBin][tBin] += origData[f][t];
                weights[fBin][tBin] += 1;
            }
        }

        // firstly we weigh the original data
        for (int f = 0; f < width; f++) {
            for (int t = 0; t < height; t++) {
                if (weights[f][t] > 0) {
                    data[f][t] /= weights[f][t];
                }
            }
        }

        resampleData(data, weights, width, height);

        for (int f = 0; f < width; f++) {
            for (int t = 0; t < height; t++) {
                if (data[f][t] > 0)
                    weights[f][t] = 1;
            }
        }

        resampleData(data, weights, width, height);

        final Float[][] newData = new Float[width][height];
        for (int f = 0; f < width; f++) {
            for (int t = 0; t < height; t++) {
                newData[f][t] = data[f][t];
            }
        }
        setData(newData);
    }

    /**
     * Internal work.
     * 
     * @param data The data values.
     * @param weights Their weights.
     * @param width Width.
     * @param height Height.
     */
    private void resampleData(float[][] data, float[][] weights, int width, int height)
    {
        // for every "newly blank" point we will need the positions of its nearest neighbors from the original data set;
        // then we can interpolate correctly from these values

        final int[][] nearestLeftValues = new int[width][height];
        for (int t = 0; t < height; t++) {
            nearestLeftValues[0][t] = -1;
            for (int f = 1; f < width; f++) {
                if (weights[f][t] > 0) {
                    nearestLeftValues[f][t] = -1;
                } else if (weights[f - 1][t] > 0) {
                    nearestLeftValues[f][t] = f - 1;
                } else {
                    nearestLeftValues[f][t] = nearestLeftValues[f - 1][t];
                }
            }
        }

        final int[][] nearestRightValues = new int[width][height];
        for (int t = 0; t < height; t++) {
            nearestRightValues[width - 1][t] = -1;
            for (int f = width - 2; f >= 0; f--) {
                if (weights[f][t] > 0) {
                    nearestRightValues[f][t] = -1;
                } else if (weights[f + 1][t] > 0) {
                    nearestRightValues[f][t] = f + 1;
                } else {
                    nearestRightValues[f][t] = nearestRightValues[f + 1][t];
                }
            }
        }

        final int[][] nearestTopValues = new int[width][height];
        for (int f = 0; f < width; f++) {
            nearestTopValues[f][0] = -1;
            for (int t = 1; t < height; t++) {
                if (weights[f][t] > 0) {
                    nearestTopValues[f][t] = -1;
                } else if (weights[f][t - 1] > 0) {
                    nearestTopValues[f][t] = t - 1;
                } else {
                    nearestTopValues[f][t] = nearestTopValues[f][t - 1];
                }
            }
        }

        final int[][] nearestBottomValues = new int[width][height];
        for (int f = 0; f < width; f++) {
            nearestBottomValues[f][height - 1] = -1;
            for (int t = height - 2; t >= 0; t--) {
                if (weights[f][t] > 0) {
                    nearestBottomValues[f][t] = -1;
                } else if (weights[f][t + 1] > 0) {
                    nearestBottomValues[f][t] = t + 1;
                } else {
                    nearestBottomValues[f][t] = nearestBottomValues[f][t + 1];
                }
            }
        }

        final int maxDecayBins = (int) (Math.ceil(width / (float) NUM_FREQUENCY_BINS) * 4);

        for (int f = 0; f < width; f++) {
            for (int t = 0; t < height; t++) {
                if (weights[f][t] > 0) {
                    // has already been done
                } else {
                    final int nlv = nearestLeftValues[f][t];
                    final int nrv = nearestRightValues[f][t];
                    float freqValue = 0;
                    if (nlv != -1 && nrv != -1 && (f - nlv) <= maxDecayBins && (nrv - f) <= maxDecayBins) {
                        final float left = (f - nlv) * weights[nlv][t];
                        final float right = (nrv - f) * weights[nrv][t];
                        final float leftPos = left / (left + right);
                        freqValue = data[nlv][t] * leftPos + data[nrv][t] * (1 - leftPos);
                    } else if (nlv != -1 && (f - nlv) <= maxDecayBins) {
                        final float left = (f - nlv) * weights[nlv][t];
                        final float right = nlv + maxDecayBins;
                        final float leftPos = left / (left + right);
                        freqValue = data[nlv][t] * leftPos;
                    } else if (nrv != -1 && (nrv - f) <= maxDecayBins) {
                        final float left = nrv - maxDecayBins;
                        final float right = (nrv - f) * weights[nrv][t];
                        final float leftPos = left / (left + right);
                        freqValue = data[nrv][t] * (1 - leftPos);
                    }

                    final int ntv = nearestTopValues[f][t];
                    final int nbv = nearestBottomValues[f][t];
                    float timeValue = 0;
                    if (ntv != -1 && nbv != -1 && (t - ntv) <= maxDecayBins && (nbv - t) <= maxDecayBins) {
                        final float top = (t - ntv) * weights[f][ntv];
                        final float bottom = (nbv - t) * weights[f][nbv];
                        final float topPos = top / (top + bottom);
                        timeValue = data[f][ntv] * topPos + data[f][nbv] * (1 - topPos);
                    } else if (ntv != -1 && (t - ntv) <= maxDecayBins) {
                        final float top = (t - ntv) * weights[f][ntv];
                        final float bottom = ntv + maxDecayBins;
                        final float topPos = top / (top + bottom);
                        timeValue = data[f][ntv] * topPos;
                    } else if (nbv != -1 && (nbv - t) <= maxDecayBins) {
                        final float top = nbv - maxDecayBins;
                        final float bottom = (nbv - t) * weights[f][nbv];
                        final float topPos = top / (top + bottom);
                        timeValue = data[f][nbv] * (1 - topPos);
                    }

                    data[f][t] = (freqValue + timeValue) / 2;
                }
            }
        }
    }

    /**
     * Return the best new frequency bin corresponding to the given frequency.
     * 
     * @param frequency The frequency to get bin for.
     * @param numFreqBins The number of frequency bins to take into account.
     * @return The new frequency bin - a number in interval &lt;0;numFreqBins-1&gt;
     */
    private int getFreqBin(float frequency, int numFreqBins)
    {
        return (int) interpolate(frequency, (float) MIN_FREQUENCY, (float) MAX_FREQUENCY, 0, numFreqBins - 1);
    }

    /**
     * Return the best new time delay bin for the given old time delay bin after resizing to
     * <code>numNewDelayBins</code> bins.
     * 
     * @param oldDelayBin The bin in old number of bins.
     * @param numNewDelayBins The new number of bins.
     * @return The new position of the bin.
     */
    private int getTimeBin(int oldDelayBin, int numNewDelayBins)
    {
        return (int) interpolate(oldDelayBin, 0, NUM_TIME_DELAY_BINS - 1, 0, numNewDelayBins - 1);
    }

    /**
     * Interpolate <code>value</code> from interval <code>&lt;valueMin;valueMax&gt;</code> to
     * <code>&lt;newMin;newMax&gt;</code>.
     * 
     * @param value The value to interpolate.
     * @param valueMin Old min.
     * @param valueMax Old max.
     * @param newMin New min.
     * @param newMax New max.
     * @return The interpolated value.
     */
    private float interpolate(float value, float valueMin, float valueMax, float newMin, float newMax)
    {
        return newMin + (value - valueMin) / (valueMax - valueMin) * (newMax - newMin);
    }

    /**
     * Return the interpolated column keys for the given width of ionogram.
     * 
     * @param width The width of ionogram.
     * @return The column keys.
     */
    private Float[] createColumnKeys(int width)
    {
        final Float[] result = new Float[width];

        final double binWidth = FREQUENCY_RANGE / width;
        for (int i = 0; i < width; i++)
            result[i] = (float) (MIN_FREQUENCY + i * binWidth);

        return result;
    }

    @Override
    public Float[] getColumnKeys()
    {
        return this.columnKeys;
    }

    @Override
    public Point getDataPosition(Float row, Float column)
    {
        if (row < getMinRowValue() || row > getMaxRowValue())
            throw new IllegalArgumentException("Row value must lie within the interval <" + getMinRowValue() + "; "
                    + getMaxRowValue() + ">, but " + row + " was given.");

        if (column < getMinColumnValue() || column > getMaxColumnValue()) {
            throw new IllegalArgumentException("Column value must lie within the interval <" + getMinColumnValue()
                    + "; " + getMaxColumnValue() + ">, but " + column + " was given.");
        }

        final int rowPosition = getColumns()[0].getDataPosition(row, null).x;

        int colPosition = (int) ((column - getMinColumnValue()) / (getMaxColumnValue() - getMinColumnValue()) * getColumnKeys().length);
        colPosition = Math.min(colPosition, getColumnKeys().length - 1);

        return new Point(rowPosition, colPosition);
    }

}
