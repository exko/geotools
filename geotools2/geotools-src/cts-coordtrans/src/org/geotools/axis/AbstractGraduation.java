/*
 * Geotools 2 - OpenSource mapping toolkit
 * (C) 2003, Geotools Project Managment Committee (PMC)
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
package org.geotools.axis;

// J2SE dependencies
import java.util.Locale;
import java.io.Serializable;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

// Geotools dependencies
import org.geotools.units.Unit;
import org.geotools.units.UnitException;
import org.geotools.resources.Utilities;
import org.geotools.resources.cts.Resources;
import org.geotools.resources.cts.ResourceKeys;


/**
 * Base class for graduation.
 *
 * @version $Id: AbstractGraduation.java,v 1.3 2003/05/13 10:58:46 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public abstract class AbstractGraduation implements Graduation, Serializable {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 5215728323932315112L;

    /**
     * The axis's units, or <code>null</code> if unknow.
     */
    private Unit unit;

    /**
     * The axis title for this graduation.
     */
    private String title;

    /**
     * The locale for formatting labels.
     */
    private Locale locale = Locale.getDefault();

    /**
     * A list of event listeners for this component.
     */
    protected final PropertyChangeSupport listenerList;

    /**
     * Construct a graduation with the supplied units.
     *
     * @param units The axis's units, or <code>null</code> if unknow.
     */
    public AbstractGraduation(final Unit unit) {
        listenerList = new PropertyChangeSupport(this);
        this.unit = unit;
    }

    /**
     * Set the minimum value for this graduation. If the new minimum is greater
     * than the current maximum, then the maximum will also be set to a value
     * greater than or equals to the minimum.
     *
     * @param  value The new minimum in {@link #getUnit} units.
     * @return <code>true</code> if the state of this graduation changed
     *         as a result of this call, or <code>false</code> if the new
     *         value is identical to the previous one.
     * @throws IllegalArgumentException If <code>value</code> is NaN ou infinite.
     *
     * @see #getMinimum
     * @see #setMaximum(double)
     */
    public abstract boolean setMinimum(final double value) throws IllegalArgumentException;

    /**
     * Set the maximum value for this graduation. If the new maximum is less
     * than the current minimum, then the minimum will also be set to a value
     * less than or equals to the maximum.
     *
     * @param  value The new maximum in {@link #getUnit} units.
     * @return <code>true</code> if the state of this graduation changed
     *         as a result of this call, or <code>false</code> if the new
     *         value is identical to the previous one.
     * @throws IllegalArgumentException If <code>value</code> is NaN ou infinite.
     *
     * @see #getMaximum
     * @see #setMinimum(double)
     */
    public abstract boolean setMaximum(final double value) throws IllegalArgumentException;
    
    /**
     * Returns the axis title. If <code>includeUnits</code> is <code>true</code>,
     * then the returned string will includes units as in "Temperature (�C)". The
     * exact formatting is local-dependent.
     *
     * @param  includeSymbol <code>true</code> to format unit symbol after the name.
     * @return The graduation name (also to be use as axis title).
     */
    public synchronized String getTitle(final boolean includeSymbol) {
        if (includeSymbol) {
            final String symbol = getSymbol();
            if (symbol!=null && symbol.length()!=0) {
                // TODO: localize if needed.
                return (title!=null) ? title+" ("+symbol+')' : symbol;
            }
        }
        return title;
    }

    /**
     * Set the axis title, not including unit symbol. This method will fire a
     * property change event with the <code>"title"</code> property name.
     *
     * @param title New axis title, or <code>null</code> to remove any previous setting.
     */
    public void setTitle(final String title) {
        final String old;
        synchronized (this) {
            old = this.title;
            this.title = title;
        }
        listenerList.firePropertyChange("title", old, title);
    }

    /**
     * Returns a string representation of axis's units, or <code>null</code>
     * if there is none. The default implementation returns the string
     * representation of {@link #getUnit}.
     */
    String getSymbol() {
        final Unit unit = getUnit();
        return (unit!=null) ? unit.toString() : null;
    }

    /**
     * Returns the graduation's units, or <code>null</code> if unknow.
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * Changes the graduation's units. Subclasses will automatically
     * convert minimum and maximum values from the old units to the
     * new one. This method fires a property change event with the
     * <code>"unit"</code> property name.
     *
     * @param  unit The new units, or <code>null</code> if unknow.
     *         If null, minimum and maximum values are not converted.
     * @throws UnitException if units are not convertible, or if the
     *         specified units is illegal for this graduation.
     */
    public void setUnit(final Unit unit) throws UnitException {
        final Unit oldUnit;
        synchronized (this) {
            oldUnit = this.unit;
            this.unit = unit;
        }
        listenerList.firePropertyChange("unit", oldUnit, unit);
    }

    /**
     * Returns the locale to use for formatting labels.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Set the locale to use for formatting labels.
     * This will fire a property change event with
     * the <code>"locale"</code> property name.
     */
    public synchronized void setLocale(final Locale locale) {
        final Locale old;
        synchronized (this) {
            old = this.locale;
            this.locale = locale;
        }
        listenerList.firePropertyChange("locale", old, locale);
    }

    /**
     * Adds a {@link PropertyChangeListener} to the listener list. The listener is
     * registered for all properties. A {@link PropertyChangeEvent} will get fired
     * in response to setting a property, such as {@link #setTitle} or {@link #setLocale}.
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        listenerList.addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@link PropertyChangeListener} from the listener list.
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        listenerList.removePropertyChangeListener(listener);
    }

    /**
     * Retourne la longueur de l'axe, en pixels ou en points (1/72 de pouce).
     */
    static float getVisualAxisLength(final RenderingHints hints) {
        return getValue(hints, VISUAL_AXIS_LENGTH, 600);
    }

    /**
     * Retourne l'espace approximatif (en pixels ou en points) � laisser entre les
     * graduations principales. L'espace r�el entre les graduations peut �tre l�g�rement
     * diff�rent, par exemple pour avoir des �tiquettes qui correspondent � des valeurs
     * arrondies.
     */
    static float getVisualTickSpacing(final RenderingHints hints) {
        return getValue(hints, VISUAL_TICK_SPACING, 48);
    }

    /**
     * Retourne une valeur sous forme de nombre r�el.
     */
    private static float getValue(final RenderingHints   hints,
                                  final RenderingHints.Key key,
                                  final float defaultValue)
    {
        if (hints != null) {
            final Object object = hints.get(key);
            if (object instanceof Number) {
                final float value = ((Number) object).floatValue();
                if (value!=0 && !Float.isInfinite(value)) {
                    return value;
                }
            }
        }
        return defaultValue;
    }
    
    /**
     * V�rifie que le nombre sp�cifi� est non-nul. S'il
     * est 0, NaN ou infini, une exception sera lanc�e.
     *
     * @param  name Nom de l'argument.
     * @param  n Nombre � v�rifier.
     * @throws IllegalArgumentException Si <var>n</var> est NaN ou infini.
     */
    static void ensureNonNull(final String name, final double n) throws IllegalArgumentException {
        if (Double.isNaN(n) || Double.isInfinite(n) || n==0) {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2,
                                                                name, new Double(n)));
        }
    }
    
    /**
     * V�rifie que le nombre sp�cifi� est r�el. S'il
     * est NaN ou infini, une exception sera lanc�e.
     *
     * @param  name Nom de l'argument.
     * @param  n Nombre � v�rifier.
     * @throws IllegalArgumentException Si <var>n</var> est NaN ou infini.
     */
    static void ensureFinite(final String name, final double n) throws IllegalArgumentException {
        if (Double.isNaN(n) || Double.isInfinite(n)) {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2,
                                                                name, new Double(n)));
        }
    }

    /**
     * V�rifie que le nombre sp�cifi� est r�el. S'il
     * est NaN ou infini, une exception sera lanc�e.
     *
     * @param  name Nom de l'argument.
     * @param  n Nombre � v�rifier.
     * @throws IllegalArgumentException Si <var>n</var> est NaN ou infini.
     */
    static void ensureFinite(final String name, final float n) throws IllegalArgumentException {
        if (Float.isNaN(n) || Float.isInfinite(n)) {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2,
                                                                name, new Float(n)));
        }
    }

    /**
     * Compare this graduation with the specified object for equality.
     * This method do not compare listeners registered in {@link #listenerList}.
     */
    public boolean equals(final Object object) {
        if (object!=null && object.getClass().equals(getClass())) {
            final AbstractGraduation that = (AbstractGraduation) object;
            return Utilities.equals(this.unit,   that.unit  ) &&
                   Utilities.equals(this.title,  that.title ) &&
                   Utilities.equals(this.locale, that.locale);
        }
        return false;
    }

    /**
     * Returns a hash value for this graduation.
     */
    public int hashCode() {
        int code = (int)serialVersionUID;
        if (title != null) {
            code ^= title.hashCode();
        }
        if (unit != null) {
            code ^= unit.hashCode();
        }
        return code;
    }
}
