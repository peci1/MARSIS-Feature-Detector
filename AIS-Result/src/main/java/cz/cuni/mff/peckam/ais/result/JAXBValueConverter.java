/**  */
package cz.cuni.mff.peckam.ais.result;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Converts values between textual (XML) and Java representation.
 * 
 * @author Martin Pecka
 */
public class JAXBValueConverter
{
    private static final DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static String printDateTime(DateTime time)
    {
        return time.toString(dateTimeFormat);
    }

    public static String printFloatThreeDecimals(Float number)
    {
        return String.format(Locale.ENGLISH, "%.3f", number);
    }

}
