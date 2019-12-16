package de.gsi.math.transforms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.AxisChangeEvent;
import de.gsi.dataset.event.AxisNameChangeEvent;
import de.gsi.dataset.event.AxisRangeChangeEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.InvalidatedEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.event.UpdatedMetaDataEvent;

/**
 * Abstract class to do the heavy lifting for Algorithms which modify DataSets.
 * 
 * @author Alexander Krimm
 */
abstract public class AbstractTransform implements Transform, EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTransform.class);

    private double lastStep = 0.0;
    private double updateStep = 1.0;
    private double progress = 0.0;
    private DataSet input;
    private DataSet output;
    private UpdateStrategy updateStrategy;
    private boolean isPaused = true;
//    private Task computationTask;

    @Override
    public void setUpdateSteps(double step) {
        if (step < 0 || step > 1.0) {
            throw new IllegalArgumentException("UpdateStep value must be between 0.0 and 1.0");
        }
        updateStep = step;
    }

    @Override
    public double getUpdateSteps() {
        return updateStep;
    }

    public AbstractTransform(DataSet input) {
        this.input = input;
        isPaused = false;
        input.addListener(this);
    }

    @Override
    public void setUpdateStrategy(UpdateStrategy updateStrategy) {
        this.updateStrategy = updateStrategy;
    }

    @Override
    public UpdateStrategy getUpdateStrategy() {
        return updateStrategy;
    }

    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public void pause(boolean pause) {
        if (isPaused) {
            isPaused = false;
            input.addListener(this);
            return;
        }
        isPaused = true;
        input.removeListener(this);
    }

    @Override
    public void handle(UpdateEvent event) {
        if (event instanceof UpdatedMetaDataEvent) {
            metadataInvalidated();
        } else if (event instanceof UpdatedDataEvent) {
            dataInvalidated();
        } else if (event instanceof RemovedDataEvent) {
            dataInvalidated();
        } else if (event instanceof InvalidatedEvent) {
            allInvalidated();
        } else if (event instanceof AxisRangeChangeEvent) {
            axisRangeInvalidated();
        } else if (event instanceof AxisNameChangeEvent) {
            axisNameInvalidated();
        } else if (event instanceof AxisChangeEvent) {
            axisInvalidated();
        } else if (event instanceof AddedDataEvent) {
            dataInvalidated();
        } else {
            allInvalidated();
        }
    }

    /**
     * called whenever the axis data is invalidated
     */
    private void axisInvalidated() {
        allInvalidated();
    }

    /**
     * called whenever the Axis name is invalidated
     */
    private void axisNameInvalidated() {
        allInvalidated();
    }

    /**
     * called whenever the range of the axis is invalidated
     */
    private void axisRangeInvalidated() {
        allInvalidated();
    }

    /**
     * Called whenever the dataset changes and it is not specified what changed.
     * Recalculates everything.
     */
    private void allInvalidated() {
        // check rate limiting
        // check if job is running
            // check strategy
        // add job?
    }

    /**
     * Called whenever the data of the input dataset is changed
     */
    private void dataInvalidated() {
        allInvalidated();
    }

    /**
     * called whenever the metadata of the dataset is changed.
     */
    private void metadataInvalidated() {
        allInvalidated();
    }
}
