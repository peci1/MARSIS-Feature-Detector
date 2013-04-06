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

import java.util.List;

import cz.cuni.mff.peckam.ais.Product;

/**
 * One feature detected in 2D product.
 * 
 * @author Martin Pecka
 */
public interface DetectedFeature
{
    /** Type for vertical lines. */
    public final static String TYPE_VERTICAL_LINE             = "verticalLine";

    /** Type for horizontal lines. */
    public final static String TYPE_HORIZONTAL_LINE           = "horizontalLine";

    /** Type for repeating vertical lines. */
    public final static String TYPE_REPEATING_VERTICAL_LINE   = "repeatingVerticalLine";

    /** Type for repeating horizontal lines. */
    public final static String TYPE_REPEATING_HORIZONTAL_LINE = "repeatingHorizontalLine";

    /** Type for general curves. */
    public final static String TYPE_GENERAL_CURVE             = "generalCurve";

    /**
     * @return Some string identifications of the kind of the feature. One of them should be one of the TYPE constants
     *         of this interface.
     */
    List<String> getTypes();

    /**
     * Add the given type to this feature.
     * 
     * @param type The type to add.
     */
    void addType(String type);

    /**
     * @return The unique ID of the feature.
     */
    String getId();

    /**
     * Show the product to the feature in order to be able to transform it according to the result (if needed). Don't
     * save references to the product, though, because it will block its freeing from memory which is important.
     * 
     * @param product The product.
     */
    void readProductData(Product<?, ?, ?> product);
}