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

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import cz.cuni.mff.peckam.ais.detection.DetectAndSave;
import cz.cuni.mff.peckam.ais.detection.DetectionResult;
import cz.cuni.mff.peckam.ais.detection.FeatureDetector;

/**
 * The base for all feature detector presentation panels.
 * 
 * @author Martin Pecka
 * 
 * @param <DetectorType> Type of the presented detector.
 */
public abstract class DetectorPresentation<DetectorType extends FeatureDetector<Float>> extends JPanel
{

    /**  */
    private static final long serialVersionUID = 1824227449311749226L;

    /** The detector to use. */
    private final DetectorType detector;

    /** The results of the detection. */
    private List<DetectionResult> results          = null;

    /** Infolabel with some metadata about the result. */
    private JLabel                headerLbl;

    /** Textarea displaying the results. */
    private JTextArea             resultsArea;

    /**
     * Create the panel.
     */
    public DetectorPresentation()
    {
        setLayout(new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("pref:grow"),
                FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"),
                FormFactory.RELATED_GAP_ROWSPEC, }));

        headerLbl = new JLabel("");
        add(headerLbl, "2, 2");

        JScrollPane scrollPane = new JScrollPane();
        add(scrollPane, "2, 4, fill, fill");

        resultsArea = new JTextArea();
        scrollPane.setViewportView(resultsArea);
        resultsArea.setWrapStyleWord(true);
        resultsArea.setLineWrap(true);
        resultsArea.setEditable(false);
        detector = createDetector();
    }

    /**
     * Update the visual states of all components based on the state of other components.
     */
    public abstract void updateComponentStates();

    /**
     * @return Title of the tab.
     */
    public abstract String getTabTitle();

    /**
     * @return The presented detector.
     */
    public DetectorType getDetector()
    {
        return detector;
    }

    /**
     * @return The detector instance to present.
     */
    protected abstract DetectorType createDetector();

    /**
     * Detect features in all the given orbit files.
     * 
     * @param orbitFiles The orbit files to read.
     * 
     * @throws IOException When IO error occurs when reading/writing.
     */
    public void detectFeatures(final Collection<File> orbitFiles) throws IOException
    {
        // TODO
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                resultsArea.setText("Detecting...");
            }
        });
        results = new LinkedList<>();
        for (File file : orbitFiles) {
            try {
                final List<DetectionResult> results = detectFeatures(file);
                if (results != null)
                    this.results.addAll(results);
                else
                    break; // the operation was probably cancelled
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                if (!results.isEmpty()) {
                    final Dimension productSize = results.get(0).getSourceProductSize();
                    headerLbl.setText(String.format("Processed %d orbit file(s) starting with %s, sized %dx%d",
                        orbitFiles.size(), orbitFiles.iterator().next().getName(), productSize.width,
                        productSize.height));
                    final StringBuilder builder = new StringBuilder();
                    for (DetectionResult result : results) {
                        builder.append(result).append("\n");
                    }
                    resultsArea.setText(builder.toString());
                } else {
                    headerLbl.setText("");
                    resultsArea.setText("Detection failed");
                }
            }
        });
    }

    /**
     * Detect results in the given orbit file.
     * 
     * @param orbitFile The file with orbits to detect in.
     * @return The detected features.
     * 
     * @throws IOException On IO error.
     */
    protected List<DetectionResult> detectFeatures(File orbitFile) throws IOException
    {
        final ProgressMonitor pm = new ProgressMonitor(this, "Detecting...", "", 0, 1000);
        try {
            return DetectAndSave.detectAndSave(orbitFile, detector, getResultFileSuffix(), pm);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            pm.setProgress(pm.getMaximum());
        }
    }

    /**
     * @return Suffix of the TRACE result file.
     */
    protected abstract String getResultFileSuffix();

    /**
     * Reset this presentation to its default state without any assigned products or results.
     */
    public void reset()
    {
        results = null;
    }
}
