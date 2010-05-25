package org.esa.beam.dataio.netcdf4.convention.beam;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.AbstractModelFactory;
import org.esa.beam.dataio.netcdf4.convention.InitialisationPart;
import org.esa.beam.dataio.netcdf4.convention.Model;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfMetadataPart;
import org.esa.beam.framework.dataio.DecodeQualification;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

/**
 * User: Thomas Storm
 * Date: 29.03.2010
 * Time: 09:00:07
 */
public class BeamModelFactory extends AbstractModelFactory {

    public BeamModelFactory() {
        super();
    }

    @Override
    public ModelPart getMetadataPart() {
        return new CfMetadataPart();
    }

    @Override
    public ModelPart getBandPart() {
        return new BeamBandPart();
    }

    @Override
    public ModelPart getDescriptionPart() {
        return null;
    }

    @Override
    public ModelPart getEndTimePart() {
        return null;
    }

    @Override
    public ModelPart getFlagCodingPart() {
        return new BeamFlagCodingPart();
    }

    @Override
    public ModelPart getGeocodingPart() {
        return new BeamGeocodingPart();
    }

    @Override
    public ModelPart getImageInfoPart() {
        return new BeamImageInfoPart();
    }

    @Override
    public ModelPart getIndexCodingPart() {
        return new BeamIndexCodingPart();
    }

    @Override
    public InitialisationPart getInitialisationPart() {
        return new BeamInitialisationPart();
    }

    @Override
    public ModelPart getMaskOverlayPart() {
        return new BeamMaskOverlayPart();
    }

    @Override
    public ModelPart getStartTimePart() {
        return null;
    }

    @Override
    public ModelPart getStxPart() {
        return new BeamStxPart();
    }

    @Override
    public ModelPart getTiePointGridPart() {
        return new BeamTiePointGridPart();
    }

    @Override
    protected DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        Attribute attribute = netcdfFile.findGlobalAttribute("metadata_profile");
        if (attribute != null) {
            String value = attribute.getStringValue();
            if (value != null && value.equals("beam")) {
                return DecodeQualification.INTENDED;
            }
        }
        return DecodeQualification.UNABLE;
    }
}