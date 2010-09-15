/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.dataio.placemark.PlacemarkIO;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.FloatCellEditor;
import org.esa.beam.framework.ui.FloatTableCellRenderer;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.pet.Coordinate;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class PixelExtractionParametersForm {

    private JPanel panel;
    private JLabel windowLabel;
    private JSpinner windowSpinner;
    private AppContext appContext;

    static final String LAST_OPEN_PLACEMARK_DIR = "beam.petOp.lastOpenPlacemarkDir";
    private CoordinateTableModel tableModel;

    public PixelExtractionParametersForm(AppContext appContext, PropertyContainer container) {

        this.appContext = appContext;

        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setCellFill(0, 1, TableLayout.Fill.BOTH);
        tableLayout.setCellWeightY(0, 1, 1.0);
        tableLayout.setCellFill(0, 1, TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        panel = new JPanel(tableLayout);
        final BindingContext bindingContext = new BindingContext(container);

        panel.add(new JLabel("Coordinates:"));
        final JComponent[] coordinatesComponents = createCoordinatesComponents();
        panel.add(coordinatesComponents[0]);
        panel.add(coordinatesComponents[1]);

        panel.add(new JLabel("Export:"));
        final JPanel exportPanel = createExportPanel(bindingContext);
        panel.add(exportPanel);
        panel.add(new JLabel());

        panel.add(new JLabel("Window size:"));
        windowSpinner = createWindowSizeEditor(bindingContext);
        panel.add(windowSpinner);
        windowLabel = new JLabel();
        windowLabel.setHorizontalAlignment(SwingConstants.CENTER);
        windowSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateWindowLabel();
            }
        });
        updateWindowLabel();
        panel.add(windowLabel);
    }

    public Coordinate[] getCoordinates() {
        Coordinate[] coordinates = new Coordinate[tableModel.getRowCount()];
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            final Placemark placemark = tableModel.getPlacemarkAt(i);
            coordinates[i] = new Coordinate(placemark.getName(), placemark.getGeoPos());
        }
        return coordinates;
    }

    private JPanel createExportPanel(BindingContext bindingContext) {
        final TableLayout tableLayout = new TableLayout(4);
        tableLayout.setTablePadding(4, 0);
        tableLayout.setTableFill(TableLayout.Fill.VERTICAL);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setColumnWeightX(3, 1.0);
        tableLayout.setTableWeightY(1.0);
        final JPanel exportPanel = new JPanel(tableLayout);

        exportPanel.add(createIncludeCheckbox(bindingContext, "Bands", "exportBands"));
        exportPanel.add(createIncludeCheckbox(bindingContext, "Tie-point grids", "exportTiePoints"));
        exportPanel.add(createIncludeCheckbox(bindingContext, "Masks", "exportMasks"));
        exportPanel.add(tableLayout.createHorizontalSpacer());
        return exportPanel;
    }

    private void updateWindowLabel() {
        windowLabel.setText(String.format("%1$d x %1$d", (Integer) windowSpinner.getValue()));
    }

    private JCheckBox createIncludeCheckbox(BindingContext bindingContext, String labelText, String propertyName) {
        final Property squareSizeProperty = bindingContext.getPropertySet().getProperty(propertyName);
        final Boolean defaultValue = (Boolean) squareSizeProperty.getDescriptor().getDefaultValue();
        final JCheckBox checkbox = new JCheckBox(labelText, defaultValue);
        bindingContext.bind(propertyName, checkbox);
        return checkbox;
    }

    private JComponent[] createCoordinatesComponents() {
        tableModel = new CoordinateTableModel();
        Product selectedProduct = appContext.getSelectedProduct();
        if (selectedProduct != null) {
            final PlacemarkGroup pinGroup = selectedProduct.getPinGroup();
            for (int i = 0; i < pinGroup.getNodeCount(); i++) {
                tableModel.addPlacemark(pinGroup.get(i));
            }
        }

        final JTable coordinateTable = new JTable(tableModel);
        coordinateTable.setName("coordinateTable");
        coordinateTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        coordinateTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        coordinateTable.setRowSelectionAllowed(true);
        coordinateTable.getTableHeader().setReorderingAllowed(false);
        coordinateTable.setDefaultRenderer(Float.class, new FloatTableCellRenderer(new DecimalFormat("0.0000")));
        coordinateTable.setPreferredScrollableViewportSize(new Dimension(300, 200));
        coordinateTable.getColumnModel().getColumn(1).setCellEditor(new FloatCellEditor(-90, 90));
        coordinateTable.getColumnModel().getColumn(2).setCellEditor(new FloatCellEditor(-180, 180));
        final JScrollPane rasterScrollPane = new JScrollPane(coordinateTable);
        rasterScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rasterScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        final AbstractButton addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"),
                                                                        false);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JPopupMenu popup = new JPopupMenu("Add");
                final Rectangle buttonBounds = addButton.getBounds();
                popup.add(new AddCoordinateAction());
                popup.add(new AddPlacemarkFileAction());
                popup.show(addButton, 1, buttonBounds.height + 1);
            }
        });
        final AbstractButton removeButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"),
                                                                           false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = coordinateTable.getSelectedRows();
                for (int selectedRow : selectedRows) {
                    tableModel.removePlacemarkAt(selectedRow);
                }
            }
        });
        final JPanel buttonPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
        buttonPanel.setLayout(layout);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        return new JComponent[]{rasterScrollPane, buttonPanel};
    }

    private JSpinner createWindowSizeEditor(BindingContext bindingContext) {
        final Property squareSizeProperty = bindingContext.getPropertySet().getProperty("windowSize");
        final Number defaultValue = (Number) squareSizeProperty.getDescriptor().getDefaultValue();
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, null, 2));
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final Object value = spinner.getValue();
                if (value instanceof Integer) {
                    int intValue = (Integer) value;
                    if (intValue % 2 == 0) {
                        spinner.setValue(intValue + 1);
                    }
                }
            }
        });
        bindingContext.bind("windowSize", spinner);
        return spinner;
    }

    public JPanel getPanel() {
        return panel;
    }

    private class AddCoordinateAction extends AbstractAction {

        private AddCoordinateAction() {
            super("Add coordinate");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Placemark placemark = new Placemark("Coord_" + tableModel.getRowCount(), "", "",
                                                      new PixelPos(), new GeoPos(0, 0),
                                                      PinDescriptor.INSTANCE, null);
            tableModel.addPlacemark(placemark);
        }
    }

    private class AddPlacemarkFileAction extends AbstractAction {

        private AddPlacemarkFileAction() {
            super("Add coordinates from file");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            PropertyMap preferences = appContext.getPreferences();
            String lastDir = preferences.getPropertyString(LAST_OPEN_PLACEMARK_DIR,
                                                           SystemUtils.getUserHomeDir().getPath());
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(PlacemarkIO.createPlacemarkFileFilter());
            fileChooser.setFileFilter(PlacemarkIO.createTextFileFilter());

            fileChooser.setCurrentDirectory(new File(lastDir));
            int answer = fileChooser.showDialog(panel, "Select");
            if (answer == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                preferences.setPropertyString(LAST_OPEN_PLACEMARK_DIR, selectedFile.getParent());
                FileReader reader = null;
                try {
                    reader = new FileReader(selectedFile);
                    final List<Placemark> placemarks = PlacemarkIO.readPlacemarks(reader, null, PinDescriptor.INSTANCE);
                    for (Placemark placemark : placemarks) {
                        tableModel.addPlacemark(placemark);
                    }
                } catch (IOException ioe) {
                    appContext.handleError(String.format("Error occurred while reading file: %s", selectedFile), ioe);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }

        }
    }
}