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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import cz.cuni.mff.peckam.ais.AISLBLProductReader;
import cz.cuni.mff.peckam.ais.EvenlySampledIonogram;
import cz.cuni.mff.peckam.ais.Ionogram;
import cz.cuni.mff.peckam.ais.Product;
import cz.cuni.mff.peckam.ais.detection.DetectionResult;
import cz.cuni.mff.peckam.ais.detection.DetectionResultConverter;
import cz.cuni.mff.peckam.ais.detection.FeatureDetector;
import cz.cuni.mff.peckam.ais.result.FrameType;

/**
 * Manually acquired referential data presentation.
 * 
 * @author Martin Pecka
 */
public class ReferenceDataPresentation extends DetectorPresentation<ReferenceDataPresentation.ReferenceDetector>
{

    /**  */
    private static final long serialVersionUID = 8567678517660575613L;

    /** The ionogram reader to be used. */
    private final AISLBLProductReader reader           = new AISLBLProductReader();

    @Override
    public void updateComponentStates()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getTabTitle()
    {
        return "Manually acquired";
    }

    @Override
    protected ReferenceDetector createDetector()
    {
        return new ReferenceDetector();
    }

    @Override
    protected String getResultFileSuffix()
    {
        return null;
    }

    @Override
    protected List<DetectionResult> detectFeatures(File orbitFile) throws IOException
    {
        final Ionogram[] ionograms = reader.readFile(orbitFile);
        final List<DetectionResult> results = new LinkedList<>();
        final ProgressMonitor pm = new ProgressMonitor(this, "Detecting...", "Resampling the ionograms", 0,
                ionograms.length - 1);
        for (int i = 0; i < ionograms.length; i++) {
            {
                final Ionogram iono = new EvenlySampledIonogram(ionograms[i]);
                final DetectionResult result = getDetector().detectFeatures(iono);
                results.add(result);
                result.readProductData(iono);
            }
            ionograms[i] = null;
            System.gc();

            if (pm.isCanceled())
                return null;

            final int progress = i;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run()
                {
                    pm.setProgress(progress);
                }
            });
        }

        return results;
    }

    /**
     * A dummy detector for the presentation.
     * 
     * @author Martin Pecka
     */
    static class ReferenceDetector implements FeatureDetector<Float>
    {

        @Override
        public DetectionResult detectFeatures(Product<Float, ?, ?> product)
        {
            final Ionogram ionogram = (Ionogram) product;
            final FrameType referenceResult = ionogram.getReferenceDetectionResult();
            return DetectionResultConverter.convert(referenceResult, ionogram);
        }

        @Override
        public List<DetectionResult> detectFeatures(List<? extends Product<Float, ?, ?>> products)
        {
            final List<DetectionResult> result = new LinkedList<>();

            for (Product<Float, ?, ?> product : products)
                result.add(detectFeatures(product));

            return result;
        }

    }

}
