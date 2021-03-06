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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import cz.cuni.mff.peckam.ais.AISLBLProductReader;
import cz.cuni.mff.peckam.ais.EvenlySampledIonogram;
import cz.cuni.mff.peckam.ais.Ionogram;
import cz.cuni.mff.peckam.ais.result.ObjectFactory;
import cz.cuni.mff.peckam.ais.result.Orbit;
import cz.cuni.mff.peckam.ais.result.ResultWriter;

/**
 * Provides static method to perform detection to all frames in a .LBL file and save results to a XML file in the same
 * directory.
 * 
 * @author Martin Pecka
 */
public class DetectAndSave
{
    /** The ionogram reader to be used. */
    private static final AISLBLProductReader             reader    = new AISLBLProductReader();

    /** Object factory for {@link Orbit}. */
    private static final ObjectFactory                   factory   = new ObjectFactory();

    /**
     * Perform detection to all frames in <code>lblFile</code> using <code>detector</code> and save the results to a XML
     * file named TRACE_<code>resultSuffix</code>.XML in the same directory as <code>lblFile</code>.
     * 
     * @param lblFile The LBL file to parse.
     * @param detector The detector to use.
     * @param resultSuffix Suffix of the results file.
     * @param pm The progress monitor.
     * 
     * @return The results of the detection.
     * @throws IOException On IO error in either reading or writing.
     */
    public static List<DetectionResult> detectAndSave(File lblFile, FeatureDetector<Float> detector,
            String resultSuffix, final ProgressMonitor pm)
            throws IOException
    {
        if (pm != null)
            pm.setNote("Reading the .LBL file");
        final Ionogram[] ionograms = reader.readFile(lblFile);

        final double pmPieceSize = (pm != null) ? (pm.getMaximum() - pm.getMinimum()) / ionograms.length : 0;
        double finishedPiecesSize = 0;

        final Orbit orbit = factory.createOrbit();
        final int orbitNum = ionograms[0].getOrbitNumber();
        orbit.setId(orbitNum);

        final List<DetectionResult> results = new LinkedList<>();

        for (int i = 0; i < ionograms.length; i++) {
            if (pm != null) {
                final int frame = i;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        pm.setNote("Performing detection in orbit " + orbitNum + " frame " + frame + " of "
                                + ionograms.length);
                    }
                });
            }
            ionograms[i] = new EvenlySampledIonogram(ionograms[i]);
            final DetectionResult result = detector.detectFeatures(ionograms[i]);
            results.add(result);
            orbit.getFrames().add(DetectionResultConverter.convert(result, ionograms[i]));

            ionograms[i] = null;
            System.gc();

            if (pm != null) {
                final int progress = (int) (finishedPiecesSize += pmPieceSize);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        pm.setProgress(progress);
                    }
                });
                if (pm.isCanceled())
                    return null;
            }
        }


        final String outFileName = String.format(Locale.ENGLISH, "TRACE_%04d_%s.XML", orbitNum, resultSuffix);
        final File outFile = new File(lblFile.getParent(), outFileName);
        final ResultWriter writer = new ResultWriter(orbit);

        try (OutputStream output = new FileOutputStream(outFile)) {
            writer.writeXML(output);
        }

        if (pm != null)
            pm.setProgress(pm.getMaximum());
        return results;
    }
}
