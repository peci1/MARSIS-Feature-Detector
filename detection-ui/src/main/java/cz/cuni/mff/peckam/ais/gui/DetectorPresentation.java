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
import java.util.Collection;

import javax.swing.JPanel;

import cz.cuni.mff.peckam.ais.detection.DetectAndSave;
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

    /**
     * Create the panel.
     */
    public DetectorPresentation()
    {
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
    public void detectFeatures(Collection<File> orbitFiles) throws IOException
    {
        // TODO
        final String suffix = getResultFileSuffix();
        for (File file : orbitFiles) {
            DetectAndSave.detectAndSave(file, detector, suffix);
        }
    }

    /**
     * @return Suffix of the TRACE result file.
     */
    protected abstract String getResultFileSuffix();
}
