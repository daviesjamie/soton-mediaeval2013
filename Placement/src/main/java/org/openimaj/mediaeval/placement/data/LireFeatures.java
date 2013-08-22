package org.openimaj.mediaeval.placement.data;

import net.semanticmetadata.lire.imageanalysis.AutoColorCorrelogram;
import net.semanticmetadata.lire.imageanalysis.BasicFeatures;
import net.semanticmetadata.lire.imageanalysis.CEDD;
import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.imageanalysis.EdgeHistogram;
import net.semanticmetadata.lire.imageanalysis.FCTH;
import net.semanticmetadata.lire.imageanalysis.FuzzyOpponentHistogram;
import net.semanticmetadata.lire.imageanalysis.Gabor;
import net.semanticmetadata.lire.imageanalysis.joint.JointHistogram;
import net.semanticmetadata.lire.imageanalysis.joint.RankAndOpponent;
import net.semanticmetadata.lire.imageanalysis.ScalableColor;
import net.semanticmetadata.lire.imageanalysis.SimpleColorHistogram;
import net.semanticmetadata.lire.imageanalysis.Tamura;

public enum LireFeatures {
    ACC ( "AutoColorCorrelogram", AutoColorCorrelogram.class ),
    BF ( "BasicFeatures", BasicFeatures.class ),
    CEDD ( "CEDD", CEDD.class ),
    COL ( "ColorLayout", ColorLayout.class ),
    EDGEHISTOGRAM ( "EdgeHistogram", EdgeHistogram.class ),
    FCTH ( "FCTH", FCTH.class ),
    OPHIST ( "FuzzyOpponentHistogram", FuzzyOpponentHistogram.class ),
    GABOR ( "Gabor", Gabor.class ),
    JHIST ( "JointHistogram", JointHistogram.class ),
    JOPHIST ( "JointOpponentHistogram", RankAndOpponent.class ),
    SCALABLECOLOR ( "ScalableColor", ScalableColor.class ),
    RGB( "SimpeColorHistogram", SimpleColorHistogram.class ),
    TAMURA ( "Tamura", Tamura.class );

    public static final String CSVREGEX = 
        " (acc|bf|cedd|col|edgehistogram|fcth|ophist|gabor|jhist|jophist|scalablecolor|RGB|tamura) ";

    public final String name;
    public final Class<?> fclass;

    private LireFeatures( String name, Class<?> fclass ) {
        this.name = name;
        this.fclass = fclass;
    }
}
