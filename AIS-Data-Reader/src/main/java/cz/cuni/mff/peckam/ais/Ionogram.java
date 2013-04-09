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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import cz.cuni.mff.peckam.ais.result.FrameType;
import cz.cuni.mff.peckam.ais.result.ObjectFactory;
import cz.cuni.mff.peckam.ais.result.PointType;

/**
 * A set of {@link AISProduct}s forming one complete data frame.
 * 
 * @author Martin Pecka
 */
public class Ionogram implements Product<Float, Float, Float>
{
    /** The minimum value of a ionogram. */
    public static final double                                                                  MIN_VALUE                = 10E-17;

    /** The minimum value of a ionogram. */
    public static final double                                                                  MAX_VALUE                = 10E-9;

    // frequency table at http://www-pw.physics.uiowa.edu/plasma-wave/marsx/restricted/super/DOCUMENT/AIS_FREQ_TABLE.TXT

    /** The maximum frequency in MHz. */
    public static final double                                                                  MIN_FREQUENCY            = 100000 / 1E6;

    /** The maximum frequency in MHz. */
    public static final double                                                                  MAX_FREQUENCY            = 5500000 / 1E6;

    /** Range of the allowed frequencies in MHz. */
    public static final double                                                                  FREQUENCY_RANGE          = MAX_FREQUENCY
                                                                                                                                 - MIN_FREQUENCY;

    /** The number of frequencies used for sounding. */
    public static final int                                                                     NUM_FREQUENCY_BINS       = 160;

    /** The minimum delay time in ms. */
    public static final double                                                                  MIN_DELAY_TIME           = 253.92856999999998 / 1E3;
    // public static final double MIN_DELAY_TIME = 162.5 / 1E3;

    /** The maximum delay time in ms. */
    public static final double                                                                  MAX_DELAY_TIME           = 7568.21416999999942 / 1E3;

    /** The width of the time delay range. */
    public static final double                                                                  DELAY_TIME_RANGE         = MAX_DELAY_TIME
                                                                                                                                 - MIN_DELAY_TIME;

    /** Width of one time delay measuring bin. */
    public static final double                                                                  DELAY_TIME_BIN_WIDTH     = 91.428569999999993 / 1E3;

    /** The number of time delay measuring bins. */
    public static final int                                                                     NUM_TIME_DELAY_BINS      = 80;

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
    private Float[][]                                                                           data;

    /** Altitude over surface. */
    private final Float                                                                         altitude;

    /** Keys - the column frequencies. */
    private final Float[]        columnKeys;

    /** The overlays. */
    private final Map<ProductOverlayType, ProductOverlay<?, Float, Float, ? extends Product<Float, Float, Float>>> overlays                 = new HashMap<>();

    /** The referential result of feature detection. */
    private FrameType                                                                           referenceDetectionResult = null;

    /**  */
    private final ObjectFactory                                                                 factory                  = new ObjectFactory();
    /** Coefficient to be applied to y data coordinates to get time delay. */
    private final float                                                                         yCoef;

