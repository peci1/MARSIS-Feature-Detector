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

import java.awt.Point;

import cz.cuni.mff.peckam.ais.Ionogram;
import cz.cuni.mff.peckam.ais.result.FrameType;
import cz.cuni.mff.peckam.ais.result.ObjectFactory;
import cz.cuni.mff.peckam.ais.result.PointType;
import cz.cuni.mff.peckam.ais.result.TraceType;

/**
 * Convert {@link DetectionResult} to {@link FrameType}.
 * 
 * @author Martin Pecka
 */
public class DetectionResultToFrameConverter
{
    /** Object factory for the {@link FrameType}. */
    private final ObjectFactory factory = new ObjectFactory();

    /**
     * Convert the given detection result (performed on the given product) to a FrameType.
     * 
     * @param result The result to convert.
     * @param ionogram The ionogram the detection was performed upon.
     * 
     * @return The corresponding FrameType.
     */
    public FrameType convert(DetectionResult result, Ionogram ionogram)
    {
        final FrameType frame = factory.createFrameType();
        frame.setTime(ionogram.getStartTime());

        final float horizScale = (ionogram.getMaxColumnValue() - ionogram.getMinColumnValue()) / (ionogram.getWidth());
        final float vertScale = (ionogram.getMaxRowValue() - ionogram.getMinRowValue()) / (ionogram.getHeight());

        for (DetectedFeature feature : result.getFeatures()) {
            switch (feature.getTypes().get(0)) {
                case DetectedFeature.TYPE_REPEATING_HORIZONTAL_LINE:
                    frame.setHperiod((float) ((RepeatingLine) feature).getPeriod() * horizScale);
                    break;
                case DetectedFeature.TYPE_REPEATING_VERTICAL_LINE:
                    frame.setVperiod((float) ((RepeatingLine) feature).getPeriod() * vertScale);
                    break;
                case DetectedFeature.TYPE_GENERAL_CURVE: // TODO differentiate iono and ground
                    final Point[] points = ((GeneralCurve) feature).getPoints();

                    final TraceType trace = factory.createTraceType();
                    frame.setIonospheretrace(trace);

                    for (Point point : points) {
                        final PointType pointType = factory.createPointType();
                        trace.getPoints().add(pointType);
                        pointType.setX(ionogram.getMinColumnValue() + point.x * horizScale);
                        pointType.setY(ionogram.getMinRowValue() + point.y * vertScale);
                    }
                    break;
            }
        }

        return frame;
    }
}
