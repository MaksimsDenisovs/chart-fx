package de.gsi.math.transforms;

/**
 * Interface for transforms on DataSets.
 * All classes providing this interface, allow to update a DataSet based on another one.
 * 
 * @author Alexander Krimm
 */
public interface Transform {
    
    /**
     * Defines how the Transform should react, when a Dataset is updated while a computation is allready running.
     * If a transform does not support a certain strategy, it can return an UnsupportedOperationException.
     */
    public enum UpdateStrategy {
        /**
         * Cancels the currently running job and starts a new one with the current state.
         * Note that this means that the computation might never finish if the data is constantly updated.
         */
        ABANDON_RUNNING_ON_UPDATE,
        /**
         * Waits for the current job to finnish. immediately afterwards, a new job with the current data is started.
         * Changes occuring while the job is running are skipped.
         * Note that is only possible for transforms that keep a copy of the input data.
         */
        FINNISH_RUNNING_FIRST;
    }

    /**
     * Sets the step size for applying updates to the result as a fraction of 1.0.
     * Default is 1.0, which means the data is only updated after the computation has finished.
     * A value of 0.05 means the data is added in 5% steps.
     * The Transform can have different means of updating, e.g. the data count might increase or invalid data might be
     * filled with a constant or old data.
     * 
     * @param step update step size as a fraction of 1.0
     */
    public void setUpdateSteps(final double step);

    /**
     * @return step update step size as a fraction of 1.0
     */
    public double getUpdateSteps();
    
    /**
     * Sets the update strategy
     * @param updateStrategy new value for the update strategy
     */
    public void setUpdateStrategy(UpdateStrategy updateStrategy);
    
    /**
     * @return the currently used update strategy
     */
    public UpdateStrategy getUpdateStrategy();
    
    /**
     * Returns the progress of the currently running transform (0.0-1.0)
     * @return the progress
     */
    public double getProgress();
    
    public void pause(boolean pause);
    public boolean isPaused();
//    /**
//     * Perform the transform on a DataSet into a new DataSet.
//     * The result is returned immediately and a background job is started to update the result Dataset.
//     * The progress is written and updated to the result DataSet's metadata and can be queried with the
//     * getStatus Method. Additionally the waitForCompletion() Method blocks until the computation is finished.
//     * The result DataSet can be updated periodically during the transformation (if implemented) by setting updateStep
//     * to a value lower than 1.0.
//     * 
//     * @param input Input DataSet
//     * @return output Dataset
//     */
//    public DataSet transform(final DataSet input);
//
//    /**
//     * Perform the transform on a DataSet into an existing DataSet.
//     * The old contents of the DataSet are overwritten. The output DataSet must be compatible with the result of the
//     * transform in terms of type and size.
//     * The method returns immediately and a background job is started to update the output DataSet.
//     * The progress is written and updated to the result DataSet's metadata and can be queried with the
//     * getStatus Method. Additionally the waitForCompletion() Method blocks until the computation is finished.
//     * The result DataSet can be updated periodically during the transformation (if implemented) by setting updateStep 
//     * to a value lower than 1.0.
//     * 
//     * @param input Input DataSet
//     * @param output Output DataSet
//     */
//    public void transform(final DataSet input, final DataSet output);
//
//    /**
//     * Same as transform, but the transform installs a listener to the original Dataset and keeps updating the result
//     * whenever the input changes.
//     * 
//     * @param input
//     * @return new dataSet which will be updated with the new data
//     */
//    public DataSet transformContinuous(final DataSet input);
//
//    /**
//     * Same as transform, but the transform installs a listener to the original Dataset and keeps updating the result
//     * whenever the input changes.
//     *
//     * @param input Input DataSet
//     * @param output Output DataSet
//     */
//    public void transformContinuous(final DataSet input, final DataSet output);

}
