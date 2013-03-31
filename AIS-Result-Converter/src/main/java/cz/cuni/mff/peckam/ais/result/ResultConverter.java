package cz.cuni.mff.peckam.ais.result;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import cz.cuni.mff.peckam.ais.AISLBLProductReader;
import cz.cuni.mff.peckam.ais.Ionogram;

/**  */

/**
 * Converts multiple result files into combined XML result files.
 * 
 * @author Martin Pecka
 */
public class ResultConverter
{

    /** base folder */
    private final File        dataBaseFolder;
    /** hPeriod file */
    private final InputStream hPeriodFile;
    /** ionosphere trace base folder */
    private final File        ionosphereTraceBaseFolder;
    /** min orbit to process */
    private final int         minOrbit;
    /** max orbit to process */
    private final int         maxOrbit;

    /**
     * @param dataBaseFolder base folder
     * @param hPeriodFile hPeriod file
     * @param ionosphereTraceBaseFolder ionosphere trace base folder
     * @param minOrbit min orbit to process
     * @param maxOrbit max orbit to process
     */
    public ResultConverter(File dataBaseFolder, InputStream hPeriodFile, File ionosphereTraceBaseFolder, int minOrbit,
            int maxOrbit)
    {
        this.dataBaseFolder = dataBaseFolder;
        this.hPeriodFile = hPeriodFile;
        this.ionosphereTraceBaseFolder = ionosphereTraceBaseFolder;
        this.minOrbit = minOrbit;
        this.maxOrbit = maxOrbit;
    }

    /**
     * Convert the results.
     * 
     * @throws IOException If IO error occurs.
     */
    public void convert() throws IOException
    {
        final Map<String, Float> hPeriodData = new TreeMap<>();
        {
            for (String line : new TextFile(hPeriodFile)) {
                final String[] parts = line.split("\\s+");
                float value = Float.parseFloat(parts[1]);
                hPeriodData.put(parts[0], value);
            }
        }
        
        final Iterable<Integer> orbitNums = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator()
            {
                return new DataFilesIterator();
            }
        };

        final AISLBLProductReader productReader = new AISLBLProductReader();
        final DateTimeFormatter hPeriodDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        final DateTimeFormatter traceDateFormat = DateTimeFormat.forPattern("yyyyMMdd'_'HHmmss");

        int usedTraceFilesSum = 0;
        for (final int orbitNum : orbitNums) {
            final ResultBuilder builder = new ResultBuilder(orbitNum);

            final File orbitFile = getOrbitFile(orbitNum);
            final Ionogram[] ionograms = productReader.readFile(orbitFile);

            int usedTraceFiles = 0;
            for (Ionogram ionogram : ionograms) {
                final DateTime time = ionogram.getStartTime();
                final String timeString = time.toString(hPeriodDateFormat);
                
                final Float hPeriod = hPeriodData.get(timeString);
                final Integer hPeriodQuality = (hPeriod != null) ? 0 : null;

                final LinkedHashMap<Float, Float> ionoTrace = new LinkedHashMap<>();
                final File traceFile = new File(ionosphereTraceBaseFolder, time.toString(traceDateFormat) + ".txt");
                if (traceFile.exists()) {
                    usedTraceFiles++;
                    boolean firstLine = true;
                    for (String line : new TextFile(traceFile)) {
                        // ignore the first line
                        if (firstLine) {
                            firstLine = false;
                            continue;
                        }
                        if (!line.trim().isEmpty()) {
                            final String[] parts = line.split("\\s+");
                            try {
                                final float freq = Float.parseFloat(parts[0]);
                                final float delay = Float.parseFloat(parts[1]);
                                ionoTrace.put(freq, delay);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid float at file " + traceFile.getName());
                                throw e;
                            }
                        }
                    }
                }

                builder.addFrame(time, hPeriod, hPeriodQuality, null, null, ionoTrace, null);
            }

            final Orbit result = builder.getResult();

            usedTraceFilesSum += usedTraceFiles;
            if (!result.getFrames().isEmpty()) {
                final ResultWriter writer = new ResultWriter(result);
                final File resultFile = new File(orbitFile.getParentFile(), "TRACE_" + orbitNum + ".XML");
                try (final OutputStream outputStream = new FileOutputStream(resultFile)) {
                    writer.writeXML(outputStream);
                    System.out.println("Written trace for orbit " + orbitNum + ", used " + usedTraceFiles
                            + " trace files, in sum: " + usedTraceFilesSum);
                }
            }
        }
    }

    /**
     * Iterates over the ionogram sets between the min and max orbit number.
     * 
     * @author Martin Pecka
     */
    private class DataFilesIterator implements Iterator<Integer>
    {
        /** The current orbit number. */
        private int currentOrbit = minOrbit;

        @Override
        public boolean hasNext()
        {
            return getNextOrbitNum() != null;
        }

        @Override
        public Integer next()
        {
            final Integer nextOrbit = getNextOrbitNum();
            if (nextOrbit == null)
                throw new NoSuchElementException();

            currentOrbit = nextOrbit;
            return currentOrbit;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Return the number of the orbit next to the current orbit.
         * 
         * @return The succeeding orbit or <code>null</code> if no such exists.
         */
        private Integer getNextOrbitNum()
        {
            for (int i = currentOrbit + 1; i <= maxOrbit; i++) {
                if (getOrbitFile(i).exists())
                    return i;
            }
            return null;
        }

    }

    /**
     * Return the file (possibly non-existant) corresponding to the given orbit.
     * 
     * @param orbit The orbit number.
     * @return File with the orbit's data.
     */
    private File getOrbitFile(int orbit)
    {
        int orbitDirNum = (int) Math.floor(orbit / 10.0);
        return new File(dataBaseFolder, orbitDirNum + "X/FRM_AIS_RDR_" + orbit + ".LBL");
    }

    /**
     * @param args 0 =&gt; base folder, 1 =&gt; hPeriod file, 2 =&gt; ionosphere trace base folder, 3 =&gt; min orbit to
     *            process, 4 =&gt; max orbit to process
     * @throws IOException On IO error while converting or if hPeriod file is not found.
     */
    public static void main(String[] args) throws IOException
    {
        final File baseFolder = new File(args[0]);
        final File ionoBaseFolder = new File(args[2]);
        final int minOrbit = Integer.parseInt(args[3]);
        final int maxOrbit = Integer.parseInt(args[4]);

        try (final InputStream hPeriodFile = new FileInputStream(args[1])) {
            new ResultConverter(baseFolder, hPeriodFile, ionoBaseFolder, minOrbit, maxOrbit).convert();
        }
    }

}
