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

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import cz.cuni.mff.peckam.ais.AISLBLProductReader;
import cz.cuni.mff.peckam.ais.EvenlySampledIonogram;
import cz.cuni.mff.peckam.ais.Ionogram;
import cz.cuni.mff.peckam.ais.Product;
import cz.cuni.mff.peckam.ais.Tuple;

/**
 * Detector using vectorization techniques.
 * 
 * @author Martin Pecka
 */
public class VectorizationDetector extends FloatFeatureDetector
{

    /** The strategy used for computing. */
    private ComputationStrategy strategy = ComputationStrategy.THINNING;

    /**
     * The strategy used for computation.
     * 
     * @author Martin Pecka
     */
    public enum ComputationStrategy
    {

        /**
         * Skeletonization method -- does not work!
         * 
         * @author Martin Pecka
         */
        @Deprecated
        SKELETONIZATION
        {
            @Override
            List<DetectedFeature> detect(float[][] data)
            {
                skeletonize(data);
                return Collections.emptyList();
            }

            /**
             * Perform skeletonization on the data.
             * 
             * @param data The data to skeletonize.
             */
            private void skeletonize(float[][] data)
            {
                final int w = data.length, h = data[0].length;

                final int sigma = 2; // the regularization threshold
                final int maxIter = 100;
                final double EPS = 1E-20;
                final double[][] gradI0x = new double[w][h];
                final double[][] gradI0y = new double[w][h];
                final double[][] gradI1x = new double[w][h];
                final double[][] gradI1y = new double[w][h];
                final int[][] numRegularized = new int[w][h];

                for (int x = 0; x + 1 < w; x++) {
                    for (int y = 0; y + 1 < h; y++) {
                        gradI0x[x][y] = data[x + 1][y] - data[x][y];
                        gradI0y[x][y] = data[x][y + 1] - data[x][y];
                    }
                }

                // print(gradI0y);

                int numIter = 0;
                double stability = 1;
                while (numIter <= maxIter && stability != 0) {
                    stability = 0;
                    for (int x = 0; x < w; x++) {
                        for (int y = 0; y < h; y++) {
                            if (numRegularized[x][y] < sigma || max(abs(gradI0x[x][y]), abs(gradI0y[x][y])) < EPS) {
                                double gradMx = 0, gradMy = 0;
                                for (int nx = max(x - 1, 0); nx < min(w, x + 1); nx++) {
                                    for (int ny = max(y - 1, 0); ny < min(h, y + 1); ny++) {
                                        if (nx != x || ny != y) {
                                            gradMx += gradI0x[nx][ny];
                                            gradMy += gradI0y[nx][ny];
                                        }
                                    }
                                }
                                gradMx /= 8;
                                gradMy /= 8;

                                if (abs(gradMx) > EPS && abs(gradMy) > EPS) {
                                    stability += abs(gradI1x[x][y] - gradMx);
                                    stability += abs(gradI1y[x][y] - gradMy);
                                    gradI1x[x][y] = gradMx;
                                    gradI1y[x][y] = gradMy;
                                    numRegularized[x][y]++;
                                }
                            }
                        }
                    }

                    for (int x = 0; x < w; x++) {
                        for (int y = 0; y < h; y++) {
                            gradI0x[x][y] = gradI1x[x][y];
                            gradI0y[x][y] = gradI1y[x][y];
                        }
                    }

                    numIter++;
                }

                final double[][] theta = new double[w][h];
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        theta[x][y] = Math.atan2(gradI0y[x][y], gradI0x[x][y]);
                    }
                }
                // print(theta);
                final double[][] skeletonStrength = new double[w][h];
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        double max = -Double.MAX_VALUE;
                        if (x - 1 >= 0 && x + 1 < w)
                            max = max(max, abs(theta[x - 1][y] - theta[x + 1][y]));
                        if (y - 1 >= 0 && y + 1 < h)
                            max = max(max, abs(theta[x][y - 1] - theta[x][y + 1]));
                        if (x - 1 >= 0 && x + 1 < w && y - 1 >= 0 && y + 1 < h) {
                            max = max(max, abs(theta[x - 1][y - 1] - theta[x + 1][y + 1]));
                            max = max(max, abs(theta[x + 1][y - 1] - theta[x - 1][y + 1]));
                        }
                        // if (max <= Math.PI && max >= (15 / 16 * Math.PI))
                            skeletonStrength[x][y] = max;
                    }
                }

                print(skeletonStrength);
            }

            private void print(double[][] arr)
            {
                try (final BufferedWriter w = new BufferedWriter(new FileWriter("skeleton"));) {
                    for (int y = arr[0].length - 1; y >= 0; y--) {
                        for (int x = 0; x < arr.length; x++) {
                            w.write(arr[x][y] + " ");
                        }
                        w.newLine();
                    }
                } catch (IOException e) {

                }
            }

            @Override
            public String toString()
            {
                return "gradient diffusion";
            }

        },

        /**
         * Iterative thinning.
         * 
         * @author Martin Pecka
         */
        THINNING
        {
            @Override
            List<DetectedFeature> detect(float[][] data)
            {
                final int w = data.length, h = data[0].length;
                final float[][] horizThinned = new float[w/2][h];
                final float[][] vertThinned = new float[w][h];
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        if (x < w/2) {
                            horizThinned[x][y] = data[x][y];
                        }
                        vertThinned[x][y] = data[x][y];
                    }
                }
                
                thinHorizontal(horizThinned);
                thinVertical(vertThinned);
                
                final DetectedFeature hPeriod = detectHPeriod(horizThinned);
                final DetectedFeature vPeriod = detectVPeriod(vertThinned);
                final DetectedFeature ground = detectGroundEcho(vertThinned);
                final DetectedFeature iono = detectIonoEcho(vertThinned);
                
                final List<DetectedFeature> result = new LinkedList<>();
                @SuppressWarnings("unused")
                Object foo;
                foo = (hPeriod != null) ? result.add(hPeriod) : null;
                foo = (vPeriod != null) ? result.add(vPeriod) : null;
                foo = (ground != null) ? result.add(ground) : null;
                foo = (iono != null) ? result.add(iono) : null;
                
                return result;
            }
            
            private DetectedFeature detectHPeriod(float[][] horizThinned)
            {
                final float[] colSums = new float[horizThinned.length];
                final int[] colCounts = new int[horizThinned.length];
                for (int x = 0; x < horizThinned.length; x++) {
                    for (int y = 0; y < horizThinned[0].length; y++) {
                        if (horizThinned[x][y] > 0) {
                            colSums[x] += horizThinned[x][y];
                            colCounts[x]++;
                        }
                    }
                }
                
                // do some smoothing
                final int smoothingLength = 4;
                for (int x = 0; x < horizThinned.length - smoothingLength; x++) {
                    if (colSums[x] > 0) {
                        inner: for (int i = 1; i <= smoothingLength; i++) {
                            if (colSums[x + i] > colSums[x]) {
                                colSums[x + i] += colSums[x];
                                colCounts[x + i] += colCounts[x];
                                colSums[x] = 0;
                                colCounts[x] = 0;
                                break inner;
                            }
                        }
                    }
                }

                final float[] peaks = new float[horizThinned.length];
                float sum = 0;
                int offset = -1, end = 0;
                // remove columns shorter than 20 px and not having a value at the first 4 pixels
                for (int x = 0; x < horizThinned.length; x++) {
                    if (colCounts[x] > 0 && colCounts[x] < 20 && horizThinned[x][0] == 0 && horizThinned[x][1] == 0
                            && horizThinned[x][2] == 0 && horizThinned[x][3] == 0) {
                        colSums[x] = 0;
                    } else {
                        if (colCounts[x] > 0) {
                            peaks[x] = 1;
                            sum += colSums[x];
                            if (offset == -1)
                                offset = x;
                            end = x;
                        }
                    }
                }

                // normalize the weights
                for (int x = 0; x < horizThinned.length; x++) {
                    colSums[x] = colSums[x] / sum;
                }

                // final Tuple<Integer, Double[]> result =
                // SummingDetector.ComputationStrategy.COMBINED_QUANTILE_PERIODOGRAM
                final Tuple<Integer, Double[]> result = SummingDetector.ComputationStrategy.PERIODOGRAM
                        .computePeriod(peaks, colSums);
                if (result == null || result.getY().length == 0)
                    return new ElectronPlasmaOscillation(0, 0, 0);

                return new ElectronPlasmaOscillation(offset, result.getY()[0], end);
            }

            private DetectedFeature detectVPeriod(float[][] vertThinned)
            {
                // we only take the first fourth into account, which is sufficient
                final boolean[] possibleEchoes = new boolean[vertThinned[0].length];
                for (int y = 0; y < possibleEchoes.length; y++) {
                    if (vertThinned[0][y] > 0)
                        possibleEchoes[y] = true;
                }

                // only retain echoes that go over 2E-15 in the 20px strip at the left; this should rule out some noise
                for (int y = 0; y < possibleEchoes.length; y++) {
                    if (possibleEchoes[y]) {
                        possibleEchoes[y] = false;
                        inner: for (int x = 0; x < 20; x++) {
                            if (vertThinned[x][y] > 2E-15) {
                                possibleEchoes[y] = true;
                                break inner;
                            }
                        }
                    }
                }

                int echoCount = 0;
                int firstEcho = -1;
                for (int y = 0; y < possibleEchoes.length; y++) {
                    if (possibleEchoes[y]) {
                        echoCount++;
                        if (firstEcho == -1)
                            firstEcho = y;
                    }
                }

                // no vPeriod
                if (echoCount == 0)
                    return new ElectronCyclotronEchoes(0, 0, 0);

                // since we have 80 time bins, we cannot detect more than 40 harmonics
                // mainly this should rule out cases with the first hPeriod line at the left
                if (echoCount > 40)
                    return new ElectronCyclotronEchoes(0, 0, 0);

                // if the first echo is in the lower half, then there will be only one harmonic line and the period
                // estimation could fail; instead, we just return the position of this echo as the period
                if (firstEcho > possibleEchoes.length / 2) {
                    return new ElectronCyclotronEchoes(0, firstEcho, firstEcho);
                }

                // if the only echo is in the upper half, it is probably an erroneous detection
                if (echoCount == 1)
                    return new ElectronCyclotronEchoes(0, 0, 0);

                // now we have at least two echoes and can perform period estimation

                final float[] peaks = new float[possibleEchoes.length];
                final float[] weights = new float[possibleEchoes.length];
                int end = 0;
                // we give the echoes equal weights because they are often really short and the weights may not be of
                // great value
                final float weight = 1 / (float) echoCount;
                for (int y = 0; y < possibleEchoes.length; y++) {
                    if (possibleEchoes[y]) {
                        peaks[y] = 1;
                        weights[y] = weight;
                        end = y;
                    }
                }

                final Tuple<Integer, Double[]> result = SummingDetector.ComputationStrategy.QUANTILE_PEAK_DISTANCE_ESTIMATION
                        .computePeriod(peaks, weights);
                if (result == null || result.getY().length == 0)
                    return new ElectronCyclotronEchoes(0, 0, 0);

                return new ElectronCyclotronEchoes(0, result.getY()[0], end);
            }

            private DetectedFeature detectGroundEcho(float[][] vertThinned)
            {
                final List<Point> points = traceEcho(vertThinned, vertThinned.length / 2, vertThinned.length
                        - vertThinned.length / 2, 0, 40, 80);
                if (points == null)
                    return null;

                return new GroundEcho(points.toArray(new Point[0]));
            }

            private DetectedFeature detectIonoEcho(float[][] vertThinned)
            {
                final List<Point> points = traceEcho(vertThinned, 0, vertThinned.length / 2, 20, 20, 40);
                if (points == null)
                    return null;

                return new IonosphericEcho(points.toArray(new Point[0]));
            }

            private List<Point> traceEcho(float[][] vertThinned, int xoffset, int xlength, int startY, int minRowCount,
                    int searchRectangleWidth)
            {
                final int[] rowCounts = new int[vertThinned[0].length];
                int maxCount = 0;
                int maxCountIndex = -1;
                for (int y = startY; y < rowCounts.length; y++) {
                    for (int x = xoffset; x < xoffset + xlength; x++) {
                        if (vertThinned[x][y] > 0) {
                            rowCounts[y]++;
                            if (rowCounts[y] > maxCount) {
                                maxCount = rowCounts[y];
                                maxCountIndex = y;
                            }
                        }
                    }
                }

                // if we got too few points in the best line, nothing is probably present
                if (maxCount < minRowCount)
                    return null;

                final List<Point> points = new LinkedList<>();
                int y = maxCountIndex;
                int firstX = -1;

                // first find the "center" and go to the left from it
                for (int x = xoffset + xlength - 1; x >= xoffset; x--) {
                    if (vertThinned[x][y] > 0) {
                        points.add(new Point(x, y));
                        if (firstX == -1)
                            firstX = x;
                        continue;
                    }

                    // until we get to the first point of echo, skip
                    if (firstX == -1)
                        continue;

                    final Tuple<Integer, Integer> next = findNextCoords(vertThinned, x, y, xoffset,
                            searchRectangleWidth, -1);

                    // 60 is a limit for the echo not to run up a false perpendicular line
                    if (next.getX() < 0 || next.getY() < 0 || abs(next.getY() - maxCountIndex) > 60)
                        break;

                    x = next.getX();
                    y = next.getY();
                }

                Collections.reverse(points);

                // then go from the "center" to the right
                for (int x = firstX + 1; x < xoffset + xlength; x++) {
                    if (vertThinned[x][y] > 0) {
                        points.add(new Point(x, y));
                        continue;
                    }

                    final Tuple<Integer, Integer> next = findNextCoords(vertThinned, x, y, xoffset,
                            searchRectangleWidth, 1);

                    if (next.getX() < 0 || next.getY() < 0 || abs(next.getY() - maxCountIndex) > 60)
                        break;

                    x = next.getX();
                    y = next.getY();
                }

                if (points.size() == 0)
                    return null;

                return points;
            }

            private Tuple<Integer, Integer> findNextCoords(float[][] vertThinned, int x, int y, int xoffset,
                    int searchRectangleWidth, int direction)
            {
                float max = 0;
                int nextY = -1, nextX = -1;
                // explore a searchRectangleWidth'x'20 box to the left of the last GE point to find the best line's
                // continuation
                for (int xx = x; (direction == -1 && xx >= max(x - searchRectangleWidth, xoffset))
                        || (direction == 1 && xx <= min(x + searchRectangleWidth, vertThinned.length - 1)); xx += direction) {
                    for (int yDiff = 0; yDiff <= 10; yDiff++) {
                        for (int sign = -1; sign <= 1; sign += 2) {
                            final int yy = y + sign * yDiff;
                            if (yy >= 0 && yy < vertThinned[0].length) {
                                if (vertThinned[xx][yy] > max) {
                                    max = vertThinned[xx][yy];
                                    nextX = xx;
                                    nextY = yy;
                                }
                            }
                        }
                    }
                }

                return new Tuple<>(nextX, nextY);
            }

            /**
             * Perform horizontal thinning.
             * 
             * @param data The data array. This method will change it!
             */
            private void thinHorizontal(float[][] data)
            {
                final int w = data.length, h = data[0].length;

                for (int x = 0; x < w; x++) {
                    yLoop: for (int y = 0; y < h; y++) {
                        if (data[x][y] < 1E-15)
                            data[x][y] = 0;

                        for (int i = -3; i <= 3; i++) {
                            if (x + i >= 0 && x + i < w && data[x + i][y] > data[x][y]) {
                                data[x][y] = 0;
                                continue yLoop;
                            }
                        }
                    }
                }

                for (int x = 0; x < w; x++) {
                    yLoop: for (int y = 0; y < h; y++) {
                        if (data[x][y] == 0)
                            continue;

                        for (int i = 1; i <= 15; i++) {
                            if (y + i < h && data[x][y + i] == 0) {
                                final int badI = i;
                                for (; i >= 0; i--) {
                                    data[x][y + i] = 0;
                                }

                                y = y + badI - 1;
                                continue yLoop;
                            }
                        }
                    }
                }

                // print(data);
            }

            /**
             * Perform vertical thinning.
             * 
             * @param data The data array. This method will change it!
             */
            private void thinVertical(float[][] data)
            {
                final int w = data.length, h = data[0].length;

                for (int y = 0; y < h; y++) {
                    xLoop: for (int x = 0; x < w; x++) {
                        if (data[x][y] < 1E-15)
                            data[x][y] = 0;

                        final int height = 10;
                        for (int i = -height; i <= height; i++) {
                            if (y + i >= 0 && y + i < h && data[x][y + i] > data[x][y]) {
                                data[x][y] = 0;
                                continue xLoop;
                            }
                        }
                    }
                }

                for (int y = 0; y < h; y++) {
                    xLoop: for (int x = 0; x < w; x++) {
                        if (data[x][y] == 0)
                            continue;

                        for (int i = 1; i <= 5; i++) {
                            if (x + i < w && data[x + i][y] == 0) {
                                final int badI = i;
                                for (; i >= 0; i--) {
                                    data[x + i][y] = 0;
                                }

                                x = x + badI - 1;
                                continue xLoop;
                            }
                        }
                    }
                }

                print(data);
            }

            private void print(float[][] arr)
            {
                final BufferedImage im = new BufferedImage(arr.length, arr[0].length, BufferedImage.TYPE_INT_RGB);
                final int col = Color.white.getRGB();
                try (final BufferedWriter w = new BufferedWriter(new FileWriter("thin"));) {
                    for (int y = arr[0].length - 1; y >= 0; y--) {
                        for (int x = 0; x < arr.length; x++) {
                            w.write(arr[x][y] + " ");
                            if (arr[x][y] > 1E-19) {
                                im.setRGB(x, y, col);
                            } else {
                                im.setRGB(x, y, 0);
                            }
                        }
                        w.newLine();
                    }
                } catch (IOException e) {

                }

                try {
                    ImageIO.write(im, "png", new File("thin.png"));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public float[][] prepareData(Float[][] data)
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

            @Override
            public boolean detectsGroundEcho()
            {
                return true;
            }

            @Override
            public String toString()
            {
                return "thinning";
            }

        };

        /**
         * Detect features in the data.
         * 
         * @param data The data to perform detection on. The algorithm can change the contents of the array.
         * @return The detected features.
         */
        abstract List<DetectedFeature> detect(float[][] data);

        /**
         * Process the input data and do whatever is needed to be able to work on them.
         * 
         * @param data The data.
         * @return The processed data.
         */
        public float[][] prepareData(Float[][] data)
        {
            final int w = data.length, h = data[0].length;
            final float[][] result = new float[w][h];
            final double coef = Math.log(10);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    result[x][y] = (float) (Math.log(data[x][y]) / coef);
                }
            }

            return result;
        }

        /**
         * @return True if GE is computed during the common computation.
         */
        public boolean detectsGroundEcho()
        {
            return false;
        }
    }

    @Override
    protected List<DetectedFeature> detectFeaturesImpl(Product<Float, ?, ?> product)
    {
        final float[][] data = strategy.prepareData(product.getData());

        return strategy.detect(data);
    }

    @Override
    protected DetectedFeature detectGroundEcho(Ionogram product, float altitude)
    {
        if (strategy.detectsGroundEcho())
            return null;
        return super.detectGroundEcho(product, altitude);
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
        System.out.println(new VectorizationDetector().detectFeatures(ionogram));
    }

}
