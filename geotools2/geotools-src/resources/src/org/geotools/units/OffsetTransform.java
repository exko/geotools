/*
 * Units - Temporary implementation for Geotools 2
 * Copyright (C) 1998 University Corporation for Atmospheric Research (Unidata)
 *               1998 Bill Hibbard & al. (VisAD)
 *               1999 P�ches et Oc�ans Canada
 *               2000 Institut de Recherche pour le D�veloppement
 *               2002 Centre for Computational Geography
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 *    This package is inspired from the units package of VisAD.
 *    Unidata and Visad's work is fully acknowledged here.
 *
 *                   THIS IS A TEMPORARY CLASS
 *
 *    This is a placeholder for future <code>Unit</code> class.
 *    This skeleton will be removed when the real classes from
 *    JSR-108: Units specification will be publicly available.
 */
package org.geotools.units;

// Divers
import java.io.ObjectStreamException;
import org.geotools.resources.rsc.Resources;
import org.geotools.resources.rsc.ResourceKeys;


/**
 * Repr�sente une transformation entre deux unit�s
 * qui diff�rent seulement par un d�calage.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class OffsetTransform extends UnitTransform {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -5512331418450968824L;
    
    /**
     * Nombre � soustraire � {@link #fromUnit}
     * pour obtenir {@link #toUnit}.
     */
    public final double offset;
    
    /**
     * Construit un objet qui aura la charge de convertir
     * des donn�es exprim�es selon les unit�s sp�cifi�es.
     *
     * @throws UnitException si <code>offset</code> n'est pas valide.
     */
    private OffsetTransform(final Unit fromUnit, final Unit toUnit, final double offset) throws UnitException {
        super(fromUnit, toUnit);
        this.offset = offset;
        if (Double.isNaN(offset) || Double.isInfinite(offset)) {
            throw new UnitException(Resources.format(
                                    ResourceKeys.ERROR_NOT_A_NUMBER_$1,
                                    new Double(offset)));
        }
    }
    
    /**
     * Construit un objet qui aura la charge de convertir
     * des donn�es exprim�es selon les unit�s sp�cifi�es.
     *
     * @throws UnitException si <code>offset</code> n'est pas valide.
     */
    public static UnitTransform getInstance(final Unit fromUnit, final Unit toUnit, final double offset) throws UnitException {
        if (offset==0) {
            return IdentityTransform.getInstance(fromUnit, toUnit);
        } else {
            return new OffsetTransform(fromUnit, toUnit, offset).intern();
        }
    }
    
    /**
     * Indique si cette transformation repr�sente la transformation
     * identit�e. Cette m�thode retourne toujours <code>false</code>,
     * sauf si {@link #offset} �gal 0.
     */
    public boolean isIdentity() {
        return offset==0;
    }
    
    /**
     * Effectue la conversion d'unit�s d'une valeur.
     * @param value Valeur exprim�e selon les unit�s {@link #fromUnit}.
     * @return Valeur exprim�e selon les unit�s {@link #toUnit}.
     */
    public double convert(final double value) {
        return value-offset;
    }
    
    /**
     * Effectue la conversion d'unit�s d'un tableaux de valeurs.
     * @param values Valeurs exprim�es selon les unit�s {@link #fromUnit}.
     *        Elles seront converties sur place en valeurs exprim�es selon
     *        les unit�s <code>this</code>.
     */
    public void convert(final double[] values) {
        for (int i=0; i<values.length; i++) {
            values[i] -= offset;
        }
    }
    
    /**
     * Effectue la conversion d'unit�s d'un tableaux de valeurs.
     * @param values Valeurs exprim�es selon les unit�s {@link #fromUnit}.
     *        Elles seront converties sur place en valeurs exprim�es selon
     *        les unit�s <code>this</code>.
     */
    public void convert(final float[] values) {
        for (int i=0; i<values.length; i++) {
            values[i] = (float) (values[i]-offset);
        }
    }
    
    /**
     * Effectue la conversion inverse d'unit�s d'une valeur.
     * @param value Valeur exprim�e selon les unit�s {@link #toUnit}.
     * @return Valeur exprim�e selon les unit�s {@link #fromUnit}.
     */
    public double inverseConvert(final double value) {
        return value+offset;
    }
    
    /**
     * Effectue la conversion inverse d'unit�s d'un tableaux de valeurs.
     * @param values Valeurs exprim�es selon les unit�s {@link #toUnit}.
     *        Elles seront converties sur place en valeurs exprim�es selon
     *        les unit�s {@link #fromUnits}.
     */
    public void inverseConvert(final double[] values) {
        for (int i=0; i<values.length; i++) {
            values[i] += offset;
        }
    }
    
    /**
     * Effectue la conversion inverse d'unit�s d'un tableaux de valeurs.
     * @param values Valeurs exprim�es selon les unit�s {@link #toUnit}.
     *        Elles seront converties sur place en valeurs exprim�es selon
     *        les unit�s {@link #fromUnit}.
     */
    public void inverseConvert(final float[] values) {
        for (int i=0; i<values.length; i++) {
            values[i] = (float) (values[i]+offset);
        }
    }
    
    /**
     * V�rifie si cette transformation d'unit�s est
     * identique � la transformation sp�cifi�e.
     */
    public boolean equals(final Object o) {
        return super.equals(o) && Double.doubleToLongBits(offset)==Double.doubleToLongBits(((OffsetTransform) o).offset);
    }
}
