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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import cz.cuni.mff.peckam.ais.Product;
import cz.cuni.mff.peckam.ais.ProductOverlay;

/**
 * Renderer for data products.
 * <p>
 * Provides properties "product", "colorScale".
 * 
 * @author Martin Pecka
 */
public class ProductRenderer extends JPanel
{
    /**
     * 
     */
    public ProductRenderer()
    {
    }

    /**  */
    private static final long serialVersionUID = -8158771042857186802L;

    /** Popup. */
    private final JPopupMenu             popupMenu               = new JPopupMenu();

    /** The product to render. */
    private Product<?, ?, ?>  product                 = null;

    /** Color scale. */
    private ColorScale<?>     colorScale       = null;

    /** The image to draw. */
    private BufferedImage     image            = null;

    /** The horizontal scale. */
    private BufferedImage     horizontalScale         = null;

    /** Height of the horizontal scale. */
    private static final int             HORIZONTAL_SCALE_HEIGHT = 60;

    /** The binary hierarchy levels of labels. */
    private Integer           prevNumLabelLevels      = null;

    /** The loading text to be displayed. */
    private String            loadingText             = null;

    /** The factory creating overlay renderers. */
    private final OverlayRendererFactory overlayRendererFactory;

    // initializer
    {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateHorizontalScale();
                repaint();
            }
        });

        overlayRendererFactory = createOverlayRendererFactory();

        setComponentPopupMenu(popupMenu);
        JMenuItem item = new JMenuItem("Save image");
        popupMenu.add(item);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final JFileChooser chooser = new JFileChooser();
                if (chooser.showSaveDialog(ProductRenderer.this) == JFileChooser.APPROVE_OPTION) {
                    final File file = chooser.getSelectedFile();
                    try {
                        ImageIO.write(image, "PNG", file);
                        JOptionPane.showMessageDialog(ProductRenderer.this, "The image has been saved.");
                    } catch (IOException e1) {
                        JOptionPane
                                .showMessageDialog(ProductRenderer.this, "An error occured when saving the picture.");
                        e1.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (product == null || image == null) {
            g.drawString("Loading...", getWidth() / 2, getHeight() / 2);
            return;
        }

        final int w = getWidth();
        final int h = getHeight() - HORIZONTAL_SCALE_HEIGHT;
        final int iw = image.getWidth();
        final int ih = image.getHeight();

        final Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.black);
        g2.clearRect(0, 0, w, h);

        g2.drawImage(image, 0, 0, w, h, 0, 0, iw, ih, null);

        g2.setColor(Color.white);
        g2.clearRect(0, h, w, h + HORIZONTAL_SCALE_HEIGHT);
        // since we are going to rotate, mixing width and height here is correct.
        final AffineTransform transform = AffineTransform.getRotateInstance(Math.toRadians(-90));
        transform.translate(-h - HORIZONTAL_SCALE_HEIGHT, 0);
        g2.drawImage(horizontalScale, transform, null);

        if (loadingText != null) {
            g.drawString(loadingText, getWidth() / 2, getHeight() / 2);
        }
    }

    /**
     * @param loadingText The text to show. <code>null</code> to turn the message off.
     */
    public synchronized void setLoadingText(String loadingText)
    {
        this.loadingText = loadingText;
        repaint();
    }

    /**
     * @return The product to render.
     */
    public Product<?, ?, ?> getProduct()
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
     * @param <C> Type of the column keys.
     * @param <R> Type of the row keys.
     * @param product The product to render.
     * @param colorScale The color scale used to render the product.
     */
    public <N extends Number, C, R> void setProductAndColorScale(final Product<N, C, R> product,
            final ColorScale<N> colorScale)
    {
        setLoadingText("Switching the displayed data...");

        final Product<?, ?, ?> oldProduct = this.product;
        this.product = product;
        firePropertyChange("product", oldProduct, product);

        final ColorScale<?> oldScale = this.colorScale;
        this.colorScale = colorScale;
        firePropertyChange("colorScale", oldScale, colorScale);

        setPreferredSize(new Dimension(product.getWidth(), product.getHeight() + HORIZONTAL_SCALE_HEIGHT));

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception
            {
                final int w = product.getWidth();
                final int h = product.getHeight();
                final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

                final N[][] data = product.getData();
                final int[] imageData = new int[w * h];
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        imageData[x + y * w] = colorScale.getColor(data[x][y]).getRGB();
                    }
                }
                image.setRGB(0, 0, w, h, imageData, 0, w);

                for (ProductOverlay<?, C, R, ?> overlay : product.getOverlays()) {
                    final OverlayRenderer renderer = overlayRendererFactory.createRenderer(overlay);
                    renderer.render(image, overlay, product);
                }

                prevNumLabelLevels = null;
                updateHorizontalScale();

                return image;
            }

            @Override
            protected void done()
            {
                try {
                    image = get();
                    setLoadingText(null);
                    repaint();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    /**  */
    private final static Font        font;
    /**  */
    private final static LineMetrics lineMetrics;
    /**  */
    private final static int         textHeight;
    /**  */
    private final static int         lineHeight;

    static {
        font = UIManager.getFont("Label.font").deriveFont(Font.PLAIN).deriveFont(25f);
        lineMetrics = font.getLineMetrics(String.format("%.2f", 0.12345f), new FontRenderContext(null, true, false));
        textHeight = (int) lineMetrics.getHeight();
        lineHeight = textHeight + 10; // add some more line-spacing
    }

    /** Cached values. */
    private SortedSet<Integer>       labelIndices = null;

    /**
     * Update the image for the horizontal scale.
     */
    protected void updateHorizontalScale()
    {
        if (getWidth() == 0 || getProduct() == null)
            return;

        final Object[] keys = getProduct().getColumnKeys();
        // since we are going to rotate, mixing width and height here is correct.
        horizontalScale = new BufferedImage(HORIZONTAL_SCALE_HEIGHT, getWidth(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = horizontalScale.createGraphics();
        g.setBackground(Color.white);
        g.clearRect(0, 0, horizontalScale.getWidth(), horizontalScale.getHeight());
        g.setColor(Color.black);
        g.setFont(font);

        // subtracting textHeight is due to the last label to be displayed correctly
        final int maxLabelsToDisplay = (int) Math.floor((horizontalScale.getHeight() - textHeight)
                / (double) lineHeight);
        // -1 for the last label
        final int numLabelLevels = (int) Math.floor(log2(maxLabelsToDisplay - 1));

        if (prevNumLabelLevels == null || prevNumLabelLevels != numLabelLevels) {
            prevNumLabelLevels = numLabelLevels;
            labelIndices = getLabelIndicesForLevel(0, keys.length - 2, numLabelLevels);
            labelIndices.add(keys.length - 1);
        }

        for (Integer i : labelIndices) {
            String label = keys[i].toString();
            if (keys[i] instanceof Float || keys[i] instanceof Double)
                label = String.format("%.2f", keys[i]);

            // subtracting textHeight is due to the last label to be displayed correctly;
            // adding textHeight is due to "y" being the baseline position, not the "topline" position
            int y = (int) (textHeight + (horizontalScale.getHeight() - textHeight) * (i / (float) keys.length));
            // the last label has to be handled separately
            if (i == keys.length - 1)
                y = (int) (horizontalScale.getHeight() - lineMetrics.getDescent());

            g.drawString(label, 0, y);
        }
    }

    /**
     * @return The overlay renderer factory.
     */
    protected OverlayRendererFactory createOverlayRendererFactory()
    {
        return new OverlayRendererFactory();
    }

    /**
     * @param x The value.
     * @return Base 2 log of the value.
     */
    private static double log2(double x)
    {
        return Math.log(x) / Math.log(2);
    }

    /**
     * Return label indices up to the given binary hierarchy level.
     * 
     * @param startIndex First index to use.
     * @param endIndex Last index to use.
     * @param level The level.
     * 
     * @return Set of indices.
     */
    private static SortedSet<Integer> getLabelIndicesForLevel(int startIndex, int endIndex, int level)
    {
        if (level == 0)
            return new TreeSet<>(Arrays.asList(new Integer[] { startIndex }));

        final SortedSet<Integer> left = getLabelIndicesForLevel(startIndex, startIndex + (endIndex - startIndex) / 2,
                level - 1);
        final SortedSet<Integer> right = getLabelIndicesForLevel(startIndex + (endIndex - startIndex) / 2 + 1,
                endIndex, level - 1);

        left.addAll(right);
        return left;
    }

}