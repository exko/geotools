/*
 *    Geotools - OpenSource mapping toolkit
 *    (C) 2002, Centre for Computational Geography
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
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.geotools.styling;

/**
 * A rule is used to attach a condition to, and group, the individual
 * symbolizers used for rendering.  The Title and Abstract describe the rule
 * and may be used to generate a legend, as may the LegendGraphic. The Filter,
 * ElseFilter, MinScale and MaxScale elements allow the selection of features
 * and rendering scales for a rule.  The scale selection works as follows.
 * When a map is to be rendered, the scale denominator is computed and all
 * rules in all UserStyles that have a scale outside of the request range are
 * dropped.  (This also includes Rules that have an ElseFilter.)  An
 * ElseFilter is simply an ELSE condition to the conditions (Filters) of all
 * other rules in the same UserStyle. The exact meaning of the ElseFilter is
 * determined after Rules have been eliminated for not fitting the rendering
 * scale.  This definition of the behaviour of ElseFilters may seem a little
 * strange, but it allows for scale-dependent and scale-independent ELSE
 * conditions.  For the Filter, only SqlExpression is available for
 * specification, but this is a hack and should be replaced with Filter as
 * defined in WFS. A missing Filter element means "always true".  If a set of
 * Rules has no ElseFilters, then some features may not be rendered (which is
 * presumably the desired behavior).  The Scales are actually scale
 * denominators (as double floats), so "10e6" would be interpreted as 1:10M.
 * A missing MinScale means there is no lower bound to the scale-denominator
 * range (lim[x->0+](x)), and a missing MaxScale means there is no upper bound
 * (infinity).  0.28mm
 */
import org.geotools.filter.Filter;


public interface Rule {
    /**
     * Gets the name of the rule.
     *
     * @return The name of the rule.  This provides a way to identify a rule.
     */
    String getName();

    /**
     * Sets the name of the rule.
     *
     * @param name The name of the rule.  This provides a way to identify a
     *        rule.
     */
    void setName(String name);

    /**
     * Gets the title.
     *
     * @return The title of the rule.  This is a brief, human readable,
     *         description of the rule.
     */
    String getTitle();

    /**
     * Sets the title.
     *
     * @param title The title of the rule.  This is a brief, human readable,
     *        description of the rule.
     */
    void setTitle(String title);

    /**
     * Gets the abstract text for the rule.
     *
     * @return The abstract text, a more detailed description of the rule.
     */
    String getAbstract();

    /**
     * Sets the abstract text for the rule.
     *
     * @param abstractStr The abstract text, a more detailed description of the
     *        rule.
     */
    void setAbstract(String abstractStr);

    /**
     * The smallest value for scale denominator at which symbolizers contained
     * by this rule should be applied.
     *
     * @return The smallest (inclusive) denominator value that this rule will
     *         be active for.
     */
    double getMinScaleDenominator();

    /**
     * The smallest value for scale denominator at which symbolizers contained
     * by this rule should be applied.
     *
     * @param scale The smallest (inclusive) denominator value that this rule will be
     *        active for.
     */
    void setMinScaleDenominator(double scale);

    /**
     * The largest value for scale denominator at which symbolizers contained
     * by this rule should be applied.
     *
     * @return The largest (exclusive) denominator value that this rule will be
     *         active for.
     */
    double getMaxScaleDenominator();

    /**
     * The largest value for scale denominator at which symbolizers contained
     * by this rule should be applied.
     *
     * @param scale The largest (exclusive) denominator value that this rule will be
     *        active for.
     */
    void setMaxScaleDenominator(double scale);

    Filter getFilter();

    void setFilter(Filter filter);

    boolean hasElseFilter();

    void setIsElseFilter(boolean defaultb);

    /**
     * A set of equivalent Graphics in different formats which can be used as a
     * legend against features stylized by the symbolizers in this rule.
     *
     * @return An array of Graphic objects, any of which can be used as the
     *         legend.
     */
    Graphic[] getLegendGraphic();

    /**
     * A set of equivalent Graphics in different formats which can be used as a
     * legend against features stylized by the symbolizers in this rule.
     *
     * @param graphics An array of Graphic objects, any of which can be used as the
     *        legend.
     */
    void setLegendGraphic(Graphic[] graphics);

    /**
     * The symbolizers contain the actual styling information for different
     * geometry types.  A single feature may be rendered by more than one of
     * the symbolizers returned by this method.  It is important that the
     * symbolizers be applied in the order in which they are returned if the
     * end result is to be as intended. All symbolizers should be applied to
     * all features which make it through the filters in this rule regardless
     * of the features' geometry. For example, a polygon symbolizer should be
     * applied to line geometries and even points.  If this is not the desired
     * beaviour, ensure that either the filters block inappropriate features
     * or that the FeatureTypeStyler which contains this rule has its
     * FeatureTypeName or SemanticTypeIdentifier set appropriately.
     *
     * @return An array of symbolizers to be applied, in sequence, to all of
     *         the features addressed by the FeatureTypeStyler which contains
     *         this rule.
     */
    Symbolizer[] getSymbolizers();

    /**
     * The symbolizers contain the actual styling information for different
     * geometry types.  A single feature may be rendered by more than one of
     * the symbolizers returned by this method.  It is important that the
     * symbolizers be applied in the order in which they are returned if the
     * end result is to be as intended. All symbolizers should be applied to
     * all features which make it through the filters in this rule regardless
     * of the features' geometry. For example, a polygon symbolizer should be
     * applied to line geometries and even points.  If this is not the desired
     * beaviour, ensure that either the filters block inappropriate features
     * or that the FeatureTypeStyler which contains this rule has its
     * FeatureTypeName or SemanticTypeIdentifier set appropriately.
     *
     * @param symbolizers An array of symbolizers to be applied, in sequence, to all of the
     *        features addressed by the FeatureTypeStyler which contains this
     *        rule.
     */
    void setSymbolizers(Symbolizer[] symbolizers);

    void accept(StyleVisitor visitor);

    Object clone() throws CloneNotSupportedException;
}
