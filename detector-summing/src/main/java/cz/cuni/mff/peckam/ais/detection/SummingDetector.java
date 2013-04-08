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
package cz.cuni.mff.peckam.ais.detection;

import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.analysis.function.HarmonicOscillator.Parametric;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.fitting.HarmonicFitter;
import org.apache.commons.math3.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;

import cz.cuni.mff.peckam.ais.AISLBLProductReader;
import cz.cuni.mff.peckam.ais.EvenlySampledIonogram;
import cz.cuni.mff.peckam.ais.Ionogram;
import cz.cuni.mff.peckam.ais.Product;
import cz.cuni.mff.peckam.ais.Tuple;

/**
 * Detector using sums of rows/columns.
 * 
 * @author Martin Pecka
 */
public class SummingDetector extends FloatFeatureDetector
{

    /** The strategy used for computing. */
    private ComputationStrategy strategy = ComputationStrategy.QUANTILE_PEAK_DISTANCE_ESTIMATION;

    /**
     * The strategy used for computation.
     * 
     * @author Martin Pecka
     */
    public enum ComputationStrategy
    {
        /**
         * Compute the period using periodogram as defined in (Scargle, 1982).
         * 
         * @author Martin Pecka
         */
        PERIODOGRAM
        {
            @Override
            Tuple<Integer, Double> computePeriod(float[] peaks, float[] weights)
            {
                // variable names from (Scargle, 1982)
                final int n0 = peaks.length, t = n0;
                double bestFreq = 0;
                double bestValue = -Float.MAX_VALUE;
                float[] periodogram = new float[n0];
                // the selection of n is such that we get periods lower than width/2 and higher than the lowest
                // detectable period
                for (int n = (int) (t / getMinPeakDistance(peaks)); n <= n0 / 2; n++) {
                    final double freq = 2 * PI * n / t;
                    final double periodogramVal = computePeriodogram(freq, peaks);
                    periodogram[n - 1] = (float) periodogramVal;
                    if (periodogramVal > bestValue) {
                        bestValue = periodogramVal;
                        bestFreq = freq;
                    }
                }

                // bestFreq contains the frequency with the highest periodogram peak, which should correspond to the
                // most probable frequency
                final double period = 2 * PI / bestFreq;
                return new Tuple<>(null, period);
            }

            /**
             * Compute an improved periodogram value as defined in (Scargle, 1982).
             * 
             * @param freq The input frequency.
             * @param values The values to compute the periodogram for.
             * @return The periodogram value, P_X(freq)
             */
            private double computePeriodogram(double freq, float[] values)
            {
                double tau_sinSum = 0;
                double tau_cosSum = 0;
                for (int i = 1; i <= values.length; i++) {
                    tau_sinSum += sin(2 * freq * i);
                    tau_cosSum += cos(2 * freq * i);
                }

                double tau;
                if (tau_cosSum <= 10E-15) {
                    tau = 0;
                } else {
                    final double tan_tau = tau_sinSum / tau_cosSum;
                    tau = atan(tan_tau) / (2 * PI);
                }

                double cos_valSum = 0, cos_sum = 0, sin_valSum = 0, sin_sum = 0;
                for (int i = 1; i <= values.length; i++) {
                    final double cos = cos(freq * (i - tau));
                    final double sin = sin(freq * (i - tau));
                    cos_valSum += values[i - 1] * cos;
                    cos_sum += cos * cos;
                    sin_valSum += values[i - 1] * sin;
                    sin_sum += sin * sin;
                }

                if (cos_sum <= 10E-15 || sin_sum <= 10E-15)
                    return 0;
                if (cos_valSum <= 10E-15)
                    cos_valSum = 0;
                if (sin_valSum <= 10E-15)
                    sin_valSum = 0;
                return 0.5 * (cos_valSum * cos_valSum / cos_sum + sin_valSum * sin_valSum / sin_sum);
            }

            @Override
            public String toString()
            {
                return "periodogram";
            }

        },

