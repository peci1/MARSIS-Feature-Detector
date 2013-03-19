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

import org.joda.time.DateTime;

/**
 * A set of {@link AISProduct}s forming one complete data frame.
 * 
 * @author Martin Pecka
 */
/**
 * 
 * 
 * @author Martin Pecka
 */
public class Ionogram implements Product<Float>
{
    /** The data columns. */
    private final AISProduct[] columns;

    /** The orbit number. */
    private final int          orbitNumber;

    /** Position of this data set in the series read from a single AIS data file. */
    private final int          positionInSeries;

    /** Number of the frequency table. */
    private final byte         frequencyTableNumber;

    /** Spacecraft time of the first data row. */
    private final DateTime     startTime;

    /** The data. */
    private final Float[][]    data;

    /**
     * @param columns Data columns.
     * @param orbitNumber The orbit number.
     * @param positionInSeries Position of this data set in the series read from a single AIS data file.
     */
    public Ionogram(AISProduct[] columns, int orbitNumber, int positionInSeries)
    {
        this.columns = columns;
        this.orbitNumber = orbitNumber;
        this.positionInSeries = positionInSeries;

        this.data = new Float[this.columns.length][];
        for (int i = 0; i < data.length; i++) {
            data[i] = this.columns[i].getData()[0];
        }

        this.frequencyTableNumber = columns[0].getFrequencyTableNumber();
        this.startTime = columns[0].getSpaceCraftClock();
    }

    /**
     * @return The data columns.
     */
    public AISProduct[] getColumns()
    {
        return columns;
    }

    @Override
    public Float[][] getData()
    {
        return data;
    }

    @Override
    public int getWidth()
    {
        return data.length;
    }

    @Override
    public int getHeight()
    {
        return data[0].length;
    }

    /**
     * @return The orbit number.
     */
    public int getOrbitNumber()
    {
        return orbitNumber;
    }

    /**
     * @return Position of this data set in the series read from a single AIS data file.
     */
    public int getPositionInSeries()
    {
        return positionInSeries;
    }

    /**
     * @return The number of the frequency table.
     */
    public byte getFrequencyTableNumber()
    {
        return frequencyTableNumber;
    }

    /**
     * @return Spacecraft time of the first data row.
     */
    public DateTime getStartTime()
    {
        return startTime;
    }

    @Override
    public String toString()
    {
        return "Ionogram [startTime=" + columns[0].getSpaceCraftClock() + ", orbitNumber=" + orbitNumber
                + ", positionInSeries=" + positionInSeries + "]";
    }

    @Override
    public String getMetadataString()
    {
        final StringBuilder builder = new StringBuilder();

        builder.append("Orbit: ").append(getOrbitNumber());
        builder.append("   Frequency table: ").append(getFrequencyTableNumber());
        builder.append("   Start time: ").append(getStartTime());

        return builder.toString();
    }
}