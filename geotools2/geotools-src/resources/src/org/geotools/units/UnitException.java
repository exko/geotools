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
 * Exceptions signalant qu'une certaine op�ration ne peut pas �tre faite
 * sur une certaine unit�. Cette exception peut �tre lanc�e lorsqu'une conversion est impossible,
 * ou qu'on ne peut pas multiplier une unit� par une autre.
 *
 * @version 1.0
 * @author Steven R. Emmerson
 * @author Bill Hibbard
 * @author Martin Desruisseaux
 */
public class UnitException extends RuntimeException {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -6935210984697824869L;
    
    /**
     * Premi�re unit�s en cause lors de l'exception. Il peut s'agir des unit�s <var>A</var> dans l'expression
     * <code>A.{@link Unit#multiply multiply}(B)</code> ou <code>A.{@link Unit#convert convert}(values, B)</code>.
     * Ce champ peut �tre nul s'il ne s'applique pas.
     */
    /*public*/ final Unit unitA;
    
    /**
     * Deuxi�me unit�s en cause lors de l'exception. Il peut s'agir des unit�s <var>B</var> dans l'expression
     * <code>A.{@link Unit#multiply multiply}(B)</code> ou <code>A.{@link Unit#convert convert}(values, B)</code>.
     * Ce champ peut �tre nul s'il ne s'applique pas. Par exemple <code>unitB</code> sera nul si l'exception
     * provient de la m�thode <code>A.{@link Unit#pow pow}(n)</code>.
     */
    /*public*/ final Unit unitB;
    
    /**
     * Cr�e une exception sans messages.
     */
    public UnitException() {
        unitA=null;
        unitB=null;
    }
    
    /**
     * Cr�e une exception avec le message sp�cifi�.
     */
    public UnitException(final String msg) {
        super(msg);
        unitA=null;
        unitB=null;
    }
    
    /**
     * Cr�e une exception avec le message sp�cifi�
     * et en d�signant les deux unit�s coupables.
     */
    /*public*/ UnitException(final String msg, final Unit A, final Unit B) {
        super(msg);
        unitA=A;
        unitB=B;
    }
}
