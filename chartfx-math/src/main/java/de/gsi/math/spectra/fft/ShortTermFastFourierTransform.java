package de.gsi.math.spectra.fft;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.spi.DoubleDataSet3D;
import de.gsi.dataset.spi.MultiDimDoubleDataSet;
import de.gsi.math.spectra.Apodization;
import de.gsi.math.spectra.SpectrumTools;

/**
 * @author Alexander Krimm
 */
public class ShortTermFastFourierTransform {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortTermFastFourierTransform.class);

    public static DataSet3D getComplexSpectrogram(final MultiDimDoubleDataSet input, final int nQuantf, int nQuantt,
            final Apodization apodization, final Padding padding) {
        // validate input data
        if (input.getDataCount(DIM_X) - nQuantf < nQuantt - 1) {
            LOGGER.atWarn().addArgument(nQuantt).log("Not enough samples for requested time resolution: {}");
        }

        // set time axis
        double[] timeAxis = new double[nQuantt];
        int nData = (input.getDataCount(DIM_X) - nQuantf);
        nData = nData < 1 ? 1 : nData;
        for (int i = 0; i < nQuantt; i++) {
            timeAxis[i] = input.get(DIM_X, Math.floorDiv(i * nData, nQuantt));
        }

        // set frequency axis
        double fs = input.getDataCount(DIM_X) / (input.get(DIM_X, input.getDataCount(DIM_X) - 1) - input.get(DIM_X, 0));
        double fStep = fs / nQuantf;
        double[] frequencyAxis = new double[nQuantf];
        for (int i = -nQuantf / 2; i < nQuantf / 2; i++) {
            frequencyAxis[i + nQuantf / 2] = i * fStep;
        }

        // set amplitude data
        double[][] amplitudeData = new double[nQuantf][nQuantt];
        double amplitudeMin = Double.POSITIVE_INFINITY;
        double amplitudeMax = Double.NEGATIVE_INFINITY;

        // calculate scalogram
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(nQuantf);
        double[] raw = new double[2 * nQuantf];
        double[] current = new double[nQuantf];
        for (int i = 0; i < nQuantt; i++) {
            for (int j = 0; j < nQuantf; j++) {
                final int index = Math.floorDiv(i * nData, nQuantt) + j;
                if (index >= input.getDataCount(DIM_X)) {
                    switch (padding) {
                    case ZERO:
                        raw[2 * j] = 0;
                        raw[2 * j + 1] = 0;
                        break;
                    case ZOH:
                        raw[2 * j] = input.get(DIM_Y, nData - 1);
                        raw[2 * j + 1] = input.get(DIM_Y + 1, nData - 1);
                        break;
                    case MIRROR:
                        int mirroredIndex = nData - (nData - index) % nData;
                        mirroredIndex = mirroredIndex < 0 ? -mirroredIndex : mirroredIndex;
                        raw[2 * j] = input.get(DIM_Y, mirroredIndex);
                        raw[2 * j + 1] = input.get(DIM_Y + 1, mirroredIndex);
                        break;
                    default:
                    }
                } else {
                    raw[2 * j] = apodization.getIndex(j, nQuantf) * input.get(DIM_Y, index);
                    raw[2 * j + 1] = apodization.getIndex(j, nQuantf) * input.get(DIM_Y + 1, index);
                }
            }
            fastFourierTrafo.complexForward(raw);
            current = SpectrumTools.computeMagnitudeSpectrum_dB(raw, true);
            for (int j = 0; j < nQuantf; j++) {
                amplitudeMin = Math.min(amplitudeMin, current[j]);
                amplitudeMax = Math.max(amplitudeMax, current[j]);
                if (j < nQuantf / 2) {
                    amplitudeData[j + nQuantf / 2][i] = current[j];
                } else {
                    amplitudeData[j - nQuantf / 2][i] = current[j];
                }
            }
        }

        // initialize result dataset
        DoubleDataSet3D result = new DoubleDataSet3D("SFFT(" + input.getName() + ")", timeAxis, frequencyAxis,
                amplitudeData);
        result.getMetaInfo().put("SFFT-nFFT", Integer.toString(nQuantf));
        result.getMetaInfo().put("SFFT-nT", Integer.toString(nQuantt));

        // Set Axis Labels and Units
        final String timeUnit = input.getAxisDescription(DIM_X).getUnit();
        result.getAxisDescription(DIM_X).set("Time", timeUnit, timeAxis[0], timeAxis[timeAxis.length - 1]);
        final String freqUnit = timeUnit.equals("s") ? "Hz" : "1/" + timeUnit;
        result.getAxisDescription(DIM_Y).set("Frequency", freqUnit, frequencyAxis[0],
                frequencyAxis[frequencyAxis.length - 1]);
        result.getAxisDescription(DIM_Z).set("Magnitude", "dB(" + input.getAxisDescription(DIM_Y).getUnit() + ")",
                amplitudeMin, amplitudeMax);
        LOGGER.atInfo().addArgument(result).log("result of complex sfft: {}");
        return result;
    }

    /**
     * Perform a Short term fourier transform
     * 
     * @param input a dataset with equidistantly spaced y(t) data
     * @param nQuantf the number of frequency bins
     * @param overlap the overlap between consecutive ffts
     * @return the spectrogram, a DataSet3D with dimensions [nf = nQuantx x nY = ]
     */
    public static DataSet3D getSpectrogram(final DataSet input, final int nQuantf, final double overlap) {
        int nQuantt = (int) Math.floor(input.getDataCount() / (nQuantf * (1 - overlap)));
        return getSpectrogram(input, nQuantf, nQuantt);
    }

    public static DataSet3D getSpectrogram(final DataSet input, int nQuantf, int nQuantt) {
        return getSpectrogram(input, nQuantf, nQuantt, Apodization.Hann, Padding.ZERO);
    }

    /**
     * Perform a Short term fourier transform
     * 
     * @param input a dataset with equidistantly spaced y(t) data
     * @param nQuantf the number of frequency bins
     * @param nQuantt the number of time bins
     * @param apodization function, by default Hann window is used
     * @param padding how to pad the slices at the start and end of the time axis: ZERO(default), ZOH or MIRROR
     * @return the spectrogram, a DataSet3D with dimensions [nf = nQuantx x nY = ]
     */
    public static DataSet3D getSpectrogram(final DataSet input, final int nQuantf, int nQuantt,
            final Apodization apodization, final Padding padding) {
        // validate input data
        if (input.getDataCount(DIM_X) - nQuantf < nQuantt - 1) {
            LOGGER.atWarn().addArgument(nQuantt).log("Not enough samples for requested time resolution: {}");
        }

        // set time axis
        double[] timeAxis = new double[nQuantt];
        int nData = (input.getDataCount(DIM_X) - nQuantf);
        nData = nData < 1 ? 1 : nData;
        for (int i = 0; i < nQuantt; i++) {
            timeAxis[i] = input.get(DIM_X, Math.floorDiv(i * nData, nQuantt));
        }

        // set frequency axis
        double fs = input.getDataCount(DIM_X) / (input.get(DIM_X, input.getDataCount(DIM_X) - 1) - input.get(DIM_X, 0));
        double fStep = fs / nQuantf;
        double[] frequencyAxis = new double[nQuantf / 2];
        for (int i = 0; i < nQuantf / 2; i++) {
            frequencyAxis[i] = i * fStep;
        }

        // set amplitude data
        double[][] amplitudeData = new double[nQuantf / 2][nQuantt];
        double amplitudeMin = Double.POSITIVE_INFINITY;
        double amplitudeMax = Double.NEGATIVE_INFINITY;


        // calculate scalogram
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(nQuantf);
        double[] raw = new double[nQuantf];
        double[] mean = new double[nQuantf / 2];
        double[] current = new double[nQuantf / 2];
        for (int i = 0; i < nQuantt; i++) {
            for (int j = 0; j < nQuantf; j++) {
                final int index = Math.floorDiv(i * nData, nQuantt) + j;
                if (index >= input.getDataCount(DIM_X)) {
                    switch (padding) {
                    case ZERO:
                        raw[j] = 0;
                        break;
                    case ZOH:
                        raw[j] = input.get(DIM_X, nData - 1);
                        break;
                    case MIRROR:
                        int mirroredIndex = nData - (nData - index) % nData;
                        mirroredIndex = mirroredIndex < 0 ? -mirroredIndex : mirroredIndex;
                        raw[j] = input.get(DIM_Y, mirroredIndex);
                        break;
                    default:
                    }
                } else {
                    raw[j] = apodization.getIndex(j, nQuantf) * input.get(DIM_Y, index);
                }
            }
            fastFourierTrafo.realForward(raw);
            current = SpectrumTools.computeMagnitudeSpectrum_dB(raw, true);
            for (int j = 0; j < nQuantf / 2; j++) {
                amplitudeMin = Math.min(amplitudeMin, current[j]);
                amplitudeMax = Math.max(amplitudeMax, current[j]);
                amplitudeData[j][i] = current[j];
            }
        }

        // initialize result dataset
        DoubleDataSet3D result = new DoubleDataSet3D("SFFT(" + input.getName() + ")", timeAxis, frequencyAxis,
                amplitudeData);
        result.getMetaInfo().put("SFFT-nFFT", Integer.toString(nQuantf));
        result.getMetaInfo().put("SFFT-nT", Integer.toString(nQuantt));

        // Set Axis Labels and Units
        final String timeUnit = input.getAxisDescription(DIM_X).getUnit();
        result.getAxisDescription(DIM_X).set("Time", timeUnit, timeAxis[0], timeAxis[timeAxis.length - 1]);
        final String freqUnit = timeUnit.equals("s") ? "Hz" : "1/" + timeUnit;
        result.getAxisDescription(DIM_Y).set("Frequency", freqUnit, frequencyAxis[0],
                frequencyAxis[frequencyAxis.length - 1]);
        result.getAxisDescription(DIM_Z).set("Magnitude", "dB(" + input.getAxisDescription(DIM_Y).getUnit() + ")",
                amplitudeMin, amplitudeMax);
        LOGGER.atInfo().addArgument(result).log("result of real sfft: {}");
        return result;
    }

    public enum Padding {
        ZERO,
        ZOH,
        MIRROR;
    }
}
