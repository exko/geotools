/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *    
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.styling;

import java.util.HashMap;
import java.util.Map;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.util.Utilities;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.style.Description;
import org.opengis.style.StyleVisitor;
import org.opengis.util.Cloneable;


/**
 * Provides a Java representation of an SLD TextSymbolizer that defines how
 * text symbols should be rendered.
 *
 * @author Ian Turton, CCG
 * @author Johann Sorel (Geomatys)
 * @source $URL$
 * @version $Id$
 */
public class TextSymbolizerImpl implements TextSymbolizer2, Cloneable {
    
    private DescriptionImpl desc;
    private String name;
    private Unit<Length> uom;
    private Font font;
    
    private final FilterFactory filterFactory;
    private FillImpl fill;
    private HaloImpl halo;
    private LabelPlacement placement;
    private String geometryPropertyName = null;
    private Expression label = null;
    private Graphic graphic = null;
    private Expression priority = null;
    private HashMap<String,String> optionsMap = null; //null=nothing in it
    private Expression abxtract = null;
    private Expression description = null;
    private OtherText otherText = null;

    protected TextSymbolizerImpl() {
        this( CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints()) );
    }
    
    /**
     * Creates a new instance of DefaultTextSymbolizer
     */
    protected TextSymbolizerImpl( FilterFactory factory ) {
        this(factory,null,null,null);
    }
    
    protected TextSymbolizerImpl( FilterFactory factory, Description desc, String name, Unit<Length> uom ) {
        this.filterFactory = factory;
        this.desc = DescriptionImpl.cast(desc);
        this.uom = uom;
        this.name = name;
        fill = new FillImpl();
        fill.setColor(filterFactory.literal("#000000")); // default text fill is black
        halo = null;
        placement = new PointPlacementImpl();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public DescriptionImpl getDescription() {
        return desc;
    }
    
    public void setDescription(org.opengis.style.Description description) {
        this.desc = DescriptionImpl.cast(description);
    }
    public Unit<Length> getUnitOfMeasure() {
        return uom;
    }

    public void setUnitOfMeasure(Unit<Length> uom) {
    	this.uom = uom;
	}

    /**
     * This property defines the geometry to be used for styling.<br>
     * The property is optional and if it is absent (null) then the "default"
     * geometry property of the feature should be used. Geometry types other
     * than inherently point types can be used. The geometryPropertyName is
     * the name of a geometry property in the Feature being styled.
     * Typically, features only have one geometry so, in general, the need to
     * select one is not required. Note: this moves a little away from the SLD
     * spec which provides an XPath reference to a Geometry object, but does
     * follow it in spirit.
     *
     * @return String The name of the attribute in the feature being styled
     *         that should be used.  If null then the default geometry should
     *         be used.
     */
    public String geometryPropertyName() {
        return geometryPropertyName;
    }

    /**
     * Returns the fill to be used to fill the text when rendered.
     *
     * @return The fill to be used.
     */
    public FillImpl getFill() {
        return fill;
    }

    /**
     * Setter for property fill.
     *
     * @param fill New value of property fill.
     */
    public void setFill(org.opengis.style.Fill fill) {
        if (this.fill == fill) {
            return;
        }
        this.fill = FillImpl.cast( fill );
    }

    public Font getFont() {
        return font;
    }
    
    public void setFont( org.opengis.style.Font font ){
        if( this.font == font ){
            return;
        }
        this.font = FontImpl.cast( font );
    }
    /**
     * Returns a device independent Font object that is to be used to render
     * the label.
     *
     * @return Device independent Font object to be used to render the label.
     */
    @Deprecated
    public  org.geotools.styling.Font[] getFonts() {
        
        if(font == null){
            return new org.geotools.styling.Font[0];
        }else{
            return new org.geotools.styling.Font[]{(org.geotools.styling.Font)font} ;
        }
    }

    /**
     * Setter for property font.
     *
     * @param font New value of property font.
     */
    @Deprecated
    public void addFont(org.geotools.styling.Font font) {
        this.font = font;
    }

    /**
     * Sets the list of fonts in the TextSymbolizer to the provided array of
     * Fonts.
     *
     * @param fonts The array of fonts to use in the symbolizer.
     */
    @Deprecated
    public void setFonts(org.geotools.styling.Font[] fonts) {
        
        if(fonts != null && fonts.length >0){
            this.font = fonts[0]; 
        }else{
            this.font = null;
        }
    }

    /**
     * A halo fills an extended area outside the glyphs of a rendered text
     * label to make the label easier to read over a background.
     *
     */
    public HaloImpl getHalo() {
        return halo;
    }

    /**
     * Setter for property halo.
     *
     * @param halo New value of property halo.
     */
    public void setHalo(org.opengis.style.Halo halo) {
        if (this.halo == halo) {
            return;
        }
        this.halo = HaloImpl.cast(halo);
    }

    /**
     * Returns the label expression.
     *
     * @return Label expression.
     */
    public Expression getLabel() {
        return label;
    }

    /**
     * Setter for property label.
     *
     * @param label New value of property label.
     */
    public void setLabel(Expression label) {
        this.label = label;
    }

    /**
     * A pointPlacement specifies how a text element should be rendered
     * relative to its geometric point.
     *
     * @return Value of property labelPlacement.
     */
    @Deprecated
    public LabelPlacement getPlacement() {
        return getLabelPlacement();
    }

    /**
     * Setter for property labelPlacement.
     *
     * @param labelPlacement New value of property labelPlacement.
     * @deprecated Use setLabelPlacement
     */
    public void setPlacement(LabelPlacement labelPlacement) {
        setLabelPlacement( placement );
    }

    /**
     * A pointPlacement specifies how a text element should be rendered
     * relative to its geometric point.
     *
     * @return Value of property labelPlacement.
     *
     */
    public LabelPlacement getLabelPlacement() {
        return placement;
    }

    /**
     * Setter for property labelPlacement.
     *
     * @param labelPlacement New value of property labelPlacement.
     */

    public void setLabelPlacement( org.opengis.style.LabelPlacement labelPlacement) {
        if (this.placement == labelPlacement) {
            return;
        }
        if( labelPlacement instanceof LinePlacement){
            this.placement = LinePlacementImpl.cast( labelPlacement );
        }
        else {
            this.placement = PointPlacementImpl.cast( labelPlacement );
        }
    }

    /**
     * Getter for property geometryPropertyName.
     *
     * @return Value of property geometryPropertyName.
     */
    public java.lang.String getGeometryPropertyName() {
        return geometryPropertyName;
    }

    /**
     * Setter for property geometryPropertyName.
     *
     * @param geometryPropertyName New value of property geometryPropertyName.
     */
    public void setGeometryPropertyName(java.lang.String geometryPropertyName) {
        this.geometryPropertyName = geometryPropertyName;
    }

    /**
     * Accept a StyleVisitor to perform an operation on this symbolizer.
     *
     * @param visitor The StyleVisitor to accept.
     */
    public Object accept(StyleVisitor visitor,Object data) {
        return visitor.visit(this,data);
    }

    public void accept(org.geotools.styling.StyleVisitor visitor) {
        visitor.visit(this);
    }
    
    /**
     * Creates a deep copy clone.   TODO: Need to complete the deep copy,
     * currently only shallow copy.
     *
     * @return The deep copy clone.
     *
     * @throws AssertionError DOCUMENT ME!
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e); // this should never happen.
        }
    }

    public int hashCode() {
        final int PRIME = 1000003;
        int result = 0;

        if (fill != null) {
            result = (PRIME * result) + fill.hashCode();
        }

        if (font != null) {
            result = (PRIME * result) + font.hashCode();
        }
        
        if (uom != null) {
            result = (PRIME * result) + uom.hashCode();
        }
        
        if (desc != null) {
            result = (PRIME * result) + desc.hashCode();
        }
        
        if (name != null) {
            result = (PRIME * result) + name.hashCode();
        }

        if (halo != null) {
            result = (PRIME * result) + halo.hashCode();
        }

        if (placement != null) {
            result = (PRIME * result) + placement.hashCode();
        }

        if (geometryPropertyName != null) {
            result = (PRIME * result) + geometryPropertyName.hashCode();
        }

        if (label != null) {
            result = (PRIME * result) + label.hashCode();
        }

        return result;
    }

    public boolean equals(Object oth) {
        if (this == oth) {
            return true;
        }

        if (oth == null) {
            return false;
        }

        if (oth instanceof TextSymbolizerImpl) {
            TextSymbolizerImpl other = (TextSymbolizerImpl) oth;

            return Utilities.equals(this.geometryPropertyName,
                other.geometryPropertyName)
            && Utilities.equals(this.label, other.label)
            && Utilities.equals(this.halo, other.halo)
            && Utilities.equals(this.font, other.font)
            && Utilities.equals(this.desc, other.desc)
            && Utilities.equals(this.uom, other.uom)
            && Utilities.equals(this.placement, other.placement)
            && Utilities.equals(this.fill, other.fill);
        }

        return false;
    }

    
    
    public void setPriority(Expression priority) {
        if (this.priority == priority) {
            return;
        }
        this.priority = priority;
    }

    public Expression getPriority() {
        return priority;
    }

    public void addToOptions(String key, String value) {
        if (optionsMap == null) {
            optionsMap = new HashMap<String,String>();
        }
        optionsMap.put(key, value.trim());
    }

    public String getOption(String key) {
        if (optionsMap == null) {
            return null;
        }

        return (String) optionsMap.get(key);
    }

    public Map<String,String> getOptions() {
        return optionsMap;
    }

    public Graphic getGraphic() {
        return graphic;
    }

    public void setGraphic(Graphic graphic) {
        if (this.graphic == graphic) {
            return;
        }
        this.graphic = graphic;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("<TextSymbolizerImp property=");
        buf.append( geometryPropertyName );
        buf.append( " label=");
        buf.append( label );
        buf.append(">");
        buf.append( this.font );
        return buf.toString();
    }
    
    public Expression getSnippet() {
        return abxtract;
    }
    
    public void setSnippet(Expression abxtract) {
        this.abxtract = abxtract;
    }
    
    public Expression getFeatureDescription() {
        return description;
    }
    
    public void setFeatureDescription(Expression description) {
        this.description = description;
    }
    
    public OtherText getOtherText() {
        return otherText;
    }
    
    public void setOtherText(OtherText otherText) {
        this.otherText = otherText;
    }

    static TextSymbolizerImpl cast(org.opengis.style.Symbolizer symbolizer) {
        if( symbolizer == null ){
            return null;
        }
        else if (symbolizer instanceof TextSymbolizerImpl){
            return (TextSymbolizerImpl) symbolizer;
        }
        else {
            org.opengis.style.TextSymbolizer textSymbolizer = (org.opengis.style.TextSymbolizer) symbolizer;
            TextSymbolizerImpl copy = new TextSymbolizerImpl();
            copy.setDescription( textSymbolizer.getDescription());
            copy.setFill( textSymbolizer.getFill() );
            copy.setFont( textSymbolizer.getFont() );
            copy.setGeometryPropertyName( textSymbolizer.getGeometryPropertyName() );
            copy.setHalo(textSymbolizer.getHalo() );
            copy.setLabel(textSymbolizer.getLabel() );
            copy.setLabelPlacement(textSymbolizer.getLabelPlacement() );
            copy.setName(textSymbolizer.getName() );
            copy.setUnitOfMeasure( textSymbolizer.getUnitOfMeasure() );
            
            return copy;
        }
    }

}
