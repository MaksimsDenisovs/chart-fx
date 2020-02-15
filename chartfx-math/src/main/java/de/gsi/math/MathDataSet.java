package de.gsi.math;

import static de.gsi.dataset.event.EventRateLimiter.UpdateStrategy.INSTANTANEOUS_RATE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.EventRateLimiter;
import de.gsi.dataset.event.EventRateLimiter.UpdateStrategy;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.DoubleDataSet;

/**
 * DataSet that automatically transforms source DataSet accordance to DataSetFunction or DataSetValueFunction
 * definition.
 * An optional rate limit is available to limit the number of redundant (GUI) updates if desired.
 *
 * @author rstein
 */
public class MathDataSet extends DoubleDataSet {
    private static final long serialVersionUID = -4978160822533565009L;
    private static final long DEFAULT_UPDATE_LIMIT = 40;
    private final transient EventListener eventListener;
    private final transient List<DataSet> sourceDataSets;
    private final transient DataSetFunction dataSetFunction;
    private final transient DataSetsFunction dataSetsFunction;
    private final transient DataSetValueFunction dataSetValueFunction;
    private final transient long updateRateLimit; // NOPMD
    private final transient UpdateStrategy updateStrategy; // NOPMD
    private final transient String transformName;

    public MathDataSet(final String transformName, DataSetFunction dataSetFunction, final DataSet source) {
        this(transformName, dataSetFunction, null, null, DEFAULT_UPDATE_LIMIT, INSTANTANEOUS_RATE, source);
    }

    public MathDataSet(final String transformName, final DataSetFunction dataSetFunction, final long updateRateLimit,
            final UpdateStrategy updateStrategy, final DataSet source) {
        this(transformName, dataSetFunction, null, null, updateRateLimit, updateStrategy, source);
    }

    public MathDataSet(final String transformName, final DataSetsFunction dataSetsFunction, final DataSet... sources) {
        this(transformName, null, dataSetsFunction, null, DEFAULT_UPDATE_LIMIT, INSTANTANEOUS_RATE, sources);
    }

    public MathDataSet(final String transformName, final DataSetsFunction dataSetsFunction, final long updateRateLimit,
            final UpdateStrategy updateStrategy, final DataSet... sources) {
        this(transformName, null, dataSetsFunction, null, updateRateLimit, updateStrategy, sources);
    }

    public MathDataSet(final String transformName, final DataSetValueFunction dataSetValueFunction,
            final long updateRateLimit, final UpdateStrategy updateStrategy, final DataSet source) {
        this(transformName, null, null, dataSetValueFunction, updateRateLimit, updateStrategy, source);
    }

    public MathDataSet(final String transformName, DataSetValueFunction dataSetValueFunction, final DataSet source) {
        this(transformName, null, null, dataSetValueFunction, DEFAULT_UPDATE_LIMIT, INSTANTANEOUS_RATE, source);
    }

    protected MathDataSet(final String transformName, DataSetFunction dataSetFunction,
            DataSetsFunction dataSetsFunction, DataSetValueFunction dataSetValueFunction, final long updateRateLimit,
            UpdateStrategy updateStrategy, final DataSet... sources) {
        super(getCompositeDataSetName(transformName, sources));
        this.sourceDataSets = new ArrayList<>(Arrays.asList(sources));
        this.updateRateLimit = updateRateLimit;
        this.updateStrategy = updateStrategy == null ? INSTANTANEOUS_RATE : updateStrategy;
        this.dataSetFunction = dataSetFunction;
        this.dataSetsFunction = dataSetsFunction;
        this.dataSetValueFunction = dataSetValueFunction;
        this.transformName = transformName;

        if (dataSetFunction == null && dataSetsFunction == null && dataSetValueFunction == null) {
            throw new IllegalArgumentException(
                    "dataSetFunction, dataSetsFunction and dataSetValueFunction cannot all be null");
        }

        if (dataSetValueFunction != null && sourceDataSets.size() > 1) {
            throw new IllegalArgumentException(
                    "sources list may not be larger than one if the 'dataSetValueFunction' interface is used"
                            + " -> try to use 'DataSetFunction' instead");
            // N.B. rationale is that if one combines data from more than one DataSet
            // that it's very likely that they have different x vectors/sampling.
            // This usually requires a more sophisticated approach better handled through the 'DataSetFunction' interface
        }

        if (updateRateLimit > 0) {
            eventListener = new EventRateLimiter(this::handle, this.updateRateLimit, this.updateStrategy);
        } else {
            eventListener = this::handle;
        }
        registerListener(); // NOPMD

        // exceptionally call handler during DataSet creation
        handle(new UpdatedDataEvent(this, MathDataSet.class.getSimpleName() + " - initial constructor update"));
    }

    public final void deregisterListener() {
        sourceDataSets.forEach(srcDataSet -> srcDataSet.removeListener(eventListener));
    }

    public final List<DataSet> getSourceDataSets() {
        return sourceDataSets;
    }

    public final void registerListener() {
        sourceDataSets.forEach(srcDataSet -> srcDataSet.addListener(eventListener));
    }

    protected void handle(UpdateEvent event) {
        boolean isKnownEvent = event instanceof AddedDataEvent || event instanceof RemovedDataEvent
                || event instanceof UpdatedDataEvent;
        if (event == null || !isKnownEvent) {
            return;
        }
        this.lock().writeLockGuard(() -> {
            if (dataSetFunction != null) {
                set(dataSetFunction.transform(sourceDataSets.get(0)));
            } else if (dataSetsFunction != null) {
                set(dataSetsFunction.transform(sourceDataSets));
            } else {
                if (sourceDataSets.isEmpty()) {
                    return;
                }
                final DataSet dataSet = sourceDataSets.get(0);
                final double[] xSourceVector = dataSet.getValues(DIM_X);
                final double[] ySourceVector = dataSet.getValues(DIM_Y);
                final int length = dataSet.getDataCount();
                this.set(xSourceVector, dataSetValueFunction.transform(ySourceVector, length), length, true);
            }

            this.setName(getCompositeDataSetName(transformName, sourceDataSets.toArray(new DataSet[0])));
        });
        fireInvalidated(new UpdatedDataEvent(this, "propagated update from source " + this.getName()));
    }

    protected static String getCompositeDataSetName(final String transformName, final DataSet... sources) {
        final List<DataSet> dataSets = Arrays.asList(sources);
        final String sanitizedFunctionName = transformName == null ? "" : transformName;
        return dataSets.stream().map(DataSet::getName)
                .collect(Collectors.joining(",", sanitizedFunctionName + "(", ")"));
    }

    /**
     * simple DataSet transform function defintion for single input DataSets
     *
     * @author rstein
     */
    public interface DataSetFunction {

        DataSet transform(final DataSet inputDataSet);

    }

    /**
     * simple DataSet transform function defintion for multiple input DataSets
     *
     * @author rstein
     */
    public interface DataSetsFunction {

        DataSet transform(final List<DataSet> inputDataSet);

    }

    /**
     * simple DataSet transform function defintion, only the y value is being transformed
     *
     * @author rstein
     */
    public interface DataSetValueFunction {

        double[] transform(final double[] inputDataSet, final int length);
    }
}
