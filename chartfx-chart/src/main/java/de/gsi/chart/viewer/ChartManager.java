package de.gsi.chart.viewer;

import org.controlsfx.control.MasterDetailPane;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanPropertyUtils;

import de.gsi.chart.Chart;
import de.gsi.chart.renderer.Renderer;
import de.gsi.dataset.DataSet;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * A JavaFx control, which can be given a root chartfx element, and which then allows to reorganize the charts.
 * The root element can be a DataViewer, DataView, DataViewWindow, Chart or Renderer.
 * All chartfx related child elements are shown in a tree View and can be reorganized using drag and drop.
 * The lower part of the control allows to modify the currently selected element's properties in a PropertySheet
 * control.
 * 
 * @author Alexander Krimm
 */
public class ChartManager extends MasterDetailPane {

    /**
     * Creates a new ChartManager
     * 
     * @param root The root element on which the manager should act
     */
    public ChartManager(final Object root) {
        super();
        setShowDetailNode(false);
        TreeView<Object> treeView = new TreeView<>();
        PropertySheet propertySheet = new PropertySheet();
        setDetailNode(propertySheet);
        setMasterNode(treeView);
        treeView.setRoot(AbstractChartFxTreeItem.get(root));
        treeView.setCellFactory(element -> new ChartFxTreeCell(element));
        treeView.getSelectionModel().selectedItemProperty().addListener((prop, oldVal, newVal) -> {
            if (newVal != oldVal && newVal != null) {
                propertySheet.getItems().setAll(BeanPropertyUtils.getProperties(newVal));
            }
        });
    }

    protected class ChartFxTreeCell extends TreeCell<Object> {
        private Object element;

        /**
         * @param element
         */
        public ChartFxTreeCell(Object element) {
            super();
            this.element = element;
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (!empty && item != null) {
                setText("test");
                setGraphic(null);
                // setContextMenu() // add menu here to remove, add additional components
            } else {
                setText("");
                setGraphic(null);
            }
        }
    }

    public static class AbstractChartFxTreeItem extends TreeItem<Object> {
        protected ChartFxElementType type;
        protected String name;
        protected Object reference;

        /**
         * @param root A chartfx component
         */
        protected AbstractChartFxTreeItem(Object root) {
            super(root);
            reference = root;
        }

        public static AbstractChartFxTreeItem get(final Object root) {
            if (root instanceof Chart) {
                return new ChartFxChartTreeItem(root);
            } else if (root instanceof DataSet) {
                return new ChartFxDataSetTreeItem(root);
            } else if (root instanceof Renderer) {
                return new ChartFxRendererTreeItem(root);
            }
            return null;
        }
    }

    public static class ChartFxChartTreeItem extends AbstractChartFxTreeItem {
        /**
         * @param root
         */
        protected ChartFxChartTreeItem(Object root) {
            super(root);
            Chart chart = (Chart) root;
            type = ChartFxElementType.CHART;
            name = chart.getTitle();
            chart.getDatasets().forEach(dataSet -> {
                this.getChildren().add(new ChartFxDataSetTreeItem(dataSet));
            });
        }
    }

    public static class ChartFxDataSetTreeItem extends AbstractChartFxTreeItem {
        /**
         * @param root
         */
        public ChartFxDataSetTreeItem(Object root) {
            super(root);
        }
    }
    
    public static class ChartFxRendererTreeItem extends AbstractChartFxTreeItem {
        /**
         * @param root
         */
        public ChartFxRendererTreeItem(Object root) {
            super(root);
        }
    }

    public enum ChartFxElementType {
        DATA_VIEWER,
        DATA_VIEW,
        DATA_VIEW_WINDOW,
        CHART,
        RENDERER,
        PLUGIN,
        AXIS,
        DATASET;
    }
}
