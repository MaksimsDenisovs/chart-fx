package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.EventRateLimiter.UpdateStrategy;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.DoubleDataSet;

/**
 * Basic tests for DataSetMath class
 *
 * @author rstein
 */
public class MathDataSetTests {

    @Test
    public void testConstructorParameterAssertions() {
        final int nBins = 512;
        final DoubleDataSet dsRef1 = generateSineWaveData(nBins);
        final DoubleDataSet dsRef2 = generateSineWaveData(nBins);

        assertThrows(IllegalArgumentException.class,
                () -> new MathDataSet("I", null, null, null, 20, UpdateStrategy.INSTANTANEOUS_RATE));

        assertThrows(IllegalArgumentException.class, () -> new MathDataSet("I", null, null, (array, len) -> array, 20,
                UpdateStrategy.INSTANTANEOUS_RATE, dsRef1, dsRef2));

        assertDoesNotThrow(
                () -> new MathDataSet("I", ds -> ds, null, null, 20, UpdateStrategy.INSTANTANEOUS_RATE, dsRef1));

        assertDoesNotThrow(
                () -> new MathDataSet("I", ds -> ds, null, null, -1, UpdateStrategy.INSTANTANEOUS_RATE, dsRef1));

        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, null, null, 20, null, dsRef1));

        // test specific constructors

        // DataSet -> DataSet
        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, dsRef1));
        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, 20, null, dsRef1));

        // List<DataSet> -> DataSet
        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds.get(0), dsRef1, dsRef2));
        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds.get(0), 20, null, dsRef1, dsRef2));

        // modify only yValues in DataSet
        assertDoesNotThrow(() -> new MathDataSet("I", (yVector, len) -> yVector, dsRef1));
        assertDoesNotThrow(() -> new MathDataSet("I", (yVector, len) -> yVector, 20, null, dsRef1));
    }

    @Test
    public void testDataSetFunction() {
        final int nBins = 512;
        final DoubleDataSet rawDataSetRef = generateSineWaveData(nBins);
        final DoubleDataSet magDataSetRef = generateSineWaveSpectrumData(nBins);
        assertEquals(nBins, rawDataSetRef.getDataCount());

        MathDataSet magDataSet = new MathDataSet("magI", dataSets -> {
            assertEquals(nBins, dataSets.getDataCount());
            return DataSetMath.magnitudeSpectrumDecibel(dataSets);
        }, rawDataSetRef);
        assertArrayEquals(magDataSetRef.getYValues(), magDataSet.getYValues());

        magDataSet = new MathDataSet(null, dataSets -> {
            assertEquals(nBins, dataSets.getDataCount());
            return DataSetMath.magnitudeSpectrumDecibel(dataSets);
        }, rawDataSetRef);
        assertArrayEquals(magDataSetRef.getYValues(), magDataSet.getYValues());
    }

    @Test
    public void testIdentity() {
        final int nBins = 512;
        final DoubleDataSet rawDataSetRef = generateSineWaveData(nBins);
        assertEquals(nBins, rawDataSetRef.getDataCount());

        MathDataSet identityDataSet = new MathDataSet("I", (final double[] input, final int length) -> {
            assertEquals(nBins, input.length);
            assertEquals(nBins, length);
            assertArrayEquals(rawDataSetRef.getYValues(), input, "yValue input equality with source");

            return input;
        }, rawDataSetRef);
        assertArrayEquals(rawDataSetRef.getYValues(), identityDataSet.getYValues());

        identityDataSet = new MathDataSet(null, (final double[] input, final int length) -> {
            assertEquals(nBins, input.length);
            assertEquals(nBins, length);
            assertArrayEquals(rawDataSetRef.getYValues(), input, "yValue input equality with source");

            return input;
        }, rawDataSetRef);
        assertArrayEquals(rawDataSetRef.getYValues(), identityDataSet.getYValues());
    }

    @Test
    public void testNotifies() {
        final int nBins = 512;
        final DoubleDataSet rawDataSetRef = generateSineWaveData(nBins);
        final AtomicInteger counter1 = new AtomicInteger();
        final AtomicInteger counter2 = new AtomicInteger();

        MathDataSet identityDataSet = new MathDataSet("N", null, null, (final double[] input, final int length) -> {
            counter1.incrementAndGet();
            return input;
        }, -1, null, rawDataSetRef);
        assertArrayEquals(rawDataSetRef.getYValues(), identityDataSet.getYValues());
        identityDataSet.addListener(evt -> counter2.incrementAndGet());

        // has been initialised once during construction
        assertEquals(1, counter1.get());
        assertEquals(0, counter2.get());
        counter1.set(0);

        // null does not invoke update
        rawDataSetRef.invokeListener(null, false);
        assertEquals(0, counter1.get());
        assertEquals(0, counter2.get());

        // null does not invoke update
        rawDataSetRef.invokeListener(new UpdateEvent(rawDataSetRef, "wrong event"), false);
        assertEquals(0, counter1.get());
        assertEquals(0, counter2.get());

        // null does not invoke update
        rawDataSetRef.invokeListener(new UpdateEvent(identityDataSet, "wrong reference", false));
        assertEquals(0, counter1.get());
        assertEquals(0, counter2.get());

        // AddedDataEvent does invoke update
        rawDataSetRef.invokeListener(new AddedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());

        // RemovedDataEvent does invoke update
        rawDataSetRef.invokeListener(new RemovedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(2, counter1.get());
        assertEquals(2, counter2.get());
        rawDataSetRef.invokeListener(new RemovedDataEvent(identityDataSet, "wrong reference", false));
        assertEquals(3, counter1.get());
        assertEquals(3, counter2.get());

        // UpdatedDataEvent does invoke update
        rawDataSetRef.invokeListener(new UpdatedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(4, counter1.get());
        assertEquals(4, counter2.get());

        assertEquals(1, rawDataSetRef.updateEventListener().size());
        identityDataSet.deregisterListener();
        assertEquals(0, rawDataSetRef.updateEventListener().size());
        rawDataSetRef.invokeListener(new UpdatedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(4, counter1.get());
        assertEquals(4, counter2.get());

        identityDataSet.registerListener();
        rawDataSetRef.invokeListener(new UpdatedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(5, counter1.get());
        assertEquals(5, counter2.get());

        assertEquals(1, identityDataSet.getSourceDataSets().size());
        identityDataSet.getSourceDataSets().clear();
        assertEquals(0, identityDataSet.getSourceDataSets().size());

        rawDataSetRef.invokeListener(new UpdatedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(5, counter1.get());
        assertEquals(6, counter2.get());
    }

    protected static DoubleDataSet generateSineWaveData(final int nData) {
        DoubleDataSet function = new DoubleDataSet("composite sine", nData);
        for (int i = 0; i < nData; i++) {
            final double t = i;
            double y = 0;
            final double centreFrequency = 0.25;
            final double diffFrequency = 0.05;
            for (int j = 0; j < 8; j++) {
                final double a = 2.0 * Math.pow(10, -j);
                final double diff = j == 0 ? 0 : (j % 2 - 0.5) * j * diffFrequency;
                y += a * Math.sin(2.0 * Math.PI * (centreFrequency + diff) * t);
            }

            function.add(t, y);
        }
        return function;
    }

    protected static DoubleDataSet generateSineWaveSpectrumData(final int nData) {
        return new DoubleDataSet(DataSetMath.magnitudeSpectrumDecibel(generateSineWaveData(nData)));
    }

}
