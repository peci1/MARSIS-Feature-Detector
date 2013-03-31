/**  */
package cz.cuni.mff.peckam.ais.result;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.joda.time.DateTime;

/**
 * A builder for AIS detection results.
 *
 * @author Martin Pecka
 */
public class ResultBuilder
{
    private final Orbit                result;
    private final static ObjectFactory factory = new ObjectFactory();
    
    public ResultBuilder(int orbitId)
    {
        this.result = factory.createOrbit();
        result.setId(orbitId);
    }
    
    /**
     * Add a frame to the result.
     * <p>
     * Pass <code>null</code> to any argument that has to remain empty or unknown.
     * 
     * @param time Start time of the frame. Cannot be <code>null</code>.
     * @param hPeriod The horiz. period.
     * @param hPeriodQuality The horiz. period's quality.
     * @param vPeriod The vert. period.
     * @param cutoff The cutoff frequency.
     * @param ionosphereTrace The ionosphere trace.
     * @param groundTrace The ground trace.
     * 
     * @return Itself.
     */
    public ResultBuilder addFrame(DateTime time, Float hPeriod, Integer hPeriodQuality, Float vPeriod, Float cutoff,
            LinkedHashMap<Float, Float> ionosphereTrace, LinkedHashMap<Float, Float> groundTrace)
    {
        if (time == null)
            throw new NullPointerException("time cannot be null");

        final FrameType frame = factory.createFrameType();
        result.getFrames().add(frame);

        frame.setTime(time);
        frame.setHperiod(hPeriod);
        frame.setHperiodquality(hPeriodQuality);
        frame.setVperiod(vPeriod);
        frame.setCutoff(cutoff);

        final TraceType iTrace = factory.createTraceType();
        frame.setIonospheretrace(iTrace);

        if (ionosphereTrace != null) {
            for (Entry<Float, Float> entry : ionosphereTrace.entrySet()) {
                PointType point = factory.createPointType();
                point.setX(entry.getKey());
                point.setY(entry.getValue());
                iTrace.getPoints().add(point);
            }
        }

        final TraceType gTrace = factory.createTraceType();
        frame.setGroundtrace(gTrace);

        if (groundTrace != null) {
            for (Entry<Float, Float> entry : groundTrace.entrySet()) {
                PointType point = factory.createPointType();
                point.setX(entry.getKey());
                point.setY(entry.getValue());
                gTrace.getPoints().add(point);
            }
        }

        return this;
    }

    /**
     * @return The AIS detection result.
     */
    public Orbit getResult()
    {
        return result;
    }

}
