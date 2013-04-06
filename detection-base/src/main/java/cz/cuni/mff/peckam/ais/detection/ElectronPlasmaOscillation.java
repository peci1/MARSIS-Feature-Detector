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

import cz.cuni.mff.peckam.ais.Ionogram;
import cz.cuni.mff.peckam.ais.Product;

/**
 * Electron plasma oscillations harmonics.
 * 
 * @author Martin Pecka
 */
public class ElectronPlasmaOscillation extends RepeatingLine
{

    /** Period transformed by the product. */
    private Double productPeriod = null;

    /**
     * @param offset Offset of the first repeat along horizontal axis.
     * @param period The repetition period.
     * @param end Offset of the end along the vertical axis.
     */
    public ElectronPlasmaOscillation(int offset, double period, int end)
    {
        super(Direction.HORIZONTALLY_REPEATING, offset, period, 0, end);
    }

    /** Unique ID of the feature. */
    public static final String ID = "electronPlasmaHarmonics";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String toString()
    {
        if (productPeriod != null)
            return String.format("hPeriod=%.3f MHz", productPeriod);
        else
            return String.format("hPeriod=%.3f px", getPeriod());
    }

    @Override
    public void readProductData(Product<?, ?, ?> product)
    {
        if (product instanceof Ionogram) {
            final Ionogram iono = (Ionogram) product;
            productPeriod = (getPeriod() / iono.getWidth())
                    * (iono.getMaxColumnValue() - iono.getMinColumnValue());
        }
    }

}
