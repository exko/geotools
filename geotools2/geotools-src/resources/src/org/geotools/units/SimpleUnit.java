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
import org.geotools.resources.units.Units;


/**
 * Classe de base pour les unit�s fondamentales ({@link BaseUnit}) ou les
 * unit�s d�riv�es ({@link DerivedUnit}). Aucune autre sous-classe n'est permise. Les objets
 * <code>SimpleUnit</code> repr�sentent donc toujours une combinaison d'unit�s fondamentales,
 * sans facteur multiplicatif ni constante ajout�e.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
/*public*/ 
abstract class SimpleUnit extends Unit {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1702845175242358392L;
    
    /**
     * La quantit� de cette unit�. Cette quantit� peut �tre par exemple
     * "mass" pour les kilogramme ou "speed" pour les m�tres par seconde),
     * ou <code>null</code> si elle n'est pas connue.
     */
    final String quantityName;
    
    /**
     * Construit des unit�s repr�sent�es
     * par le ou les symboles sp�cifi�s.
     */
    SimpleUnit(final String quantityName, final String symbol, final PrefixSet prefix) {
        super(symbol, prefix);
        this.quantityName=quantityName;
    }
    
    /**
     * Retourne la quantit� que repr�sente cette unit�. Les quantit�s sont des cha�nes de
     * caract�res qui d�crivent le param�tre physique mesur�, comme "mass" ou "speed". Si
     * aucune quantit� n'est d�finie pour cette unit�, retourne <code>null</code>.
     */
    public final String getQuantityName() {
        return quantityName;
    }
    
    /**
     * Retourne le nom de l'unit� dans la langue de l'utilisateur.
     * Par exemple le symbole "kg" sera traduit par "kilogramme"
     * dans la langue fran�aise. Si aucun nom n'est disponible
     * pour l'unit� courante, retourne simplement son symbole.
     */
    public String getLocalizedName() {
        String unpref=getUnprefixedSymbol();
        if (prefix!=null && symbol.endsWith(unpref)) {
            final Prefix p=prefix.getPrefix(symbol.substring(0, symbol.length()-unpref.length()));
            if (p!=null) {
                return p.getLocalizedName()+Units.localize(unpref);
            }
        }
        return super.getLocalizedName();
    }
    
    /**
     * Retourne le symbole {@link #symbol} sans son pr�fix. L'impl�mentation par d�faut
     * retourne {@link #symbol}, ce qui est correct pour la presque totalit� des unit�s
     * des classes {@link BaseUnit} et {@link DerivedUnit}. Dans le syst�me SI, la seule
     * exception notable (qui justifie � elle seule l'existence de cette m�thode) est le
     * kilogramme (symbole "kg"). Dans ce dernier cas, le symbole sans pr�fix est "g".
     */
    String getUnprefixedSymbol() {
        return symbol;
    }
    
    /**
     * �l�ve ces unit�s � une puissance enti�re. Contrairement � la m�thode
     * {@link Unit#pow}, cette m�thode ne lance jamais d'exception puisque
     * cette op�ration est toujours d�finie pour les unit�s fondamentales
     * ou d�riv�es.
     * <br><br>
     * Note: Si <em>JavaSoft</em> donne suite aux RFE 4144488 ou 4106143,
     *       alors la signature de cette m�thode sera modifi�e pour retourner
     *       explicitement un objet <code>SimpleUnit</code>.
     *
     * @param power La puissance � laquelle �lever cette unit�.
     * @return Les unit�s r�sultant de l'�l�vation des unit�s
     *         <code>this</code> � la puissance <code>power</code>.
     *
     * @see #multiply
     * @see #divide
     * @see #scale
     * @see #shift
     */
    public abstract Unit pow(int power); // CAST
    
    final              Unit inverseMultiply    (final ScaledUnit      that) throws UnitException {return ScaledUnit.getInstance(that.amount, (SimpleUnit) that.unit.multiply(this));}
    abstract /*Simple*/Unit inverseMultiply    (final BaseUnit        that) throws UnitException; // CAST
    abstract /*Simple*/Unit inverseMultiply    (final DerivedUnit     that) throws UnitException; // CAST
    final              Unit inverseDivide      (final ScaledUnit      that) throws UnitException {return ScaledUnit.getInstance(that.amount, (SimpleUnit) that.unit.divide(this));}
    abstract /*Simple*/Unit inverseDivide      (final BaseUnit        that) throws UnitException; // CAST
    abstract /*Simple*/Unit inverseDivide      (final DerivedUnit     that) throws UnitException; // CAST
    abstract  UnitTransform getTransform       (final BaseUnit    fromUnit) throws UnitException;
    abstract  UnitTransform getTransform       (final DerivedUnit fromUnit) throws UnitException;
    abstract  UnitTransform getInverseTransform(final BaseUnit      toUnit) throws UnitException;
    abstract  UnitTransform getInverseTransform(final DerivedUnit   toUnit) throws UnitException;
    
    /**
     * Cr�e une nouvelle unit� proportionnelle � cette unit�. Par exemple
     * pour convertir en kilom�tres des mesures exprim�es en m�tres, il
     * faut les diviser par 1000. On peut exprimer cette relation par le
     * code <code>Unit&nbsp;km=metre.scale(1000)</code>.
     *
     * @param  amount Facteur par lequel il faudra diviser les valeurs
     *         exprim�es selon ces unit�s pour obtenir des valeurs
     *         exprim�es selon les nouvelles unit�s.
     * @return Les nouvelles unit�s.
     *
     * @see #pow
     * @see #multiply
     * @see #divide
     * @see #shift
     */
    public final Unit scale(final double amount) {
        return ScaledUnit.getInstance(amount, this);
    }
    
    /**
     * Cr�e une nouvelle unit� d�cal�e par rapport � cette unit�. Par exemple
     * pour convertir des degr�s Kelvin en degr�s Celsius, il faut soustraire
     * 273.15 aux degr�s Kelvin. On peut exprimer cette relation par le code
     * <code>Unit&nbsp;celsius=kelvin.shift(273.15)</code>.
     *
     * @param  offset Constante � soustraire aux valeurs exprim�es selon ces
     *         unit�s pour obtenir des valeurs exprim�es selon les nouvelles
     *         unit�s.
     * @return Les nouvelles unit�s.
     *
     * @see #pow
     * @see #multiply
     * @see #divide
     * @see #scale
     */
    public final Unit shift(final double offset) {
        return OffsetUnit.getInstance(offset, this);
    }
}
