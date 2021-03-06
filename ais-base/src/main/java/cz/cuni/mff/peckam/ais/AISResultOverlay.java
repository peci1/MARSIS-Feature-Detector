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

import java.util.HashMap;
import java.util.Map;

import cz.cuni.mff.peckam.ais.result.FrameType;
import cz.cuni.mff.peckam.ais.result.PointType;

/**
 * Overlay of ionogram with AIS detection results.
 * 
 * @author Martin Pecka
 */
public class AISResultOverlay implements ProductOverlay<Boolean, Float, Float, Ionogram>
{

    /** The data of the overlay. */
    private final FrameType                         resultData;

    /** The overlaid ionogram. */
    private final Ionogram                          ionogram;

    /** The overlay values. */
    private final Map<Tuple<Float, Float>, Boolean> values;

    /** The type of the overlay. */
    private final ProductOverlayType                type;

    /**
     * @param ionogram The overlaid ionogram.
     * @param resultData The data of the overlay.
     * @param type The type of the overlay.
     */
    public AISResultOverlay(Ionogram ionogram, FrameType resultData, ProductOverlayType type)
    {
        this.ionogram = ionogram;
        this.resultData = resultData;
        this.type = type;

        values = new HashMap<>();

        if (resultData.getIonospheretrace() != null && !resultData.getIonospheretrace().getPoints().isEmpty()) {
            for (PointType point : resultData.getIonospheretrace().getPoints()) {
                addPoint(point.getY(), point.getX());
            }
        }

        if (resultData.getGroundtrace() != null && !resultData.getGroundtrace().getPoints().isEmpty()) {
            for (PointType point : resultData.getGroundtrace().getPoints()) {
                addPoint(point.getY(), point.getX());
            }
        }

        if (resultData.getHperiod() != null && resultData.getHperiod() != 0f) {
            float period = resultData.getHperiod();
            while (period <= ionogram.getMaxColumnValue()) {
                if (period >= ionogram.getMinColumnValue()) {
                    for (int i = 0; i < 8; i++) {
                        final float t = ionogram.getFreqTimePosition(0, i).getY();
                        addPoint(t, period);
                    }
                }
                period += resultData.getHperiod();
            }
        }

        if (resultData.getVperiod() != null && resultData.getVperiod() != 0f) {
            float period = resultData.getVperiod();
            while (period <= ionogram.getMaxRowValue()) {
                if (period >= ionogram.getMinRowValue()) {
                    for (int i = 0; i < 8; i++) {
                        final float f = ionogram.getFreqTimePosition(i, 0).getX();
                        addPoint(period, f);
                    }
                }
                period += resultData.getVperiod();
            }
        }
    }

    /**
     * Adds the given point to this.values.
     * 
     * @param delay Time position.
     * @param frequency Frequency position.
     */
    private void addPoint(float delay, float frequency)
    {
        values.put(new Tuple<>(delay, frequency), true);
    }

    @Override
    public Boolean getValue(Float rowValue, Float columnValue)
    {
        return values.get(new Tuple<>(rowValue, columnValue));
    }

    @Override
    public Map<Tuple<Float, Float>, Boolean> getValues()
    {
        return values;
    }

    @Override
    public Ionogram getProduct()
    {
        return ionogram;
    }

    @Override
    public ProductOverlayType getType()
    {
        return type;
    }

    @Override
    public String getDescription()
    {
        return "hPeriod = "
                + resultData.getHperiod()
                + " MHz, vPeriod = "
                + resultData.getVperiod()
                + " ms, "
                + ((resultData.getIonospheretrace() == null || resultData.getIonospheretrace().getPoints().size() == 0) ? "no ionosphere trace, "
                        : "ionosphere trace with " + resultData.getIonospheretrace().getPoints().size() + " points, ")
                + ((resultData.getGroundtrace() == null || resultData.getGroundtrace().getPoints().size() == 0) ? "no ground trace"
                        : "ground trace with " + resultData.getGroundtrace().getPoints().size() + " points");
    }

}
