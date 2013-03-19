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

import org.joda.time.DateTime;

/**
 * Active Ionospheric Sounding data envelope.
 * 
 * @author Martin Pecka
 */
public class AISProduct implements Product<Float>
{
    /**  */
    private DateTime spaceCraftClock;
    /**  */
    private byte     processId;
    /**  */
    private byte     instrumentDataType;
    /**  */
    private byte     instrumentSelectionMode;
    /**  */
    private byte     transmitPowerLevel;
    /**  */
    private byte     frequencyTableNumber;
    /**  */
    private int      frequencyNumber;
    /**  */
    private byte     bandNumber;
    /**  */
    private byte     receiverAttenuation;
    /**  */
    private float    frequency;
    /**  */
    private Float[][] spectralDensity;

    /**
     * @param spaceCraftClock Capture time.
     * @param processId Process ID.
     * @param instrumentDataType Instrument data type.
     * @param instrumentSelectionMode Instrument selection mode.
     * @param transmitPowerLevel Transmit power level.
     * @param frequencyTableNumber Frequency table number.
     * @param frequencyNumber Frequency number.
     * @param bandNumber Band number.
     * @param receiverAttenuation Receiver attenuation.
     * @param frequency Frequency.
     * @param spectralDensity The data
     */
    AISProduct(DateTime spaceCraftClock, byte processId, byte instrumentDataType, byte instrumentSelectionMode,
            byte transmitPowerLevel, byte frequencyTableNumber, int frequencyNumber, byte bandNumber,
            byte receiverAttenuation, float frequency, Float[][] spectralDensity)
    {
        this.spaceCraftClock = spaceCraftClock;
        this.processId = processId;
        this.instrumentDataType = instrumentDataType;
        this.instrumentSelectionMode = instrumentSelectionMode;
        this.transmitPowerLevel = transmitPowerLevel;
        this.frequencyTableNumber = frequencyTableNumber;
        this.frequencyNumber = frequencyNumber;
        this.bandNumber = bandNumber;
        this.receiverAttenuation = receiverAttenuation;
        this.frequency = frequency;
        this.spectralDensity = spectralDensity;
    }

    /**
     * Spacecraft clock measured counter since the epoch of May 3, 2003 (123).
     * 
     * @return The spacecraft clock value.
     */
    public DateTime getSpaceCraftClock()
    {
        return spaceCraftClock;
    }

    /**
     * The seven bits from the 20,3 telemetry packet header which determine the instrument process id.
     * 
     * 0x4D (77d) = Subsurface Sounder (SS1 to SS4)
     * 0x4E (78d) = Active Ionospheric Sounder (AIS)
     * 0x4F (79d) = Calibration (CAL)
     * 0x50 (80d) = Receive Only (RCV)
     * 
     * @return The instrument process.
     * 
     * @throws IllegalStateException If process ID doesn't correspond to one of the defined values.
     */
    public InstrumentProcessId getProcessId() throws IllegalStateException
    {
        switch (processId) {
            case 0x4D:
                return InstrumentProcessId.SUBSURFACE_SOUNDER;
            case 0x4E:
                return InstrumentProcessId.ACTIVE_IONOSPHERIC_SOUNDER;
            case 0x4F:
                return InstrumentProcessId.CALIBRATION;
            case 0x50:
                return InstrumentProcessId.RECEIVE_ONLY;
        }
        throw new IllegalStateException("Process ID has an unexpected value: " + processId + ". Expected 77 to 80.");
    }

    /**
     * The data type of the instrument.
     * 
     * 0001 = AIS, Calibration, or Receive Only
     * 0000 = SS1-SS5 Individual Echoes
     * 0010 = SS1-SS5 Acquisition
     * 0011 = SS1-SS5 Tracking
     * 
     * @return The data type of the instrument.
     * @throws IllegalStateException If instrument data type doesn't correspond to one of the defined values.
     */
    public InstrumentDataType getInstrumentDataType() throws IllegalStateException
    {
        final int value = instrumentDataType >> 4;
        switch (value) {
            case 0b01:
                return InstrumentDataType.AIS_CAL_RCV;
            case 0b00:
                return InstrumentDataType.INDIVIDUAL_ECHOES;
            case 0b10:
                return InstrumentDataType.ACQUISITION;
            case 0b11:
                return InstrumentDataType.TRACKING;
        }
        throw new IllegalStateException("Instrument data type has an unexpected value : " + value
                + ". Expected 0 to 3.");
    }

    /**
     * The selection mode of the instrument.
     * 
     * 0101 = Calibration
     * 0110 = Receive Only
     * 0111 = Active Ionospheric Sounder
     * 1000 = Subsurface Sounder 1
     * 1001 = Subsurface Sounder 2
     * 1010 = Subsurface Sounder 3
     * 1011 = Subsurface Sounder 4
     * 1100 = Subsurface Sounder 5
     * 
     * @return The selection mode of the instrument.
     * @throws IllegalStateException If instrument selection mode doesn't correspond to one of the defined values.
     */
    public InstrumentSelectionMode getInstrumentSelectionMode() throws IllegalStateException
    {
        final int value = instrumentSelectionMode & 0x0f;
        switch (value) {
            case 0b0101:
                return InstrumentSelectionMode.CALIBRATION;
            case 0b0110:
                return InstrumentSelectionMode.RECEIVE_ONLY;
            case 0b0111:
                return InstrumentSelectionMode.ACTIVE_IONOSPHERIC_SOUNDER;
            case 0b1000:
                return InstrumentSelectionMode.SUBSURFACE_SOUNDER_1;
            case 0b1001:
                return InstrumentSelectionMode.SUBSURFACE_SOUNDER_2;
            case 0b1010:
                return InstrumentSelectionMode.SUBSURFACE_SOUNDER_3;
            case 0b1011:
                return InstrumentSelectionMode.SUBSURFACE_SOUNDER_4;
            case 0b1100:
                return InstrumentSelectionMode.SUBSURFACE_SOUNDER_5;
        }
        throw new IllegalStateException("Instrument selection mode has an unexpected value : " + value + ". Expected "
                + 0b0101 + " to " + 0b1100 + ".");
    }

