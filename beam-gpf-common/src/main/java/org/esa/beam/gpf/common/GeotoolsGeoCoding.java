package org.esa.beam.gpf.common;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.AbstractGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.AbstractCoordinateOperationFactory;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.CRSUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;

public class GeotoolsGeoCoding extends AbstractGeoCoding {

    private final Datum datum;
    private final BeamGridGeometry gridGeometry;
    private MathTransform image2Base;
    private MathTransform base2image;

    public GeotoolsGeoCoding(CoordinateReferenceSystem modelCrs, BeamGridGeometry gridGeometry) throws FactoryException, NoninvertibleTransformException {
        this.gridGeometry = gridGeometry;
        setModelCRS(modelCrs);
        org.opengis.referencing.datum.Ellipsoid gtEllipsoid = CRS.getEllipsoid(modelCrs);
        String ellipsoidName = gtEllipsoid.getName().getCode();
        Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName, gtEllipsoid.getSemiMajorAxis(),
                                            gtEllipsoid.getSemiMinorAxis());
        org.opengis.referencing.datum.Datum gtDatum = CRSUtilities.getDatum(modelCrs);
        String datumName = gtDatum.getName().getCode();
        this.datum = new Datum(datumName, ellipsoid, 0, 0, 0);

        MathTransform imageToModel = new AffineTransform2D(gridGeometry.getImageToModel());
        MathTransform model2Base = CRS.findMathTransform(gridGeometry.getCRS(), getBaseCRS());

        final CoordinateOperationFactory factory = ReferencingFactoryFinder.getCoordinateOperationFactory(null);
        final MathTransformFactory mtFactory;
        if (factory instanceof AbstractCoordinateOperationFactory) {
            mtFactory = ((AbstractCoordinateOperationFactory) factory).getMathTransformFactory();
        } else {
            mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        }
        image2Base = mtFactory.createConcatenatedTransform(imageToModel, model2Base);
        base2image = image2Base.inverse();
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }

    @Override
    public Datum getDatum() {
        return datum;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        try {
            DirectPosition directPixelPos = new DirectPosition2D(pixelPos);
            DirectPosition directGeoPos = image2Base.transform(directPixelPos, null);
            geoPos.setLocation((float) directGeoPos.getOrdinate(1), (float) directGeoPos.getOrdinate(0));
        } catch (Exception e) {
            geoPos.setInvalid();
        }
        return geoPos;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        try {
            DirectPosition directGeoPos = new DirectPosition2D(geoPos.getLon(), geoPos.getLat());
            DirectPosition directPixelPos = base2image.transform(directGeoPos, null);
            pixelPos.setLocation((float) directPixelPos.getOrdinate(0), (float) directPixelPos.getOrdinate(1));
        } catch (Exception e) {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        // TODO Auto-generated method stub
        return false;
    }
}
