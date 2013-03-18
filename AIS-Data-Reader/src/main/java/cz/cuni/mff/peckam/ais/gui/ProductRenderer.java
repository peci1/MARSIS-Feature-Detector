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
 * Provides property "product".
 * 
 * @author Martin Pecka
 */
public class ProductRenderer extends JPanel
{
    /**  */
    private static final long serialVersionUID = -8158771042857186802L;

    /** The product to render. */
    private Product<?>        product          = null;

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
     * @param product The product to render.
     */
    public void setProduct(Product<?> product)
    {
        final Product<?> oldProduct = this.product;
        this.product = product;
        firePropertyChange("product", oldProduct, product);

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        final Number[][] data = product.getData();
        for (Number[] row : data) {
            for (Number pixel : row) {
                if (pixel.doubleValue() > max)
                    max = pixel.doubleValue();
                if (pixel.doubleValue() < min)
                    min = pixel.doubleValue();
            }
        }

        min = Math.log10(min);
        max = Math.log10(max);
        final float range = (float) (max - min);
        final float threshold = (float) (min + range / 100f);

        image = new BufferedImage(product.getWidth(), product.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                final float val = (float) Math.log10(data[x][y].floatValue());
                int color;
                if (val > threshold) {
                    final float value = (float) ((val - min) / (max - min));
                    color = Color.HSBtoRGB((1f - value) * 265f / 360f, 1f, 1f);
                } else {
                    color = Color.black.getRGB();
                }
                image.setRGB(x, y, color);
            }
        }

        repaint();
    }
}
