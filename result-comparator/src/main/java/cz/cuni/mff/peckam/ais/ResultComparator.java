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
package cz.cuni.mff.peckam.ais;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import cz.cuni.mff.peckam.ais.result.FrameType;
import cz.cuni.mff.peckam.ais.result.Orbit;
import cz.cuni.mff.peckam.ais.result.ResultReader;

/**
 * Compares results of detection.
 * 
 * @author Martin Pecka
 */
public class ResultComparator
{

    /**
     * @param args 0=> base dir, 1 => suffix1, 2 => suffix2, 3 => startOrbit, 4 => endOrbit
     * @throws IOException io
     */
    public static void main(String[] args) throws IOException
    {
        final File baseDir = new File(args[0]);
        final String suffix1 = args[1];
        final String suffix2 = args[2];
        final int start = Integer.parseInt(args[3]);
        final int end = Integer.parseInt(args[4]);

        try (final BufferedWriter hpW = new BufferedWriter(new FileWriter("hPeriod"));
                final BufferedWriter hpmW = new BufferedWriter(new FileWriter("hPeriodMultiples"));
                final BufferedWriter vpW = new BufferedWriter(new FileWriter("vPeriod"));
                final BufferedWriter vpmW = new BufferedWriter(new FileWriter("vPeriodMultiples"));
                final BufferedWriter ioW = new BufferedWriter(new FileWriter("iono"));
                final BufferedWriter grW = new BufferedWriter(new FileWriter("ground"));) {

            final ResultReader reader = new ResultReader();
            for (int orbit = start; orbit <= end; orbit++) {
                final String tenDir = ((int) Math.floor(orbit / 10.0)) + "X";
                final File resFile1 = new File(baseDir, tenDir + File.separator + "TRACE_" + orbit + suffix1 + ".XML");
                final File resFile2 = new File(baseDir, tenDir + File.separator + "TRACE_" + orbit + suffix2 + ".XML");

                if (!resFile1.exists() || !resFile2.exists())
                    continue;

                final Orbit res1 = reader.readResult(resFile1);
                final Orbit res2 = reader.readResult(resFile2);

                final Iterator<FrameType> it1 = res1.getFrames().iterator();
                final Iterator<FrameType> it2 = res2.getFrames().iterator();

                while (it1.hasNext() && it2.hasNext()) {
                    final FrameType fr1 = it1.next();
                    final FrameType fr2 = it2.next();

                    final float hp1 = fr1.getHperiod() != null ? fr1.getHperiod() : 0;
                    final float hp2 = fr2.getHperiod() != null ? fr2.getHperiod() : 0;
                    if (hp1 > 0 && hp2 > 0) {
                        hpW.write(String.format(Locale.ENGLISH, "%f %f", hp1 - hp2, hp1));
                        hpW.newLine();
                        float h1 = hp1, h2 = hp2;
                        if (h1 < h2) {
                            h1 = hp2;
                            h2 = hp1;
                        }
                        final int multiplier = (int) (h1 / h2);
                        hpmW.write(String.format(Locale.ENGLISH, "%f %f", h1 - multiplier * h2, h1));
                        hpmW.newLine();
                    }

                    final float vp1 = fr1.getVperiod() != null ? fr1.getVperiod() : 0;
                    final float vp2 = fr2.getVperiod() != null ? fr2.getVperiod() : 0;
                    if (vp1 > 0 && vp2 > 0) {
                        vpW.write(String.format(Locale.ENGLISH, "%f %f", vp1 - vp2, vp1));
                        vpW.newLine();
                        float v1 = vp1, v2 = vp2;
                        if (v1 < v2) {
                            v1 = vp2;
                            v2 = vp1;
                        }
                        final int multiplier = (int) (v1 / v2);
                        vpmW.write(String.format(Locale.ENGLISH, "%f %f", v1 - multiplier * v2, v1));
                        vpmW.newLine();
                    }
                }

                System.out.println(orbit);
            }
        }

        System.out.println("Done.");
    }

}
