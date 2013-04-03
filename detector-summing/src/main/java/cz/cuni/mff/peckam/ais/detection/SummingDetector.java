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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
public class SummingDetector extends FeatureDetectorBase<Float>
{

    @Override
    protected List<DetectedFeature> detectFeaturesImpl(Product<Float, ?, ?> product)
    {
        final List<DetectedFeature> result = new LinkedList<>();

        {
            final Tuple<Integer, Double> horizRepeat = detectRepetition(getColumnSums(product));
            if (horizRepeat != null) {
                result.add(new ElectronPlasmaOscillation(horizRepeat.getX(), horizRepeat.getY(), 8));
            }
        }

        {
            final Tuple<Integer, Double> vertRepeat = detectRepetition(getRowSums(product));
            if (vertRepeat != null) {
                result.add(new ElectronCyclotronEchoes(vertRepeat.getX(), vertRepeat.getY(), 8));
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
        return null; // TODO
    }

    /**
     * Return the row sums of the given product.
     * 
     * @param product The product to sum up.
     * @return The row sums
     */
    private float[] getRowSums(Product<Float, ?, ?> product)
    {
        final int w = product.getWidth(), h = product.getHeight();

        final float[] result = new float[h];
        final Float[][] data = product.getData();

        for (int y = 0; y < h; y++) {
            float sum = 0;
            for (int x = 0; x < w; x++) {
                sum += data[x][y];
            }
            result[y] = sum;
        }

        System.err.println(Arrays.toString(result));
        return result;
    }

    /**
     * Return the column sums of the given product.
     * 
     * @param product The product to sum up.
     * @return The column sums
     */
    private float[] getColumnSums(Product<Float, ?, ?> product)
    {
        final int w = product.getWidth(), h = product.getHeight();

        final float[] result = new float[w];
        final Float[][] data = product.getData();

        for (int x = 0; x < w; x++) {
            float sum = 0;
            for (int y = 0; y < h; y++) {
                sum += data[x][y];
            }
            result[x] = sum;
        }

        System.err.println(Arrays.toString(result));
        return result;
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
}
