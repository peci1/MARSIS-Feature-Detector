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
package cz.cuni.mff.peckam.ais.detection;

/**
 * A repeating horizontal or vertical line.
 * 
 * @author Martin Pecka
 */
public abstract class RepeatingLine extends DetectedFeatureBase
{

    /** The direction of the lines. */
    private final Direction direction;

    /** Offset of the first repeat along the axis perpendicular to {@link #direction}. */
    private final int       offset;

    /** The repetition period. */
    private final double    period;

    /** Offset of the start along the axis parallel to {@link #direction}. */
    private final int       start;

    /** Offset of the end along the axis parallel to {@link #direction}. */
    private final int       end;

    /**
     * @param direction The direction of the lines.
     * @param offset Offset of the first repeat along the axis perpendicular to {@link #direction}.
     * @param period The repetition period.
     * @param start Offset of the start along the axis parallel to {@link #direction}.
     * @param end Offset of the end along the axis parallel to {@link #direction}.
     */
    public RepeatingLine(Direction direction, int offset, double period, int start, int end)
    {
        this.direction = direction;
        this.offset = offset;
        this.period = period;
        this.start = start;
        this.end = end;

        addType(direction.getType());
    }

    /**
     * @return The direction of the lines.
     */
    public Direction getDirection()
    {
        return direction;
    }

    /**
     * @return Offset of the first repeat along the axis perpendicular to {@link #direction}.
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * @return The repetition period.
     */
    public double getPeriod()
    {
        return period;
    }

    /**
     * @return Offset of the start along the axis parallel to {@link #getDirection()}.
     */
    public int getStart()
    {
        return start;
    }

    /**
     * @return Offset of the end along the axis parallel to {@link #getDirection()}.
     */
    public int getEnd()
    {
        return end;
    }

    /**
     * Direction of the line.
     * 
     * @author Martin Pecka
     */
    public enum Direction
    {
        /** Horizontal line. */
        HORIZONTAL
        {
            @Override
            protected String getType()
            {
                return DetectedFeature.TYPE_REPEATING_HORIZONTAL_LINE;
            }
        },
        /** Vertical line. */
        VERTICAL
        {
            @Override
            protected String getType()
            {
                return DetectedFeature.TYPE_REPEATING_VERTICAL_LINE;
            }
        };

        /**
         * @return The feature type corresponding to the direction.
         */
        protected abstract String getType();
    }

}
