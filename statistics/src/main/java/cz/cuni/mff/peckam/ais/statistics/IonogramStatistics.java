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
package cz.cuni.mff.peckam.ais.statistics;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math3.util.FastMath;

import cz.cuni.mff.peckam.ais.AISLBLProductReader;
import cz.cuni.mff.peckam.ais.Ionogram;
import cz.cuni.mff.peckam.ais.result.FrameType;
import cz.cuni.mff.peckam.ais.result.PointType;

/**
 * Various ionogram statistics.
 * 
 * @author Martin Pecka
 */
public class IonogramStatistics
{

    /**  */
    private final BufferedWriter meanWriter;
    /**  */
    private final BufferedWriter sdWriter;
    /**  */
    private final BufferedWriter maxWriter;
    /**  */
    private final BufferedWriter featuresWriter;
    /**  */
    private final BufferedWriter      traceWriter;

    /**  */
    private final AISLBLProductReader reader = new AISLBLProductReader();

    /**
     * @param meanWriter mean
     * @param sdWriter std. dev
     * @param maxWriter max values
     * @param featuresWriter features
     * @param traceWriter trace points values
     */
    public IonogramStatistics(BufferedWriter meanWriter, BufferedWriter sdWriter, BufferedWriter maxWriter,
            BufferedWriter featuresWriter, BufferedWriter traceWriter)
    {
        this.meanWriter = meanWriter;
        this.sdWriter = sdWriter;
        this.maxWriter = maxWriter;
        this.featuresWriter = featuresWriter;
        this.traceWriter = traceWriter;
    }

    /**
     * Compute the stats.
     * 
     * @param lblFile The .LBL file with orbit data.
     * @throws IOException On IO exception.
     */
    public void execute(File lblFile) throws IOException
    {
        final Ionogram[] ionograms = reader.readFile(lblFile);
        int i = -1;
        for (Ionogram iono : ionograms) {
            i++;
            System.out.println("Orbit " + iono.getOrbitNumber() + " frame " + i);
            final Float[][] data = iono.getData();
            final int numItems = iono.getWidth() * iono.getHeight();
            float sum = 0, max = 0;
            for (int x = 0; x < data.length; x++) {
                for (int y = 0; y < data[0].length; y++) {
                    sum += data[x][y];
                    if (data[x][y] > max)
                        max = data[x][y];
                }
            }
            final float mean = sum / numItems;

            float sdSum = 0;
            for (int x = 0; x < data.length; x++) {
                for (int y = 0; y < data[0].length; y++) {
                    sdSum += FastMath.pow(data[x][y] - mean, 2);
                    if (data[x][y] > max)
                        max = data[x][y];
                }
            }
            final float sd = (float) FastMath.sqrt(sdSum / numItems);

            if (iono.getReferenceDetectionResult() == null) {
                System.err.println("No result data for orbit " + iono.getOrbitNumber() + " frame " + i);
                continue;
            }

            final FrameType result = iono.getReferenceDetectionResult();
            
            if (result.getHperiod() == null && result.getVperiod() == null && result.getIonospheretrace() == null
                    && result.getIonospheretrace() == null)
                continue;

            final Float hp = result.getHperiod() != null && result.getHperiod() > 0 ? result.getHperiod() : null;
            final Float vp = result.getVperiod() != null && result.getVperiod() > 0 ? result.getVperiod() : null;
            final List<PointType> gp = result.getGroundtrace() != null
                    && result.getGroundtrace().getPoints().size() > 0 ? result.getGroundtrace().getPoints() : null;
            final List<PointType> ip = result.getIonospheretrace() != null && result.getIonospheretrace().getPoints().size() > 0 ? result.getIonospheretrace().getPoints() : null;
            
            final boolean hasFeatures = hp != null || vp != null || gp != null || ip != null;

            meanWriter.write(format(mean) + " " + (hasFeatures ? 1 : 0));
            meanWriter.newLine();

            sdWriter.write(format(sd) + " " + (hasFeatures ? 1 : 0));
            sdWriter.newLine();

            maxWriter.write(format(max) + " " + (hasFeatures ? 1 : 0));
            maxWriter.newLine();
            
            if (ip != null)
                writeTracePoints(result.getIonospheretrace().getPoints(), iono, "iono");
            if (gp != null)
                writeTracePoints(result.getGroundtrace().getPoints(), iono, "ground");
            
            final int w = iono.getWidth(), h = iono.getHeight();
            for (int x = 0; x < data.length; x++) {
                for (int y = 0; y < data[0].length; y++) {
                    final float val = data[x][y];
                    if (val > 1E-12) {
                        final PointType pos = iono.getFreqTimePosition(x,y);
                        final float posX = pos.getX();
                        final float posY = pos.getY();

                        if (hp != null && y < h / 2 && FastMath.abs(Math.round(posX / hp) * hp - posX) < 0.03) {
                            featuresWriter.write(iono.getOrbitNumber() + ":" + i + " " + format(val) + "\thPeriod");
                            featuresWriter.newLine();
                        } else if (vp != null && x < w / 2 && FastMath.abs(Math.round(posY / vp) * vp - posY) < 0.1) {
                            featuresWriter.write(iono.getOrbitNumber() + ":" + i + " " + format(val) + "\tvPeriod");
                            featuresWriter.newLine();
                        } else if (gp != null && findInTrace(pos, gp)) {
                            featuresWriter.write(iono.getOrbitNumber() + ":" + i + " " + format(val) + "\tgroundtrace");
                            featuresWriter.newLine();
                        } else if (ip != null && findInTrace(pos, ip)) {
                            featuresWriter.write(iono.getOrbitNumber() + ":" + i + " " + format(val)
                                    + "\tionospheretrace");
                            featuresWriter.newLine();
                        } else {
                            featuresWriter.write(iono.getOrbitNumber() + ":" + i + " " + format(val) + "\tnoFeature");
                            featuresWriter.newLine();
                        }
                    }
                }
            }
        }
    }

