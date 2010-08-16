/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2010, Open Source Geospatial Foundation (OSGeo)
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
 */

package org.geotools.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * A class to perform XML schema validation against schemas found using an {@link AppSchemaResolver}
 * .
 * 
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class AppSchemaValidator {

    /**
     * The resolver used to find XML schemas.
     */
    final private AppSchemaResolver resolver;

    /**
     * Failures found during parsing of an XML instance document.
     */
    final private List<String> failures = new ArrayList<String>();

    /**
     * Are validation warnings considered failures? The default is true.
     */
    private boolean failOnWarning = true;

    /**
     * Construct an {@link AppSchemaValidator} that performs schema validation against schemas found
     * on the classpath using the convention described in
     * {@link AppSchemaResolver#getSimpleHttpResourcePath(java.net.URI)}.
     */
    private AppSchemaValidator() {
        this(new AppSchemaResolver());
    }

    /**
     * Construct an {@link AppSchemaValidator} that performs schema validation against schemas found
     * using an {@link AppSchemaResolver}.
     * 
     * @param resolver
     *            resolver used to locate XML schemas
     */
    private AppSchemaValidator(AppSchemaResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Return the list of failures found during parsing.
     */
    public List<String> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    /**
     * Are validation warnings considered failures?
     */
    public boolean isFailOnWarning() {
        return failOnWarning;
    }

    /**
     * Should validation warnings be considered failures?
     */
    public void setFailOnWarning(boolean failOnWarning) {
        this.failOnWarning = failOnWarning;
    }

    /**
     * Configure the parser.
     * 
     * @param parser
     *            the XML parser to configure
     */
    private void configure(XMLReader parser) {
        try {
            // See: http://xerces.apache.org/xerces2-j/features.html
            // (1) Enable validation in the parser.
            parser.setFeature("http://xml.org/sax/features/validation", true);
            // (2) Specify that validation is against XML schemas.
            parser.setFeature("http://apache.org/xml/features/validation/schema", true);
            // (2a) Enable the most pedantic level of checking of the schemas themselves.
            // This has no effect on checking of instance documents, so we leave it turned off.
            // It is left here for the education of the reader.
            // parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking",
            // true);
            // (3) Set the entity resolver, which is used to look up XML schemas.
            // See: http://xerces.apache.org/xerces2-j/faq-xcatalogs.html
            parser.setProperty("http://apache.org/xml/properties/internal/entity-resolver",
                    new AppSchemaXMLEntityResolver());
        } catch (Exception e) {
            // Validation failures do not throw. This block is entered if there is, for example, an
            // I/O error occurs or a schema cannot be located during parsing.
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse an XML instance document read from an {@link InputStream}, recording any validation
     * failures failures.
     * 
     * @param input
     *            stream from which XML instance document is read
     */
    public void parse(InputStream input) {
        XMLReader parser = new SAXParser();
        configure(parser);
        // We principally care about the failures themselves, but it is also possible to install a
        // ContentHandler to output annotated XML that identifies the precise location of failures.
        // That can be done with a serializer that implements both ContentHandler and ErrorHandler.
        // It should be installed here (and used for the error handler):
        // parser.setContentHandler(contentHandler);
        parser.setErrorHandler(new AppSchemaValidatorErrorHandler());
        try {
            parser.parse(new InputSource(input));
        } catch (RuntimeException e) {
            // Avoid gratuitous exception chaining.
            // Resolver failures pass through this block.
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Throw a {@link RuntimeException} if the validator has found any failures. The exception
     * detail contains the failure messages.
     */
    public void check() {
        if (failures.size() > 0) {
            throw new RuntimeException(buildFailureMessage());
        }
    }

    /**
     * Build an exception detail message that contains all the validation failure messages.
     */
    private String buildFailureMessage() {
        String newline = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder();
        builder.append("Schema validation failures: " + failures.size());
        for (String failure : failures) {
            builder.append(newline);
            builder.append(failure);
        }
        return builder.toString();
    }

    /**
     * Construct an {@link AppSchemaValidator} that performs schema validation against schemas found
     * on the classpath using the convention described in
     * {@link AppSchemaResolver#getSimpleHttpResourcePath(java.net.URI)}.
     */
    public static AppSchemaValidator buildValidator() {
        return new AppSchemaValidator();
    }

    /**
     * Construct an {@link AppSchemaValidator} that performs schema validation against schemas found
     * using an {@link AppSchemaResolver}.
     * 
     * @param resolver
     *            the resolver used to find schemas
     */
    public static AppSchemaValidator buildValidator(AppSchemaResolver resolver) {
        return new AppSchemaValidator(resolver);
    }

    /**
     * 
     * Perform schema validation of an XML instance document read from a classpath resource against
     * schemas found on the classpath using the convention described in
     * {@link AppSchemaResolver#getSimpleHttpResourcePath(java.net.URI)}.
     * 
     * <p>
     * 
     * If validation fails, a {@link RuntimeException} is thrown containing details of all failures.
     * 
     * @param name
     *            resource name of XML instance document
     */
    public static void validate(String name) {
        InputStream input = null;
        try {
            input = AppSchemaValidator.class.getResourceAsStream(name);
            validate(input);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // we tried
                }
            }
        }
    }

    /**
     * 
     * Perform schema validation of an XML instance document read from an input stream against
     * schemas found on the classpath using the convention described in
     * {@link AppSchemaResolver#getSimpleHttpResourcePath(java.net.URI)}.
     * 
     * <p>
     * 
     * If validation fails, a {@link RuntimeException} is thrown containing details of all failures.
     * 
     * @param input
     *            stream providing XML instance document
     */
    public static void validate(InputStream input) {
        AppSchemaValidator validator = buildValidator();
        validator.parse(input);
        validator.check();
    }

    /**
     * An {@link XMLEntityResolver} that uses the enclosing instance's {@link AppSchemaResolver} to
     * look up XML entities (that is, XML schemas).
     * 
     */
    private class AppSchemaXMLEntityResolver implements XMLEntityResolver {

        /**
         * @see org.apache.xerces.xni.parser.XMLEntityResolver#resolveEntity(org.apache.xerces.xni.XMLResourceIdentifier)
         */
        public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier)
                throws XNIException, IOException {
            return new XMLInputSource(resourceIdentifier.getPublicId(), resolver
                    .resolve(resourceIdentifier.getExpandedSystemId()), resourceIdentifier
                    .getBaseSystemId());
        }

    }

    /**
     * An {@link ErrorHandler} that appends validation failure messages to the failure list in the
     * enclosing instance.
     */
    private class AppSchemaValidatorErrorHandler implements ErrorHandler {

        /**
         * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
         */
        public void error(SAXParseException exception) throws SAXException {
            failures.add("ERROR: " + exception.getMessage());
        }

        /**
         * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
         */
        public void fatalError(SAXParseException exception) throws SAXException {
            failures.add("FATAL ERROR: " + exception.getMessage());
        }

        /**
         * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
         */
        public void warning(SAXParseException exception) throws SAXException {
            if (failOnWarning) {
                failures.add("WARNING: " + exception.getMessage());
            }
        }

    }

}