package de.gsi.chart.plugins;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.utils.DataSetUtils;

/**
 * Plugin allowing to take a screenshot of the complete chart.
 * Allows to copy image to clipboard or to save as a file.
 *
 * @author Alexander Krimm
 */
public class Screenshot extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(Screenshot.class);
    private static final String FONT_AWESOME = "FontAwesome";
    private static final int FONT_SIZE = 20;

    private final HBox screenshotButtons = getScreenshotInteractorBar();
    private final StringProperty pattern = new SimpleStringProperty(this, "pattern", "");
    private final StringProperty directory = new SimpleStringProperty(this, "directory",
            System.getProperty("user.home"));
    private boolean toFile = false; // copy to clipboard by default;

    /**
     * Create a screenshot plugin instance
     */
    public Screenshot() {
        super();

        chartProperty().addListener((change, o, n) -> {
            if (o != null) {
                o.getToolBar().getChildren().remove(screenshotButtons);
            }
            if (n != null && isAddButtonsToToolBar()) {
                n.getToolBar().getChildren().add(screenshotButtons);
            }
        });
    }

    /**
     * Gets the pattern property for filename generation.
     * For the pattern syntax see {@link de.gsi.dataset.utils.DataSetUtils#getFileName(DataSet, string)}
     * returns The pattern property to generate template filenames
     */
    public StringProperty patternProperty() {
        return pattern;
    }

    /**
     * Sets the pattern for the initial filename.
     * For the pattern syntax see {@link de.gsi.dataset.utils.DataSetUtils#getFileName(DataSet, string)}
     * 
     * @param pattern A pattern to generate template filenames
     */
    public void setPattern(final String pattern) {
        this.pattern.set(pattern);
    }

    /**
     * Gets the pattern for the initial filename.
     * For the pattern syntax see {@link de.gsi.dataset.utils.DataSetUtils#getFileName(DataSet, string)}
     * 
     * @returns The pattern to generate template filenames
     */
    public String getPattern() {
        return pattern.get();
    }

    /**
     * Gets the pattern property for filename generation.
     * For the pattern syntax see {@link de.gsi.dataset.utils.DataSetUtils#getFileName(DataSet, string)}
     * 
     * @return the property holding the save directory
     */
    public StringProperty directoryProperty() {
        return directory;
    }

    /**
     * Sets the pattern for the initial filename.
     * For the pattern syntax see {@link de.gsi.dataset.utils.DataSetUtils#getFileName(DataSet, string)}
     * 
     * @param pattern A pattern to generate template filenames
     */
    public void setDirectory(final String directory) {
        this.directory.set(directory);
    }

    /**
     * Gets the pattern for the initial filename.
     * 
     * @param pattern A pattern to generate template filenames
     */
    public String getDirectory() {
        return directory.get();
    }

    /**
     * @return A node with screenshot buttons which can be inserted into the toolbar
     */
    public HBox getScreenshotInteractorBar() {
        final HBox buttonBar = new HBox();
        final Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);
        SplitMenuButton button = new SplitMenuButton();
        button.setGraphic(new HBox(0.1, new Glyph(FONT_AWESOME, FontAwesome.Glyph.CAMERA).size(FONT_SIZE),
                new Glyph(FONT_AWESOME, FontAwesome.Glyph.CLIPBOARD).size(FONT_SIZE - 8)));
        button.setOnAction(evt -> {
            if (toFile) {
                screenshotToFile();
            } else {
                screenshotToClipboard();
            }
        });
        MenuItem toClipMenu = new MenuItem("Screenshot to clipboard",
                new Glyph(FONT_AWESOME, FontAwesome.Glyph.CLIPBOARD));
        toClipMenu.setOnAction(evt -> {
            toFile = false;
            button.setGraphic(new HBox(0.1, new Glyph(FONT_AWESOME, FontAwesome.Glyph.CAMERA).size(FONT_SIZE),
                    new Glyph(FONT_AWESOME, FontAwesome.Glyph.CLIPBOARD).size(FONT_SIZE - 8)));
            button.setTooltip(new Tooltip("Copy screenshot of plot to Clipboard"));
            screenshotToClipboard();
        });
        MenuItem toFileMenu = new MenuItem("Screenshot to file", new Glyph(FONT_AWESOME, FontAwesome.Glyph.FILE));
        toFileMenu.setOnAction(evt -> {
            toFile = true;
            button.setGraphic(new HBox(0.1, new Glyph(FONT_AWESOME, FontAwesome.Glyph.CAMERA).size(FONT_SIZE),
                    new Glyph(FONT_AWESOME, FontAwesome.Glyph.FILE).size(FONT_SIZE - 8)));
            button.setTooltip(new Tooltip("Save plot as image"));
            screenshotToFile();
        });
        MenuItem settingsMenu = new MenuItem("Screenshot settings", new Glyph(FONT_AWESOME, FontAwesome.Glyph.WRENCH));
        settingsMenu.setOnAction(evt -> {
            ScreenshotDialog alert = new ScreenshotDialog();
            alert.showAndWait() //
                    .filter(response -> response == ButtonType.OK) //
                    .ifPresent(response -> {
                        directory.set(alert.getDirectory());
                        pattern.set(alert.getPattern());
                    });
        });
        button.getItems().addAll(toClipMenu, toFileMenu, new SeparatorMenuItem(), settingsMenu);

        buttonBar.getChildren().addAll(separator, button);
        return buttonBar;
    }

    /**
     * Save screenshot to clipbaord
     */
    public void screenshotToClipboard() {
        Image image = getScreenshot();
        Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putImage(image);
        clipboard.setContent(content);
        LOGGER.atInfo().log("Copied screenshot to clipboard");
    }

    /**
     * saves a screenshot to a file that can be chosen with a file opener
     */
    public void screenshotToFile() {
        Image image = getScreenshot();
        String initName;

        if (!pattern.get().isEmpty()) {
            if (getChart().getAllDatasets().isEmpty()) {
                initName = DataSetUtils.getFileName(null, pattern.get());
            } else {
                initName = DataSetUtils.getFileName(getChart().getAllDatasets().get(0), pattern.get());
            }
        } else if (getChart().getTitle() != null && !getChart().getTitle().isBlank()) {
            initName = getChart().getTitle();
        } else if (getChart().getId() != null && !getChart().getId().isBlank()) {
            initName = getChart().getId();
        } else if (!getChart().getAllDatasets().isEmpty() && getChart().getAllDatasets().get(0).getName() != null
                && !getChart().getAllDatasets().get(0).getName().isBlank()) {
            initName = getChart().getAllDatasets().get(0).getName();
        } else {
            initName = "UnknownChart";
        }
        File file = showFileDialog(initName);
        if (file == null)
            return;
        saveImage(image, file);
        LOGGER.atInfo().addArgument(file.getName()).log("Saved screenshot to {}");
    }

    /**
     * @return An image containing a screenshot of the complete chart
     */
    public Image getScreenshot() {
        SnapshotParameters snapParams = new SnapshotParameters();
        // hide toolbar
        boolean oldval = getChart().getToolBar().isVisible();
        getChart().getToolBar().setVisible(false);
        // take screenshot
        WritableImage result = getChart().snapshot(snapParams, null);
        // restore toolbar
        getChart().getToolBar().setVisible(oldval);
        return result;
    }

    /**
     * Saves a file to a png file
     * 
     * @param image the imaga data
     * @param file The file to save to
     */
    private static void saveImage(final Image image, final File file) {
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            LOGGER.atError().addArgument(file.getName()).log("Error saving screenshot to {}");
        }
    }

    /**
     * @return The file to save the screenshot to (or null if canceled)
     */
    private File showFileDialog(final String initName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().setAll(new ExtensionFilter("PNG-Image", new String[] { "*.png" }));
        fileChooser.setInitialDirectory(new File(directory.get()));
        fileChooser.setInitialFileName(initName);
        File file = fileChooser.showSaveDialog(getChart().getScene().getWindow());
        if (file != null && !directory.isBound()) {
            directory.set(file.getParent());
        }
        return file;
    }

    protected class ScreenshotDialog extends Alert {
        /**
         * @param alertType
         */
        public ScreenshotDialog() {
            super(AlertType.CONFIRMATION, "Screenshot Settings");
            TextField dirTextbox = new TextField();
            Button dirButton = new Button("choose...");
            TextField patternTextbox = new TextField();
            this.getDialogPane().setContent(new VBox(new HBox(new Label("directory: "), dirTextbox, dirButton),
                    new HBox(new Label("pattern:"), patternTextbox)));
            // TODO: use grid/form layout
            // TODO: add help text for formatting strings
        }

        /**
         * @return the configured filename pattern
         */
        public String getPattern() {
            return null;
        }

        /**
         * @return the configured output directory
         */
        public String getDirectory() {
            return null;
        }

    }
}
