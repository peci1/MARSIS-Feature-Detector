/*
 * License for Java 1.5 'Tiger': A Developer's Notebook
 * (O'Reilly) example package
 * 
 * Java 1.5 'Tiger': A Developer's Notebook (O'Reilly)
 * by Brett McLaughlin and David Flanagan.
 * ISBN: 0-596-00738-8
 * 
 * You can use the examples and the source code any way you want, but
 * please include a reference to where it comes from if you use it in
 * your own products or services. Also note that this software is
 * provided by the author "as is", with no expressed or implied warranties.
 * In no event shall the author be liable for any direct or indirect
 * damages arising in any way out of the use of this software.
 * 
 * Taken from: http://www.java2s.com/Code/Java/Language-Basics/Javaforinforinlinebylineiterationthroughatextfile.htm
 */
package cz.cuni.mff.peckam.ais.result;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

/**
 * This class allows line-by-line iteration through a text file.
 * The iterator's remove() method throws UnsupportedOperationException.
 * The iterator wraps and rethrows IOExceptions as IllegalArgumentExceptions.
 */
class TextFile implements Iterable<String>
{

    /** Used by the TextFileIterator class below */
    final Reader reader;


    /**
     * @param filename The file to open.
     * @throws FileNotFoundException If the file is not found.
     */
    public TextFile(String filename) throws FileNotFoundException
    {
        this.reader = new FileReader(filename);
    }

    /**
     * @param file The file to open.
     * @throws FileNotFoundException If the file is not found.
     */
    public TextFile(File file) throws FileNotFoundException
    {
        this.reader = new FileReader(file);
    }

    /**
     * @param reader The reader to read from.
     */
    public TextFile(Reader reader)
    {
        this.reader = reader;
    }

    /**
     * @param stream The stream to read from.
     */
    public TextFile(InputStream stream)
    {
        this.reader = new InputStreamReader(stream);
    }

    // This is the one method of the Iterable interface
    @Override
    public Iterator<String> iterator()
    {
        return new TextFileIterator();
    }

    /**
     * This non-static member class is the iterator implementation
     */
    class TextFileIterator implements Iterator<String>
    {

        /** The stream we're reading from */
        BufferedReader in;

        /** Return value of next call to next() */
        String         nextline;

        /**
         * Create the iterator.
         */
        public TextFileIterator()
        {
            // Open the file and read and remember the first line.
            // We peek ahead like this for the benefit of hasNext().
            try {
                in = new BufferedReader(reader);
                nextline = in.readLine();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public boolean hasNext()
        {
            return nextline != null;
        }

        @Override
        public String next()
        {
            try {
                String result = nextline;

                // If we haven't reached EOF yet
                if (nextline != null) {
                    nextline = in.readLine(); // Read another line
                    if (nextline == null)
                        in.close(); // And close on EOF
                }

                // Return the line we read last time through.
                return result;
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        // The file is read-only; we don't allow lines to be removed.
        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}