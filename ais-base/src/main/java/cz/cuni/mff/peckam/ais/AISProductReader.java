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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.joda.time.DateTime;

/**
 * Reader for AIS products.
 * 
 * @author Martin Pecka
 */
public class AISProductReader
{
    /** Number of items of the spectral density array. */
    private static final int NUM_DENSITY_ITEMS = Ionogram.NUM_TIME_DELAY_BINS;
    /** Size of one record in bytes. */
    private static final int AIS_RECORD_SIZE = 400;

    /**
     * Read all AIS records from the given file.
     * 
     * @param aisFile The file to read records from.
     * @return All records from the given file.
     * 
     * @throws IOException If read errors occur.
     * @throws FileNotFoundException If the given file cannot be found.
     */
    public AISProduct[] readFile(File aisFile) throws IOException, FileNotFoundException
    {
        final AISProduct[] result = new AISProduct[(int) (aisFile.length() / AIS_RECORD_SIZE)];

        try (final DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(aisFile),
                AIS_RECORD_SIZE))) {

            for (int i = 0; i < result.length; i++) {
                // maybe needs to consider it is unsigned int
                @SuppressWarnings("unused")
                final int sclk_second = stream.readInt();
                @SuppressWarnings("unused")
                final int sclk_column = stream.readUnsignedShort();
                @SuppressWarnings("unused")
                final int sclk_fine = stream.readUnsignedShort();
                @SuppressWarnings("unused")
                final int scet_days = stream.readInt();
                @SuppressWarnings("unused")
                final int scet_msec = stream.readInt();

                // unused 8 bytes
                stream.readLong();

                final byte[] scet_string_bytes = new byte[24];
                stream.readFully(scet_string_bytes);
                final String scet_string = new String(scet_string_bytes).trim();

                final DateTime spacecraft_clock = DateTime.parse(scet_string);

                final byte process_id = stream.readByte();
                final byte instrument_mode = stream.readByte();

                // unused 9 bytes
                stream.readLong();
                stream.readByte();

                final byte transmit_power = stream.readByte();
                final byte frequency_table_number = stream.readByte();
                final int frequency_number = stream.readUnsignedByte();
                final byte band_number = stream.readByte();
                final byte receiver_attenuation = stream.readByte();

                // unused 12 bytes
                stream.readLong();
                stream.readInt();

                final float frequency = stream.readFloat();
                final Float[][] spectral_density = new Float[1][NUM_DENSITY_ITEMS];
                for (int j = 0; j < NUM_DENSITY_ITEMS; j++) {
                    spectral_density[0][j] = stream.readFloat();
                }

                result[i] = new AISProduct(spacecraft_clock, process_id, instrument_mode, instrument_mode,
                        transmit_power, frequency_table_number, frequency_number, band_number, receiver_attenuation,
                        frequency, spectral_density);
            }
        }

        return result;
    }

    /**
     * @param args Arg 1: filename to parse.
     */
    public static final void main(String[] args)
    {
        if (args.length != 1) {
            System.out.println("Provide exactly one argument - the filename to be parsed as AIS records.");
            System.exit(-1);
        }

        final AISProductReader reader = new AISProductReader();
        try {
            final AISProduct[] result = reader.readFile(new File(args[0]));
            System.out.println(result.length + " AIS records read from the file: ");
            System.out.println(Arrays.toString(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
