/*
 * Geotools 2 - OpenSource mapping toolkit
 * (C) 2003, Geotools Project Management Committee (PMC)
 * (C) 2001, Institut de Recherche pour le D�veloppement
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
package org.geotools.renderer.geom;

// J2SE dependencies
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.io.PrintWriter;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// Geotools dependencies
import org.geotools.renderer.geom.Polygon;
import org.geotools.resources.Arguments;


/**
 * Visual check of {@link Polygon}.
 *
 * @version $Id: PolygonTest.java,v 1.3 2003/05/13 11:00:48 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public class PolygonTest extends TestCase {

    /**
     * Construct a test case.
     */
    public PolygonTest(String testName) {
        super(testName);
    }

    /**
     * Run the test suite from the command line.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Returns the test suite.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(PolygonTest.class);
        return suite;
    }

    /**
     * V�rifie le bon fonctionnement de {@link Polygon}. Cette m�thode dessinera
     * une carte imaginaire dans un cadre et placera des points un peu partout.
     * Les points jaunes sont sens�s �tre � l'int�rieur de l'�le et les points
     * gris � l'ext�rieur.
     * <br><br>
     * Argument: (optionel) R�solution (en degr�s) du cercle � tracer. La valeur
     *           par d�faut est 20. Une valeur plus �lev�e se traduira par une �le
     *           plus grossi�re, tandis qu'une valeur plus faible se traduira par
     *           une �le plus finement trac�e.
     */
    public void testPolygon() throws Exception {
        String[] args = new String[0];
        final Arguments arguments = new Arguments(args);
        final int firstPointCount = 4;
        final int  lastPointCount = 4;
        /*
         * Cr�� une carte imaginaire. La carte ressemblera � un cercle,
         * mais des pertubations al�atoires seront ajout�es de fa�on �
         * rendre l'�le un peu plus irr�guli�re. L'�le cr��e se trouvera
         * m�moris�e dans un objet <code>GeneralPath</code>, une classe
         * standard du Java qui servira de t�moin puisqu'elle est sens�e
         * bien fonctionner.
         */
        final PrintWriter  out = arguments.out;
        final GeneralPath path = new GeneralPath();
        final double    radius = 150;
        int angleStep=20;
        if (args.length > 1) {
            angleStep = Integer.parseInt(args[1]);
            if (angleStep == 0) {
                angleStep = 20;
            }
        }
        final double delta = (1-Math.cos(angleStep))*radius;
        for (int i=0; i<360; i+=Math.abs(angleStep)) {
            final double theta = Math.toRadians(i);
            double x=250 - radius*Math.cos(theta)+delta*(Math.random()-0.5);
            double y=250 - radius*Math.sin(theta)+delta*(Math.random()-0.5);
            if (i==0) path.moveTo((float) x, (float) y);
            else      path.lineTo((float) x, (float) y);
        }
        path.closePath();
        /*
         * Donne quelques informations sur l'�le. Les
         * principales m�thodes publiques seront test�es.
         */
        final Rectangle2D       clip = new Rectangle2D.Double(100, 100, 200, 200);
        final Polygon        polygon = Polygon.getInstances(path, null)[0];
        final Polygon        clipped = polygon.clip(new Clipper(clip, null));
        final Collection    pointSet = polygon.getPoints();
        final Point2D[]        first = new Point2D.Float[firstPointCount];
        final Point2D[]         last = new Point2D.Float[ lastPointCount];
        final Point2D[]        extrm = new Point2D.Float[pointSet.size()];
        final Collection subPointSet = new ArrayList();
        for (int i=0; i<first.length; i++) {
            subPointSet.add(null);
        }
        subPointSet.addAll(polygon.subpoly(first.length, extrm.length-last.length).getPoints());
        polygon.getFirstPoints(first);
        polygon.getLastPoints (last);
        System.arraycopy(first, 0, extrm, 0,                       first.length);
        System.arraycopy(last,  0, extrm, extrm.length-last.length, last.length);

        out.println(polygon.toString());
        out.println();
        Polygon.print(new String[]     {"Polygon", "First/Last", "SubPoly", "Clipped"},
                      new Collection[] {pointSet, Arrays.asList(extrm), subPointSet, clipped.getPoints()},
                      out, arguments.locale);
        /*
         * Fait appara�tre le polyligne dans une fen�tre. Cette fen�tre
         * offira quelques menus qui permettront � l'utilisateur de
         * v�rifier si des points sont � l'int�rieur ou a l'ext�rieur
         * du polyligne.
         */
        out.println();
        out.print("Compression: ");
        out.print(100*polygon.compress(0));
        out.println('%');
        ShapePanel.show(path   ).setTitle("Uncompressed GeneralPath");
        ShapePanel.show(polygon).setTitle("Compressed Polygon");
        ShapePanel.show(clipped).setTitle("Clipped Polygon (uncompressed)");
        out.flush();
    }
}
