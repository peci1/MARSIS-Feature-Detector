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

import java.util.Map;

/**
 * Overlay layer over a product
 * 
 * @author Martin Pecka
 * 
 * @param <DataType> Type of the samples this overlay contains.
 * @param <ColType> Type of the column keys.
 * @param <RowType> Type of the row keys.
 * @param <ProductType> Type of the product this overlay is intended to work on.
 */
public interface ProductOverlay<DataType, ColType, RowType, ProductType extends Product<?, ColType, RowType>>
{
    /** The default overlay type. */
    public static final ProductOverlayType TYPE_DEFAULT = new ProductOverlayType.Manual();

    /**
     * Return the value of this overlay at the specified coordinates in its product.
     * 
     * @param rowValue The row value.
     * @param columnValue The column value.
     * @return The value of this overlay or <code>null</code> if it doesn't hold a value for the given coordinates.
     */
    DataType getValue(RowType rowValue, ColType columnValue);

    /**
     * Return the values of this overlay at over its product.
     * 
     * @return The values corresponding to coordinates. <code>null</code>s at coordinates not defined by this overlay.
     */
    Map<Tuple<RowType, ColType>, ? extends DataType> getValues();

    /**
     * @return The product this overlay is based on.
     */
    ProductType getProduct();

    /**
     * @return The type of this field. It may be used to e.g. decide how to visualize the overlay. Defaults to
     *         {@link #TYPE_DEFAULT}.
     */
    ProductOverlayType getType();
}
