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
package cz.cuni.mff.peckam.ais;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cz.cuni.mff.peckam.ais.result.Orbit;
import cz.cuni.mff.peckam.ais.result.ResultReader;

/**
 * Type of product overlay.
 * 
 * @author Martin Pecka
 */
public abstract class ProductOverlayType
{
    /** The result reader. */
    protected final ResultReader reader = new ResultReader();

    /** Cache. */
    private final Map<Integer, Orbit> cache  = new HashMap<>();

    /**
     * Return the detection result for this type.
     * 
     * @param baseFolder The base folder with orbit data.
     * @param orbit The orbit to get results for.
     * @return The result.
     */
    public Orbit getResult(File baseFolder, int orbit)
    {
        final Orbit cached = cache.get(orbit);
        if (cached != null)
            return cached;
        return getResultImpl(baseFolder, orbit);
    }

    /**
     * Return the detection result for this type.
     * 
     * @param baseFolder The base folder with orbit data.
     * @param orbit The orbit to get results for.
     * @return The result.
     */
    protected abstract Orbit getResultImpl(File baseFolder, int orbit);
    
    /**
     * Auto-detected overlay.
     * 
     * @author Martin Pecka
     */
    public interface Automatic
    {

    }

    /**
     * Manual results.
     * 
     * @author Martin Pecka
     */
    public static class Manual extends ProductOverlayType
    {

        @Override
        public Orbit getResultImpl(File baseFolder, int orbit)
        {
            try {
                return reader.readResult(new File(baseFolder, "TRACE_" + orbit + ".XML"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String toString()
        {
            return "Manual results";
        }
    }

    /**
     * Row/col sums w/ periodogram.
     * 
     * @author Martin Pecka
     */
    public static class SumsPeriodogram extends ProductOverlayType implements Automatic
    {

        @Override
        public Orbit getResultImpl(File baseFolder, int orbit)
        {
            try {
                return reader.readResult(new File(baseFolder, "TRACE_" + orbit + "_SUM_PERIODOGRAM.XML"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String toString()
        {
            return "Row/column sums+periodogram";
        }
    }

    /**
     * Row/col sums w/ curve fitting.
     * 
     * @author Martin Pecka
     */
    public static class SumsFitting extends ProductOverlayType implements Automatic
    {

        @Override
        public Orbit getResultImpl(File baseFolder, int orbit)
        {
            try {
                return reader.readResult(new File(baseFolder, "TRACE_" + orbit + "_SUM_FITTING.XML"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String toString()
        {
            return "Row/column sums+curve fitting";
        }
    }

    /**
     * Row/col sums w/ quantile filtering.
     * 
     * @author Martin Pecka
     */
    public static class SumsQuantile extends ProductOverlayType implements Automatic
    {

        @Override
        public Orbit getResultImpl(File baseFolder, int orbit)
        {
            try {
                return reader.readResult(new File(baseFolder, "TRACE_" + orbit + "_SUM_QUANTILE.XML"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String toString()
        {
            return "Row/column sums+quantile";
        }
    }
}
