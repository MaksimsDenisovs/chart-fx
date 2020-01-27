package de.gsi.chart.plugins;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * A Plugin which allows to add other plugins to the current chart
 * The first time the dialog is openend, it looks for all the available Plugins.
 * Plugins with an empty default constructor should be easy.
 * Plugins which take axes/Datasets/renderers etc as an argument should present the user dropdowns with all the
 * available ones in the chart and allow them to use it.
 * Primitive type constructor args should be able to input via text field.
 * Additionally it could inspect the plugin for publicly accessible properties/property getters and allow the user to
 * set these.
 * 
 * Could also be extended to add additional Renderers, Datasets, etc
 * 
 * @author Alexander Krimm
 */
public class AddPlugin extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddPlugin.class);

    // list of all available plugins, will be filled on first use
    // this seems to be more difficult than expected
    private static Class<ChartPlugin>[] availablePlugins = null;

    // Add plugins which are not useful to add to the chart here
    private final static String[] ignoredPlugins = new String[] { "ChartPlugin" };
    // Add plugins which can only be added to the chart once here (e.g. Zoomer);
    // Maybe this could be replaced by having all default ctor only plugins in this category
    private final static String[] instancePlugins = new String[] { "Zoomer", "TableViewer" };
    

    private static final String FONT_AWESOME = "FontAwesome";
    private static final int FONT_SIZE = 20;

    private final HBox addPluginButtons = getAddPluginInteractorBar();

    /**
     * Create a screenshot plugin instance
     */
    public AddPlugin() {
        super();

        chartProperty().addListener((change, o, n) -> {
            if (o != null) {
                o.getToolBar().getChildren().remove(addPluginButtons);
            }
            if (n != null) {
                if (isAddButtonsToToolBar()) {
                    n.getToolBar().getChildren().add(addPluginButtons);
                }
            }
        });
    }

    /**
     * @return A nodewith screenshot buttons which can be inserted into the toolbar 
     */
    public HBox getAddPluginInteractorBar() {
        final Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);
        final HBox buttonBar = new HBox();
        buttonBar.setPadding(new Insets(1, 1, 1, 1));
        final Button addPluginBtn = new Button(null, new Glyph(FONT_AWESOME, FontAwesome.Glyph.PLUS).size(FONT_SIZE));
        addPluginBtn.setPadding(new Insets(3, 3, 3, 3));
        addPluginBtn.setTooltip(new Tooltip("Add a Plugin"));
        addPluginBtn.setOnAction(evt -> addPlugin());
        final Button rmPluginBtn = new Button(null, new Glyph(FONT_AWESOME, FontAwesome.Glyph.MINUS).size(FONT_SIZE));
        rmPluginBtn.setPadding(new Insets(3, 3, 3, 3));
        rmPluginBtn.setTooltip(new Tooltip("Remove a Plugin"));
        rmPluginBtn.setOnAction(evt -> rmPlugin());

        buttonBar.getChildren().addAll(separator, rmPluginBtn);
        return buttonBar;
    }

    /**
     * Shows a dialog which allows to remove a plugin
     */
    private void rmPlugin() {
    }

    /**
     * Shows a dialog which allows to add a new Plugin to the chart
     */
    private void addPlugin() {
        Chart chart = getChart();
        // show Dialog
        // populate available plugins
        // get constructor signatures
        // add plugin
    }
}
