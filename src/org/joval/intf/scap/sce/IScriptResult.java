// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.sce;

import org.openscap.sce.results.SceResultsType;

import org.joval.intf.xml.ITransformable;

/**
 * A representation of a result for an SCE script run.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IScriptResult extends ITransformable {
    /**
     * Get the result of the script execution.
     */
    SceResultsType getResult();
}