        /**
         * Compute the period using harmonics fitting least squares.
         * 
         * @author Martin Pecka
         */
        HARMONICS_FITTING
        {

            @Override
            Tuple<Integer, Double> computePeriod(final float[] peaks, float[] weights)
            {
                final HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());

                // square root the weights in order to get them more equal, which helps
                float[] newWeights = new float[weights.length];
                for (int i = 0; i < weights.length; i++)
                    newWeights[i] = (float) FastMath.sqrt(weights[i]);
                newWeights = normalize(newWeights);

                // register the weighted peak values to the fitter
                for (int i = 0; i < peaks.length; i++) {
                    // the fitter requires non-zero weights
                    fitter.addObservedPoint(newWeights[i] > 0 ? newWeights[i] : 0.001, i, peaks[i]);
                }

                // guess initial values for the fitting and set amplitude to 1 (since we want the result to have
                // amplitude 1)
                final double[] guess = new HarmonicFitter.ParameterGuesser(fitter.getObservations()).guess();
                guess[0] = 1;

                // the fitter cannot handle parameter constraints, so we extend the Harmonic function to return a big
                // negative number for all parameters we don't like (e.g. amplitude != 1, frequency is too big/small).
                final Parametric func = new Parametric() {
                    /** The maximum period we can detect is half of the data width. */
                    private final double minFreq = 2 * PI / (peaks.length / 2);
                    /** The minimum period we can detect. */
                    // we won't detect periods lower than the minimum distance of two peaks
                    private final double maxFreq = 2 * PI / getMinPeakDistance(peaks);

                    @Override
                    public double value(double x, double... param) throws NullArgumentException,
                            DimensionMismatchException
                    {
                        if (!allowedParams(param))
                            return -100;
                        return super.value(x, param);
                    }

                    @Override
                    public double[] gradient(double x, double... param) throws NullArgumentException,
                            DimensionMismatchException
                    {
                        final double[] result = super.gradient(x, param);
                        if (!allowedParams(param)) {
                            // try to direct the fitter to the wanted parameter values using the derivative
                            final double da = param[0] > 1 ? -10 : param[0] < 1 ? 1 : 0;
                            final double df = param[1] < minFreq ? 10 : param[1] > maxFreq ? -10 : result[1];
                            return new double[] { da, df, result[2] };

                        }
                        return result;
                    }

                    private boolean allowedParams(double[] params)
                    {
                        return params[1] >= minFreq && params[1] <= maxFreq && FastMath.abs(params[0] - 1) < 0.1;
                    }
                };

                // perform the fitting
                final double[] fit = fitter.fit(func, guess);
                final double period = 2 * PI / fit[1];
                final double phase = fit[2];
                return new Tuple<>((int) phase, period);
            }

            @Override
            public String toString()
            {
                return "fitting";
            }
        },

