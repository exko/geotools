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


/**
 * Classe repr�sentant des unit�s d'angle �crit sous la forme "degr�s,
 * minutes, secondes". Ce travail ne devrait pas �tre le travail d'un
 * objet {@link Unit}, mais plut�t celui de {@link org.geotools.pt.AngleFormat}.
 * Mais la base de donn�es EPSG insiste pour utiliser des nombres de
 * ce type, et nous en avons parfois besoin pour interagir avec des
 * logiciels existants...
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class DMSUnit extends Unit {
    /**
     * Unit� des degr�s.
     */
    private static final Unit DEGREE = Unit.DEGREE;
    
    /**
     * Construit un objet <code>DMSUnit</code>.
     */
    public DMSUnit() {
        super("DMS", null);
    }
    
    /**
     * Indique si les unit�s <code>this</code> et <code>that</code> sont compatibles.
     */
    public boolean canConvert(final Unit that) {
        return DEGREE.canConvert(that);
    }
    
    /**
     * Effectue la conversion d'une mesure exprim�e selon d'autres unit�s.
     */
    public double convert(double value, final Unit fromUnit) throws UnitException {
        value = DEGREE.convert(value, fromUnit);
        final int deg,min,sec;  deg = (int) value; // Round toward 0
        value = (value-deg)*60; min = (int) value; // Round toward 0
        value = (value-min)*60; sec = (int) value; // Round toward 0
        return ((deg*100 + min)*100 + sec) + value;
    }
    
    /**
     * Effectue sur-place la conversion de mesures exprim�es selon d'autres
     * unit�s. Les valeurs converties remplaceront les anciennes valeurs.
     */
    public void convert(final double[] values, final Unit fromUnit) throws UnitException {
        DEGREE.convert(values, fromUnit);
        for (int i=0; i<values.length; i++) {
            double value;
            final int deg,min,sec;
            value = values[i];      deg = (int) value; // Round toward 0
            value = (value-deg)*60; min = (int) value; // Round toward 0
            value = (value-min)*60; sec = (int) value; // Round toward 0
            values[i] = ((deg*100 + min)*100 + sec) + value;
        }
    }
    
    /**
     * Effectue sur-place la conversion de mesures exprim�es selon d'autres
     * unit�s. Les valeurs converties remplaceront les anciennes valeurs.
     */
    public void convert(float[] values, Unit fromUnit) throws UnitException {
        DEGREE.convert(values, fromUnit);
        for (int i=0; i<values.length; i++) {
            float value;
            final int deg,min,sec;
            value = values[i];      deg = (int) value; // Round toward 0
            value = (value-deg)*60; min = (int) value; // Round toward 0
            value = (value-min)*60; sec = (int) value; // Round toward 0
            values[i] = ((deg*100 + min)*100 + sec) + value;
        }
    }
    
    /**
     * Convertit une mesure vers d'autre unit�s.
     */
    double inverseConvert(double value, final Unit toUnit) throws UnitException {
        final int deg,min;
        deg = (int) (value/10000); value -= 10000*deg;
        min = (int) (value/  100); value -=   100*min;
        if (min<=-60 || min>=60) {
            throw new UnitException("Invalid minutes: "+min);
        }
        if (value<=-60 || value>=60) // Accept NaN
        {
            throw new UnitException("Invalid secondes: "+value);
        }
        value = ((value/60) + min)/60 + deg;
        return DEGREE.inverseConvert(value, toUnit);
    }
    
    /**
     * Effectue sur-place la conversion de mesures vers d'autres unit�s.
     */
    void inverseConvert(final double[] values, final Unit toUnit) throws UnitException {
        for (int i=0; i<values.length; i++) {
            double value = values[i];
            final int deg,min;
            deg = (int) (value/10000); value -= 10000*deg;
            min = (int) (value/  100); value -=   100*min;
            if (min<=-60 || min>=60) {
                throw new UnitException("Invalid minutes: "+min);
            }
            if (value<=-60 || value>=60) // Accept NaN
            {
                throw new UnitException("Invalid secondes: "+value);
            }
            values[i] = ((value/60) + min)/60 + deg;
        }
        DEGREE.inverseConvert(values, toUnit);
    }
    
    /**
     * Effectue sur-place la conversion de mesures vers d'autres unit�s.
     */
    void inverseConvert(final float[] values, final Unit toUnit) throws UnitException {
        for (int i=0; i<values.length; i++) {
            float value = values[i];
            final int deg,min;
            deg = (int) (value/10000); value -= 10000*deg;
            min = (int) (value/  100); value -=   100*min;
            if (min<=-60 || min>=60) {
                throw new UnitException("Invalid minutes: "+min);
            }
            if (value<=-60 || value>=60) // Accept NaN
            {
                throw new UnitException("Invalid secondes: "+value);
            }
            values[i] = ((value/60) + min)/60 + deg;
        }
        DEGREE.inverseConvert(values, toUnit);
    }
    
    /**
     * Retourne un objet qui saura convertir selon ces unit�s les valeurs exprim�es
     * selon d'autres unit�s. Cette m�thode est avantageuse si on pr�voie fa�re
     * plusieurs conversions, car la transformation � utiliser est d�termin�e une
     * fois pour toute.
     */
    public UnitTransform getTransform(Unit fromUnit) throws UnitException {
        throw new UnitException("Not implemented");
    }
    
    /**
     * Retourne un objet qui saura convertir selon d'autres unit�s les
     * valeurs exprim�es selon ces unit�s. Cette m�thode est l'inverse
     * de {@link #getTransform}.
     */
    UnitTransform getInverseTransform(final Unit toUnit) throws UnitException {
        throw new UnitException("Not implemented");
    }
    
    /**
     * Cr�e une nouvelle unit� proportionnelle � cette unit�.
     */
    public Unit scale(final double amount) {
        throw new UnitException("Not implemented");
    }
    
    /**
     * Cr�e une nouvelle unit� d�cal�e par rapport � cette unit�.
     */
    public Unit shift(final double offset) {
        throw new UnitException("Not implemented");
    }
    
    /**
     * Retourne la quantit� que repr�sente cette unit�. Les quantit�s sont des cha�nes de
     * caract�res qui d�crivent le param�tre physique mesur�, comme "mass" ou "speed". Si
     * aucune quantit� n'est d�finie pour cette unit�, retourne <code>null</code>.
     */
    public String getQuantityName() {
        return null;
    }
    
    /**
     * Indique si deux unit�s sont �gales, en ignorant leurs symboles. Le
     * champs {@link #symbol} de chacune des deux unit�s ne sera pas pris
     * en compte.
     */
    boolean equalsIgnoreSymbol(final Unit unit) {
        return (unit instanceof DMSUnit);
    }
    
    /**
     * Renvoie une unit� identique � celle-ci, mais
     * avec un nouveau symbole et de nouveaux pr�fix.
     */
    public Unit rename(final String symbol, final PrefixSet prefix) {
        throw new UnitException("Not implemented");
    }
    
    /**
     * Returns an hash code for this object.
     */
    public int hashCode() {
        return 457829627;
    }
}
