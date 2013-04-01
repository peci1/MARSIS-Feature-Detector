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
package cz.cuni.mff.peckam.ais.gui;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Map.Entry;

import cz.cuni.mff.peckam.ais.Product;
import cz.cuni.mff.peckam.ais.ProductOverlay;
import cz.cuni.mff.peckam.ais.Tuple;

/**
 * Overlay renderer that renders "boolean" overlays having at each pixel either a color (when a value is defined) or transparency.
 *
 * @author Martin Pecka
 */
public class SingleColorOverlayRenderer implements OverlayRenderer
{

    /** The RGB value of the color to use. */
    private final int color;

    /**
     * @param color The color to use for rendering.
     */
    public SingleColorOverlayRenderer(Color color)
    {
        this.color = color.getRGB();
    }

    @Override
    public <ColType, RowType> void render(BufferedImage image, ProductOverlay<?, ColType, RowType, ?> overlay,
            Product<?, ColType, RowType> product)
    {
        for (Entry<Tuple<RowType, ColType>, ?> entry : overlay.getValues().entrySet()) {
            if (entry.getValue() != null) {
                final Tuple<RowType, ColType> key = entry.getKey();
                final Point point = product.getDataPosition(key.getX(), key.getY());
                image.setRGB(point.y, point.x, color);
            }
        }
    }
}