        /**
         * Estimate the period from the quantiles of peak distances.
         * 
         * @author Martin Pecka
         */
        QUANTILE_PEAK_DISTANCE_ESTIMATION
        {

            @Override
            Tuple<Integer, Double> computePeriod(float[] peaks, float[] weights)
            {
                // compute peak distances and interpolate weights for them
                final List<Integer> dists = new ArrayList<>();
                final List<Float> selectedWeights = new ArrayList<>();
                int prevPeak = -1;
                for (int i = 0; i < peaks.length; i++) {
                    if (peaks[i] == 0)
                        continue;
                    if (prevPeak >= 0) {
                        dists.add(i - prevPeak);
                        selectedWeights.add((weights[i] + weights[prevPeak]) / 2);
                    }
                    prevPeak = i;
                }

                if (dists.size() == 0)
                    return null;
                else if (dists.size() == 1)
                    return new Tuple<>(null, (double) dists.get(0));

                // convert to double[] for use with Commons Math
                final double[] distances = new double[dists.size()];
                final double[] newWeightsArr = new double[dists.size()];
                for (int i = 0; i < distances.length; i++) {
                    distances[i] = dists.get(i);
                    newWeightsArr[i] = selectedWeights.get(i);
                }

                // take the 0- and 65-percentils of distances, which means throw away big values;
                final double low = new Min().evaluate(distances), high = new Percentile(65)
                        .evaluate(distances);
                final double[] nearMedians = new double[distances.length / 2];
                final double[] nearMedianWeights = new double[distances.length / 2];
                for (int i = 0, j = 0; i < distances.length && j < nearMedians.length; i++) {
                    if (distances[i] >= low && distances[i] <= high) {
                        nearMedians[j] = distances[i];
                        nearMedianWeights[j] = newWeightsArr[i];
                        j++;
                    }
                }

                // perform a weighted sum of all the distances left after the previous step
                final double period = nearMedians.length > 1 ? MathArrays.linearCombination(nearMedians,
                        MathArrays.normalizeArray(nearMedianWeights, 1)) : nearMedians[0];
                return new Tuple<>(null, period);
            }

            @Override
            public String toString()
            {
                return "quantile";
            }
        };

