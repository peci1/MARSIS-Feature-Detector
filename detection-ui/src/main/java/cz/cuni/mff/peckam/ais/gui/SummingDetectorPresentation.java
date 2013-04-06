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

import cz.cuni.mff.peckam.ais.detection.SummingDetector;
import cz.cuni.mff.peckam.ais.detection.SummingDetector.ComputationStrategy;

/**
 * Presentation for the {@link SummingDetector}.
 * 
 * @author Martin Pecka
 */
public class SummingDetectorPresentation extends DetectorPresentation<SummingDetector>
{

    /**  */
    private static final long serialVersionUID = -2472019239328249370L;

    /**
     * Create the presentation of the summing detector using the given strategy.
     * 
     * @param strategy The computation strategy to use.
     */
    public SummingDetectorPresentation(ComputationStrategy strategy)
    {
        getDetector().setStrategy(strategy);
    }

    @Override
    public void updateComponentStates()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getTabTitle()
    {
        return "Row/col summing (" + getDetector().getStrategy() + ")";
    }

    @Override
    protected SummingDetector createDetector()
    {
        return new SummingDetector();
    }

    @Override
    protected String getResultFileSuffix()
    {
        return "SUM_" + getDetector().getStrategy().toString().toUpperCase();
    }

}