    /**
     * Return true if the given point is near a point in the given trace.
     * 
     * @param pos The point.
     * @param trace The trace.
     * @return Return true if the given point is near a point in the given trace.
     */
    private boolean findInTrace(PointType pos, List<PointType> trace)
    {
        for (PointType point : trace) {
            if (FastMath.abs(pos.getX() - point.getX()) < 0.001 && FastMath.abs(pos.getY() - point.getY()) < 0.01)
                return true;
        }
        return false;
    }

    /**
     * Write trace point values.
     * 
     * @param points The points
     * @param iono The ionogram.
     * @param tag Tag to add to the resulting file.
     * @throws IOException On IO error.
     */
    private void writeTracePoints(List<PointType> points, Ionogram iono, String tag) throws IOException
    {
        final Float[][] data = iono.getData();
        for (PointType p : points) {
            final Point coords = iono.getDataPosition(p.getY(), p.getX());
            float val = 0;
            for (int i = 0; i <= 6 && coords.y + i < data[0].length; i++) {
                if (data[coords.x][coords.y + i] > val)
                    val = data[coords.x][coords.y + i];
            }

            if (val > 0) {
                traceWriter.write(iono.getOrbitNumber() + ":" + iono.getPositionInSeries() + " " + format(val) + "\t"
                        + tag);
                traceWriter.newLine();
            }
        }
    }

    /**
     * Convert the float to string.
     * 
     * @param f The float.
     * @return The string.
     */
    private static String format(float f)
    {
        return String.format(Locale.ENGLISH, "%.25f", f);
    }

    /**
     * @param args 0 => Orbit base dir, 1 => min orbit, 2 => max orbit
     * @throws IOException On IO error.
     */
    public static void main(String[] args) throws IOException
    {
        final File orbitDir = new File(args[0]);
        final int minOrbit = Integer.parseInt(args[1]);
        final int maxOrbit = Integer.parseInt(args[2]);

        try (final BufferedWriter meanWriter = new BufferedWriter(new FileWriter("mean"));
                final BufferedWriter sdWriter = new BufferedWriter(new FileWriter("sd"));
                final BufferedWriter maxWriter = new BufferedWriter(new FileWriter("max"));
                final BufferedWriter featuresWriter = new BufferedWriter(new FileWriter("features"));
                final BufferedWriter traceWriter = new BufferedWriter(new FileWriter("trace"))) {
            final IonogramStatistics stats = new IonogramStatistics(meanWriter, sdWriter, maxWriter, featuresWriter,
                    traceWriter);

            for (int i = minOrbit; i <= maxOrbit; i++) {
                final File orbitFile = new File(orbitDir, (i + "").replaceAll(".$", "X") + File.separator + "FRM_AIS_RDR_"
                        + i + ".LBL");
                if (orbitFile.exists()) {
                    stats.execute(orbitFile);
                }
            }
        }
    }

}
