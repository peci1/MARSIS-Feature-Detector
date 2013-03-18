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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import cz.cuni.mff.peckam.ais.Product;

/**
 * Renderer for data products.
 * <p>
 * Provides properties "product", "colorScale".
 * 
 * @author Martin Pecka
 */
public class ProductRenderer extends JPanel
{
    /**  */
    private static final long serialVersionUID = -8158771042857186802L;

    /** The product to render. */
    private Product<?>        product          = null;

    /** Color scale. */
    private ColorScale<?>     colorScale       = null;

    /** The image to draw. */
    private BufferedImage     image            = null;

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (product == null)
            return;

        final int w = getWidth();
        final int h = getHeight();
        final int iw = image.getWidth();
        final int ih = image.getHeight();

        final Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.black);
        g2.clearRect(0, 0, w, h);

        g2.drawImage(image, 0, 0, w, h, 0, 0, iw, ih, null);
    }

    /**
     * @return The product to render.
     */
    public Product<?> getProduct()
    {
        return product;
    }

    /**
     * @return The color scale.
     */
    public ColorScale<?> getColorScale()
    {
        return colorScale;
    }

    /**
     * @param <N> The numeric type of the product.
     * @param product The product to render.
     * @param colorScale The color scale used to render the product.
     */
    public <N extends Number> void setProductAndColorScale(Product<N> product, ColorScale<N> colorScale)
    {
        final Product<?> oldProduct = this.product;
        this.product = product;
        firePropertyChange("product", oldProduct, product);

        final ColorScale<?> oldScale = this.colorScale;
        this.colorScale = colorScale;
        firePropertyChange("colorScale", oldScale, colorScale);

        image = new BufferedImage(product.getWidth(), product.getHeight(), BufferedImage.TYPE_INT_RGB);

        final N[][] data = product.getData();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                final Color color = colorScale.getColor(data[x][y]);
                image.setRGB(x, y, color.getRGB());
            }
        }

        repaint();
    }
}
