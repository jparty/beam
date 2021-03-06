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

package org.esa.beam.processor.binning.store;

import junit.framework.TestCase;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.BinLocator;
import org.esa.beam.processor.binning.database.FloatArrayBin;
import org.esa.beam.processor.binning.database.SeaWiFSBinLocator;

import java.awt.Point;
import java.io.File;
import java.io.IOException;


public class QuadTreeBinStoreTest extends TestCase {
    private BinStore store;
    private Bin bin;
    private Point point0;
    private Point point1;
    private File dbDir;
    private String dbName = "testQuad";

    @Override
    public void setUp() throws IOException, ProcessorException {
        BinLocator locator = new SeaWiFSBinLocator(100);
        dbDir = new File("testdata");
        store = new QuadTreeBinStore(dbDir, dbName, locator.getWidth(), locator.getHeight(), 5);
        bin = new FloatArrayBin(new int[]{2, 3});
        point0 = new Point(0, 0);
        point1 = new Point(1, 1);
    }

    @Override
    public void tearDown() throws IOException {
        store.delete();
        dbDir.delete();
    }

    public void testAll() throws IOException, ProcessorException {

        bin.load(new float[]{1f, 2f, 3f, 4f, 5f});
        store.write(point0, bin);
        assertTrue("contains data", bin.containsData());
        bin.clear();
        assertFalse("contains no data", bin.containsData());
        store.read(point1, bin);
        assertFalse("contains no data", bin.containsData());
        store.read(point0, bin);
        assertTrue("contains data", bin.containsData());
        bin.setBandIndex(0);
        assertEquals("1f", 1f, bin.read(0), 0.00001f);
        assertEquals("2f", 2f, bin.read(1), 0.00001f);
        bin.setBandIndex(1);
        assertEquals("3f", 3f, bin.read(0), 0.00001f);
        assertEquals("4f", 4f, bin.read(1), 0.00001f);
        assertEquals("5f", 5f, bin.read(2), 0.00001f);
        store.flush();
        store.close();

        BinStore store2 = new QuadTreeBinStore(dbDir, dbName);
        bin.clear();
        assertFalse("contains no data", bin.containsData());
        store2.read(point0, bin);
        assertTrue("contains data", bin.containsData());
        bin.setBandIndex(0);
        assertEquals("1f", 1f, bin.read(0), 0.00001f);
        assertEquals("2f", 2f, bin.read(1), 0.00001f);
        bin.setBandIndex(1);
        assertEquals("3f", 3f, bin.read(0), 0.00001f);
        assertEquals("4f", 4f, bin.read(1), 0.00001f);
        assertEquals("5f", 5f, bin.read(2), 0.00001f);
        store2.close();
    }
}
