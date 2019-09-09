package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DoubleDataSet3D;
import de.gsi.dataset.spi.DoubleErrorDataSet;

/**
 * @author akrimm
 */
public class DataSetUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetUtilsTest.class);
    private static final double EPSILON = 1e-6;
    
    @BeforeAll
    public static void resetLocalization(){
        Locale.setDefault(Locale.US);
    }

    private static DataSet getTestDataSet() {
        double[] xvalues = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] yvalues = new double[] { 1.3, 3.7, 4.2, 2.3, 1.8 };
        return new DataSetBuilder() //
                .setName("TestSerialize") //
                .setXValues(xvalues) //
                .setYValues(yvalues) //
                .setMetaInfoMap(Map.of("test", "asdf", "testval", "5.24532")) //
                .build();
    }

    @DisplayName("Serialize and Deserialize DefaultDataSet into StringBuffer and back")
    @ParameterizedTest(name = "binary: {0}, float: {1}")
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    public void serializeAndDeserializeDefaultDataSet(boolean binary, boolean useFloat) {
        // initialize dataSet
        DataSet dataSet = getTestDataSet();
        // assert that dataSet was created correctly
        assertTrue(dataSet instanceof DefaultDataSet);
        // write and read dataSet
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        DataSetUtils.writeDataSetToByteArray(dataSet, byteBuffer, binary, useFloat);
        DataSet dataSetRead = DataSetUtils.readDataSetFromByteArray(byteBuffer.toByteArray());
        // assert that DataSet was written and read correctly
        assertTrue(dataSetRead instanceof DoubleErrorDataSet);
        assertEquals(dataSet.getDataCount(), dataSetRead.getDataCount());
        assertEquals(dataSetRead.getName(), dataSet.getName());
        int dataCount = dataSet.getDataCount();
        assertArrayEquals( //
                Arrays.copyOfRange(dataSet.getXValues(), 0, dataCount), //
                Arrays.copyOfRange(dataSetRead.getXValues(), 0, dataCount), //
                EPSILON);
        assertArrayEquals( //
                Arrays.copyOfRange(dataSet.getYValues(), 0, dataCount), //
                Arrays.copyOfRange(dataSetRead.getYValues(), 0, dataCount), //
                EPSILON);
        assertEquals("asdf", ((DoubleErrorDataSet) dataSetRead).getMetaInfo().get("test"));
    }

    @DisplayName("Serialize and Deserialize DataSet3D into StringBuffer and back")
    @ParameterizedTest(name = "binary: {0}, float: {1}")
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    public void serializeAndDeserializeDataSet3D(boolean binary, boolean useFloat) {
        // initialize dataSet
        int nx = 3;
        double[] xvalues = new double[] { 1.0, 2.0, 3.0 };
        int ny = 2;
        double[] yvalues = new double[] { 0.001, 4.2 };
        int n = 6;
        double[][] zvalues = new double[][] { { 1.3, 3.7, 4.2 }, { 2.3, 1.8, 5.0 } };
        DataSet3D dataSet = new DoubleDataSet3D("Test 3D Dataset", xvalues, yvalues, zvalues);
        // assert that dataSet was created correctly
        assertArrayEquals(xvalues, dataSet.getXValues(), EPSILON);
        assertArrayEquals(yvalues, dataSet.getYValues(), EPSILON);
        // write and read dataSet
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        DataSetUtils.writeDataSetToByteArray(dataSet, byteBuffer, binary, useFloat);
        DataSet dataSetRead = DataSetUtils.readDataSetFromByteArray(byteBuffer.toByteArray());
        // assert that DataSet was written and read correctly
        assertTrue(dataSetRead instanceof DataSet3D);
        DataSet3D dataSetRead3D = (DataSet3D) dataSetRead;
        assertEquals(dataSetRead.getDataCount(), n);
        assertEquals(dataSetRead.getName(), dataSet.getName());
        assertEquals(dataSetRead3D.getXDataCount(), 3);
        assertEquals(dataSetRead3D.getYDataCount(), 2);
        assertArrayEquals(xvalues, dataSetRead.getXValues(), EPSILON);
        assertArrayEquals(yvalues, dataSetRead.getYValues(), EPSILON);
        for (int ix = 1; ix < nx; ix++) {
            for (int iy = 1; iy < ny; iy++) {
                assertEquals(zvalues[iy][ix], dataSetRead3D.getZ(ix, iy), EPSILON);
            }
        }
    }

    @ParameterizedTest()
    @CsvSource({ //
            "test.csv,               test.csv               ", // plain filename
            "test_{dataSetName}.csv, test_TestSerialize.csv ", // data Set name
            "test_{xMin}-{xMax}.csv, test_1.0-5.0.csv ",       // x min/max
            "test_{yMin}-{yMax}.csv, test_1.3-4.2.csv ",       // y min/max
            "{test;string}.csv     , asdf.csv ",               // metadata field
            "val_{testval;float;%.2f}.csv, val_5.25.csv ",      // metadata field with formated cast to double
    })
    @DisplayName("Test Filename Generation")
    public void getFileNameTest(String pattern, String fileName) {
        DataSet dataSet = getTestDataSet();
        assertEquals(fileName, DataSetUtils.getFileName(dataSet, pattern));
    }

}
