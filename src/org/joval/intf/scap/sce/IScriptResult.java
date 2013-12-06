// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.sce;

import javax.xml.bind.JAXBElement;

import org.openscap.sce.results.SceResultsType;

import org.joval.intf.xml.ITransformable;

/**
 * A representation of a result for an SCE script run.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IScriptResult extends ITransformable<JAXBElement<SceResultsType>> {
    /**
     * Returns true if there was an error preventing the script from running.
     */
    boolean hasError();

    /**
     * Returns the error that prevented the script from running, or null if there was none.
     */
    Throwable getError();
}
