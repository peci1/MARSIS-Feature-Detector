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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import cz.cuni.mff.peckam.ais.result.FrameType;
import cz.cuni.mff.peckam.ais.result.Orbit;
import cz.cuni.mff.peckam.ais.result.ResultReader;

/**
 * Reader for .LBL files associated with AIS products.
 * 
 * @author Martin Pecka
 */
public class AISLBLProductReader
{
    /** Number of columns each ionogram contains. */
    private static final int                     NUM_COLUMNS            = Ionogram.NUM_FREQUENCY_BINS;

    /** Overlay type defining manually obtained data. */
    public static final ProductOverlayType       OVERLAY_TYPE_MANUAL    = new ProductOverlayType.Manual();

    /** Overlay type defining automatically obtained data. */
    public static final String OVERLAY_TYPE_AUTOMATIC = "automatic";

    /** Cache for loaded ionograms. */
    private static final Cache<File, Ionogram[]> ionogramCache          = CacheBuilder.newBuilder().softValues()
                                                                                .build();

    /**
     * Read {@link Ionogram}s from the given file.
     * 
     * @param lblFile The file to read from.
     * @return The ionograms from the given file.
     * 
     * @throws IOException On file read error.
     */
    public Ionogram[] readFile(File lblFile) throws IOException
    {
        {
            final Ionogram[] cachedResult = ionogramCache.getIfPresent(lblFile);
            if (cachedResult != null)
                return cachedResult;
        }

        final Map<String, String> entries = new HashMap<>();

        try (final BufferedReader reader = new BufferedReader(new FileReader(lblFile))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("") || line.trim().equals("END"))
                    continue;

                final int equalsIndex = line.indexOf("=");
                if (equalsIndex == -1)
                    throw new IllegalStateException("Invalid format of the LBL file. No '=' found when expected.");

                final String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1, line.length()).trim();
                if (value.startsWith("\"")) {
                    value = value.substring(1);
                    if (value.endsWith("\"")) {
                        value = value.substring(0, value.length() - 1);
                    } else {
                        while ((line = reader.readLine()) != null) {
                            value = value + line.trim();
                            if (line.trim().endsWith("\"")) {
                                value = value.substring(0, value.length() - 1);
                                break;
                            }
                        }
                        if (line == null)
                            throw new IllegalStateException("Premature end of the LBL file.");
                    }
                }

                entries.put(key, value);
            }
        }

        final int file_records = Integer.parseInt(entries.get("FILE_RECORDS"));
        final String ais_table = entries.get("^AIS_TABLE");
        final int orbit_number = Integer.parseInt(entries.get("ORBIT_NUMBER"));

        final Ionogram[] result = new Ionogram[file_records / NUM_COLUMNS];
        final AISProductReader aisReader = new AISProductReader();
        final AISProduct[] products = aisReader.readFile(new File(lblFile.getParent(), ais_table));

        if (products.length != file_records)
            throw new IllegalStateException("FILE_RECORDS from .LBL doesn't match number of records in ^AIS_TABLE");

        final File altitudeFile = new File(lblFile.getParent(), "EPHEMERIS_" + orbit_number + ".TXT");
        BufferedReader altitudeReader = null;
        if (altitudeFile.exists())
            altitudeReader = new BufferedReader(new FileReader(altitudeFile));

        for (int i = 0; i < result.length; i++) {
            AISProduct[] columns = new AISProduct[NUM_COLUMNS];
            for (int j = 0; j < NUM_COLUMNS; j++) {
                columns[j] = products[i * NUM_COLUMNS + j];
            }

            Float altitude = null;
            if (altitudeReader != null) {
            	String altitudeString = altitudeReader.readLine();
            	if (altitudeString != null) {
            		altitude = Float.parseFloat(altitudeString);
            	}
            }
            result[i] = new Ionogram(columns, orbit_number, i, altitude);
        }

        if (altitudeReader != null)
            altitudeReader.close();

        { // add AIS detection result overlay
            final File resultsFile = new File(lblFile.getParent(), "TRACE_" + orbit_number + ".XML");
            if (resultsFile.exists()) {
                final Orbit results = new ResultReader().readResult(resultsFile);
                final Map<DateTime, FrameType> framesByTime = new HashMap<>();
                for (FrameType frame : results.getFrames()) {
                    framesByTime.put(frame.getTime(), frame);
                }

                for (Ionogram ionogram : result) {
                    final FrameType frame = framesByTime.get(ionogram.getStartTime());
                    if (frame != null) {
                        ionogram.addOverlay(new AISResultOverlay(ionogram, frame, OVERLAY_TYPE_MANUAL));
                        ionogram.setReferenceDetectionResult(frame);
                    }
                }
            }
        }

        // TODO resolve memory problems
        // ionogramCache.put(lblFile, result);

        return result;
    }

    /**
     * @param args Arg 1: filename to parse.
     */
    public static void main(String[] args)
    {
        if (args.length != 1) {
            System.out.println("Provide exactly one argument - the filename to be parsed as AIS LBL file.");
            System.exit(-1);
        }

        final AISLBLProductReader reader = new AISLBLProductReader();
        try {
            final Ionogram[] result = reader.readFile(new File(args[0]));
            System.out.println(result.length + " ionograms read from the file: ");
            for (Ionogram set : result)
                System.out.println(set);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
