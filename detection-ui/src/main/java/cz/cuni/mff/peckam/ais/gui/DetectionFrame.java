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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.peckam.JFileInput;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * The main window for detector comparisons.
 * 
 * @author Martin Pecka
 */
public class DetectionFrame
{

    /** The frame itself. */
    private JFrame frmAisFetureDetectors;

    /** The base data path selection input. */
    private JFileInput  fileInput;

    /**  */
    private JLabel      lblStartOrbit;

    /** The lowest orbit number to detect in. */
    private JComboBox<Integer> startOrbit;

    /**  */
    private JLabel      lblEndOrbit;

    /** The highest orbit number to detect in. */
    private JComboBox<Integer> endOrbit;

    /** Button to start detection. */
    private JButton     btnDetect;

    /** Tabbed pane for the particular detector presntations. */
    private JTabbedPane tabbedPane;

    /**
     * The folder containing the fodlers with orbit data. <code>null</code> if the folder selected in {@link #fileInput}
     * is invalid.
     */
    private File               baseFolder = null;

    /** Orbit numbers available in {@link #baseFolder}. */
    private Vector<Integer>                  availableOrbitNumbers = null;

    /** Translation table from orbit numbers to their corresponding .LBL files. */
    private final SortedMap<Integer, File>      orbitNumToOrbitFile           = new TreeMap<>();

    /** True if the detection process is in progress right now. */
    private volatile boolean   detectionInProgress = false;

    /** The presentations of the detectors. */
    private final List<DetectorPresentation<?>> detectorPresentations         = new LinkedList<>();

    /** Configuration. */
    private final Properties                 props                 = new Properties();
    /** The config file. */
    private final static File                CONFIG_FILE           = new File("config.properties");

    /** True if the base folder is being changed right now. */
    private volatile boolean                 baseFolderChangeInProgress    = false;

    /** Queue of tasks to be done after base folder change finishes. */
    private final List<Runnable>             performAfterFolderChangeTasks = Collections
                                                                                   .synchronizedList(new LinkedList<Runnable>());

    /**
     * Launch the application.
     * 
     * @param args no args
     */
    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                try {
                    DetectionFrame window = new DetectionFrame();
                    window.frmAisFetureDetectors.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public DetectionFrame()
    {
        try {
            props.load(new FileInputStream(CONFIG_FILE));
        } catch (IOException e2) {
            // supress
        }

        registerDetectorPresentations();

        initialize();

        setup();
    }

    /**
     * Add all detector presentations that should be displayed. Add them to {@link #detectorPresentations}.
     */
    protected void registerDetectorPresentations()
    {
        detectorPresentations.add(new ReferenceDataPresentation());
        detectorPresentations.add(new SummingDetectorPresentation());
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
        frmAisFetureDetectors = new JFrame();
        frmAisFetureDetectors.setTitle("AIS Feature Detectors Presentation");
        frmAisFetureDetectors.setBounds(100, 100, 706, 459);
        frmAisFetureDetectors.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmAisFetureDetectors.getContentPane().setLayout(
                new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormFactory.UNRELATED_GAP_COLSPEC, FormFactory.PREF_COLSPEC, FormFactory.RELATED_GAP_COLSPEC,
                        FormFactory.PREF_COLSPEC, FormFactory.UNRELATED_GAP_COLSPEC, FormFactory.PREF_COLSPEC,
                        FormFactory.RELATED_GAP_COLSPEC, FormFactory.PREF_COLSPEC, FormFactory.UNRELATED_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC, FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
                        FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("default:grow"), }));

        fileInput = new JFileInput();
        fileInput.setToolTipText("Select the base folder containing the folders with orbit data");
        frmAisFetureDetectors.getContentPane().add(fileInput, "2, 2");

        lblStartOrbit = new JLabel("Start orbit");
        frmAisFetureDetectors.getContentPane().add(lblStartOrbit, "4, 2, right, default");

        startOrbit = new JComboBox<>();
        startOrbit.setEnabled(false);
        frmAisFetureDetectors.getContentPane().add(startOrbit, "6, 2, fill, default");

        lblEndOrbit = new JLabel("End orbit");
        frmAisFetureDetectors.getContentPane().add(lblEndOrbit, "8, 2, right, default");

        endOrbit = new JComboBox<>();
        endOrbit.setEnabled(false);
        frmAisFetureDetectors.getContentPane().add(endOrbit, "10, 2, fill, default");

        btnDetect = new JButton("Detect!");
        btnDetect.setEnabled(false);
        frmAisFetureDetectors.getContentPane().add(btnDetect, "12, 2");

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        frmAisFetureDetectors.getContentPane().add(tabbedPane, "2, 4, 11, 1, fill, fill");

