package org.geotools.jdbc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.geotools.factory.GeoTools;

public class JDBCJNDITestSetup extends JDBCDelegatingTestSetup {

    public JDBCJNDITestSetup(JDBCTestSetup delegate) {
        super(delegate);
    }

    protected void setupJNDIEnvironment() throws IOException {
        
        File jndi = new File( "target/jndi");
        jndi.mkdirs();
        
        BufferedReader in = new BufferedReader( 
            new InputStreamReader( delegate.getClass().getResourceAsStream( "ds.properties")));
        OutputStreamWriter out = new OutputStreamWriter( new FileOutputStream( new File( jndi, "ds.properties")));
        
        String l = null;
        while( ( l = in.readLine()) != null ) {
            out.write( l ); out.write( "\n");
        }
        
        out.flush();
        out.close();
        in.close();
        
        String IC_FACTORY_PROPERTY = "java.naming.factory.initial";
        String JNDI_ROOT = "org.osjava.sj.root";
        String JNDI_DELIM = "org.osjava.jndi.delimiter";

        if (System.getProperty(IC_FACTORY_PROPERTY) == null) {
            System.setProperty(IC_FACTORY_PROPERTY, "org.osjava.sj.SimpleContextFactory");
        }

        if (System.getProperty(JNDI_ROOT) == null) {
            System.setProperty(JNDI_ROOT, jndi.getAbsolutePath());
        }
        
        if (System.getProperty(JNDI_DELIM) == null)
            System.setProperty(JNDI_DELIM, "/");
        
        LOGGER.fine( IC_FACTORY_PROPERTY + " = " + System.getProperty(IC_FACTORY_PROPERTY) );
        LOGGER.fine( JNDI_ROOT + " = " + System.getProperty(JNDI_ROOT) );
        LOGGER.fine( JNDI_DELIM + " = " + System.getProperty(JNDI_DELIM) );
    }

    @Override
    protected DataSource createDataSource() throws IOException {
        setupJNDIEnvironment();
        
        DataSource ds = null;
        try {
            Context ctx = GeoTools.getInitialContext(GeoTools.getDefaultHints());
            ds = (DataSource) ctx.lookup("ds");
        } 
        catch (NamingException e) {
            e.printStackTrace();
        }
        
        return ds;
    }

}