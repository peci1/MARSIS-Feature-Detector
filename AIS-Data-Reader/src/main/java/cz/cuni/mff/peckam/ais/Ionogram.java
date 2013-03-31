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

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;

/**
 * A set of {@link AISProduct}s forming one complete data frame.
 * 
 * @author Martin Pecka
 */
public class Ionogram implements Product<Float, Float, Float>
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

    /** Keys - the column frequencies. */
    private final Float[]        columnKeys;

    /** The minimal column value. */
    private final Float          minColumnValue;

    /** The maximal column value. */
    private final Float          maxColumnValue;

    /** The overlays. */
    private final List<ProductOverlay<?, Float, Float, ? extends Product<Float, Float, Float>>> overlays = new LinkedList<>();

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
        this.columnKeys = new Float[this.columns.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = this.columns[i].getData()[0];
            columnKeys[i] = this.columns[i].getFrequency() / 1E6f;
        }

        this.frequencyTableNumber = columns[0].getFrequencyTableNumber();
        this.startTime = columns[0].getSpaceCraftClock();

        this.minColumnValue = columnKeys[0];
        this.maxColumnValue = columnKeys[columnKeys.length - 1];
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

    @Override
    public Float[] getColumnKeys()
    {
        return columnKeys;
    }

    @Override
    public Float[] getRowKeys()
    {
        return columns[0].getRowKeys();
    }

    /**
     * @return The minimal column value.
     */
    protected Float getMinColumnValue()
    {
        return minColumnValue;
    }

    /**
     * @return The maximal column value.
     */
    protected Float getMaxColumnValue()
    {
        return maxColumnValue;
    }

    /**
     * @return The height of a row.
     */
    protected Float getRowHeight()
    {
        return columns[0].getRowHeight();
    }

    /**
     * @return The minimal row value.
     */
    protected Float getMinRowValue()
    {
        return columns[0].getMinRowValue();
    }

    /**
     * @return The maximal row value.
     */
    protected Float getMaxRowValue()
    {
        return columns[0].getMaxRowValue();
    }

    @Override
    public Point getDataPosition(Float row, Float column)
    {
        if (row < getMinRowValue() || row > getMaxRowValue())
            throw new IllegalArgumentException("Row value must lie within the interval <" + getMinRowValue() + "; "
                    + getMaxRowValue() + ">, but " + row + " was given.");

        if (column < getMinColumnValue() || column > getMaxColumnValue()) {
            throw new IllegalArgumentException("Column value must lie within the interval <" + getMinColumnValue()
                    + "; " + getMaxColumnValue() + ">, but " + column + " was given.");
        }

        int rowPosition = columns[0].getDataPosition(row, null).x;

        Integer bestColumnKeyIndex = null;
        Float bestColumnKeyDistance = null;

        final Float[] columnKeys = getColumnKeys();
        for (int i = 0; i < columnKeys.length; i++) {
            final float columnKey = columnKeys[i];
            final float distance = Math.abs(columnKey - column);
            if (bestColumnKeyDistance == null || bestColumnKeyDistance > distance) {
                bestColumnKeyDistance = distance;
                bestColumnKeyIndex = i;
            }
        }

        if (bestColumnKeyIndex == null)
            throw new IllegalStateException("Couldn't find corresponding column key for column value " + column);

        return new Point(rowPosition, bestColumnKeyIndex);
    }

    @Override
    public List<ProductOverlay<?, Float, Float, ? extends Product<Float, Float, Float>>> getOverlays()
    {
        return overlays;
    }

    /**
     * Add the given overlay to this ionogram.
     * 
     * @param overlay The overlay to add.
     */
    public void addOverlay(ProductOverlay<?, Float, Float, ? extends Product<Float, Float, Float>> overlay)
    {
        this.overlays.add(overlay);
    }
}