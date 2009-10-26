/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.wcs11.validation;

import net.opengis.ows11.CodeType;

import org.eclipse.emf.common.util.EList;

/**
 * A sample validator interface for {@link net.opengis.wcs11.FieldSubsetType}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface FieldSubsetTypeValidator {
    boolean validate();

    boolean validateIdentifier(CodeType value);
    boolean validateInterpolationType(String value);
    boolean validateAxisSubset(EList value);
}