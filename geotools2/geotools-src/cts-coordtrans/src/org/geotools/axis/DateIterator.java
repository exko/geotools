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

// Date and time
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;
import java.text.DateFormat;

// Other J2SE dependencies
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.io.StringWriter;
import java.io.PrintWriter;

// Geotools dependencies
import org.geotools.resources.XMath;


/**
 * It�rateur balayant les barres et �tiquettes de graduation d'un axe
 * du temps.   Cet it�rateur retourne les positions des graduations �
 * partir de la date la plus ancienne jusqu'� la date la plus r�cente.
 * Il choisit les intervalles de graduation en supposant qu'on utilise
 * un calendrier gr�gorien.
 *
 * @version $Id: DateIterator.java,v 1.3 2003/05/13 10:58:46 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
final class DateIterator implements TickIterator {
    /**
     * Nombre de millisecondes dans certaines unit�s de temps.
     */
    private static final long SEC  = 1000,
                              MIN  = 60*SEC,
                              HRE  = 60*MIN,
                              DAY  = 24*HRE,
                              YEAR = 365*DAY + (DAY/4) - (DAY/100) + (DAY/400),
                              MNT  = YEAR/12;

    /**
     * Liste des intervales souhait�s pour la graduation. Les �l�ments de
     * cette table doivent obligatoirement appara�tre en ordre croissant.
     * Voici un exemple d'interpr�tation: la pr�sence de <code>5*MIN</code>
     * suivit de <code>10*MIN</code> implique que si le pas estim� se trouve
     * entre 5 et 10 minutes, ce sera le pas de 10 minutes qui sera s�lectionn�.
     */
    private static final long[] INTERVAL = {
        SEC,  2*SEC,  5*SEC, 10*SEC, 15*SEC, 20*SEC, 30*SEC,
        MIN,  2*MIN,  5*MIN, 10*MIN, 15*MIN, 20*MIN, 30*MIN,
        HRE,  2*HRE,  3*HRE,  4*HRE,  6*HRE,  8*HRE, 12*HRE,
        DAY,  2*DAY,  3*DAY,  7*DAY, 14*DAY, 21*DAY,
        MNT,  2*MNT,  3*MNT,  4*MNT,  6*MNT,
        YEAR, 2*YEAR, 3*YEAR, 4*YEAR, 5*YEAR
    };

    /**
     * Intervalles des graduations principales et des sous-graduations correspondants
     * � chaque des intervalles du tableau {@link #INTERVAL}.  Cette classe cherchera
     * d'abord un intervalle en millisecondes dans le tableau {@link #INTERVAL}, puis
     * traduira cet intervalle en champ du calendrier gr�gorien en lisant les �l�ments
     * correspondants de ce tableau {@link #ROLL}.
     */
    private static final byte[] ROLL = {
         1, (byte)Calendar.SECOND,        25, (byte)Calendar.MILLISECOND, // x10 millis
         2, (byte)Calendar.SECOND,        50, (byte)Calendar.MILLISECOND, // x10 millis
         5, (byte)Calendar.SECOND,         1, (byte)Calendar.SECOND,
        10, (byte)Calendar.SECOND,         2, (byte)Calendar.SECOND,
        15, (byte)Calendar.SECOND,         5, (byte)Calendar.SECOND,
        20, (byte)Calendar.SECOND,         5, (byte)Calendar.SECOND,
        30, (byte)Calendar.SECOND,         5, (byte)Calendar.SECOND,
         1, (byte)Calendar.MINUTE,        10, (byte)Calendar.SECOND,
         2, (byte)Calendar.MINUTE,        30, (byte)Calendar.SECOND,
         5, (byte)Calendar.MINUTE,         1, (byte)Calendar.MINUTE,
        10, (byte)Calendar.MINUTE,         2, (byte)Calendar.MINUTE,
        15, (byte)Calendar.MINUTE,         5, (byte)Calendar.MINUTE,
        20, (byte)Calendar.MINUTE,         5, (byte)Calendar.MINUTE,
        30, (byte)Calendar.MINUTE,         5, (byte)Calendar.MINUTE,
         1, (byte)Calendar.HOUR_OF_DAY,   15, (byte)Calendar.MINUTE,
         2, (byte)Calendar.HOUR_OF_DAY,   30, (byte)Calendar.MINUTE,
         3, (byte)Calendar.HOUR_OF_DAY,   30, (byte)Calendar.MINUTE,
         4, (byte)Calendar.HOUR_OF_DAY,    1, (byte)Calendar.HOUR_OF_DAY,
         6, (byte)Calendar.HOUR_OF_DAY,    1, (byte)Calendar.HOUR_OF_DAY,
         8, (byte)Calendar.HOUR_OF_DAY,    2, (byte)Calendar.HOUR_OF_DAY,
        12, (byte)Calendar.HOUR_OF_DAY,    2, (byte)Calendar.HOUR_OF_DAY,
         1, (byte)Calendar.DAY_OF_MONTH,   4, (byte)Calendar.HOUR_OF_DAY,
         2, (byte)Calendar.DAY_OF_MONTH,   6, (byte)Calendar.HOUR_OF_DAY,
         3, (byte)Calendar.DAY_OF_MONTH,  12, (byte)Calendar.HOUR_OF_DAY,
         7, (byte)Calendar.DAY_OF_MONTH,   1, (byte)Calendar.DAY_OF_MONTH,
        14, (byte)Calendar.DAY_OF_MONTH,   2, (byte)Calendar.DAY_OF_MONTH,
        21, (byte)Calendar.DAY_OF_MONTH,   7, (byte)Calendar.DAY_OF_MONTH,
         1, (byte)Calendar.MONTH,          7, (byte)Calendar.DAY_OF_MONTH,
         2, (byte)Calendar.MONTH,         14, (byte)Calendar.DAY_OF_MONTH,
         3, (byte)Calendar.MONTH,         14, (byte)Calendar.DAY_OF_MONTH,
         4, (byte)Calendar.MONTH,          1, (byte)Calendar.MONTH,
         6, (byte)Calendar.MONTH,          1, (byte)Calendar.MONTH,
         1, (byte)Calendar.YEAR,           2, (byte)Calendar.MONTH,
         2, (byte)Calendar.YEAR,           4, (byte)Calendar.MONTH,
         3, (byte)Calendar.YEAR,           6, (byte)Calendar.MONTH,
         4, (byte)Calendar.YEAR,           1, (byte)Calendar.YEAR,
         5, (byte)Calendar.YEAR,           1, (byte)Calendar.YEAR
    };

    /**
     * Nombre de colonne dans le tableau {@link ROLL}. Le tableau {@link ROLL} doit
     * �tre interpr�t� comme une matrice de 4 colonnes et d'un nombre ind�termin� de
     * lignes.
     */
    private static final int ROLL_WIDTH = 4;

    /**
     * Liste des champs de dates qui apparaissent dans le tableau {@link ROLL}.
     * Cette liste doit �tre du champ le plus grand (YEAR) vers le champ le plus
     * petit (MILLISECOND).
     */
    private static final int[] FIELD = {
        Calendar.YEAR,
        Calendar.MONTH,
        Calendar.DAY_OF_MONTH,
        Calendar.HOUR_OF_DAY,
        Calendar.MINUTE,
        Calendar.SECOND,
        Calendar.MILLISECOND
    };

    /**
     * Liste des noms des champs (� des fins de d�boguage seulement).
     * Cette liste doit �tre dans le m�me ordre que les �l�ments de
     * {@link #FIELD}.
     */
    private static final String[] FIELD_NAME = {
        "YEAR",
        "MONTH",
        "DAY",
        "HOUR",
        "MINUTE",
        "SECOND",
        "MILLISECOND"
    };

    /**
     * Date de la premi�re graduation principale.
     * Cette valeur est fix�e par {@link #init}.
     */
    private long minimum;

    /**
     * Date limite des graduations. La derni�re
     * graduation ne sera pas n�cessairement �
     * cette date. Cette valeur est fix�e par
     * {@link #init}.
     */
    private long maximum;

    /**
     * Estimation de l'intervalle entre deux graduations
     * principales. Cette valeur est fix�e par {@link #init}.
     */
    private long increment;

    /**
     * Longueur de l'axe (en points). Cette information
     * est conserv�e afin d'�viter de refaire toute la
     * proc�dure {@link #init} si les param�tres n'ont
     * pas chang�s.
     */
    private float visualLength;

    /**
     * Espace � laisser (en points) entre les graduations principales.
     * Cette information est conserv�e afin d'�viter de refaire toute
     * la proc�dure {@link #init} si les param�tres n'ont pas chang�s.
     */
    private float visualTickSpacing;

    /**
     * Nombre de fois qu'il faut incr�menter le champ {@link #tickField} du
     * calendrier pour passer � la graduation suivante. Cette op�ration peut
     * se faire avec <code>calendar.add(tickField, tickAdd)</code>.
     */
    private int tickAdd;

    /**
     * Champ du calendrier qu'il faut incr�menter pour passer �
     * la graduation suivante. Cette op�ration peut se faire avec
     * <code>calendar.add(tickField, tickAdd)</code>.
     */
    private int tickField;

    /**
     * Nombre de fois qu'il faut incr�menter le champ {@link #tickField} du
     * calendrier pour passer � la sous-graduation suivante. Cette op�ration
     * peut se faire avec <code>calendar.add(tickField, tickAdd)</code>.
     */
    private int subTickAdd;

    /**
     * Champ du calendrier qu'il faut incr�menter pour passer � la
     * sous-graduation suivante. Cette op�ration peut se faire avec
     * <code>calendar.add(tickField, tickAdd)</code>.
     */
    private int subTickField;

    /**
     * Date de la graduation principale ou secondaire actuelle.
     * Cette valeur sera modifi�e � chaque appel � {@link #next}.
     */
    private long value;

    /**
     * Date de la prochaine graduation principale. Cette
     * valeur sera modifi�e � chaque appel � {@link #next}.
     */
    private long nextTick;

    /**
     * Date de la prochaine graduation secondaire. Cette
     * valeur sera modifi�e � chaque appel � {@link #next}.
     */
    private long nextSubTick;

    /**
     * Indique si {@link #value} repr�sente
     * une graduation principale.
     */
    private boolean isMajorTick;

    /**
     * Valeurs de {@link #value}, {@link #nextTick} et {@link #nextSubTick0}
     * imm�diatement apr�s l'appel de {@link #rewind}.
     */
    private long value0, nextTick0, nextSubTick0;

    /**
     * Valeur de {@link #isMajorTick} imm�diatement
     * apr�s l'appel de {@link #rewind}.
     */
    private boolean isMajorTick0;

    /**
     * Calendrier servant � avancer d'une certaine p�riode de temps (jour, semaine, mois...).
     * <strong>Note: Par convention et pour des raisons de performances (pour �viter d'imposer
     * au calendrier de recalculer ses champs trop souvent), ce calendrier devrait toujours
     * contenir la date {@link #nextSubTick}.
     */
    private Calendar calendar;

    /**
     * Objet temporaire � utiliser pour passer des dates
     * en argument � {@link #calendar} et {@link #format}.
     */
    private final Date date = new Date();

    /**
     * Format � utiliser pour �crire les �tiquettes de graduation. Ce format ne
     * sera construit que la premi�re fois o� {@link #currentLabel} sera appel�e.
     */
    private transient DateFormat format;

    /**
     * Code du format utilis� pour construire le champ de date de {@link #format}. Les codes
     * valides sont notamment  {@link DateFormat#SHORT}, {@link DateFormat#MEDIUM} ou {@link
     * DateFormat#LONG}. La valeur -1 indique que le format ne contient pas de champ de date,
     * seulement un champ des heures.
     */
    private transient int dateFormat = -1;

    /**
     * Code du format utilis� pour construire le champ des heures de {@link #format}. Les codes
     * valides sont notamment  {@link DateFormat#SHORT},  {@link DateFormat#MEDIUM}  ou  {@link
     * DateFormat#LONG}. La valeur -1 indique que le format ne contient pas de champ des heures,
     * seulement un champ de date.
     */
    private transient int timeFormat = -1;

    /**
     * Indique si {@link #format} est valide. Le format peut
     * devenir invalide si {@link #init} a �t� appel�e. Dans
     * ce cas, il peut falloir changer le nombre de chiffres
     * apr�s la virgule qu'il �crit.
     */
    private transient boolean formatValid;

    /**
     * Conventions � utiliser pour
     * le formatage des nombres.
     */
    private Locale locale;

    /**
     * Construit un it�rateur pour la graduation d'un axe du temps.
     * La m�thode {@link #init} <u>doit</u> �tre appel�e avant que
     * l'it�rateur ne soit utilisable.
     *
     * @param timezone Fuseau horaire des dates.
     * @param locale   Conventions � utiliser pour le formatage des dates.
     */
    protected DateIterator(final TimeZone timezone, final Locale locale) {
        assert INTERVAL.length*ROLL_WIDTH == ROLL.length;
        calendar = Calendar.getInstance(timezone, locale);
        this.locale=locale;
    }

    /**
     * Initialise l'it�rateur.
     *
     * @param minimum         Date minimale de la premi�re graduation.
     * @param maximum         Date limite des graduations. La derni�re graduation
     *                        ne sera pas n�cessairement � cette date.
     * @param visualLength    Longueur visuelle de l'axe sur laquelle tracer la graduation.
     *                        Cette longueur doit �tre exprim�e en pixels ou en points.
     * @param visualTickSpace Espace � laisser visuellement entre deux marques de graduation.
     *                        Cet espace doit �tre exprim� en pixels ou en points (1/72 de pouce).
     */
    protected void init(final long  minimum,
                        final long  maximum,
                        final float visualLength,
                        final float visualTickSpacing)
    {
        if (minimum           == this.minimum      &&
            maximum           == this.maximum      &&
            visualLength      == this.visualLength &&
            visualTickSpacing == this.visualTickSpacing)
        {
            rewind();
            return;
        }
        AbstractGraduation.ensureNonNull("visualLength",      visualLength);
        AbstractGraduation.ensureNonNull("visualTickSpacing", visualTickSpacing);
        this.visualLength      = visualLength;
        this.visualTickSpacing = visualTickSpacing;
        this.formatValid       = false;
        this.minimum           = minimum;
        this.maximum           = maximum;
        this.increment         = Math.round((maximum-minimum) *
                                            ((double)visualTickSpacing/(double)visualLength));
        /*
         * Apr�s avoir fait une estimation de l'intervalle d'�chantillonage,
         * v�rifie si on trouve cette estimation dans le tableau 'INTERVAL'.
         * Si on trouve la valeur exacte, tant mieux! Sinon, on cherchera
         * l'intervalle le plus proche.
         */
        int index = Arrays.binarySearch(INTERVAL, increment);
        if (index < 0) {
            index= ~index;
            if (index == 0) {
                // L'intervalle est plus petit que le
                // plus petit �l�ment de 'INTERVAL'.
                round(Calendar.MILLISECOND);
                findFirstTick();
                return;
            } else if (index>=INTERVAL.length) {
                // L'intervalle est plus grand que le
                // plus grand �l�ment de 'INTERVAL'.
                increment /= YEAR;
                round(Calendar.YEAR);
                increment *= YEAR;
                findFirstTick();
                return;
            } else {
                // L'index pointe vers un intervalle plus grand que
                // l'intervalle demand�. V�rifie si l'intervalle
                // inf�rieur ne serait pas plus proche.
                if (increment-INTERVAL[index-1] < INTERVAL[index]-increment) {
                    index--;
                }
            }
        }
        this.increment    = INTERVAL[index]; index *= ROLL_WIDTH;
        this.tickAdd      = ROLL[index+0];
        this.tickField    = ROLL[index+1];
        this.subTickAdd   = ROLL[index+2];
        this.subTickField = ROLL[index+3];
        if (subTickField == Calendar.MILLISECOND) {
            subTickAdd *= 10;
        }
        findFirstTick();
    }

    /**
     * Arrondi {@link #increment} � un nombre qui se lit bien. Le nombre
     * choisit sera un de ceux de la suite 1, 2, 5, 10, 20, 50, 100, 200,
     * 500, etc.
     */
    private void round(final int field) {
        int factor=1;
        while (factor <= increment) {
            factor *= 10;
        }
        if (factor >= 10) {
            factor /= 10;
        }
        increment /= factor;
        if      (increment<=0                ) increment= 1;
        else if (increment>=3 && increment<=4) increment= 5;
        else if (increment>=6                ) increment=10;
        increment    = Math.max(increment*factor, 5);
        tickAdd      = (int)increment;
        subTickAdd   = (int)(increment/(increment==2 ? 4 : 5));
        tickField    = field;
        subTickField = field;
    }

    /**
     * Replace l'it�rateur sur la premi�re graduation. La position de la
     * premi�re graduation sera calcul�e et retenue pour un positionnement
     * plus rapide � l'avenir.
     */
    private void findFirstTick() {
        calendar.clear();
        value = minimum;
        date.setTime(value);
        calendar.setTime(date);
        if (true) {
            // Arrondie la date de d�part. Note: ce calcul exige que
            // tous les champs commencent � 0 plut�t que 1, y compris
            // les mois et le jour du mois.
            final int offset = calendar.getActualMinimum(tickField);
            int      toRound = calendar.get(tickField)-offset;
            toRound = (toRound/tickAdd)*tickAdd;
            calendar.set(tickField, toRound+offset);
        }
        truncate(calendar, tickField);
        nextTick=calendar.getTime().getTime();
        nextSubTick=nextTick;
        while (nextTick < minimum) {
            calendar.add(tickField, tickAdd);
            nextTick = calendar.getTime().getTime();
        }
        date.setTime(nextSubTick);
        calendar.setTime(date);
        while (nextSubTick < minimum) {
            calendar.add(subTickField, subTickAdd);
            nextSubTick = calendar.getTime().getTime();
        }
        /* 'calendar' a maintenant la valeur 'nextSubTick',
         * comme le veut la sp�cification de ce champ.  On
         * appelle maintenant 'next' pour transf�rer cette
         * valeur 'nextSubTick' vers 'value'.    Notez que
         * 'next' peut �tre appel�e m�me si value>maximum.
         */
        next();

        // Retient les positions trouv�es.
        this.value0       = this.value;
        this.nextTick0    = this.nextTick;
        this.nextSubTick0 = this.nextSubTick;
        this.isMajorTick0 = this.isMajorTick;

        assert calendar.getTime().getTime() == nextSubTick;
    }

    /**
     * Met � 0 tous les champs du calendrier inf�rieur au champ <code>field</code>
     * sp�cifi�. Note: si le calendrier sp�cifi� est {@link #calendar},  il est de
     * la responsabilit� de l'appelant de restituer {@link #calendar} dans son �tat
     * correct apr�s l'appel de cette m�thode.
     */
    private static void truncate(final Calendar calendar, int field) {
        for (int i=0; i<FIELD.length; i++) {
            if (FIELD[i] == field) {
                calendar.get(field); // Force la mise � jour des champs.
                while (++i < FIELD.length) {
                    field = FIELD[i];
                    calendar.set(field, calendar.getActualMinimum(field));
                }
                break;
            }
        }
    }

    /**
     * Indique s'il reste des graduations � retourner. Cette m�thode retourne
     * <code>true</code> tant que {@link #currentValue} ou {@link #currentLabel}
     * peuvent �tre appel�es.
     */
    public boolean hasNext() {
        return value<=maximum;
    }

    /**
     * Indique si la graduation courante est une graduation majeure.
     *
     * @return <code>true</code> si la graduation courante est une
     *         graduation majeure, ou <code>false</code> si elle
     *         est une graduation mineure.
     */
    public boolean isMajorTick() {
        return isMajorTick;
    }

    /**
     * Returns the position where to draw the current tick. The position is scaled
     * from the graduation's minimum to maximum. This is usually the same number
     * than {@link #currentValue}.
     */
    public double currentPosition() {
        return value;
    }

    /**
     * Retourne la valeur de la graduation courante. Cette m�thode
     * peut �tre appel�e pour une graduation majeure ou mineure.
     */
    public double currentValue() {
        return value;
    }

    /**
     * Retourne l'�tiquette de la graduation courante. On n'appele g�n�ralement
     * cette m�thode que pour les graduations majeures, mais elle peut aussi
     * �tre appel�e pour les graduations mineures. Cette m�thode retourne
     * <code>null</code> s'il n'y a pas d'�tiquette pour la graduation courante.
     */
    public String currentLabel() {
        if (!formatValid) {
            date.setTime(minimum);
            calendar.setTime(date);
            final int firstDay =calendar.get(Calendar.DAY_OF_YEAR);
            final int firstYear=calendar.get(Calendar.YEAR);

            date.setTime(maximum);
            calendar.setTime(date);
            final int lastDay =calendar.get(Calendar.DAY_OF_YEAR);
            final int lastYear=calendar.get(Calendar.YEAR);

            final int dateFormat = (firstYear==lastYear && firstDay==lastDay) ? -1 : DateFormat.MEDIUM;
            final int timeFormat;

            if      (increment >= DAY) timeFormat= -1;
            else if (increment >= MIN) timeFormat=DateFormat.SHORT ;
            else if (increment >= SEC) timeFormat=DateFormat.MEDIUM;
            else                       timeFormat=DateFormat.LONG  ;

            if (dateFormat!=this.dateFormat || timeFormat!=this.timeFormat || format==null) {
                this.dateFormat = dateFormat;
                this.timeFormat = timeFormat;
                if (dateFormat == -1) {
                    if (timeFormat == -1) {
                        format = DateFormat.getDateInstance(DateFormat.DEFAULT, locale);
                    } else {
                        format = DateFormat.getTimeInstance(timeFormat, locale);
                    }
                } else if (timeFormat == -1) {
                    format = DateFormat.getDateInstance(dateFormat, locale);
                } else {
                    format = DateFormat.getDateTimeInstance(dateFormat, timeFormat, locale);
                }
                format.setCalendar(calendar);
            }
            formatValid = true;
        }
        date.setTime(value);
        final String label = format.format(date);
        // Remet 'calendar' dans l'�tat qu'il est senc� avoir
        // d'apr�s la sp�cification du champ {@link #calendar}.
        date.setTime(nextSubTick);
        calendar.setTime(date);
        return label;
    }

    /**
     * Passe � la graduation suivante.
     */
    public void next() {
        assert calendar.getTime().getTime() == nextSubTick;
        if (nextSubTick < nextTick) {
            isMajorTick = false;
            value = nextSubTick;
            /*
             * IMPORTANT: On suppose ici que 'calendar' a d�j�
             *            la date 'nextSubTick'. Si ce n'�tait
             *            pas le cas, il faudrait ajouter les
             *            lignes suivantes:
             */
            if (false) {
                date.setTime(value);
                calendar.setTime(date);
                // 'setTime' oblige 'calendar' � recalculer ses
                // champs, ce qui a un impact sur la performance.
            }
            calendar.add(subTickField, subTickAdd);
            nextSubTick=calendar.getTime().getTime();
            // 'calendar' contient maintenant la date 'nextSubTick',
            // comme le veut la sp�cification du champ {@link #calendar}.
        } else {
            nextMajor();
        }
    }

    /**
     * Passe directement � la graduation majeure suivante.
     */
    public void nextMajor() {
        isMajorTick = true;
        value = nextTick;
        date.setTime(value);

        calendar.setTime(date);
        calendar.add(tickField, tickAdd);
        truncate(calendar, tickField);
        nextTick=calendar.getTime().getTime();

        calendar.setTime(date);
        calendar.add(subTickField, subTickAdd);
        nextSubTick=calendar.getTime().getTime();
        // 'calendar' contient maintenant la date 'nextSubTick',
        // comme le veut la sp�cification du champ {@link #calendar}.
    }

    /**
     * Replace l'it�rateur sur la premi�re graduation.
     */
    public void rewind() {
        this.value       = value0;
        this.nextTick    = nextTick0;
        this.nextSubTick = nextSubTick0;
        this.isMajorTick = isMajorTick0;
        // Pour �tre en accord avec la sp�cification
        // du champs {@link #calendar}...
        date.setTime(nextSubTick);
        calendar.setTime(date);
    }

    /**
     * Retourne les conventions � utiliser pour
     * �crire les �tiquettes de graduation.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Modifie les conventions � utiliser pour
     * �crire les �tiquettes de graduation.
     */
    public void setLocale(final Locale locale) {
        if (!locale.equals(this.locale)) {
            calendar    = Calendar.getInstance(getTimeZone(), locale);
            format      = null;
            formatValid = false;
            // Pour �tre en accord avec la sp�cification
            // du champs {@link #calendar}...
            date.setTime(nextSubTick);
            calendar.setTime(date);
        }
        assert calendar.getTime().getTime() == nextSubTick;
    }

    /**
     * Retourne le fuseau horaire utilis� pour
     * exprimer les dates dans la graduation.
     */
    public TimeZone getTimeZone() {
        return calendar.getTimeZone();
    }

    /**
     * Modifie le fuseau horaire utilis� pour
     * exprimer les dates dans la graduation.
     */
    public void setTimeZone(final TimeZone timezone) {
        if (!timezone.equals(getTimeZone())) {
            calendar.setTimeZone(timezone);
            format      = null;
            formatValid = false;
            // Pour �tre en accord avec la sp�cification
            // du champs {@link #calendar}...
            date.setTime(nextSubTick);
            calendar.setTime(date);
        }
        assert calendar.getTime().getTime() == nextSubTick;
    }

    /**
     * Retourne le nom du champ de {@link Calendar}
     * correspondant � la valeur sp�cifi�e.
     */
    private static String getFieldName(final int field) {
        for (int i=0; i<FIELD.length; i++) {
            if (FIELD[i] == field) {
                return FIELD_NAME[i];
            }
        }
        return String.valueOf(field);
    }

    /**
     * Returns a string representation of this iterator.
     * Used for debugging purpose only.
     */
    public String toString() {
        if (true) {
            // Note: in this particular case, using PrintWriter with 'println' generates
            //       less bytecodes than chaining StringBuffer.append(...) calls.
            final StringWriter  buf = new StringWriter();
            final PrintWriter   out = new PrintWriter(buf);
            final DateFormat format = DateFormat.getDateTimeInstance();
            format.setTimeZone(calendar.getTimeZone());
            out.print("Minimum      = "); out.println(format.format(new Date(minimum)));
            out.print("Maximum      = "); out.println(format.format(new Date(maximum)));
            out.print("Increment    = "); out.print(increment/(24*3600000f));    out.println(" days");
            out.print("Tick inc.    = "); out.print(   tickAdd); out.print(' '); out.println(getFieldName(   tickField));
            out.print("SubTick inc. = "); out.print(subTickAdd); out.print(' '); out.println(getFieldName(subTickField));
            out.print("Next tick    = "); out.println(format.format(new Date(   nextTick)));
            out.print("Next subtick = "); out.println(format.format(new Date(nextSubTick)));
            out.flush();
            return buf.toString();
        } else {
            return super.toString();
        }
    }
}
