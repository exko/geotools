/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2002, Center for Computational Geography
 * (C) 2000, Institut de Recherche pour le D�veloppement
 * (C) 1999, P�ches et Oc�ans Canada
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *                   THIS IS A TEMPORARY CLASS
 *
 *    This is a placeholder for future <code>Unit</code> class.
 *    This skeleton will be removed when the real classes from
 *    JSR-108: Units specification will be publicly available.
 */
package org.geotools.resources.units;


/**
 * Noms de quantit�s en langue fran�aise. Les quantit�s qui n'apparaissent
 * pas dans cette ressources garderont leur nom neutre.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Quantities_fr extends Quantities
{
    /**
     * Liste des quantit�s en fran�ais.
     */
    static final String[] contents = {
        "dimensionless",             "sans dimension",
        "length",                    "longueur",
        "mass",                      "masse",
        "time",                      "temps",
        "electric current",          "courant �lectrique",
        "thermodynamic temperature", "temp�rature thermodynamique",
        "amount of substance",       "quantit� de mati�re",
        "luminous intensity",        "intensit� lumineuse",
        "plane angle",               "angle plan",
        "solid angle",               "angle solide",
        "salinity",                  "salinit�",
        "area",                      "superficie",
        "volume",                    "volume",
        "speed",                     "vitesse",
        "acceleration",              "acc�l�ration",
        "magnetic field strength",   "champ magn�tique",
        "luminance",                 "luminance lumineuse",
        "frequency",                 "fr�quence",
        "force",                     "force",
        "pressure",                  "pression",
        "energy",                    "�nergie",
        "power",                     "puissance",
        "electric charge",           "charge �lectrique",
        "potential",                 "potentiel",
        "capacitance",               "capacit�",
        "resistance",                "r�sistance",
        "conductance",               "conductance",
        "magnetic flux",             "flux magn�tique",
        "magnetic flux density",     "induction magn�tique",
        "inductance",                "inductance",
        "luminous flux",             "flux lumineux",
        "illuminance",               "�clairement lumineux",
        "activity",                  "activit�",
        "absorbed dose",             "dose absorb�e",
        "dose equivalent",           "�quivalent de dose"
    };

    /**
     * Initialise les ressources fran�aises.
     */
    public Quantities_fr() {
        super(contents);
    }
}
