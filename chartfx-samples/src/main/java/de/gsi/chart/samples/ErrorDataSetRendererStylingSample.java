package de.gsi.chart.samples;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanPropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.testdata.spi.CosineFunction;
import de.gsi.dataset.testdata.spi.GaussFunction;
import de.gsi.dataset.testdata.spi.RandomStepFunction;
import de.gsi.dataset.testdata.spi.RandomWalkFunction;
import de.gsi.dataset.testdata.spi.SincFunction;
import de.gsi.dataset.testdata.spi.SineFunction;
import de.gsi.dataset.testdata.spi.SingleOutlierFunction;
import de.gsi.dataset.utils.ProcessingProfiler;

public class ErrorDataSetRendererStylingSample extends Application {
    private static final String STOP_TIMER = "stop timer";
    private static final String START_TIMER = "start timer";
    private static final Logger LOGGER = LoggerFactory.getLogger(RollingBufferSample.class);
    private static final int DEBUG_UPDATE_RATE = 1000;
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 100; // [ms]
    private static final double N_MAX_SAMPLES = 10_000;
    private DataSetType dataSetType = DataSetType.RANDOM_WALK;
    private int nSamples = 400;
    private Timer timer;

    private void generateData(final XYChart chart) {
        long startTime = ProcessingProfiler.getTimeStamp();
        final List<DataSetError> dataSet = new ArrayList<>();
        switch (dataSetType) {
        case OUTLIER:
            dataSet.add(new SingleOutlierFunction("function with single outlier", nSamples));
            break;
        case STEP:
            dataSet.add(new RandomStepFunction("random step function", nSamples));
            break;
        case SINC:
            dataSet.add(new SincFunction("sinc function", nSamples));
            break;
        case GAUSS:
            dataSet.add(new GaussFunction("gauss function", nSamples));
            break;
        case SINE:
            dataSet.add(new SineFunction("sine function", nSamples));
            break;
        case COSINE:
            dataSet.add(new CosineFunction("cosine function", nSamples));
            break;
        case MIX_TRIGONOMETRIC:
            dataSet.add(new SineFunction("dyn. sine function", nSamples, true));
            dataSet.add(new CosineFunction("dyn. cosine function", nSamples, true));
            break;
        case RANDOM_WALK:
        default:
            dataSet.add(new RandomWalkFunction("random walk data", nSamples));
            break;
        }

        final List<DataSetError> dataSetToLoad = dataSet;
        Platform.runLater(() -> {
            chart.getRenderers().get(0).getDatasets().setAll(dataSetToLoad);
            chart.requestLayout();
        });
        startTime = ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    private static Tab getAxisTab(final String name, final DefaultNumericAxis axis) {
        final PropertySheet propertySheet = new PropertySheet();
        propertySheet.getItems().addAll(BeanPropertyUtils.getProperties(axis).filtered(item -> {
            try {
                item.getValue();
                return true;
            } catch (Exception e) {
                LOGGER.atError().addArgument(item.getName()).addArgument(item.getType().getCanonicalName())
                        .log("invalid property, could not access value: {} ({})");
                return false;
            }
        }));
        return new Tab(name, propertySheet);
        // TODO: sort into groups according to class implementing the property
    }

    private static Tab getChartTab(XYChart chart) {
        final PropertySheet propertySheet = new PropertySheet();
        propertySheet.getItems().addAll(BeanPropertyUtils.getProperties(chart).filtered(item -> {
            try {
                item.getValue();
                return true;
            } catch (Exception e) {
                LOGGER.atError().addArgument(item.getName()).addArgument(item.getType().getCanonicalName())
                        .log("invalid property, could not access value: {} ({})");
                return false;
            }
        }));
        return new Tab("Chart", propertySheet);
    }

    private static Tab getRendererTab(final ErrorDataSetRenderer errorRenderer) {
        final PropertySheet propertySheet = new PropertySheet();
        propertySheet.getItems().addAll(BeanPropertyUtils.getProperties(errorRenderer).filtered(item -> {
            try {
                item.getValue();
                return true;
            } catch (Exception e) {
                LOGGER.atError().addArgument(item.getName()).addArgument(item.getType().getCanonicalName())
                        .log("invalid property, could not access value: {} ({})");
                return false;
            }
        }));
        return new Tab("Renderer", propertySheet);
        // TODO: check why errorType and all the other renderer properties are read-only
    }

    private HBox getHeaderBar(final XYChart chart, final Scene scene) {
        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(getTimerTask(chart)));

        // repetitively generate new data
        final Button startTimer = new Button(START_TIMER);
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                startTimer.setText(STOP_TIMER);
                timer = new Timer("sample-update-timer", true);
                timer.scheduleAtFixedRate(getTimerTask(chart), UPDATE_DELAY, UPDATE_PERIOD);
            } else {
                startTimer.setText(START_TIMER);
                timer.cancel();
                timer = null; // NOPMD
            }
        });

        final ComboBox<DataSetType> dataSetTypeSelector = new ComboBox<>();
        dataSetTypeSelector.getItems().addAll(DataSetType.values());
        dataSetTypeSelector.setValue(dataSetType);
        dataSetTypeSelector.valueProperty().addListener((ch, old, selection) -> {
            dataSetType = selection;
            generateData(chart);
        });

        // H-Spacer
        final Region spacer = new Region();
        spacer.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(new Label("Function Type: "), dataSetTypeSelector, newDataSet, startTimer, spacer,
                new ProfilerInfoBox(scene, DEBUG_UPDATE_RATE));
    }

    public TimerTask getTimerTask(final XYChart chart) {
        return new TimerTask() {
            private int updateCount;

            @Override
            public void run() {
                generateData(chart);

                if (updateCount % 10 == 0 && LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(updateCount).log("update iteration #{}");
                }
                updateCount++;
            }
        };
    }

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(false);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();

        final Scene scene = new Scene(root, ErrorDataSetRendererStylingSample.DEFAULT_WIDTH,
                ErrorDataSetRendererStylingSample.DEFAULT_HEIGHT);

        final DefaultNumericAxis xAxis = new DefaultNumericAxis();
        xAxis.setUnit("Largeness");
        // xAxis.setSide(Side.CENTER_HOR);
        xAxis.setMouseTransparent(true);
        final DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setUnit("Coolness");
        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getXAxis().setName("x axis");
        chart.getYAxis().setName("y axis");
        chart.legendVisibleProperty().set(true);
        // set them false to make the plot faster
        chart.setAnimated(false);
        final ErrorDataSetRenderer errorRenderer = new ErrorDataSetRenderer();
        chart.getRenderers().set(0, errorRenderer);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new Panner());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new DataPointTooltip());
        chart.getPlugins().add(new TableViewer());

        // yAxis.lookup(".axis-label")
        // .setStyle("-fx-label-padding: +10 0 +10 0;");

        final HBox headerBar = getHeaderBar(chart, scene);

        final Label sampleIndicator = new Label();
        sampleIndicator.setText(String.valueOf(nSamples));
        final Label actualSampleIndicator = new Label();
        final Slider nSampleSlider = new Slider(10, N_MAX_SAMPLES, nSamples);
        nSampleSlider.setShowTickMarks(true);
        nSampleSlider.setMajorTickUnit(200);
        nSampleSlider.setMinorTickCount(20);
        nSampleSlider.setBlockIncrement(1);
        HBox.setHgrow(nSampleSlider, Priority.ALWAYS);
        nSampleSlider.valueProperty().addListener((ch, old, n) -> {
            nSamples = n.intValue();
            sampleIndicator.setText(String.valueOf(nSamples));
            generateData(chart);
        });

        final HBox hBoxSlider = new HBox(new Label("Number of Samples:"), nSampleSlider, sampleIndicator,
                actualSampleIndicator);
        root.setTop(new VBox(headerBar, hBoxSlider));

        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(getTimerTask(chart)));

        final Button startTimer = new Button(START_TIMER);
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                startTimer.setText(STOP_TIMER);
                timer = new Timer(true);
                timer.scheduleAtFixedRate(getTimerTask(chart), ErrorDataSetRendererStylingSample.UPDATE_DELAY,
                        ErrorDataSetRendererStylingSample.UPDATE_PERIOD);
            } else {
                startTimer.setText(START_TIMER);
                timer.cancel();
                timer = null; // NOPMD
            }
        });

        final ComboBox<DataSetType> dataSetTypeSelector = new ComboBox<>();
        dataSetTypeSelector.getItems().addAll(DataSetType.values());
        dataSetTypeSelector.setValue(dataSetType);
        dataSetTypeSelector.valueProperty().addListener((ch, old, selection) -> {
            dataSetType = selection;
            generateData(chart);
        });

        // organise parameter config according to tabs
        final TabPane tabPane = new TabPane();

        tabPane.getTabs().add(getRendererTab(errorRenderer));
        tabPane.getTabs().add(getAxisTab("x-Axis", xAxis));
        tabPane.getTabs().add(getAxisTab("y-Axis", yAxis));
        tabPane.getTabs().add(getChartTab(chart));

        root.setLeft(tabPane);

        generateData(chart);

        long startTime = ProcessingProfiler.getTimeStamp();

        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();

        root.setCenter(chart);
        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
        ProcessingProfiler.getTimeDiff(startTime, "for showing");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    public enum DataSetType {
        RANDOM_WALK,
        OUTLIER,
        STEP,
        SINC,
        GAUSS,
        SINE,
        COSINE,
        MIX_TRIGONOMETRIC;
    }
}
