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

package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import junit.framework.TestCase;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

/**
 * @author Marco Peters
 */
public class CfFlagCodingPartTest extends TestCase {

    public void testWriteFlagCoding() throws Exception {
        Band flagBand = new Band("flag_band", ProductData.TYPE_UINT8, 10, 10);
        FlagCoding flagCoding = new FlagCoding("some_flags");
        flagBand.setSampleCoding(flagCoding);
        flagCoding.setDescription("A Flag Coding");
        MetadataAttribute attribute;

        attribute = new MetadataAttribute("FIRST_FLAG", ProductData.TYPE_UINT8);
        attribute.getData().setElemInt(1);
        flagCoding.addAttribute(attribute);

        attribute = new MetadataAttribute("SECOND_FLAG", ProductData.TYPE_UINT8);
        attribute.getData().setElemInt(2);
        flagCoding.addAttribute(attribute);

        NetcdfFileWriteable writeable = NetcdfFileWriteable.createNew("not stored");
        writeable.addDimension("y", flagBand.getSceneRasterHeight());
        writeable.addDimension("x", flagBand.getSceneRasterWidth());
        final DataType ncDataType = DataTypeUtils.getNetcdfDataType(flagBand.getDataType());
        CfBandPart.writeCfBandAttributes(flagBand, writeable.addVariable(flagBand.getName(), ncDataType,
                                                                         writeable.getRootGroup().getDimensions()));
        CfFlagCodingPart.writeFlagCoding(flagBand, writeable);

        Variable someFlagsVariable = writeable.findVariable("flag_band");
        assertNotNull(someFlagsVariable);
        Attribute flagMasksAttrib = someFlagsVariable.findAttribute("flag_masks");
        assertNotNull(flagMasksAttrib);
        assertEquals(someFlagsVariable.getDataType(), flagMasksAttrib.getDataType());
        assertEquals(2, flagMasksAttrib.getLength());

        Attribute descriptionAttrib = someFlagsVariable.findAttribute("long_name");
        assertNotNull(flagCoding.getDescription(), descriptionAttrib.getStringValue());

    }
}