        /**
         * Compute the period of the peaks possibly taking into account their weights.
         * 
         * @param peaks The normalized peaks and zeros elsewhere.
         * @param weights Weights of the peaks.
         * @return The period of the peaks. <code>null</code> if no period is present.
         */
        abstract Tuple<Integer, Double> computePeriod(float[] peaks, float[] weights);
    }

    @Override
    protected List<DetectedFeature> detectFeaturesImpl(Product<Float, ?, ?> product)
    {
        final List<DetectedFeature> result = new LinkedList<>();

        final float[][] data = prepareData(product.getData());

        {
            final Tuple<Integer, Double> horizRepeat = detectRepetition(getColumnSums(data));
            if (horizRepeat != null) {
                int offset = horizRepeat.getX() != null ? horizRepeat.getX() : 0;
                result.add(new ElectronPlasmaOscillation(offset, horizRepeat.getY(), 8));
            }
        }

        {
            final Tuple<Integer, Double> vertRepeat = detectRepetition(getRowSums(data));
            if (vertRepeat != null) {
                int offset = vertRepeat.getX() != null ? vertRepeat.getX() : 0;
                result.add(new ElectronCyclotronEchoes(offset, vertRepeat.getY(), 8));
            }
        }

        return result;
    }

    /**
     * Detect repetition in the given row/column sums.
     * 
     * @param sums The row/column sums.
     * 
     * @return <code>null</code> if no pattern has been found. Otherwise, the first entry in the tuple means offset,
     *         while the other entry means period of repetition.
     */
    private Tuple<Integer, Double> detectRepetition(float[] sums)
    {
        final int n0 = sums.length, t = n0;
        final float[] peaks = new float[t];
        final double quantile = new Percentile(60).evaluate(asDouble(sums));
        for (int i = 0; i < peaks.length; i++) {
            if (sums[i] >= quantile)
                peaks[i] = sums[i];
        }

        // only local maxima should remain in peaks
        filterPeaks(peaks);

        // make the peaks uniform and normalize their weights
        float[] weights = new float[peaks.length];
        System.arraycopy(peaks, 0, weights, 0, peaks.length);
        weights = normalize(weights);
        for (int i = 0; i < peaks.length; i++) {
            if (peaks[i] > 0) {
                peaks[i] = 1;
            }
        }

        return strategy.computePeriod(peaks, weights);
    }

    /**
     * Leave only local maxima in the data, put zeros elsewhere.
     * 
     * @param peaks The data to filter.
     */
    private void filterPeaks(float[] peaks)
    {
        // filter out all values that are not local maxima
        for (int i = 1; i < peaks.length - 1; i++) {
            if (peaks[i - 1] <= peaks[i] && peaks[i + 1] <= peaks[i]) {
                { // set j to the leftmost end of a nonincreasing part left from i
                    int j = i - 1;
                    while (j > 0 && peaks[j] > 0 && peaks[j] <= peaks[j + 1])
                        j--;
                    for (; j < i; j++)
                        peaks[j] = 0;
                }

                { // here we process to the right until the sequence is nonincreasing
                  // we intentionally increase the iteration variable i!
                    final int peakI = i;
                    i++;
                    for (; i < peaks.length - 1 && peaks[i] > 0 && peaks[i] >= peaks[i + 1]; i++)
                        peaks[i] = 0;
                    peaks[peakI + 1] = 0;
                }
            }
        }
    }

    /**
     * Process the input data and do whatever is needed to be able to work on them.
     * 
     * @param data The data.
     * @return The processed data.
     */
    private float[][] prepareData(Float[][] data)
    {
        final int w = data.length, h = data[0].length;
        final float[][] result = new float[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                result[x][y] = data[x][y];
            }
        }

        return result;
    }

    /**
     * Return the row sums of the given data.
     * 
     * @param data The data to sum up.
     * @return The row sums
     */
    private float[] getRowSums(float[][] data)
    {
        final int w = data.length / 2, h = data[0].length;

        final float[] result = new float[h];
        for (int y = 0; y < h; y++) {
            float sum = 0;
            for (int x = 0; x < w; x++) {
                sum += data[x][y];
            }
            result[y] = sum;
        }
        return result;
    }

    /**
     * Return the column sums of the given data.
     * 
     * @param data The data to sum up.
     * @return The column sums
     */
    private float[] getColumnSums(float[][] data)
    {
        final int w = data.length / 2, h = data[0].length;

        final float[] result = new float[w];
        for (int x = 0; x < w; x++) {
            float sum = 0;
            for (int y = 0; y < h; y++) {
                sum += data[x][y];
            }
            result[x] = sum;
        }
        return result;
    }

    /**
     * Set the computation strategy.
     * 
     * @param strategy The strategy to use.
     */
    public void setStrategy(ComputationStrategy strategy)
    {
        this.strategy = strategy;
    }

    /**
     * @return The computation strategy.
     */
    public ComputationStrategy getStrategy()
    {
        return strategy;
    }

    /**
     * Normalize the data.
     * 
     * @param data The data to normalize.
     * @return The normalized copy of data.
     */
    private static float[] normalize(float[] data)
    {
        float sum = 0;
        for (float f : data)
            sum += f;
        final float[] result = new float[data.length];
        for (int i = 0; i < result.length; i++)
            result[i] = data[i] / sum;
        return result;
    }

    /**
     * Return the minimal horizontal distance between peaks.
     * 
     * @param peaks The peaks array.
     * @return The minimal distance between peaks.
     */
    private static float getMinPeakDistance(float[] peaks)
    {
        float minPeakDistance = Float.MAX_VALUE;
        int prevPeak = -1;
        for (int i = 0; i < peaks.length; i++) {
            if (peaks[i] == 0)
                continue;
            if (prevPeak >= 0) {
                if (i - prevPeak < minPeakDistance)
                    minPeakDistance = i - prevPeak;
            }
            prevPeak = i;
        }
        return minPeakDistance;
    }

    /**
     * Test the detector on the given ionogram.
     * 
     * @param args 0 =&gt; Orbit file, 1 =&gt; position of the ionogram in the file.
     * 
     * @throws IOException On IO exception.
     */
    public static void main(String[] args) throws IOException
    {
        final File orbitFile = new File(args[0]);
        final int position = Integer.parseInt(args[1]);

        final Ionogram ionogram = new EvenlySampledIonogram(new AISLBLProductReader().readFile(orbitFile)[position]);
        System.out.println(new SummingDetector().detectFeatures(ionogram));
    }


    /**
     * @param a the array
     * @return the array
     */
    private static double[] asDouble(float[] a)
    {
        final double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i];
        }
        return r;
    }
}
