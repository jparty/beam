/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.actions;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.io.CsvReader;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;


/**
 * Experimental action that lets a user load text files that contain data associated with a geo-point (e.g. ship tracks).
 * <pre>
 *     lat.1 TAB lon.1 TAB data.1 NEWLINE
 *     lat.2 TAB lon.2 TAB data.2 NEWLINE
 *     lat.3 TAB lon.3 TAB data.3 NEWLINE
 *     ...
 *     lat.n TAB lon.n TAB data.n NEWLINE
 * </pre>
 *
 * @author Norman Fomferra
 * @version $Revision: 8410 $ $Date: 2010-02-14 14:31:41 +0100 (So, 14 Feb 2010) $
 */
public class ImportTrackAction extends ExecCommand {

    public static final String TITLE = "Open Track File";

    @Override
    public void actionPerformed(final CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();

        File file = visatApp.showFileOpenDialog(TITLE, false, null, "importTrack.lastDir");
        if (file == null) {
            return;
        }

        Product product = visatApp.getSelectedProduct();

        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        try {
            featureCollection = readTrack(file, product.getGeoCoding());
        } catch (IOException e) {
            visatApp.showErrorDialog(TITLE, "Failed to load track file:\n" + e.getMessage());
            return;
        }

        if (featureCollection.isEmpty()) {
            visatApp.showErrorDialog(TITLE, "No records found.");
            return;
        }

        String name = FileUtils.getFilenameWithoutExtension(file);
        VectorDataNode vectorDataNode = new VectorDataNode(name, featureCollection);
        product.getVectorDataGroup().add(vectorDataNode);
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProduct() != null
                           && VisatApp.getApp().getSelectedProduct().getGeoCoding() != null);
    }

    private static FeatureCollection<SimpleFeatureType, SimpleFeature> readTrack(File file, GeoCoding geoCoding) throws IOException {
        Reader reader = new FileReader(file);
        try {
            return readTrack(reader, geoCoding);
        } finally {
            reader.close();
        }
    }

    static FeatureCollection<SimpleFeatureType, SimpleFeature> readTrack(Reader reader, GeoCoding geoCoding) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{'\t', ' '}, true, "#");
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = FeatureCollections.newCollection();
        SimpleFeatureType trackFeatureType = createTrackFeatureType(geoCoding);
        double[] record;
        int i = 0;
        while ((record = csvReader.readDoubleRecord()) != null) {
            if (record.length < 3) {
                throw new IOException("Illegal track file format.");
            }

            float lat = (float) record[0];
            float lon = (float) record[1];
            double data = record[2];

            featureCollection.add(createFeature(trackFeatureType, geoCoding, i, lat, lon, data));
            i++;
        }

        return featureCollection;
    }

    private static SimpleFeatureType createTrackFeatureType(GeoCoding geoCoding) {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName("Track");
        ftb.add("timeStamp", Long.class);
        ftb.add("pixelPos", Point.class, geoCoding.getImageCRS());
        ftb.add("geoPos", Point.class, DefaultGeographicCRS.WGS84);
        ftb.add("data", Double.class);
        ftb.setDefaultGeometry(geoCoding instanceof CrsGeoCoding ? "geoPos" : "pixelPos");
        return ftb.buildFeatureType();
    }

    private static SimpleFeature createFeature(SimpleFeatureType type, GeoCoding geoCoding, long i, float lat, float lon, double data) {
        PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(lat, lon), null);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
        GeometryFactory gf = new GeometryFactory();
        fb.add(i);
        fb.add(gf.createPoint(new Coordinate(pixelPos.x, pixelPos.y)));
        fb.add(gf.createPoint(new Coordinate(lon, lat)));
        fb.add(data);
        return fb.buildFeature(Long.toHexString(System.nanoTime()));
    }


}