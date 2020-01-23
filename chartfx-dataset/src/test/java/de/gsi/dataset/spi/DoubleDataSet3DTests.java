package de.gsi.dataset.spi;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks for FloatDataSet interfaces and constructors.
 * 
 * @author rstein
 */
public class DoubleDataSet3DTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoubleDataSet3DTests.class);

    @Test
    public void testDefaultConstructor() {
        DoubleDataSet3D dataset = new DoubleDataSet3D("testdataset");
        assertEquals("testdataset", dataset.getName());
        assertEquals(0, dataset.getDataCount());
        assertEquals(0, dataset.getDataCount(DIM_X));
        assertEquals(0, dataset.getDataCount(DIM_Y));
        assertEquals(0, dataset.getDataCount(DIM_Z));
        assertThrows(IndexOutOfBoundsException.class,() -> dataset.get(DIM_X, 0));
    }
    
    @Test
    public void checkFullConstructor() {
        double[] xvalues = new double[] {1,2,3,4};
        double[] yvalues = new double[] {-3,-2,-0,2,4};
        double[][] zvalues = new double[][] {{1,2,3,4},{5,6,7,8},{9,10,11,12},{-1,-2,-3,-4},{1337,2337,4242,2323}};
        DoubleDataSet3D dataset = new DoubleDataSet3D("testdataset", xvalues, yvalues, zvalues);
        assertEquals("testdataset", dataset.getName());
        assertEquals(20, dataset.getDataCount());
        assertEquals(4, dataset.getDataCount(DIM_X));
        assertEquals(5, dataset.getDataCount(DIM_Y));
        assertEquals(20, dataset.getDataCount(DIM_Z));
        assertEquals(4242, dataset.get(DIM_Z, 18));
        assertEquals(6, dataset.get(DIM_Z, 5));
        assertEquals(7, dataset.get(DIM_Z, 6));
        assertEquals(4242, dataset.getZ(2, 4));
        assertEquals(4, dataset.get(DIM_X, 3));
        assertEquals(4, dataset.get(DIM_Y, 4));
        assertEquals(3, dataset.getIndex(DIM_X, 3.9));
        assertEquals(2, dataset.getIndex(DIM_Y, -0.5));
        assertEquals(0, dataset.getIndex(DIM_X, -1000));
        assertEquals(3, dataset.getIndex(DIM_X, 1000));
        assertThrows(IndexOutOfBoundsException.class,() -> dataset.get(DIM_X, 4));
        assertThrows(IndexOutOfBoundsException.class,() -> dataset.getZ(1, 5));
        assertThrows(IndexOutOfBoundsException.class,() -> dataset.getZ(4, 0));
//        dataset.clearData();
//        assertEquals(new DoubleDataSet3D("testdataset", 4, 5), dataset);
    }
}
