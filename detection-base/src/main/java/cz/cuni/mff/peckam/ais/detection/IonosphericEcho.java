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
import cz.cuni.mff.peckam.ais.Product;

/**
 * Ionospheric echo.
 * 
 * @author Martin Pecka
 */
public class IonosphericEcho extends GeneralCurve
{
    /** Id of the feature. */
    public static final String ID = "ionosphericEcho";

    /** Start frequency transformed by the product. */
    private Double             startFreq = null;
    /** End frequency transformed by the product. */
    private Double             endFreq   = null;

    /**
     * @param points The points of the echo.
     */
    public IonosphericEcho(Point... points)
    {
        super(points);
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String toString()
    {
        if (startFreq != null && endFreq != null) {
            return String.format("Ionospheric echo from %f MHz to %f MHz with %d points", startFreq, endFreq,
                    getPoints().length);
        } else {
            return String.format("Ionospheric echo from %d px to %d px with %d points", getPoints()[0].x,
                getPoints()[getPoints().length - 1].x, getPoints().length);
        }
    }

    @Override
    public void readProductData(Product<?, ?, ?> product)
    {
        if (product instanceof Ionogram) {
            final Ionogram iono = (Ionogram) product;
            startFreq = iono.getMinColumnValue() + (getPoints()[0].x / (double) iono.getWidth())
                    * (iono.getMaxColumnValue() - iono.getMinColumnValue());
            endFreq = iono.getMinColumnValue() + (getPoints()[getPoints().length - 1].x / (double) iono.getWidth())
                    * (iono.getMaxColumnValue() - iono.getMinColumnValue());
        }
    }

}
