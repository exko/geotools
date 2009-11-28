package org.geotools.gce.imagemosaic;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * This class simply builds an SRTREE spatial index in memory for fast indexed
 * geometric queries.
 * 
 * <p>
 * Since the {@link ImageMosaicReader} heavily uses spatial queries to find out
 * which are the involved tiles during mosaic creation, it is better to do some
 * caching and keep the index in memory as much as possible, hence we came up
 * with this index.
 * 
 * @author Simone Giannecchini, S.A.S.
 * @author Stefan Alfons Krueger (alfonx), Wikisquare.de : Support for jar:file:foo.jar/bar.properties URLs
 * @since 2.5
 *
	 * @source $URL: http://svn.osgeo.org/geotools/trunk/modules/plugin/imagemosaic/src/main/java/org/geotools/gce/imagemosaic/RasterManager.java $
 */
class JTSTRTreeGranuleIndex implements GranuleIndex {
	
	/** Logger. */
	final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(JTSTRTreeGranuleIndex.class);

	private static class JTSIndexVisitorAdapter  implements ItemVisitor {

		private GranuleIndexVisitor adaptee;
		private Filter filter;

		/**
		 * @param indexLocation
		 */
		public JTSIndexVisitorAdapter(final GranuleIndexVisitor adaptee) {
			this(adaptee,(Query)null);
		}
		
		public JTSIndexVisitorAdapter(final GranuleIndexVisitor adaptee, Query q) {
			this.adaptee=adaptee;
			this.filter=q==null?DefaultQuery.ALL.getFilter():q.getFilter();
		}
		/**
		 * @param indexLocation
		 */
		public JTSIndexVisitorAdapter(final GranuleIndexVisitor adaptee, Filter filter) {
			this.adaptee=adaptee;
			this.filter=filter==null?DefaultQuery.ALL.getFilter():filter;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.vividsolutions.jts.index.ItemVisitor#visitItem(java.lang.Object)
		 */
		public void visitItem(Object o) {
			if(o instanceof SimpleFeature){
				final SimpleFeature f=(SimpleFeature) o;
				if(filter.evaluate(f));
				adaptee.visit(f,null);
				return;
			}
			throw new IllegalArgumentException("Unable to visit provided item"+o);

		}

	}

	private final URL indexLocation;

	private ShapefileDataStore tileIndexStore;

	private String typeName;

	private FileChannel channel;

	private FileLock lock;

	private ReferencedEnvelope bounds;

	public JTSTRTreeGranuleIndex(final URL indexLocation) {
		ImageMosaicUtils.ensureNonNull("indexLocation",indexLocation);
		this.indexLocation=indexLocation;
		
		
		try{
			// lock the underlying file
			if (indexLocation.getProtocol().equals("file")) {

				//  Get a file channel for the file
				File file = DataUtilities.urlToFile(indexLocation);
				if(file.canWrite()){
					channel = new RandomAccessFile(file, "rw").getChannel();

					// Create a shared lock on the file.
					// This method blocks until it can retrieve the lock.
					lock = channel.lock(0, Long.MAX_VALUE, true);
				}
			}				
			// creating a store
			tileIndexStore = new ShapefileDataStore(this.indexLocation);
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.fine("Connected mosaic reader to its data store "+ indexLocation.toString());
			final String[] typeNames = tileIndexStore.getTypeNames();
			if (typeNames.length <= 0)
				throw new IllegalArgumentException(
						"Problems when opening the index, no typenames for the schema are defined");
	
			// loading all the features into memory to build an in-memory index.
			typeName = typeNames[0];
			
			bounds=tileIndexStore.getFeatureSource().getBounds();
		}
		catch (Throwable e) {
			try {
				if(tileIndexStore!=null)
					tileIndexStore.dispose();
			} catch (Throwable e1) {
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, e1.getLocalizedMessage(), e1);
			}
			finally{
				tileIndexStore=null;
			}	
			
			try {
				if (lock != null)
					// Release the lock
					lock.release();
			} catch (Throwable e2) {
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, e2.getLocalizedMessage(), e2);
			} finally {
				lock = null;
			}

