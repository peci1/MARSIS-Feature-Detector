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
import java.util.List;

/**
 * A general product of generic data type with basic properties.
 * 
 * @author Martin Pecka
 * 
 * @param <DataType> Type of the samples this product contains.
 * @param <ColumnKeyType> Type of the column keys.
 * @param <RowKeyType> Type of the row keys.
 */
public interface Product<DataType extends Number, ColumnKeyType, RowKeyType>
{

    /**
     * The product's data.
     * 
     * @return The product's data.
     */
    DataType[][] getData();

    /**
     * @return Keys corresponding to the data columns. Length of the returned array should be the same as
     *         {@link #getWidth()} value.
     */
    ColumnKeyType[] getColumnKeys();

    /**
     * @return Keys corresponding to the data rows. Length of the returned array should be the same as
     *         {@link #getHeight()} value.
     */
    RowKeyType[] getRowKeys();

    /**
     * @return Width of a row of the product's data.
     */
    int getWidth();

    /**
     * @return Height of a column of the product's data.
     */
    int getHeight();

    /**
     * @return The metadata string.
     */
    String getMetadataString();

    /**
     * Return the position in {@link #getData()} array corresponding to the given row and column keys.
     * <p>
     * The given "keys" don't have to be exactly the keys returned by {@link #getColumnKeys()} or {@link #getRowKeys()},
     * they can be "intervalues" (if it makes sense) interpolated from the real keys.
     * 
     * @param row The row key.
     * @param column The column key.
     * @return Position in {@link #getData()} array.
     */
    Point getDataPosition(RowKeyType row, ColumnKeyType column);

    /**
     * @return All overlays defined on this product.
     */
    List<ProductOverlay<?, ColumnKeyType, RowKeyType, ? extends Product<DataType, ColumnKeyType, RowKeyType>>> getOverlays();
}
