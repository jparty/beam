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

package org.esa.beam.framework.dataop.resamp;



/**
 * This class implements the Nearest Neighbour resampling method.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
final class NearestNeighbourResampling implements Resampling {

    public String getName() {
        return "NEAREST_NEIGHBOUR";
    }

    public final Index createIndex() {
        return new Index(0, 0);
    }

    public final void computeIndex(final float x,
                                   final float y,
                                   int width, int height, final Index index) {
        index.x = x;
        index.y = y;
        index.width = width;
        index.height = height;

        index.i0 = Index.crop((int) Math.floor(x), width - 1);
        index.j0 = Index.crop((int) Math.floor(y), height - 1);
    }

    public final float resample(final Raster raster,
                                final Index index) throws Exception {
        return raster.getSample(index.i0, index.j0);
    }

    @Override
    public String toString() {
        return "Nearest neighbour resampling";
    }
}
