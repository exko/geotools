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

// Entr�s/sorties
import java.io.ObjectStreamException;


/**
 * Repr�sente une transformation entre deux unit�s
 * dont les valeurs n'ont qu'� �tre invers�es.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class InverseTransform extends UnitTransform {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 8539885091744917242L;
    
    /**
     * Construit un objet qui aura la charge de convertir
     * des donn�es exprim�es selon les unit�s sp�cifi�es.
     */
    private InverseTransform(final Unit fromUnit, final Unit toUnit) {
        super(fromUnit, toUnit);
    }
    
    /**
     * Construit un objet qui aura la charge de convertir
     * des donn�es exprim�es selon les unit�s sp�cifi�es.
     */
    public static UnitTransform getInstance(final Unit fromUnit, final Unit toUnit) {
        return new InverseTransform(fromUnit, toUnit).intern();
    }
    
    /**
     * Effectue la conversion d'unit�s d'une valeur.
     * @param value Valeur exprim�e selon les unit�s {@link #fromUnit}.
     * @return Valeur exprim�e selon les unit�s {@link #toUnit}.
     */
    public double convert(final double value) {
        return 1/value;
    }
    
    /**
     * Effectue la conversion d'unit�s d'un tableaux de valeurs.
     * @param values Valeurs exprim�es selon les unit�s {@link #fromUnit}.
     *        Elles seront converties sur place en valeurs exprim�es selon
     *        les unit�s {@link #toUnits}.
     */
    public void convert(final double[] values) {
        for (int i=0; i<values.length; i++) {
            values[i]=1/values[i];
        }
    }
    
    /**
     * Effectue la conversion d'unit�s d'un tableaux de valeurs.
     * @param values Valeurs exprim�es selon les unit�s {@link #fromUnit}.
     *        Elles seront converties sur place en valeurs exprim�es selon
     *        les unit�s {@link #toUnits}.
     */
    public void convert(final float[] values) {
        for (int i=0; i<values.length; i++) {
            values[i]=1/values[i];
        }
    }
    
    /**
     * Effectue la conversion inverse d'unit�s d'une valeur.
     * @param value Valeur exprim�e selon les unit�s {@link #toUnit}.
     * @return Valeur exprim�e selon les unit�s {@link #fromUnit}.
     */
    public double inverseConvert(final double value) {
        return 1/value;
    }
    
    /**
     * Effectue la conversion inverse d'unit�s d'un tableaux de valeurs.
     * @param values Valeurs exprim�es selon les unit�s {@link #toUnit}.
     *        Elles seront converties sur place en valeurs exprim�es selon
     *        les unit�s {@link #fromUnits}.
     */
    public void inverseConvert(final double[] values) {
        for (int i=0; i<values.length; i++) {
            values[i]=1/values[i];
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
            values[i]=1/values[i];
        }
    }
}
