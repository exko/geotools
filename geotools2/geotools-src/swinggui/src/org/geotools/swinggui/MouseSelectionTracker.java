/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2002, Center for Computational Geography
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
 *     UNITED KINDOM: James Macgill
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
package org.geotools.swinggui;

// Geometry
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Graphics
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;

// Events
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;


/**
 * Contr�leur permettant � l'utilisateur de s�lectionner une r�gion sur une
 * composante. L'utilisateur doit cliquer sur un point de la composante, puis
 * faire glisser le curseur de la souris en tenant le bouton enfonc�. Pendant
 * le glissement, la forme qui sera dessin�e sera g�n�ralement un rectangle.
 * D'autres formes pourraient toutefois �tre utilis�es, comme par exemple une
 * ellipse. Pour utiliser cette classe, il faut cr�er une classe d�riv�e qui
 * d�finisse les m�thodes suivantes:
 *
 * <ul>
 *   <li>{@link #selectionPerformed} (obligatoire)</li>
 *   <li>{@link #getModel} (facultatif)</li>
 * </ul>
 *
 * Ce contr�leur doit ensuite �tre enregistr� aupr�s d'une et une
 * seule composante en utilisant la syntaxe suivante:
 *
 * <blockquote><pre>
 * {@link Component} component=...
 * MouseSelectionTracker control=...
 * component.addMouseListener(control);
 * </pre></blockquote>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class MouseSelectionTracker extends MouseInputAdapter
{
    /**
     * Rectangle pointill� repr�sentant la r�gion en train de se faire
     * s�lectionner par l'utilisateur. Ce rectangle peut �tre vide. Ces
     * coordonn�es ne prendront une signification qu'entre les moments
     * o� l'utilisateur a appuy� sur le bouton de la souris et le moment
     * o� il l'a rel�ch� pour d�limiter une r�gion. Par convention, la
     * valeur <code>null</code> indique qu'une ligne doit �tre utilis�e
     * au lieu d'une forme rectangulaire. Les coordonn�es sont toujours
     * exprim�es en pixels.
     */
    private transient RectangularShape mouseSelectedArea;

    /**
     * Couleur � remplacer lors des tra�age XOR sur un graphique.
     * Cette couleur est sp�cifi�e � {@link Graphics2D#setColor}.
     */
    private Color backXORColor=Color.white;

    /**
     * Couleur remplacante lors des tra�age XOR sur un graphique.
     * Cette couleur est sp�cifi�e � {@link Graphics2D#setXORMode}.
     */
    private Color lineXORColor=Color.black;

    /**
     * Coordonn�e <var>x</var> de la souris
     * lorsque le bouton a �t� enfonc�.
     */
    private transient int ox;

    /**
     * Coordonn�e <var>y</var> de la souris
     * lorsque le bouton a �t� enfonc�.
     */
    private transient int oy;

    /**
     * Coordonn�e <var>x</var> de la souris
     * lors du dernier glissement.
     */
    private transient int px;

    /**
     * Coordonn�e <var>y</var> de la souris
     * lors du dernier glissement.
     */
    private transient int py;

    /**
     * Indique si une s�lection est en cours.
     */
    private transient boolean isDragging;

    /**
     * Construit un objet qui permettra de s�lectionner
     * des r�gions rectangulaires � l'aide de la souris.
     */
    public MouseSelectionTracker() {
    }

    /**
     * Sp�cifie les couleurs � utiliser pour tracer le contour d'une bo�te
     * lorsque l'utilisateur s�lectionne une r�gion. Toutes les couleurs
     * <code>a</code> seront remplac�es par les couleurs <code>b</code> et
     * vis-versa.
     */
    public void setXORColors(final Color a, final Color b) {
        backXORColor=a;
        lineXORColor=b;
    }

    /**
     * Retourne la forme g�om�trique � utiliser pour d�limiter une r�gion.
     * Cette forme est g�n�ralement un rectangle mais pourrait aussi �tre
     * une ellipse, une fl�che ou d'autres formes encore. Les coordonn�es
     * de la forme retourn�e ne seront pas prises en compte. En fait, ces
     * coordonn�es seront r�guli�rement �cras�es.  Seule compte la classe
     * de la forme retourn�e (par exemple {@link java.awt.geom.Ellipse2D}
     * vs {@link java.awt.geom.Rectangle2D}) et ses param�tres non-reli�s
     * � sa position (par exemple l'arrondissement des coins d'un rectangle).
     *
     * La forme retourn�e sera g�n�ralement d'une classe d�riv�e de
     * {@link RectangularShape}, mais peut aussi �tre de la classe
     * {@link Line2D}. <strong>Tout autre classe risque de lancer une
     * {@link ClassCastException} au moment de l'ex�cution</strong>.
     *
     * L'impl�mentation par d�faut retourne toujours un objet {@link Rectangle}.
     *
     * @param  event Coordonn�e de la souris au moment ou le bouton a �t�
     *         enfonc�. Cette information peut �tre utilis�e par les classes
     *         d�riv�es qui voudraient tenir compte de la position de la souris
     *         avant de choisir une forme g�om�trique.
     * @return Forme de la classe {link RectangularShape} ou {link Line2D}, ou
     *         <code>null</code> pour indiquer qu'on ne veut pas faire de
     *         s�lection.
     */
    protected Shape getModel(final MouseEvent event) {
        return new Rectangle();
    }

    /**
     * M�thode appel�e automatiquement apr�s que l'utilisateur
     * ait s�lectionn�e une r�gion � l'aide de la souris. Toutes
     * les coordonn�es pass�es en param�tres sont exprim�es en
     * pixels.
     *
     * @param ox Coordonn�e <var>x</var> de la souris lorsque
     *        l'utilisateur a enfonc� le bouton de la souris.
     * @param oy Coordonn�e <var>y</var> de la souris lorsque
     *        l'utilisateur a enfonc� le bouton de la souris.
     * @param px Coordonn�e <var>x</var> de la souris lorsque
     *        l'utilisateur a relach� le bouton de la souris.
     * @param py Coordonn�e <var>y</var> de la souris lorsque
     *        l'utilisateur a relach� le bouton de la souris.
     */
    protected abstract void selectionPerformed(int ox, int oy, int px, int py);

    /**
     * Retourne la forme g�om�trique entourant la derni�re r�gion s�lection�e
     * par l'utilisateur. Une transformation affine facultative peut �tre
     * sp�cifi�e pour convertir en coordonn�es logiques la r�gion s�lectionn�e
     * par l'utilisateur. La classe de la forme retourn�e d�pend du model que
     * retourne {@link #getModel}:
     *
     * <ul>
     *   <li>Si le mod�le est nul (ce qui signifie que cet objet
     *       <code>MouseSelectionTracker</code> ne fait que dessiner une ligne
     *       entre deux points), alors l'objet retourn� sera de la classe
     *       {@link Line2D}.</li>
     *   <li>Si le mod�le est non-nul, alors l'objet retourn� peut �tre de la
     *       m�me classe (le plus souvent {@link java.awt.geom.Rectangle2D}).
     *       Il peut toutefois y avoir des situations ou l'objet retourn� sera
     *       d'une autre classe, par exemple si la transformation affine
     *       <code>transform</code> effectue une rotation.</li>
     * </ul>
     *
     * @param  transform Transformation affine qui sert � convertir les
     *         coordonn�es logiques en coordonn�es pixels. Il s'agit
     *         g�n�ralement de la transformation affine qui est utilis�e dans
     *         une m�thode <code>paint(...)</code> pour dessiner des formes
     *         exprim�es en coordonn�es logiques.
     * @return Une forme g�om�trique  entourant la derni�re r�gion s�lection�e
     *         par l'utilisateur, ou <code>null</code> si aucune s�lection n'a
     *         encore �t� faite.
     * @throws NoninvertibleTransformException Si la transformation affine
     *         <code>transform</code> ne peut pas �tre invers�e.
     */
    public Shape getSelectedArea(final AffineTransform transform) throws NoninvertibleTransformException {
        if (ox==px && oy==py) return null;
        RectangularShape shape=mouseSelectedArea;
        if (transform!=null && !transform.isIdentity()) {
            if (shape==null) {
                final Point2D.Float po=new Point2D.Float(ox,oy);
                final Point2D.Float pp=new Point2D.Float(px,py);
                transform.inverseTransform(po,po);
                transform.inverseTransform(pp,pp);
                return new Line2D.Float(po,pp);
            } else {
                if (canReshape(shape, transform)) {
                    final Point2D.Double point=new Point2D.Double();
                    double xmin=Double.POSITIVE_INFINITY;
                    double ymin=Double.POSITIVE_INFINITY;
                    double xmax=Double.NEGATIVE_INFINITY;
                    double ymax=Double.NEGATIVE_INFINITY;
                    for (int i=0; i<4; i++) {
                        point.x = (i&1)==0 ? shape.getMinX() : shape.getMaxX();
                        point.y = (i&2)==0 ? shape.getMinY() : shape.getMaxY();
                        transform.inverseTransform(point, point);
                        if (point.x<xmin) xmin=point.x;
                        if (point.x>xmax) xmax=point.x;
                        if (point.y<ymin) ymin=point.y;
                        if (point.y>ymax) ymax=point.y;
                    }
                    if (shape instanceof Rectangle) {
                        return new Rectangle2D.Float((float) xmin,
                                                     (float) ymin,
                                                     (float) (xmax-xmin),
                                                     (float) (ymax-ymin));
                    } else {
                        shape = (RectangularShape) shape.clone();
                        shape.setFrame(xmin, ymin, xmax-xmin, ymax-ymin);
                        return shape;
                    }
                }
                else {
                    return transform.createInverse().createTransformedShape(shape);
                }
            }
        }
        else {
            return (shape!=null) ? (Shape) shape.clone() : new Line2D.Float(ox,oy,px,py);
        }
    }

    /**
     * Indique si on peut transformer la forme <code>shape</code> en appellant
     * simplement sa m�thode <code>shape.setFrame(...)</code> plut�t que
     * d'utiliser l'artillerie lourde qu'est la m�thode
     * <code>transform.createTransformedShape(shape)</code>.
     */
    private static boolean canReshape(final RectangularShape shape,
                                      final AffineTransform transform) {
        final int type=transform.getType();
        if ((type & AffineTransform.TYPE_GENERAL_TRANSFORM) != 0) return false;
        if ((type & AffineTransform.TYPE_MASK_ROTATION)     != 0) return false;
        if ((type & AffineTransform.TYPE_FLIP)              != 0) {
            if (shape instanceof Rectangle2D)      return true;
            if (shape instanceof Ellipse2D)        return true;
            if (shape instanceof RoundRectangle2D) return true;
            return false;
        }
        return true;
    }

    /**
     * Retourne un objet {@link Graphics2D} � utiliser pour dessiner dans
     * la composante sp�cifi�e. Il ne faudra pas oublier d'appeller {@link
     * Graphics2D#dispose} lorsque le graphique ne sera plus n�cessaire.
     */
    private Graphics2D getGraphics(final Component c) {
        final Graphics2D graphics=(Graphics2D) c.getGraphics();
        graphics.setXORMode(lineXORColor);
        graphics.setColor  (backXORColor);
        return graphics;
    }

    /**
     * Informe ce controleur que le bouton de la souris vient d'�tre enfonc�.
     * L'impl�mentation par d�faut retient la coordonn�e de la souris (qui
     * deviendra un des coins du futur rectangle � dessiner) et pr�pare
     * <code>this</code> � observer les d�placements de la souris.
     *
     * @throws ClassCastException si {@link #getModel} ne retourne pas une
     *         forme de la classe {link RectangularShape} ou {link Line2D}.
     */
    public void mousePressed(final MouseEvent event) throws ClassCastException {
        if (!event.isConsumed() && (event.getModifiers() & MouseEvent.BUTTON1_MASK)!=0) {
            final Component source=event.getComponent();
            if (source!=null) {
                Shape model=getModel(event);
                if (model!=null) {
                    isDragging=true;
                    ox=px=event.getX();
                    oy=py=event.getY();
                    if (model instanceof Line2D) model=null;
                    mouseSelectedArea=(RectangularShape) model;
                    if (mouseSelectedArea!=null) {
                        mouseSelectedArea.setFrame(ox, oy, 0, 0);
                    }
                    source.addMouseMotionListener(this);
                }
                source.requestFocus();
                event.consume();
            }
        }
    }

    /**
     * Informe ce controleur que la souris vient de glisser avec le bouton
     * enfonc�. L'impl�mentation par d�faut observe ce glissement pour d�placer
     * un coin du rectangle servant � s�lectionner une r�gion. L'autre coin
     * reste fix� � l'endroit o� �tait la souris au moment ou le bouton a �t�
     * enfonc�.
     */
    public void mouseDragged(final MouseEvent event) {
        if (isDragging) {
            final Graphics2D graphics=getGraphics(event.getComponent());
            if (mouseSelectedArea==null) {
                graphics.drawLine(ox, oy, px, py);
                px=event.getX();
                py=event.getY();
                graphics.drawLine(ox, oy, px, py);
            } else {
                graphics.draw(mouseSelectedArea);
                int xmin=this.ox;
                int ymin=this.oy;
                int xmax=px=event.getX();
                int ymax=py=event.getY();
                if (xmin>xmax) {
                    final int xtmp=xmin;
                    xmin=xmax;xmax=xtmp;
                }
                if (ymin>ymax) {
                    final int ytmp=ymin;
                    ymin=ymax;ymax=ytmp;
                }
                mouseSelectedArea.setFrame(xmin, ymin, xmax-xmin, ymax-ymin);
                graphics.draw(mouseSelectedArea);
            }
            graphics.dispose();
            event.consume();
        }
    }

    /**
     * Informe ce controleur que le bouton de la souris vient d'�tre rel�ch�.
     * L'impl�mentation par d�faut appelle {@link #selectionPerformed} avec
     * en param�tres les limites de la r�gion s�lectionn�e.
     */
    public void mouseReleased(final MouseEvent event) {
        if (isDragging && (event.getModifiers() & MouseEvent.BUTTON1_MASK)!=0) {
            isDragging=false;
            final Component component=event.getComponent();
            component.removeMouseMotionListener(this);

            final Graphics2D graphics=getGraphics(event.getComponent());
            if (mouseSelectedArea==null) {
                graphics.drawLine(ox, oy, px, py);
            } else {
                graphics.draw(mouseSelectedArea);
            }
            graphics.dispose();
            px = event.getX();
            py = event.getY();
            selectionPerformed(ox, oy, px, py);
            event.consume();
        }
    }

    /**
     * Informe ce controleur que la souris vient d'�tre d�plac� sans que
     * ce soit dans le contexte o� l'utilisateur s�lectionne une r�gion.
     * L'impl�mentation par d�faut signale � la composante source que
     * <code>this</code> n'est plus interess� � �tre inform� des
     * d�placements de la souris.
     */
    public void mouseMoved(final MouseEvent event) {
        // Normalement pas n�cessaire, mais il semble que ce
        // "listener" reste parfois en place alors qu'il n'aurait pas d�.
        event.getComponent().removeMouseMotionListener(this);
    }
}