			try {
				if (channel != null)
					// Close the file
					channel.close();
			} catch (Throwable e3) {
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, e3.getLocalizedMessage(), e3);
			} finally {
				channel = null	;
			}
			
			throw new  IllegalArgumentException(e);
		}
		
	}
	
	/** The {@link STRtree} index. */
	private SoftReference<STRtree> index= new SoftReference<STRtree>(null);

	protected final ReadWriteLock rwLock= new ReentrantReadWriteLock(true);

	/**
	 * Constructs a {@link JTSTRTreeGranuleIndex} out of a {@link FeatureCollection}.
	 * 
	 * @param features
	 * @throws IOException
	 */
	private synchronized SpatialIndex getIndex() throws IOException {
		// check if the index has been cleared
		if(index==null)
			throw new IllegalStateException();
		
		// do your thing

		/**
		 * Comment by Stefan Krueger while patching the stuff to deal with
		 * URLs instead of Files: If it is not a URL to a file, we don't
		 * need locks, because no one can change to the index.
		 */

		STRtree tree = index.get();
		if (tree == null) {
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.fine("No index exits and we create a new one.");
			createIndex();
			tree = index.get();
		} else if (LOGGER.isLoggable(Level.FINE))
			LOGGER.fine("Index does not need to be created...");
		
		return tree;
		

	}

	/**
	 * This method shall only be called when the <code>indexLocation</code> is of protocol <code>file:</code>
	 */
	private void createIndex() {
		
		FeatureIterator<SimpleFeature> it=null;
		FeatureCollection<SimpleFeatureType, SimpleFeature> features=null;
		//
		// Load tiles informations, especially the bounds, which will be
		// reused
		//
		try{

			final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = tileIndexStore.getFeatureSource(typeName);
			if (featureSource == null) 
				throw new NullPointerException(
						"The provided FeatureSource<SimpleFeatureType, SimpleFeature> is null, it's impossible to create an index!");
			features = featureSource.getFeatures();
			if (features == null) 
				throw new NullPointerException(
						"The provided FeatureCollection<SimpleFeatureType, SimpleFeature> is null, it's impossible to create an index!");
	
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.fine("Index Loaded");
			
			//load the feature from the shapefile and create JTS index
			it = features.features();
			if (!it.hasNext()) 
				throw new IllegalArgumentException(
						"The provided FeatureCollection<SimpleFeatureType, SimpleFeature>  or empty, it's impossible to create an index!");
			
			// now build the index
			// TODO make it configurable as far the index is involved
			STRtree tree = new STRtree();
			while (it.hasNext()) {
				final SimpleFeature feature = it.next();
				final Geometry g = (Geometry) feature.getDefaultGeometry();
				tree.insert(g.getEnvelopeInternal(), feature);
			}
			
			// force index construction --> STRTrees are build on first call to
			// query
			tree.build();
			
			// save the soft reference
			index= new SoftReference<STRtree>(tree);
		}
		catch (Throwable e) {
			throw new  IllegalArgumentException(e);
		}
		finally{
	
			if(it!=null)
				// closing he iterator to free some resources.
				if(features!=null)
					features.close(it);

		}
		
	}

	/* (non-Javadoc)
	 * @see org.geotools.gce.imagemosaic.FeatureIndex#findFeatures(com.vividsolutions.jts.geom.Envelope)
	 */
	@SuppressWarnings("unchecked")
	public List<SimpleFeature> findGranules(final BoundingBox envelope) throws IOException {
		ImageMosaicUtils.ensureNonNull("envelope",envelope);
		final Lock lock=rwLock.readLock();
		try{
			lock.lock();
			return getIndex().query(ReferencedEnvelope.reference(envelope));
		}finally{
			lock.unlock();
		}			
	}
	
	/* (non-Javadoc)
	 * @see org.geotools.gce.imagemosaic.FeatureIndex#findFeatures(com.vividsolutions.jts.geom.Envelope, com.vividsolutions.jts.index.ItemVisitor)
	 */
	public void findGranules(final BoundingBox envelope, final GranuleIndexVisitor visitor) throws IOException {
		ImageMosaicUtils.ensureNonNull("envelope",envelope);
		ImageMosaicUtils.ensureNonNull("visitor",visitor);
		final Lock lock=rwLock.readLock();
		try{
			lock.lock();
			getIndex().query(ReferencedEnvelope.reference(envelope), new JTSIndexVisitorAdapter(visitor));
		}finally{
			lock.unlock();
		}				
		

	}

	public void dispose() throws IOException {
		final Lock l=rwLock.writeLock();
		try{
			l.lock();
			if(index!=null)
				index.clear();
	
	
			try {
				if(tileIndexStore!=null)
					tileIndexStore.dispose();
			} catch (Throwable e) {
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
			}
			finally{
				tileIndexStore=null;
			}	
				
			
			try {
				if (lock != null)
					// Release the lock
					lock.release();
			} catch (Throwable e2) {
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, e2.getLocalizedMessage(), e2);
			} finally {
				lock = null;
			}
	
			try {
				if (channel != null)
					// Close the file
					channel.close();
			} catch (Throwable e3) {
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, e3.getLocalizedMessage(), e3);
			} finally {
				channel = null	;
			}			
		}finally{
			
			index= null;
			l.unlock();
		
		}
		
		
	}

	public int removeGranules(final Query query) {
		throw new UnsupportedOperationException("removeGranules is not supported, this ia read only index");
//		ImageMosaicUtils.ensureNonNull("query",query);
//		final Lock lock=rwLock.writeLock();
//		try{
//			//
//			lock.lock();
//		}finally{
//			lock.unlock();
//		}
//	
//		return 0;
		
	}

	public void addGranule(final Granule granule) {
		throw new UnsupportedOperationException("addGranule is not supported, this ia read only index");
//		ImageMosaicUtils.ensureNonNull("granuleMetadata",granule);
//		final Lock lock=rwLock.writeLock();
//		try{
//			lock.lock();
//			// check if the index has been cleared
//			if(index==null)
//				throw new IllegalStateException();
//			
//			// do your thing
//		}finally{
//			lock.unlock();
//		}	
//		
	}


	@SuppressWarnings("unchecked")
	public List<SimpleFeature> findGranules(Query q) throws IOException {
		ImageMosaicUtils.ensureNonNull("q",q);
		final Lock lock=rwLock.readLock();
		try{
			lock.lock();
			
			// get filter and check bbox
			final Filter filter= q.getFilter();
			
			final List<SimpleFeature> features= getIndex().query(bounds);
			if(q.equals(DefaultQuery.ALL))
				return features;
			
			final List<SimpleFeature> retVal= new ArrayList<SimpleFeature>();
			for (Iterator<SimpleFeature> it = features.iterator();it.hasNext();)
			{
				SimpleFeature f= it.next();
				if(filter.evaluate(f))
					retVal.add(f);
			}
			return retVal;
		}finally{
			lock.unlock();
		}	
	}

	public List<SimpleFeature> findGranules() {
		throw new UnsupportedOperationException("findGranules is not supported"); 
	}

	public void findGranules(Query q, GranuleIndexVisitor visitor)
			throws IOException {
		ImageMosaicUtils.ensureNonNull("q",q);
		final Lock lock=rwLock.readLock();
		try{
			lock.lock();
			
			// get filter and check bbox
			getIndex().query(bounds,new JTSIndexVisitorAdapter(visitor,q));
			
		}finally{
			lock.unlock();
		}	
	}

}