    /**
     * @param columns Data columns.
     * @param orbitNumber The orbit number.
     * @param positionInSeries Position of this data set in the series read from a single AIS data file.
     * @param altitude Altitude over surface.
     */
    public Ionogram(AISProduct[] columns, int orbitNumber, int positionInSeries, Float altitude)
    {
        this.columns = columns;
        this.orbitNumber = orbitNumber;
        this.positionInSeries = positionInSeries;

        this.data = new Float[this.columns.length][];
        this.columnKeys = new Float[this.columns.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = this.columns[i].getData()[0];
            columnKeys[i] = this.columns[i].getFrequency();
        }

        this.frequencyTableNumber = columns[0].getFrequencyTableNumber();
        this.startTime = columns[0].getSpaceCraftClock();
        this.yCoef = (float) (DELAY_TIME_RANGE / getHeight());
        this.altitude = altitude;

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

    /**
     * @return Altitude over surface.
     */
    public Float getAltitude()
    {
        return altitude;
    }

    /**
     * @param data The new data array.
     */
    protected void setData(Float[][] data)
    {
        this.data = data;
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

    /**
     * @return The column keys of the unevenly sampled ionogram.
     */
    public Float[] getOriginalColumnKeys()
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
    public double getMinColumnValue()
    {
        return MIN_FREQUENCY;
    }

    /**
     * @return The maximal column value.
     */
    public double getMaxColumnValue()
    {
        return MAX_FREQUENCY;
    }

    /**
     * @return The height of a row.
     */
    public double getRowHeight()
    {
        return DELAY_TIME_BIN_WIDTH;
    }

    /**
     * @return The minimal row value.
     */
    public double getMinRowValue()
    {
        return MIN_DELAY_TIME;
    }

    /**
     * @return The maximal row value.
     */
    public double getMaxRowValue()
    {
        return MAX_DELAY_TIME;
    }

    @Override
    public Point getDataPosition(Float row, Float column)
    {
        if (row < getMinRowValue() || row > getMaxRowValue())
            throw new IllegalArgumentException("Row value must lie within the interval <" + getMinRowValue() + "; "
                    + getMaxRowValue() + ">, but " + row + " was given.");

        if (column + 0.01 * FREQUENCY_RANGE < getMinColumnValue()
                || column - 0.01 * FREQUENCY_RANGE > getMaxColumnValue()) {
            throw new IllegalArgumentException("Column value must lie within the interval <" + getMinColumnValue()
                    + "; " + getMaxColumnValue() + ">, but " + column + " was given.");
        }
        final float pColumn = (float) Math.max(MIN_FREQUENCY, Math.min(MAX_FREQUENCY, column));

        int rowPosition = columns[0].getDataPosition(row, null).x;

        Integer bestColumnKeyIndex = null;
        float bestColumnKeyDistance = Float.MAX_VALUE;

        final Float[] columnKeys = getColumnKeys();
        for (int i = 0; i < columnKeys.length; i++) {
            final float columnKey = columnKeys[i];
            final float distance = Math.abs(columnKey - pColumn);
            if (bestColumnKeyDistance > distance) {
                bestColumnKeyDistance = distance;
                bestColumnKeyIndex = i;
            }
        }

        if (bestColumnKeyIndex == null)
            throw new IllegalStateException("Couldn't find corresponding column key for column value " + pColumn);

        return new Point(rowPosition, bestColumnKeyIndex);
    }

    /**
     * Return the frequency/time delay coordinates for the given data coordinates.
     * 
     * @param x X coord.
     * @param y Y coord.
     * @return Coordinates in freq/time.
     */
    public PointType getFreqTimePosition(int x, int y)
    {
        final PointType result = factory.createPointType();
        result.setX(columns[x].getFrequency());
        result.setY((float) (y * yCoef + MIN_DELAY_TIME));
        return result;
    }

    @Override
    public Collection<ProductOverlay<?, Float, Float, ? extends Product<Float, Float, Float>>> getOverlays()
    {
        return overlays.values();
    }

    /**
     * Add the given overlay to this ionogram.
     * 
     * @param overlay The overlay to add.
     */
    public void addOverlay(ProductOverlay<?, Float, Float, ? extends Product<Float, Float, Float>> overlay)
    {
        this.overlays.put(overlay.getType(), overlay);
    }

    @Override
    public ProductOverlay<?, Float, Float, ? extends Product<Float, Float, Float>> getOverlay(ProductOverlayType type)
    {
        return this.overlays.get(type);
    }

    @Override
    public Object getId()
    {
        return "Orbit " + orbitNumber + " at " + getStartTime();
    }

    /**
     * @return The referential result of feature detection.
     */
    public FrameType getReferenceDetectionResult()
    {
        return referenceDetectionResult;
    }

    /**
     * @param referenceDetectionResult The referential result of feature detection.
     */
    public void setReferenceDetectionResult(FrameType referenceDetectionResult)
    {
        this.referenceDetectionResult = referenceDetectionResult;
    }

}