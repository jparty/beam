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

package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.NamedSymbol;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.layer.AbstractLayerConfigurationEditor;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;
import org.esa.beam.framework.ui.product.VectorDataFigureEditor;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ObjectUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Customizable {@link org.esa.beam.framework.ui.layer.LayerEditor} for {@link VectorDataNode}s.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class VectorDataLayerEditor extends AbstractLayerConfigurationEditor {

    private static final String FILL_COLOR_NAME = DefaultFigureStyle.FILL_COLOR.getName();
    private static final String FILL_OPACITY_NAME = DefaultFigureStyle.FILL_OPACITY.getName();
    private static final String STROKE_COLOR_NAME = DefaultFigureStyle.STROKE_COLOR.getName();
    private static final String STROKE_WIDTH_NAME = DefaultFigureStyle.STROKE_WIDTH.getName();
    private static final String STROKE_OPACITY_NAME = DefaultFigureStyle.STROKE_OPACITY.getName();
    private static final String SYMBOL_NAME_NAME = DefaultFigureStyle.SYMBOL_NAME.getName();
    private static final SimpleFeatureFigure[] NO_SIMPLE_FEATURE_FIGURES = new SimpleFeatureFigure[0];
    private static final ValueSet SYMBOL_VALUE_SET = new ValueSet(new String[]{
            NamedSymbol.PLUS.getName(),
            NamedSymbol.CROSS.getName(),
            NamedSymbol.STAR.getName(),
            NamedSymbol.SQUARE.getName(),
            NamedSymbol.CIRCLE.getName(),
            NamedSymbol.PIN.getName()
    });

    private final SelectionChangeHandler selectionChangeHandler;
    private final StyleUpdater styleUpdater;
    private final AtomicBoolean isAdjusting;

    public VectorDataLayerEditor() {
        selectionChangeHandler = new SelectionChangeHandler();
        styleUpdater = new StyleUpdater();
        isAdjusting = new AtomicBoolean(false);
    }

    @Override
    protected void addEditablePropertyDescriptors() {
        final Figure[] figures = getSelectedFigures();

        final PropertyDescriptor fillColor = new PropertyDescriptor(DefaultFigureStyle.FILL_COLOR);
        fillColor.setDefaultValue(getCommonStylePropertyValue(figures, FILL_COLOR_NAME));
        addPropertyDescriptor(fillColor);

        final PropertyDescriptor fillOpacity = new PropertyDescriptor(DefaultFigureStyle.FILL_OPACITY);
        fillOpacity.setDefaultValue(getCommonStylePropertyValue(figures, FILL_OPACITY_NAME));
        addPropertyDescriptor(fillOpacity);

        final PropertyDescriptor strokeColor = new PropertyDescriptor(DefaultFigureStyle.STROKE_COLOR);
        strokeColor.setDefaultValue(getCommonStylePropertyValue(figures, STROKE_COLOR_NAME));
        addPropertyDescriptor(strokeColor);

        final PropertyDescriptor strokeWidth = new PropertyDescriptor(DefaultFigureStyle.STROKE_WIDTH);
        strokeWidth.setDefaultValue(getCommonStylePropertyValue(figures, STROKE_WIDTH_NAME));
        addPropertyDescriptor(strokeWidth);

        final PropertyDescriptor strokeOpacity = new PropertyDescriptor(DefaultFigureStyle.STROKE_OPACITY);
        strokeOpacity.setDefaultValue(getCommonStylePropertyValue(figures, STROKE_OPACITY_NAME));
        addPropertyDescriptor(strokeOpacity);

        final PropertyDescriptor symbolName = new PropertyDescriptor(DefaultFigureStyle.SYMBOL_NAME);
        symbolName.setDefaultValue(getCommonStylePropertyValue(figures, SYMBOL_NAME_NAME));
        symbolName.setValueSet(SYMBOL_VALUE_SET);
        symbolName.setNotNull(false);
        addPropertyDescriptor(symbolName);

        getBindingContext().bindEnabledState(SYMBOL_NAME_NAME, false, SYMBOL_NAME_NAME, null);
    }

    @Override
    public void handleLayerContentChanged() {
        final BindingContext bindingContext = getBindingContext();

        if (isAdjusting.compareAndSet(false, true)) {
            try {
                final SimpleFeatureFigure[] selectedFigures = getSelectedFigures();
                updateProperties(selectedFigures, bindingContext);
            } finally {
                isAdjusting.set(false);
            }
        }
    }

    @Override
    public void handleEditorAttached() {
        FigureEditor figureEditor = getAppContext().getSelectedProductSceneView().getFigureEditor();
        if (figureEditor != null) {
            figureEditor.addSelectionChangeListener(selectionChangeHandler);
        }
        getBindingContext().addPropertyChangeListener(styleUpdater);
    }

    @Override
    public void handleEditorDetached() {
        FigureEditor figureEditor = getAppContext().getSelectedProductSceneView().getFigureEditor();
        if (figureEditor != null) {
            figureEditor.removeSelectionChangeListener(selectionChangeHandler);
        }
        getBindingContext().removePropertyChangeListener(styleUpdater);
    }

    protected VectorDataNode getVectorDataNode() {
        final FigureEditor figureEditor = getAppContext().getSelectedProductSceneView().getFigureEditor();
        if (figureEditor instanceof VectorDataFigureEditor) {
            VectorDataFigureEditor editor = (VectorDataFigureEditor) figureEditor;
            return editor.getVectorDataNode();
        } else {
            return null;
        }
    }

    protected void updateProperties(SimpleFeatureFigure[] selectedFigures, BindingContext bindingContext) {
        updateProperty(bindingContext, FILL_COLOR_NAME, getCommonStylePropertyValue(selectedFigures, FILL_COLOR_NAME));
        updateProperty(bindingContext, FILL_OPACITY_NAME, getCommonStylePropertyValue(selectedFigures, FILL_OPACITY_NAME));
        updateProperty(bindingContext, STROKE_COLOR_NAME, getCommonStylePropertyValue(selectedFigures, STROKE_COLOR_NAME));
        updateProperty(bindingContext, STROKE_WIDTH_NAME, getCommonStylePropertyValue(selectedFigures, STROKE_WIDTH_NAME));
        updateProperty(bindingContext, STROKE_OPACITY_NAME, getCommonStylePropertyValue(selectedFigures, STROKE_OPACITY_NAME));
        final Object styleProperty = getCommonStylePropertyValue(selectedFigures, SYMBOL_NAME_NAME);
        if (styleProperty != null) {
            updateProperty(bindingContext, SYMBOL_NAME_NAME, styleProperty);
        }
    }

    protected void updateProperty(BindingContext bindingContext, String propertyName, Object newValue) {
        PropertySet propertySet = bindingContext.getPropertySet();
        if (propertySet.isPropertyDefined(propertyName)) {
            final Object oldValue = propertySet.getValue(propertyName);
            if (!ObjectUtils.equalObjects(oldValue, newValue)) {
                propertySet.setValue(propertyName, newValue);
            }
        }
    }

    protected Object getCommonStylePropertyValue(Figure[] figures, String propertyName) {
        Object commonValue = null;
        for (Figure figure : figures) {
            final Object value = figure.getNormalStyle().getValue(propertyName);
            if (commonValue == null) {
                commonValue = value;
            } else {
                if (!commonValue.equals(value)) {
                    return null;
                }
            }
        }
        return commonValue;
    }

    private SimpleFeatureFigure[] getSelectedFigures() {
        SimpleFeatureFigure[] figures = NO_SIMPLE_FEATURE_FIGURES;
        if (getAppContext() != null) {
            final ProductSceneView sceneView = getAppContext().getSelectedProductSceneView();
            return sceneView.getSelectedFeatureFigures();
        }
        return figures;
    }

    private void transferPropertyValueToStyle(PropertySet propertySet, String propertyName, FigureStyle style) {
        final Object value = propertySet.getValue(propertyName);
        if (value != null) {
            style.setValue(propertyName, value);
        }
    }

    private class SelectionChangeHandler extends AbstractSelectionChangeListener {

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            handleLayerContentChanged();
        }
    }

    /**
     * Used to update the figure style, whenever users change style values using the editor.
     */
    private class StyleUpdater implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Debug.trace(String.format("VectorDataLayerEditor$StyleUpdater (1): property change: name=%s, oldValue=%s, newValue=%s",
                                      evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()));
            if (evt.getNewValue() == null) {
                return;
            }
            final SimpleFeatureFigure[] selectedFigures = getSelectedFigures();
            final BindingContext bindContext = getBindingContext();
            if (isAdjusting.compareAndSet(false, true)) {
                try {
                    for (SimpleFeatureFigure selectedFigure : selectedFigures) {
                        final Object oldFigureValue = selectedFigure.getNormalStyle().getValue(evt.getPropertyName());
                        final Object newValue = evt.getNewValue();
                        if (!newValue.equals(oldFigureValue)) {
                            Debug.trace(String.format("VectorDataLayerEditor$StyleUpdater (2): about to apply change: name=%s, oldValue=%s, newValue=%s",
                                                      evt.getPropertyName(), oldFigureValue, evt.getNewValue()));
                            // Transfer new style to affected selectedFigure.
                            final FigureStyle origStyle = selectedFigure.getNormalStyle();
                            final DefaultFigureStyle style = new DefaultFigureStyle();
                            style.fromCssString(origStyle.toCssString());
                            transferPropertyValueToStyle(bindContext.getPropertySet(), evt.getPropertyName(), style);
                            selectedFigure.setNormalStyle(style);
                            // todo - Actually selectedFigure.setNormalStyle(style); --> should fire event, so that associated
                            // placemark can save the new style. (nf 2011-11-23)
                            setFeatureStyleCss(selectedFigure, style);
                        }
                    }
                } finally {
                    isAdjusting.set(false);
                }
            }
        }

        private void setFeatureStyleCss(SimpleFeatureFigure selectedFigure, DefaultFigureStyle style) {
            final VectorDataNode vectorDataNode = getVectorDataNode();
            if (vectorDataNode != null) {
                // Transfer new style to associated placemark. Awful code :-(
                final Placemark placemark = vectorDataNode.getPlacemarkGroup().getPlacemark(selectedFigure.getSimpleFeature());
                if (placemark != null) {
                    placemark.setStyleCss(style.toCssString());
                } else {
                    final int index = selectedFigure.getSimpleFeature().getFeatureType().indexOf(Placemark.PROPERTY_NAME_STYLE_CSS);
                    if (index != -1) {
                        selectedFigure.getSimpleFeature().setAttribute(index, style.toCssString());
                    }
                }
            }
        }
    }

}
