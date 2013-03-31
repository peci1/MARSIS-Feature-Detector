/**  */
package cz.cuni.mff.peckam.ais.result;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

/**
 * Writer for AIS detection results.
 * 
 * @author Martin Pecka
 */
public class ResultWriter
{
    /** The result to write. */
    private final Orbit result;

    /**
     * @param result The result to write.
     */
    public ResultWriter(Orbit result)
    {
        this.result = result;
    }

    /**
     * Write the result as XML to the given stream.
     * 
     * @param stream The stream to write to. It won't be closed after success.
     * @throws IOException If writing fails.
     */
    public void writeXML(OutputStream stream) throws IOException
    {
        try {
            final JAXBContext context = JAXBContext.newInstance("cz.cuni.mff.peckam.ais.result", getClass()
                    .getClassLoader());

            final Marshaller m = context.createMarshaller();
            // enable indenting and newline generation
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            XMLStreamWriter writer;
            try {
                writer = new IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(stream));
                // in order not to write the namespace to the file
                writer.setDefaultNamespace("http://www.mff.cuni.cz/~peckam/java/ais-detection-result");
            } catch (XMLStreamException e) {
                throw new IOException(e);
            }

            // do the Java class->XML conversion
            m.marshal(result, writer);
        } catch (JAXBException e) {
            throw new IOException("Error in Object->XML conversion", e);
        }
    }
}
