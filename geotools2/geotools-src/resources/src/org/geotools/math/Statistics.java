/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2001, Institut de Recherche pour le D�veloppement
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
 * Contacts:
 *     UNITED KINGDOM: James Macgill
 *             mailto:j.macgill@geog.leeds.ac.uk
 *
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package org.geotools.math;

// Miscellaneous
import java.util.Locale;
import java.io.Serializable;
import org.geotools.io.TableWriter;
import org.geotools.resources.rsc.Resources;
import org.geotools.resources.rsc.ResourceKeys;


/**
 * Hold some statistics about a series of sample values. Given a series of sample values
 * <var>s<sub>0</sub></var>, <var>s<sub>1</sub></var>, <var>s<sub>2</sub></var>,
 * <var>s<sub>3</sub></var>..., this class computes {@linkplain #minimum minimum},
 * {@linkplain #maximum maximum}, {@linkplain #mean mean}, {@linkplain #rms root mean square}
 * and {@linkplain #standardDeviation standard deviation}. Statistics are computed on the fly;
 * the sample values are never stored in memory.
 *
 * An instance of <code>Statistics</code> is initially empty (i.e. all statistical values are set
 * to {@link Double#NaN NaN}). The statistics are updated every time an {@link #add(double)}
 * method is invoked with a non-{@linkplain Double#NaN NaN} value. A typical usage of this
 * class may be as below:
 *
 * <blockquote><pre>
 * double[] data = new double[1000];
 * // (Compute some data values here...)
 *
 * Statistics stats = new Statistics();
 * for (int i=0; i<data.length; i++) {
 *     stats.add(data[i]);
 * }
 * System.out.println(stats);
 * </pre></blockquote>
 *
 * @version $Id: Statistics.java,v 1.1 2003/02/04 12:30:18 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public class Statistics implements Cloneable, Serializable {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -22884277805533726L;

    /**
     * Valeur minimale qui aie �t� transmise � la m�thode
     * {@link #add(double)}. Lors de la construction, ce
     * champs est initialis� � NaN.
     */
    private double min = Double.NaN;

    /**
     * Valeur maximale qui aie �t� transmise � la m�thode
     * {@link #add(double)}. Lors de la construction, ce
     * champs est initialis� � NaN.
     */
    private double max = Double.NaN;

    /**
     * Somme de toutes les valeurs qui ont �t� transmises �
     * la m�thode {@link #add(double)}. Lors de la construction,
     * ce champs est initialis� � 0.
     */
    private double sum = 0;

    /**
     * Somme des carr�s de toutes les valeurs qui ont �t�
     * transmises � la m�thode {@link #add(double)}. Lors
     * de la construction, ce champs est initialis� � 0.
     */
    private double sum2 = 0;

    /**
     * Nombre de donn�es autres que NaN qui ont �t� transmises
     * � la m�thode {@link #add(double)}. Lors de la construction,
     * ce champs est initialis� � 0.
     */
    private int n = 0;

    /**
     * Nombre de donn�es �gales � NaN qui ont �t� transmises �
     * la m�thode {@link #add(double)}. Les NaN sont ingor�s lors
     * du calcul des statistiques, mais on les compte quand m�me
     * au passage. Lors de la construction ce champs est initialis� � 0.
     */
    private int nNaN = 0;

    /**
     * Construct an initially empty set of statistics.
     * All statistical values are initialized to {@link Double#NaN}.
     */
    public Statistics() {
    }

    /**
     * Reset the statistics to their initial {@link Double#NaN NaN} values.
     * This method reset this object state as if it was just created.
     */
    public void reset() {
        min  = Double.NaN;
        max  = Double.NaN;
        sum  = 0;
        sum2 = 0;
        n    = 0;
        nNaN = 0;
    }

    /**
     * Update statistics for the specified sample. This <code>add</code>
     * method is usually invoked inside a <code>for</code> loop.
     *
     * @param sample The sample value. {@link Double#NaN NaN} values are ignored.
     *
     * @see #add(long)
     * @see #add(Statistics)
     */
    public void add(final double sample) {
        if (!Double.isNaN(sample)) {
            /*
             *  Les deux prochaines lignes utilisent !(a>=b) au
             *  lieu de (a<b) afin de prendre en compte les NaN.
             */
            if (!(min<=sample)) min=sample;
            if (!(max>=sample)) max=sample;
            sum2 += (sample*sample);
            sum  += sample;
            n++;
        } else {
            nNaN++;
        }
    }

    /**
     * Update statistics for the specified sample. This <code>add</code>
     * method is usually invoked inside a <code>for</code> loop.
     *
     * @param sample The sample value.
     *
     * @see #add(double)
     * @see #add(Statistics)
     */
    public void add(final long sample) {
        final double fdatum = sample;
        if (!(min<=fdatum)) min=fdatum;
        if (!(max>=fdatum)) max=fdatum;
        sum2 += (fdatum*fdatum);
        sum  += fdatum;
        n++;
    }

    /**
     * Update statistics with all samples from the specified <code>stats</code>. Invoking this
     * method is equivalent (except for rounding errors)  to invoking {@link #add(double) add}
     * for all samples that were added to <code>stats</code>.
     *
     * @param stats The statistics to be added to <code>this</code>, or <code>null</code> if none.
     */
    public void add(final Statistics stats) {
        if (stats != null) {
            // "if (a<b)" �quivaut � "if (!isNaN(a) && a<b)".
            if (Double.isNaN(min) || stats.min<min) min=stats.min;
            if (Double.isNaN(max) || stats.max>max) max=stats.max;
            sum2 += stats.sum2;
            sum  += stats.sum;
            n    += stats.n;
            nNaN += stats.nNaN;
        }
    }

    /**
     * Returns the number of {@link Double#NaN NaN} samples.  <code>NaN</code> samples are
     * ignored in all other statitical computation. This method count them for information
     * purpose only.
     */
    public int countNaN() {
        return Math.max(nNaN, 0);
    }

    /**
     * Returns the number of samples, excluding {@link Double#NaN NaN} values.
     */
    public int count() {
        return n;
    }

    /**
     * Returns the minimum sample value, or {@link Double#NaN NaN} if none.
     *
     * @see #maximum
     */
    public double minimum() {
        return min;
    }

    /**
     * Returns the maximum sample value, or {@link Double#NaN NaN} if none.
     *
     * @see #minimum
     */
    public double maximum() {
        return max;
    }

    /**
     * Returns the range of sample values. This is equivalent to <code>{@link #maximum maximum} -
     * {@link #minimum minimum}, except for rounding error. If no samples were added, then returns
     * {@link Double#NaN NaN}.
     *
     * @see #minimum
     * @see #maximum
     */
    public double range() {
        return max-min;
    }

    /**
     * Returns the mean value, or {@link Double#NaN NaN} if none.
     */
    public double mean() {
        return sum/n;
    }

    /**
     * Returns the root mean square, or {@link Double#NaN NaN} if none.
     */
    public double rms() {
        return Math.sqrt(sum2/n);
    }

    /**
     * Retourne l'�cart type des �chantillons par rapport � la moyenne. Si les donn�es
     * fournies aux diff�rentes m�thodes <code>add(...)</code> se distribuent selon une
     * loi normale, alors l'�cart type est la distance de part et d'autre de la moyenne
     * dans lequel se trouveraient environ 84% des donn�es. Le tableau ci-dessous donne
     * le pourcentage approximatif des donn�es que l'on trouve de part et d'autre de la
     * moyenne � des distances telles que 2 ou 3 fois l'�cart-type.
     *
     * <table align=center>
     *   <tr><td>&nbsp;0.5&nbsp;</td><td>&nbsp;69.1%&nbsp;</td></tr>
     *   <tr><td>&nbsp;1.0&nbsp;</td><td>&nbsp;84.2%&nbsp;</td></tr>
     *   <tr><td>&nbsp;1.5&nbsp;</td><td>&nbsp;93.3%&nbsp;</td></tr>
     *   <tr><td>&nbsp;2.0&nbsp;</td><td>&nbsp;97.7%&nbsp;</td></tr>
     *   <tr><td>&nbsp;3.0&nbsp;</td><td>&nbsp;99.9%&nbsp;</td></tr>
     * </table>
     *
     * @param allPopulation La valeur <code>true</code> indique que les donn�es fournies
     *        aux diff�rentes m�thodes <code>add(...)</code> repr�sentent l'ensemble de
     *        la polulation. La valeur <code>false</code> indique que ces donn�es ne
     *        repr�sentent qu'un �chantillon de la population, ce qui est g�n�ralement le
     *        cas. Si le nombre de donn�es est �lev�, alors les valeurs <code>true</code>
     *        et <code>false</code> donneront sensiblement les m�mes r�sultats.
     */
    public double standardDeviation(final boolean allPopulation) {
        return Math.sqrt((sum2 - sum*sum/n) / (allPopulation ? n : n-1));
    }

    /**
     * Returns a clone of this statistics.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException exception) {
            // Should not happen since we are cloneable
            throw new AssertionError(exception);
        }
    }

    /**
     * Test this statistics with the specified object for equality.
     */
    public boolean equals(final Object obj) {
        if (obj!=null && getClass().equals(obj.getClass())) {
            final Statistics cast = (Statistics) obj;
            return n==cast.n &&
                   Double.doubleToLongBits(min ) == Double.doubleToLongBits(cast.min) &&
                   Double.doubleToLongBits(max ) == Double.doubleToLongBits(cast.max) &&
                   Double.doubleToLongBits(sum ) == Double.doubleToLongBits(cast.sum) &&
                   Double.doubleToLongBits(sum2) == Double.doubleToLongBits(cast.sum2);
        }
        return false;
    }

    /**
     * Returns a hash code value for this statistics.
     */
    public int hashCode() {
        final long code = (Double.doubleToLongBits(min) +
                       37*(Double.doubleToLongBits(max) +
                       37*(Double.doubleToLongBits(sum) +
                       37*(Double.doubleToLongBits(sum2)))));
        return (int) code ^ (int) (code >>> 32) ^ n;
    }

    /**
     * Returns a string representation of this statistics. This method invokes
     * {@link #toString(Locale, boolean)}  using the default locale and spaces
     * separator.
     */
    public final String toString() {
        return toString(null, false);
    }

    /**
     * Returns a localized string representation of this statistics. This string
     * will span multiple lines, one for each statistical value. For example:
     *
     * <blockquote><pre>
     *     Compte:      8726
     *     Minimum:    6.853
     *     Maximum:    8.259
     *     Moyenne:    7.421
     *     RMS:        7.846
     *     �cart-type: 6.489
     * </pre></blockquote>
     *
     * If <code>tabulations</code> is true, then labels (e.g. "Minimum") and values
     * (e.g. "6.853") are separated by tabulations. Otherwise, they are separated
     * by spaces.
     */
    public String toString(final Locale locale, final boolean tabulations) {
        String text = Resources.getResources(locale).getString(ResourceKeys.STATISTICS_TO_STRING_$6,
            new Number[] {
                new Integer(count()  ),
                new Double (minimum()),
                new Double (maximum()),
                new Double (mean()   ),
                new Double (rms()    ),
                new Double (standardDeviation(false))
        });
        if (!tabulations) {
            final TableWriter tmp = new TableWriter(null);
            tmp.write(text);
            text = tmp.toString();
        }
        return text;
    }

    /**
     * Hold some statistics about a series of sample values and the difference between them.
     * Given a series of sample values <var>s<sub>0</sub></var>, <var>s<sub>1</sub></var>,
     * <var>s<sub>2</sub></var>, <var>s<sub>3</sub></var>..., this class computes statistics
     * in the same way than {@link Statistics} and additionnaly computes statistics for
     * <var>s<sub>1</sub></var>-<var>s<sub>0</sub></var>,
     * <var>s<sub>2</sub></var>-<var>s<sub>1</sub></var>,
     * <var>s<sub>3</sub></var>-<var>s<sub>2</sub></var>...,
     * which are stored in a {@link #getDeltaStatistics delta} statistics object.
     *
     * @version $Id: Statistics.java,v 1.1 2003/02/04 12:30:18 desruisseaux Exp $
     * @author Martin Desruisseaux
     */
    public static class Delta extends Statistics {
        /**
         * Serial number for compatibility with different versions.
         */
        private static final long serialVersionUID = 3464306833883333219L;

        /**
         * Statistics about the differences between consecutive sample values.
         */
        private Statistics delta;

        /**
         * Last value given to an {@link #add(double) add} method as
         * a <code>double</code>, or {@link Double#NaN NaN} if none.
         */
        private double last = Double.NaN;

        /**
         * Last value given to an {@link #add(long) add}
         * method as a <code>long</code>, or 0 if none.
         */
        private long lastAsLong;

        /**
         * Construct an initially empty set of statistics.
         * All statistical values are initialized to {@link Double#NaN}.
         */
        public Delta() {
            delta = new Statistics();
            delta.nNaN = -1; // Do not count the first NaN, which will always be the first value.
        }

        /**
         * Construct an initially empty set of statistics using the specified
         * object for {@link #getDeltaStatistics delta} statistics. This method
         * allows chaining different kind of statistics objects. For example, one
         * could write:
         * <blockquote><pre>
         * new Statistics.Delta(new Statistics.Delta());
         * </pre></blockquote>
         * Which would compute statistics of sample values, statistics of difference between
         * consecutive sample values, and statistics of difference of difference between
         * consecutive sample values. Other kinds of {@link Statistics} object could be
         * chained as well.
         */
        public Delta(final Statistics delta) {
            this.delta = delta;
            delta.reset();
            delta.nNaN = -1; // Do not count the first NaN, which will always be the first value.
        }

        /**
         * Returns the statistics about difference between consecutives values.
         * Given a series of sample values <var>s<sub>0</sub></var>, <var>s<sub>1</sub></var>,
         * <var>s<sub>2</sub></var>, <var>s<sub>3</sub></var>..., this is statistics for
         * <var>s<sub>1</sub></var>-<var>s<sub>0</sub></var>,
         * <var>s<sub>2</sub></var>-<var>s<sub>1</sub></var>,
         * <var>s<sub>3</sub></var>-<var>s<sub>2</sub></var>...,
         */
        public Statistics getDeltaStatistics() {
            return delta;
        }

        /**
         * Reset the statistics to their initial {@link Double#NaN NaN} values.
         * This method reset this object state as if it was just created.
         */
        public void reset() {
            super.reset();
            delta.reset();
            delta.nNaN = -1; // Do not count the first NaN, which will always be the first value.
            last       = Double.NaN;
            lastAsLong = 0;
        }

        /**
         * Update statistics for the specified sample. The {@link #getDeltaStatistics delta}
         * statistics are updated with <code>sample - sample<sub>last</sub></code> value,
         * where <code>sample<sub>last</sub></code> is the last value given to the previous
         * call of an <code>add(...)</code> method.
         */
        public void add(final double sample) {
            super.add(sample);
            delta.add(sample-last);
            last       = sample;
            lastAsLong = (long)sample;
        }

        /**
         * Update statistics for the specified sample. The {@link #getDeltaStatistics delta}
         * statistics are updated with <code>sample - sample<sub>last</sub></code> value,
         * where <code>sample<sub>last</sub></code> is the last value given to the previous
         * call of an <code>add(...)</code> method.
         */
        public void add(final long sample) {
            super.add(sample);
            if (last == (double)lastAsLong) {
                // 'lastAsLong' may have more precision than 'last' since the cast to the
                // 'double' type may loose some digits. Invoke the 'delta.add(long)' version.
                delta.add(sample-lastAsLong);
            } else {
                // The sample value is either fractional, outside 'long' range,
                // infinity or NaN. Invoke the 'delta.add(double)' version.
                delta.add(sample-last);
            }
            last       = sample;
            lastAsLong = sample;
        }

        /**
         * Update statistics with all samples from the specified <code>stats</code>. Invoking this
         * method is equivalent (except for rounding errors)  to invoking {@link #add(double) add}
         * for all samples that were added to <code>stats</code>.  The <code>stats</code> argument
         * must be an instance of <code>Statistics.Delta</code>.
         *
         * @param  stats The statistics to be added to <code>this</code>,
         *         or <code>null</code> if none.
         * @throws ClassCastException If <code>stats</code> is not an instance of
         *         <code>Statistics.Delta</code>.
         */
        public void add(final Statistics stats) throws ClassCastException {
            if (stats != null) {
                final Delta toAdd = (Delta) stats;
                if (toAdd.delta.nNaN >= 0) {
                    delta.add(toAdd.delta);
                    last       = toAdd.last;
                    lastAsLong = toAdd.lastAsLong;
                    super.add(stats);
                }
            }
        }

        /**
         * Returns a clone of this statistics.
         */
        public Object clone() {
            Delta copy = (Delta) super.clone();
            copy.delta = (Statistics) copy.delta.clone();
            return copy;
        }

        /**
         * Test this statistics with the specified object for equality.
         */
        public boolean equals(final Object obj) {
            return super.equals(obj) && delta.equals(((Delta) obj).delta);
        }

        /**
         * Returns a hash code value for this statistics.
         */
        public int hashCode() {
            return super.hashCode() + 37*delta.hashCode();
        }
    }
}
