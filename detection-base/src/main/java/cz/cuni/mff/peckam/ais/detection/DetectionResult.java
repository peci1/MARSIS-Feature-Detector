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

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cz.cuni.mff.peckam.ais.Product;

/**
 * Result of feature detection.
 * 
 * @author Martin Pecka
 */
public class DetectionResult
{
    /** The detected features. */
    private final Map<String, DetectedFeature> features = new HashMap<>();

    /** Id of the source product. */
    private final Object                       sourceProductId;

    /** The size of the source product. */
    private final Dimension                    sourceProductSize;

    /**
     * @param sourceProductId Id of the source product.
     * @param sourceProductSize The size of the source product.
     * @param features The features to add.
     */
    public DetectionResult(Object sourceProductId, Dimension sourceProductSize, DetectedFeature... features)
    {
        this.sourceProductId = sourceProductId;
        this.sourceProductSize = sourceProductSize;
        if (features != null) {
            for (DetectedFeature feature : features) {
                addFeature(feature);
            }
        }
    }

    /**
     * Add the given feature to the result.
     * 
     * @param feature The feature to add.
     */
    public void addFeature(DetectedFeature feature)
    {
        features.put(feature.getId(), feature);
    }

    /**
     * @return The detected features.
     */
    public Collection<DetectedFeature> getFeatures()
    {
        return features.values();
    }

    /**
     * Return the feature with the given ID or <code>null</code> if no such feature is found.
     * 
     * @param id Unique ID of the feature.
     * @return The feature or <code>null</code> if it is not found.
     */
    public DetectedFeature getFeature(String id)
    {
        return features.get(id);
    }

    /**
     * @return Id of the source product.
     */
    public Object getSourceProductId()
    {
        return sourceProductId;
    }

    /**
     * @return The size of the source product.
     */
    public Dimension getSourceProductSize()
    {
        return sourceProductSize;
    }

    /**
     * Show the product to all the detected feature in order to be able to transform the features according to the
     * result (if needed). Don't save references to the product, though, because it will block its freeing from memory
     * which is important.
     * 
     * @param product The product.
     */
    public void readProductData(Product<?, ?, ?> product)
    {
        for (DetectedFeature feature : features.values())
            feature.readProductData(product);
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();

        builder.append(getSourceProductId());

        if (!features.isEmpty()) {
            String separator = ": ";
            for (DetectedFeature feature : features.values()) {
                builder.append(separator).append(feature.toString());
                separator = ", ";
            }
        } else {
            builder.append(": no features");
        }

        return builder.toString();
    }
}