        for (DetectorPresentation<?> detector : detectorPresentations) {
            tabbedPane.addTab(detector.getTabTitle(), detector);
        }
    }

    /**
     * Setup all the components.
     */
    private void setup()
    {
        fileInput.getFileChooser().setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        fileInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                fileInput.setEnabled(false);
                baseFolderChangeInProgress = true;

                new SwingWorker<Vector<Integer>, Void>() {
                    @Override
                    protected Vector<Integer> doInBackground() throws Exception
                    {
                        final File selectedDir = new File(fileInput.getPath());

                        // check if the base dir exists
                        if (!selectedDir.exists() || !selectedDir.isDirectory()) {
                            throw new IllegalArgumentException(
                                    "Select a directory containing orbit data directories (like RDR123X or 123X containing .LBL files)");
                        }

                        final File[] orbitDirs = selectedDir.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File pathname)
                            {
                                return pathname.exists() && pathname.isDirectory()
                                        && pathname.getName().matches("(RDR)?[0-9]{3,4}X");
                            }
                        });

                        // check if the base dir contains subdirs 123X or RDR123X
                        if (orbitDirs.length == 0) {
                            throw new IllegalArgumentException(
                                    "Select a directory containing orbit data directories (like RDR123X or 123X containing .LBL files)");
                        }

                        // find the orbit records located in the orbit dirs
                        final Vector<Integer> orbits = new Vector<>(orbitDirs.length * 5);
                        orbitNumToOrbitFile.clear();
                        for (File orbitDir : orbitDirs) {
                            final File[] orbitFiles = orbitDir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File pathname)
                                {
                                    return pathname.isFile() && pathname.getName().toLowerCase().endsWith(".lbl");
                                }
                            });

                            if (orbitFiles.length == 0)
                                System.err.println("Orbit directory " + orbitDir + " contains no .LBL files.");

                            for (File orbitFile : orbitFiles) {
                                final String orbitNumber = orbitFile.getName().toLowerCase()
                                        .replace("frm_ais_rdr_", "").replace(".lbl", "");
                                final int number = Integer.parseInt(orbitNumber);
                                orbits.add(number);
                                orbitNumToOrbitFile.put(number, orbitFile);
                            }
                        }

                        // if no orbit record has been found, it's an error
                        if (orbits.size() == 0) {
                            throw new IllegalArgumentException("No orbit data subdirectory contains a .LBL file.");
                        }

                        // the selected base folder satisfies everything we need now

                        baseFolder = selectedDir;
                        props.setProperty("baseFolder", baseFolder.getAbsolutePath());

                        Collections.sort(orbits);
                        availableOrbitNumbers = orbits;

                        return availableOrbitNumbers;
                    }

                    @Override
                    protected void done()
                    {
                        try {
                            final Vector<Integer> orbits = get();
                            startOrbit.setModel(new DefaultComboBoxModel<>(orbits));
                            startOrbit.setSelectedIndex(0);

                            baseFolderChangeInProgress = false;
                            for (Runnable r : performAfterFolderChangeTasks)
                                r.run();
                            performAfterFolderChangeTasks.clear();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            baseFolder = null;
                            availableOrbitNumbers = null;
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(frmAisFetureDetectors, e.getMessage());
                        }
                        updateComponentStates();
                    }

                }.execute();
            }
        });

        startOrbit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (availableOrbitNumbers == null)
                    return;

                final Integer start = (Integer) startOrbit.getSelectedItem();
                final Integer end = (Integer) endOrbit.getSelectedItem();

                int newSelectedIndex = -1;
                final Vector<Integer> orbits = new Vector<>();
                int i = 0;
                for (Integer orbit : availableOrbitNumbers) {
                    if (orbit >= start) {
                        orbits.add(orbit);
                        if (orbit == end)
                            newSelectedIndex = i;
                        i++;
                    }
                }

                endOrbit.setModel(new DefaultComboBoxModel<>(orbits));
                if (newSelectedIndex != -1)
                    endOrbit.setSelectedIndex(newSelectedIndex);
            }
        });

        startOrbit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                props.setProperty("startOrbit", startOrbit.getSelectedItem().toString());
            }
        });
        endOrbit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                props.setProperty("endOrbit", endOrbit.getSelectedItem().toString());
            }
        });

        btnDetect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                detectionInProgress = true;
                updateComponentStates();

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception
                    {
                        final Integer start = (Integer) startOrbit.getSelectedItem();
                        final Integer end = (Integer) endOrbit.getSelectedItem();
                        if (start == null || end == null)
                            return null;

                        final Collection<File> files = orbitNumToOrbitFile.subMap(start, end + 1).values();
                        detectorPresentations.get(tabbedPane.getSelectedIndex()).detectFeatures(files);

                        return null;
                    }

                    @Override
                    protected void done()
                    {
                        detectionInProgress = false;
                        updateComponentStates();
                    }

                }.execute();
            }
        });

        frmAisFetureDetectors.addWindowListener(new WindowAdapter() {
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

        final Integer startOrbit = Integer.parseInt(props.getProperty("startOrbit"));
        final Integer endOrbit = Integer.parseInt(props.getProperty("endOrbit"));

        final String baseFolder = props.getProperty("baseFolder");
        if (baseFolder != null)
            fileInput.setPath(baseFolder);

        executeAfterBaseFolderChange(new Runnable() {
            @Override
            public void run()
            {
                if (startOrbit != null)
                    DetectionFrame.this.startOrbit.setSelectedItem(startOrbit);

                if (endOrbit != null)
                    DetectionFrame.this.endOrbit.setSelectedItem(endOrbit);
            }
        });
    }

    /**
     * Called to instruct all the components that a component changed its state and they should reflect that.
     */
    private void updateComponentStates()
    {
        fileInput.setEnabled(!detectionInProgress);

        startOrbit.setEnabled(baseFolder != null && !detectionInProgress);
        endOrbit.setEnabled(baseFolder != null && !detectionInProgress);

        btnDetect.setEnabled(baseFolder != null && !detectionInProgress && startOrbit.getSelectedIndex() != -1
                && endOrbit.getSelectedIndex() != -1);

        if (tabbedPane.getSelectedIndex() != -1) {
            detectorPresentations.get(tabbedPane.getSelectedIndex()).updateComponentStates();
        }
    }

    /**
     * Execute the given task after a base directory chage finishes (or immediately if it doesn't happen at the time).
     * 
     * @param task The task to perform.
     */
    private void executeAfterBaseFolderChange(Runnable task)
    {
        if (!baseFolderChangeInProgress)
            task.run();
        else
            performAfterFolderChangeTasks.add(task);
    }
}
