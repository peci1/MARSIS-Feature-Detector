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

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;

import cz.cuni.mff.peckam.ais.Ionogram;
import cz.cuni.mff.peckam.ais.result.FrameType;
import cz.cuni.mff.peckam.ais.result.ObjectFactory;
import cz.cuni.mff.peckam.ais.result.PointType;
import cz.cuni.mff.peckam.ais.result.TraceType;

/**
 * Convert {@link DetectionResult} to {@link FrameType} and vice versa.
 * 
 * @author Martin Pecka
 */
public class DetectionResultConverter
{
    /** Object factory for the {@link FrameType}. */
    private static final ObjectFactory factory = new ObjectFactory();

    /**
     * Convert the given detection result (performed on the given product) to a FrameType.
     * 
     * @param result The result to convert.
     * @param ionogram The ionogram the detection was performed upon.
     * 
     * @return The corresponding FrameType.
     */
    public static FrameType convert(DetectionResult result, Ionogram ionogram)
    {
        final FrameType frame = factory.createFrameType();
        frame.setTime(ionogram.getStartTime());

        if (result == null)
            return frame;

        final double horizScale = (ionogram.getMaxColumnValue() - ionogram.getMinColumnValue()) / (ionogram.getWidth());
        final double vertScale = (ionogram.getMaxRowValue() - ionogram.getMinRowValue()) / (ionogram.getHeight());


        final ElectronPlasmaOscillation hPeriod = (ElectronPlasmaOscillation) result
                .getFeature(ElectronPlasmaOscillation.ID);
        if (hPeriod != null)
            frame.setHperiod((float) (hPeriod.getPeriod() * horizScale));

        final ElectronCyclotronEchoes vPeriod = (ElectronCyclotronEchoes) result.getFeature(ElectronCyclotronEchoes.ID);
        if (vPeriod != null)
            frame.setVperiod((float) (vPeriod.getPeriod() * vertScale));

        final IonosphericEcho iono = (IonosphericEcho) result.getFeature(IonosphericEcho.ID);
        if (iono != null)
            frame.setIonospheretrace(getTrace(iono.getPoints(), ionogram.getMinColumnValue(), horizScale,
                    ionogram.getMinRowValue(), vertScale));

        final GroundEcho ground = (GroundEcho) result.getFeature(GroundEcho.ID);
        if (ground != null)
            frame.setGroundtrace(getTrace(ground.getPoints(), ionogram.getMinColumnValue(), horizScale,
                    ionogram.getMinRowValue(), vertScale));

        return frame;
    }

    /**
     * Convert the given result frame to a detection result using the metadata of the given ionogram.
     * 
     * @param frame The frame to convert.
     * @param ionogram The ionogram supplying metadata.
     * @return The detection result.
     */
    public static DetectionResult convert(FrameType frame, Ionogram ionogram)
    {
        if (frame == null)
            return null;

        final DetectionResult result = new DetectionResult(ionogram.getId(), new Dimension(ionogram.getWidth(),
                ionogram.getHeight()));

        final double horizScale = ionogram.getWidth() / (ionogram.getMaxColumnValue() - ionogram.getMinColumnValue());
        final double vertScale = ionogram.getHeight() / (ionogram.getMaxRowValue() - ionogram.getMinRowValue());
        final double minX = ionogram.getMinColumnValue();
        final double minY = ionogram.getMinRowValue();

        if (frame.getHperiod() != null && frame.getHperiod() > 0.00001f) {
            final float period = frame.getHperiod();
            final float minShownFreq = (float) (Math.floor(ionogram.getMinColumnValue() / period) * period);
            // the offset corresponds to the first harmonics visible in the sounding frequency spectrum; the repetition
            // starts at 0, which is, however, not covered in the range, and the continues to repeat according to the
            // period
            final int offset = (int) ((minShownFreq - minX) * horizScale);
            result.addFeature(new ElectronPlasmaOscillation(offset, period * horizScale, 8));
        }

        if (frame.getVperiod() != null && frame.getVperiod() > 0.00001f) {
            final float period = frame.getVperiod();
            // the cyclotron echoes virtually start at time delay 0ms which is, however, not in the range; so the first
            // visible echo corresponds to the value of period
            final int offset = (int) ((period - minY) * vertScale);
            result.addFeature(new ElectronCyclotronEchoes(offset, period * vertScale, 8));
        }

        if (frame.getIonospheretrace() != null && !frame.getIonospheretrace().getPoints().isEmpty()) {
            final List<PointType> points = frame.getIonospheretrace().getPoints();
            final Point[] resultPoints = framePointsToResultPoints(points, minX, horizScale, minY, vertScale);
            result.addFeature(new IonosphericEcho(resultPoints));
        }

        if (frame.getGroundtrace() != null && !frame.getGroundtrace().getPoints().isEmpty()) {
            final List<PointType> points = frame.getGroundtrace().getPoints();
            final Point[] resultPoints = framePointsToResultPoints(points, minX, horizScale, minY, vertScale);
            result.addFeature(new GroundEcho(resultPoints));
        }

        return result;
    }

    /**
     * Convert the given set of points in image coordinates to a trace in frequency/time delay coordinates.
     * 
     * @param points The points to convert.
     * @param minX Minimal x value.
     * @param xScale X scale.
     * @param minY Minimal y value.
     * @param yScale Y scale.
     * 
     * @return The trace.
     */
    private static TraceType getTrace(Point[] points, double minX, double xScale, double minY, double yScale)
    {
        final TraceType trace = factory.createTraceType();
        for (Point point : points) {
            final PointType pointType = factory.createPointType();
            trace.getPoints().add(pointType);
            pointType.setX((float) (minX + point.x * xScale));
            pointType.setY((float) (minY + point.y * yScale));
        }
        return trace;
    }

    /**
     * Convert the given set of points in frequency/time delay coordinates to image coordinates.
     * 
     * @param points The points to convert.
     * @param minX Minimal x value.
     * @param xScale X scale.
     * @param minY Minimal y value.
     * @param yScale Y scale.
     * 
     * @return The converted points.
     */
    private static Point[] framePointsToResultPoints(List<PointType> points, double minX, double xScale, double minY,
            double yScale)
    {
        final Point[] result = new Point[points.size()];
        int i = 0;
        for (PointType point : points) {
            final int x = (int) ((point.getX() - minX) * xScale);
            final int y = (int) ((point.getY() - minY) * yScale);
            result[i] = new Point(x, y);
            i++;
        }
        return result;
    }
}
