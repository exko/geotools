/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2002, Institut de Recherche pour le D�veloppement
 * (C) 1998, P�ches et Oc�ans Canada
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
package org.geotools.renderer.geom;

// J2SE dependencies
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.IllegalPathStateException;

// Geotools dependencies
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.TransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XArray;


/**
 * Classe ayant la charge d'effectuer un "clip" sur plusieurs polygones. Le constructeur re�oit
 * en argument un rectangle {@link Rectangle2D}, qui repr�sente le contour du "clip". La m�thode
 * {@link #clip(Polygon, Polyline.Iterator)} pourra ensuite effectuer les clips � r�p�titions sur
 * une s�ries de polygones.
 *
 * @version $Id: Clipper.java,v 1.2 2003/02/10 23:09:35 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
final class Clipper {
    /**
     * Syst�me de coordonn�es de {@link #mapClip}.
     */
    final CoordinateSystem mapCS;

    /**
     * R�gion � couper.  Les coordonn�es de cette r�gion doivent �tre exprim�es selon
     * le syst�me de coordonn�es {@link #mapCS} (sp�cifi� lors de la construction) et
     * ne pas �tre modifi�es. Ce rectangle doit �tre en lecture seule.
     */
    final Rectangle2D mapClip;

    /**
     * R�gion � couper. Il s'agit des m�mes coordonn�es que {@link #mapClip},
     * mais projet�s dans le syst�me de coordonn�es natif du polygone � couper.
     */
    private final Rectangle2D clip = new Rectangle2D.Double();

    /**
     * Coordonn�es <var>x</var> et <var>y</var>
     * minimales et maximales de {@link #clip}.
     */
    private float xmin, ymin, xmax, ymax;

    /**
     * Ligne repr�sentant un segment d'un objet {@link Polygon}. Cet
     * objet est cr�� une fois pour toute afin d'�viter d'avoir � le
     * recr�er trop souvent. Il est utilis� � l'interne pour balayer
     * les coordonn�es d'un objet {@link Polygon}.
     */
    private final Line2D.Float line = new Line2D.Float();

    /**
     * Coordonn�es � transmettre � {@link Polygon#appendBorder}. Ce tableau est r�serv�
     * � un usage interne par {@link #clip(Polygon, Polyline.Iterator)}, qui le construira
     * et le modifiera constament.
     */
    private float[] border = new float[16];

    /**
     * Nombre d'�l�ments valides dans {@link #border}. Ce champs sera
     * incr�ment� selon les besoins par la m�thode {@link #addBorder}.
     */
    private int borderLength;

    /**
     * Coordonn�es des points d'intersections entre un polygone et {@link #clip}. Ce tableau est
     * r�serv� � un usage interne par {@link #clip(Polygon, Polyline.Iterator)}, qui le construira
     * et le modifiera constament.
     */
    private float[] intersect = new float[8];

    /**
     * Nombre d'�l�ments valides dans {@link #intersect}. Ce champs sera
     * incr�ment� selon les besoins par la m�thode {@link #addIntersect}.
     */
    private int intersectLength;

    /**
     * Construit un objet qui aurra la charge de couper
     * les polygones apparaissant dans une certaine r�gion.
     *
     * @param mapClip Les limites de la r�gion � conserver.
     * @param mapCS Syst�me de coordonn�es du <code>clip</code>.
     */
    public Clipper(final Rectangle2D mapClip, final CoordinateSystem mapCS) {
        this.mapCS   = mapCS;
        this.mapClip = mapClip;
        this.clip.setRect(mapClip);
        xmin = (float) clip.getMinX();
        xmax = (float) clip.getMaxX();
        ymin = (float) clip.getMinY();
        ymax = (float) clip.getMaxY();
    }

    /**
     * Returns the clip in the polygon's internal coordinate system. This method
     * <strong>must</strong> be invoked before {@link #clip(Polygon, Polyline.Iterator)}.
     *
     * @param  The polygon to clip.
     * @return The clip in polygon's internal coordinate system. Will never be null, but may
     *         be empty if a transformation failed. Note: this method returns an internal
     *         rectangle. <strong>Do not modify</strong>.
     */
    public Rectangle2D getInternalClip(final Polygon polygon) {
        try {
            final MathTransform2D tr;
            tr = Polygon.getMathTransform2D(polygon.getTransformationFromInternalCS(mapCS));
            if (tr != null) {
                CTSUtilities.transform((MathTransform2D) tr.inverse(), mapClip, clip);
            } else {
                clip.setRect(mapClip);
            }
        } catch (TransformException exception) {
            // Cette exception peut �tre normale.
            Polyline.unexpectedException("Clipper", "getInternalClip", exception);
            clip.setRect(0,0,0,0);
        }
        xmin = (float) clip.getMinX();
        xmax = (float) clip.getMaxX();
        ymin = (float) clip.getMinY();
        ymax = (float) clip.getMaxY();
        return clip;
    }

    /**
     * Ajoute un point (<var>x0</var>,<var>y0</var>) �
     * la fin de la bordure {@link #border}. Le tableau
     * sera automatiquement agrandit selon les besoins.
     */
    private void addBorder(final float x0, final float y0) {
        if (borderLength >= 2) {
            if (border[borderLength-2]==x0 &&
                border[borderLength-1]==y0)
            {
                return;
            }
        }
        if (borderLength >= border.length) {
            border = XArray.resize(border, 2*borderLength);
        }
        border[borderLength++] = x0;
        border[borderLength++] = y0;
    }

    /**
     * Ajoute un point (<var>x0</var>,<var>y0</var>) �
     * la liste des intersections {@link #intersect}.
     * Le tableau sera automatiquement agrandit selon
     * les besoins.
     */
    private void addIntersect(final float x0, final float y0) {
        if (intersectLength >= 2) {
            if (intersect[intersectLength-2]==x0 &&
                intersect[intersectLength-1]==y0)
            {
                return;
            }
        }
        if (intersectLength >= intersect.length) {
            intersect = XArray.resize(intersect, 2*intersectLength);
        }
        intersect[intersectLength++] = x0;
        intersect[intersectLength++] = y0;
    }

    /**
     * Construit une bordure de fa�on � relier le point (<var>x0</var>,<var>y0</var>) au point
     * (<var>x1</var>,<var>y1</var>) exclusivement sans couper le rectangle {@link #clip}. Les
     * points n�cessaires seront ajout�s au tableau {@link #border}.
     *
     * @param clockwise Indique de quelle fa�on il faudra tourner autour du rectangle
     *        {@link #clip} pour ajouter des points. Une valeur positive tournera dans
     *        le sens des aiguilles d'une montre, tandis qu'une valeur n�gative tournera
     *        dans le sens inverse. La valeur 0 n'aura aucun effet.
     * @param x0 Coordonn�es <var>x</var> du premier point d�tect� � l'ext�rieur de {@link #clip}.
     * @param y0 Coordonn�es <var>y</var> du premier point d�tect� � l'ext�rieur de {@link #clip}.
     * @param x1 Coordonn�es <var>x</var> du dernier point d�tect� � l'ext�rieur de {@link #clip}.
     * @param y1 Coordonn�es <var>y</var> du dernier point d�tect� � l'ext�rieur de {@link #clip}.
     */
    private void buildBorder(final double clockwise, float x0, float y0,
                             final float x1, final float y1)
    {
        if (clockwise > 0) {
            while (true) {
                if (y0 >= ymax) {
                    if (y1>=ymax && x1>=x0) break;
                    if (x0<xmax) addBorder(x0=xmax, y0=ymax);
                }
                if (x0 >= xmax) {
                    if (x1>=xmax && y1<=y0) break;
                    if (y0>ymin) addBorder(x0=xmax, y0=ymin);
                }
                if (y0 <= ymin) {
                    if (y1<=ymin && x1<=x0) break;
                    if (x0>xmin) addBorder(x0=xmin, y0=ymin);
                }
                if (x0 <= xmin) {
                    if (x1<=xmin && y1>=y0) break;
                    if (y0<ymax) addBorder(x0=xmin, y0=ymax);
                }
            }
        } else if (clockwise < 0) {
            while (true) {
                if (y0 >= ymax) {
                    if (y1>=ymax && x1<=x0) break;
                    if (x0>xmin) addBorder(x0=xmin, y0=ymax);
                }
                if (x0 <= xmin) {
                    if (x1<=xmin && y1<=y0) break;
                    if (y0>ymin) addBorder(x0=xmin, y0=ymin);
                }
                if (y0 <= ymin) {
                    if (y1<=ymin && x1>=x0) break;
                    if (x0<xmax) addBorder(x0=xmax, y0=ymin);
                }
                if (x0 >= xmax) {
                    if (x1>=xmax && y1>=y0) break;
                    if (y0<ymax) addBorder(x0=xmax, y0=ymax);
                }
            }
        }
    }

    /**
     * Attache le polygone <code>subpoly</code> � la fin du polygone <code>result</code>.
     * Entre les deux sera ins�r� la bordure {@link #border}, s'il y en a une. Cette
     * m�thode retourne le polygone r�sultant de la fusion.
     *
     * @param result  The first polygon, or <code>null</code>. This polygon will be modified.
     *                We usually create this polygon inside this method and reuse it in many
     *                calls in order to build the clipped polygon.
     * @param subpoly The second polygon (usually the result of a call to {@link Polygob#subpoly}.
     *                This polygon will never be modified.
     * @return <code>result</code>, or a new polygon if <code>result</code> was null.
     */
    private Polygon attach(Polygon result, Polygon subpoly) {
        if (subpoly != null) {
            try {
                if (result == null) {
                    result = (Polygon) subpoly.clone();
                    result.prependBorder(border, 0, borderLength, null);
                } else {
                    result.appendBorder(border, 0, borderLength, null);
                    result.append(subpoly); // 'subpoly.clone()' done by 'append'.
                }
                borderLength = 0;
            } catch (TransformException exception) {
                // Should not happen, since we are working
                // in polygon's native coordinate system.
                Polygon.unexpectedException("clip", exception);
            }
        }
        return result;
    }

    /**
     * Retourne un polygone qui ne contient que les points de <code>polygon</code> qui apparaissent
     * dans le rectangle sp�cifi� au constructeur. Si aucun point du polygone n'apppara�t �
     * l'int�rieur de <code>clip</code>, alors cette m�thode retourne <code>null</code>. Si tous
     * les points du polygone apparaissent � l'int�rieur de <code>clip</code>, alors cette m�thode
     * retourne <code>polygon</code>. Sinon, cette m�thode retourne un polygone qui contiendra les
     * points qui apparaissent � l'int�rieur de <code>clip</code>. Ces polygones partageront les
     * m�mes donn�es que <code>polygon</code> autant que possible, de sorte que la consomation
     * de m�moire devrait rester raisonable.
     * <br><br>
     * Note: avant d'appeller cette m�thode, {@link #getInternalClip}
     *       devra obligatoire avoir �t� appel�e.
     *
     * @param  polygon Polygone � couper dans une r�gion.
     * @return Polygone coup�.
     */
    public Polygon clip(final Polygon polygon) {
        borderLength    = 0;
        intersectLength = 0;
        Polygon result  = null;
        final InteriorType polygonType = polygon.getInteriorType();
        final Polyline.Iterator it;
        try {
            it = polygon.iterator(null);
        } catch (CannotCreateTransformException exception) {
            // Should not happen, since we are working in polygon's native CS.
            final IllegalPathStateException e;
            e = new IllegalPathStateException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        /*
         * Obtient la premi�re coordonn�e du polygone. Cette premi�re coordonn�e sera m�moris�e
         * dans les variables <code>first[X/Y]</code> afin d'�tre r�utilis�e pour �ventuellement
         * fermer le polygone. On devra v�rifier si cette premi�re coordonn�es est � l'int�rieur
         * ou � l'ext�rieur de la r�gion d'int�r�t. Cette v�rification sert � initialiser la
         * variable <code>inside</code>, qui servira pour le reste de cette m�thode.
         */
        if (it.next(line)) {
            final float firstX = line.x2;
            final float firstY = line.y2;
            boolean inside = (firstX>=xmin && firstX<=xmax) && (firstY>=ymin && firstY<=ymax);
            float  initialX1        =  Float.NaN;
            float  initialY1        =  Float.NaN;
            double initialClockwise = Double.NaN;
            float  x0               =  Float.NaN;
            float  y0               =  Float.NaN;
            double clockwise        =  0;
            int lower=0, upper=0;
            while (true) {
                /*
                 * Extrait la coordonn�es suivantes. Le point <code>line.p2</code>
                 * contiendra le point que l'on vient d'extraire, tandis que le
                 * point <code>line.p1</code> sera la coordonn�e qu'on avait lors
                 * du passage pr�c�dent de cette boucle. Si toute les coordonn�es
                 * ont �t� balay�es, on r�utilisera le premier point pour refermer
                 * le polygone.
                 */
                if (!it.next(line)) {
                    if ((line.x2!=firstX || line.y2!=firstY) && polygonType!=null) {
                        line.x2 = firstX;
                        line.y2 = firstY;
                    }
                    else break;
                }
                upper++;
                /*
                 * V�rifie si le segment (x1,y1)-(x2,y2) tourne dans le sens des aiguilles
                 * d'une montre autour du rectangle. Les segments � l'int�rieur du rectangle
                 * ne seront pas pris en compte. Dans l'exemple ci-dessous, le segment va dans
                 * le sens inverse des aiguilles d'une montre.
                 *
                 * +--------+
                 * |        |
                 * |        |   //(x2,y2)
                 * +--------+  //
                 *            //(x1,y1)
                 */
                int outcode1=0;
                int outcode2=0;
                boolean out1, out2;
                final float x1 = line.x1;
                final float y1 = line.y1;
                final float x2 = line.x2;
                final float y2 = line.y2;
                final float dx = x2-x1;
                final float dy = y2-y1;

                if (out1 = y1>ymax) outcode1 |= Rectangle2D.OUT_BOTTOM;
                if (out2 = y2>ymax) outcode2 |= Rectangle2D.OUT_BOTTOM;
                if (out1 && out2) clockwise += dx;
                else if (out1)    clockwise += dx/dy*(ymax-y1);
                else if (out2)    clockwise += dx/dy*(y2-ymax);

                if (out1 = y1<ymin) outcode1 |= Rectangle2D.OUT_TOP;
                if (out2 = y2<ymin) outcode2 |= Rectangle2D.OUT_TOP;
                if (out1 && out2) clockwise -= dx;
                else if (out1)    clockwise -= dx/dy*(ymin-y1);
                else if (out2)    clockwise -= dx/dy*(y2-ymin);

                if (out1 = x1>xmax) outcode1 |= Rectangle2D.OUT_RIGHT;
                if (out2 = x2>xmax) outcode2 |= Rectangle2D.OUT_RIGHT;
                if (out1 && out2) clockwise -= dy;
                else if (out1)    clockwise -= dy/dx*(xmax-x1);
                else if (out2)    clockwise -= dy/dx*(x2-xmax);

                if (out1 = x1<xmin) outcode1 |= Rectangle2D.OUT_LEFT;
                if (out2 = x2<xmin) outcode2 |= Rectangle2D.OUT_LEFT;
                if (out1 && out2) clockwise += dy;
                else if (out1)    clockwise += dy/dx*(xmin-x1);
                else if (out2)    clockwise += dy/dx*(x2-xmin);
                /*
                 * V�rifie maintenant si les points (x1,y1) et (x2,y2) sont tous deux �
                 * l'ext�rieur du clip. Qu'il soit tout deux � l'ext�rieur ne veux pas
                 * dire qu'il n'y a aucune intersections entre la ligne P1-P2 et le clip.
                 * il faudra v�rifier (on le ferra plus loin). Une premi�re �tape a d�j�
                 * �t� faite avec la condition <code>(outcode1 & outcode2)==0</code>, qui
                 * a permis de v�rifier que les deux points ne se trouvent pas du m�me c�t�
                 * du rectangle.
                 */
                final boolean lineInsideAndOutside = (inside != (outcode2==0));
                final boolean lineCompletlyOutside = !lineInsideAndOutside &&
                                        (outcode1!=0 && outcode2!=0 && (outcode1 & outcode2)==0);
                /*
                 * Ajoute � la bordure les points d'intersections, s'il a �t� d�termin�
                 * que les points d'intersections doivent �tre ajout�s. Cette situation
                 * se produit dans trois cas:
                 *
                 *  1) On vient d'entrer dans le clip. Le code plus bas construira toute
                 *     la bordure qui pr�c�de l'entr�. On compl�te cette bordure par le
                 *     point d'intersection entre le clip et le polygone.
                 *  2) On vient de sortir du clip. Le code plus bas m�morisera les donn�es
                 *     n�cessaires au tra�age du polygone qui se trouve enti�rement �
                 *     l'int�rieur du clip. On compl�te ces donn�es par le point d'intersection
                 *     entre le clip et le polygone. Plus tard dans la boucle, une bordure sera
                 *     ajout�e � la suite de ce point d'intersection, suivit d'un autre point
                 *     d'intersection (�tape 1).
                 *  3) Il est possible qu'on ait travers� tout le clip d'un coups, sans
                 *     "s'arr�ter" � l'int�rieur. Un code plus bas tentera de d�tecter
                 *     cette situation particuli�re.
                 */
                intersectLength = 0;
                if (lineInsideAndOutside || lineCompletlyOutside) {
                    final float cxmin = Math.max(xmin, Math.min(x1, x2));
                    final float cxmax = Math.min(xmax, Math.max(x1, x2));
                    final float cymin = Math.max(ymin, Math.min(y1, y2));
                    final float cymax = Math.min(ymax, Math.max(y1, y2));

                    if (ymax>=cymin && ymax<=cymax) {
                        final float v = dx/dy*(ymax-y1)+x1;
                        if (v>=cxmin && v<=cxmax) addIntersect(v, ymax);
                    }
                    if (ymin>=cymin && ymin<=cymax) {
                        final float v = dx/dy*(ymin-y1)+x1;
                        if (v>=cxmin && v<=cxmax) addIntersect(v, ymin);
                    }
                    if (xmax>=cxmin && xmax<=cxmax) {
                        final float v = dy/dx*(xmax-x1)+y1;
                        if (v>=cymin && v<=cymax) addIntersect(xmax, v);
                    }
                    if (xmin>=cxmin && xmin<=cxmax) {
                        final float v = dy/dx*(xmin-x1)+y1;
                        if (v>=cymin && v<=cymax) addIntersect(xmin, v);
                    }
                    /*
                     * Classe les points d'intersections en utilisant un classement � bulles.
                     * Cette m�thode est en th�orie extr�mement contre-performante lorsqu'il
                     * y a beaucoup de donn�es � classer. Mais dans notre cas, il n'y aura
                     * normalement jamais plus de 2 points � classer, ce qui rend cette
                     * technique tr�s avantageuse.
                     */
                    boolean modified; do {
                        modified = false;
                        for (int i=2; i<intersectLength; i+=2) {
                            if ((intersect[i-2]-x1)*dx+(intersect[i-1]-y1)*dy >
                                (intersect[i+0]-x1)*dx+(intersect[i+1]-y1)*dy)
                            {
                                final float x  = intersect[i-2];
                                final float y  = intersect[i-1];
                                intersect[i-2] = intersect[i+0];
                                intersect[i-1] = intersect[i+1];
                                intersect[i+0] = x;
                                intersect[i+1] = y;
                                modified = true;
                            }
                        }
                    } while (modified);
                }
                if (lineInsideAndOutside) {
                    /*
                     * Une intersection a donc �t� trouv�e. Soit qu'on vient d'entrer dans la r�gion
                     * d'int�r�t, ou soit qu'on vient d'en sortir. La variable <code>inside</code>
                     * indiquera si on vient d'entrer ou de sortir de la r�gion <code>clip</code>.
                     */
                    inside = !inside;
                    if (inside) {
                        /*
                         * Si on vient d'entrer dans la r�gion d'int�r�t {@link #clip}, alors v�rifie
                         * s'il faut ajouter des points pour contourner la bordure du clip. Ces points
                         * seront effectivement m�moris�s plus tard, lorsque l'on sortira du clip.
                         */
                        float xn,yn;
                        if (intersectLength >= 2) {
                            xn = intersect[0];
                            yn = intersect[1];
                        } else {
                            xn = x1;
                            yn = y1;
                        }
                        if (Float.isNaN(x0) || Float.isNaN(y0)) {
                            initialClockwise = clockwise;
                            initialX1        = xn;
                            initialY1        = yn;
                        } else {
                            buildBorder(clockwise, x0, y0, xn, yn);
                        }
                        x0 = Float.NaN;
                        y0 = Float.NaN;
                        clockwise = 0;
                    } else {
                        /*
                         * Si on vient de sortir de la r�gion d'int�r�t, alors on cr�era un nouveau
                         * "sous-polygone" qui contiendra seulement les donn�es qui apparaissent
                         * dans la r�gion (les donn�es ne seront pas copi�es; seul un jeu de
                         * r�f�rences sera effectu�). Les coordonn�es x0,y0 seront celles du
                         * premier point en dehors du clip.
                         */
                        if (intersectLength >= 2) {
                            x0 = intersect[intersectLength-2];
                            y0 = intersect[intersectLength-1];
                        } else {
                            x0 = x2;
                            y0 = y2;
                        }
                        assert upper <= polygon.getPointCount() : upper;
                        result = attach(result, polygon.subpoly(lower, upper));
                    }
                    lower = upper;
                    /*
                     * Ajoute les points d'intersections � la bordure.
                     * La m�thode {@link #addBorder} s'assurera au passage
                     * qu'on ne r�p�te pas deux fois les m�mes points.
                     */
                    for (int i=0; i<intersectLength;) {
                        addBorder(intersect[i++], intersect[i++]);
                    }
                } else if (lineCompletlyOutside) {
                    /*
                     * On sait maintenant que les points (x1,y1) et (x2,y2) sont
                     * tous deux � l'ext�rieur du clip. Mais �a ne veux pas dire
                     * qu'il n'y a eu aucune intersection entre la ligne P1-P2 et
                     * le clip. S'il y a au moins deux points d'intersections, la
                     * ligne traverse le clip et on devra l'ajouter � la bordure.
                     */
                    if (intersectLength >= 4) {
                        /*
                         * D'abord, on refait le calcul de <code>clockwise</code>
                         * (voir plus haut) mais en comptant seulement la composante
                         * d� � la fin de la ligne (c'est-�-dire "if (out2) ...").
                         */
                        double clockwise2 = 0;
                        if ((outcode1 & Rectangle2D.OUT_BOTTOM)==0 &&
                            (outcode2 & Rectangle2D.OUT_BOTTOM)!=0) {
                                clockwise2 += dx/dy*(y2-ymax);
                        }
                        if ((outcode1 & Rectangle2D.OUT_TOP)==0 &&
                            (outcode2 & Rectangle2D.OUT_TOP)!=0) {
                                clockwise2 -= dx/dy*(y2-ymin);
                        }
                        if ((outcode1 & Rectangle2D.OUT_RIGHT)==0 &&
                            (outcode2 & Rectangle2D.OUT_RIGHT)!=0) {
                                clockwise2 -= dy/dx*(x2-xmax);
                        }
                        if ((outcode1 & Rectangle2D.OUT_LEFT)==0 &&
                            (outcode2 & Rectangle2D.OUT_LEFT)!=0) {
                                clockwise2 += dy/dx*(x2-xmin);
                        }
                        clockwise -= clockwise2;
                        if (Float.isNaN(x0) || Float.isNaN(y0)) {
                            initialClockwise = clockwise;
                            initialX1        = line.x1;
                            initialY1        = line.y1;
                        } else {
                            buildBorder(clockwise, x0, y0, intersect[0], intersect[1]);
                        }
                        x0 = intersect[intersectLength-2];
                        y0 = intersect[intersectLength-1];
                        clockwise = clockwise2;
                        /*
                         * Ajoute les points d'intersections � la bordure.
                         * La m�thode {@link #addBorder} s'assurera au passage
                         * qu'on ne r�p�te pas deux fois les m�mes points.
                         */
                        for (int i=0; i<intersectLength;) {
                            addBorder(intersect[i++], intersect[i++]);
                        }
                    }
                }
            }
            /*
             * A la fin de la boucle, ajoute les points
             * restants s'ils �taient � l'int�rieur du clip.
             */
            if (inside) {
                assert upper <= polygon.getPointCount() : upper;
                result = attach(result, polygon.subpoly(lower, upper));
            }
            if (polygonType != null) {
                if (!Float.isNaN(x0) && !Float.isNaN(y0)) {
                    buildBorder(clockwise+initialClockwise, x0, y0, initialX1, initialY1);
                }
            }
            if (result != null) {
                try {
                    result.appendBorder(border, 0, borderLength, null);
                    result.close(polygonType);
                } catch (TransformException exception) {
                    // Should not happen, since we are working
                    // in polygon's native coordinate system.
                    Polygon.unexpectedException("clip", exception);
                }
            } else if (borderLength != 0) {
                /*
                 * Si aucun polygone n'a �t� cr��, mais qu'on a quand
                 * m�me d�tect� des intersections (c'est-�-dire si le
                 * zoom ne contient aucun point du polygone mais intercepte
                 * une des lignes du polygone), alors on ajoutera les
                 * points d'intersections et leurs bordures.
                 */
                final float tmp[];
                if (border.length == borderLength) {
                    tmp = border;
                } else {
                    tmp = new float[borderLength];
                    System.arraycopy(border, 0, tmp, 0, borderLength);
                }
                final Polygon[] results = Polygon.getInstances(tmp, polygon.getCoordinateSystem());
                assert results.length == 1;
                result = results[0];
                result.close(polygonType);
            } else if (polygon.contains(clip.getCenterX(), clip.getCenterY())) {
                /*
                 * Si absolument aucun point du polygone ne se trouve �
                 * l'int�rieur du zoom, alors le zoom est soit compl�tement
                 * � l'int�rieur ou soit compl�tement � l'ext�rieur du polygone.
                 * S'il est compl�tement � l'int�rieur, on m�morisera un rectangle
                 * qui couvrira tous le zoom.
                 */
                result = new Polygon(clip, polygon.getCoordinateSystem());
                result.close(polygonType);
            }
        }
        return result;
    }
}