    /**
     * The transmit power level, expressed as the power supply regulation voltage for the final power amplifier output.
     * 
     * 0x00 (0d) = minimum transmit power 2.5V
     * 0x0F (15d) = maximum transmit power 40.0V
     * 
     * @return The level of the transmit power.
     */
    public byte getTransmitPowerLevel()
    {
        return transmitPowerLevel;
    }

    /**
     * The transmit power, expressed as the power supply regulation voltage for the final power amplifier output.
     * 
     * 0x00 (0d) = minimum transmit power 2.5V
     * 0x0F (15d) = maximum transmit power 40.0V
     * 
     * @return The transmit power in V.
     */
    public float getTransmitPower()
    {
        return 2.5f + transmitPowerLevel / 16f * (40f - 2.5f);
    }

    /**
     * "The Active Ionospheric Sounder may select one of sixteen frequency tables to use during transmit. Each table has
     * 160 frequencies that are transmitted during an AIS capture. Table 0 is the default table.
     * 
     * @return The number of the used frequency table.
     */
    public byte getFrequencyTableNumber()
    {
        return frequencyTableNumber;
    }

    /**
     * The frequency number from the table, ranging from 0 to 159.
     * 
     * @return The number of the frequence in the frequence table obtained from {@link #getFrequencyTableNumber()}.
     */
    public int getFrequencyNumber()
    {
        return frequencyNumber;
    }

    /**
     * The band that was selected to receive the echo.
     * 0 = band 0
     * 1 = band 1
     * 2 = band 2
     * 3 = band 3
     * 4 = band 4
     * 
     * @return The band number.
     */
    public byte getBandNumber()
    {
        return bandNumber;
    }

    /**
     * The receiver attenuation for band selected measured in dB.
     * 
     * switch(arRecord[63]&0x07){
     * case 0x00: nRxAttn= 2; break;
     * case 0x01: nRxAttn= 6; break;
     * case 0x02: nRxAttn=10; break;
     * case 0x03: nRxAttn=14; break;
     * case 0x04: nRxAttn=18; break;
     * case 0x05: nRxAttn=22; break;
     * case 0x06: nRxAttn=26; break;
     * case 0x07: nRxAttn=30; break;
     * default: nRxAttn=50; break;
     * }
     * 
     * @return The attenuation in dB.
     */
    public byte getReceiverAttenuation()
    {
        final int value = receiverAttenuation & 0x07;
        switch (value) {
            case 0x00:
                return 2;
            case 0x01:
                return 6;
            case 0x02:
                return 10;
            case 0x03:
                return 14;
            case 0x04:
                return 18;
            case 0x05:
                return 22;
            case 0x06:
                return 26;
            case 0x07:
                return 30;
            default:
                return 50;
        }
    }

    /**
     * The frequency of the transmit pulse.
     * 
     * @return The frequency in Hz.
     */
    public float getFrequency()
    {
        return frequency;
    }

    /**
     * A series of calibrated spectral densities from a single transmit pulse.
     * 
     * @return The spectral density array (80 elements) in "VOLT**2/M**2/HZ.
     */
    public Float[][] getSpectralDensity()
    {
        return spectralDensity;
    }

    @Override
    public Float[][] getData()
    {
        return getSpectralDensity();
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder("AIS record for time ").append(getSpaceCraftClock()).append(
                " : \n");
        builder.append("\tTransmit frequency: ").append(getFrequency()).append("Hz (table nr. ")
                .append(getFrequencyTableNumber()).append(", freq nr. ").append(getFrequencyNumber()).append(")\n");
        builder.append("\tBand number: ").append(getBandNumber()).append("\n");
        builder.append("\tReceiver attenuation: ").append(getReceiverAttenuation()).append(" dB\n");
        builder.append("\tTransmit Power level: ").append(getTransmitPowerLevel()).append(" (~")
                .append(getTransmitPower()).append(" V)\n");

        builder.append("\tInstrument data type: ").append(getInstrumentDataType()).append("\n");
        builder.append("\tInstrument selection mode: ").append(getInstrumentSelectionMode()).append("\n");

        builder.append("\tData: ");
        for (float density : spectralDensity[0]) {
            builder.append(density).append(" ");
        }

        return builder.append("\n").toString();
    }

    @Override
    public int getWidth()
    {
        return 1;
    }

    @Override
    public int getHeight()
    {
        return spectralDensity[0].length;
    }

    @Override
    public String getMetadataString()
    {
        return this.toString();
    }

}