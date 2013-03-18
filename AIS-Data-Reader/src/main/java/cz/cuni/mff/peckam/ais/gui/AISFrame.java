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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.peckam.JFileInput;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import cz.cuni.mff.peckam.ais.AISLBLProductReader;
import cz.cuni.mff.peckam.ais.AISProductSet;

/**
 * 
 * 
 * @author Martin Pecka
 */
public class AISFrame
{

    /** The main frame. */
    private JFrame             frmAisDataVisualizer;
    /** LBL file selection input. */
    private JFileInput         lblFileInput;
    /** The AIS renderer. */
    private ProductRenderer    renderer;
    /** Position in series. */
    private JComboBox<Integer> positionInSeriesComboBox;

    /** The product sets to display. */
    private AISProductSet[]    productSets = null;

    /** Configuration. */
    private final Properties   props       = new Properties();
    /** The config file. */
    private final static File  CONFIG_FILE = new File("config.properties");

    /**
     * Launch the application.
     * 
     * @param args no args
     */
    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run()
            {
                try {
                    AISFrame window = new AISFrame();
                    window.frmAisDataVisualizer.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public AISFrame()
    {
        try {
            props.load(new FileInputStream(CONFIG_FILE));
        } catch (IOException e2) {
            // supress
        }

        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
        frmAisDataVisualizer = new JFrame();
        frmAisDataVisualizer.setTitle("AIS Data Visualizer");
        frmAisDataVisualizer.setBounds(100, 100, 679, 496);
        frmAisDataVisualizer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmAisDataVisualizer.getContentPane().setLayout(
                new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, FormFactory.MIN_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormFactory.RELATED_GAP_COLSPEC, FormFactory.PREF_COLSPEC, FormFactory.RELATED_GAP_COLSPEC, },
                        new RowSpec[] { FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"),
                                FormFactory.RELATED_GAP_ROWSPEC, }));

        JLabel lblFileInputLabel = new JLabel(".LBL file to parse");
        frmAisDataVisualizer.getContentPane().add(lblFileInputLabel, "2, 2, left, fill");

        lblFileInput = new JFileInput();
        lblFileInput.getBrowseButton().setText("Browse...");
        lblFileInputLabel.setLabelFor(lblFileInput);
        frmAisDataVisualizer.getContentPane().add(lblFileInput, "4, 2, fill, center");
        lblFileInput.setColumns(10);
        lblFileInput.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try {
                    final String path = lblFileInput.getPath();
                    productSets = new AISLBLProductReader().readFile(new File(path));
                    positionInSeriesComboBox.setModel(new NumericRangeComboBoxModel(0, productSets.length - 1));
                    positionInSeriesComboBox.setEnabled(true);
                    positionInSeriesComboBox.setSelectedIndex(0);
                    props.setProperty("defaultFile", path);
                } catch (IOException | IllegalStateException e1) {
                    e1.printStackTrace();
                    positionInSeriesComboBox.setEnabled(false);
                    JOptionPane.showMessageDialog(null, "The given file is not a valid .LBL file.");
                }

            }
        });

        positionInSeriesComboBox = new JComboBox<>();
        positionInSeriesComboBox.setEnabled(false);
        positionInSeriesComboBox.setModel(new DefaultComboBoxModel<>(new Integer[] {}));
        frmAisDataVisualizer.getContentPane().add(positionInSeriesComboBox, "6, 2, right, default");
        positionInSeriesComboBox.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e)
            {
                renderer.setProduct(productSets[(int) positionInSeriesComboBox.getSelectedItem()]);
            }
        });

        renderer = new ProductRenderer();
        frmAisDataVisualizer.getContentPane().add(renderer, "2, 4, 5, 1, fill, fill");

        frmAisDataVisualizer.addWindowListener(new WindowAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void windowClosing(WindowEvent e)
            {
                try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                    props.store(fos, null);
                } catch (IOException e1) {
                    // supress
                }
            }
        });

        final String defaultFile = props.getProperty("defaultFile");
        lblFileInput.setPath(defaultFile);
    }

}
