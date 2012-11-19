// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.sce;

import java.util.Map;

import org.openscap.sce.result.SceResultsType;
import org.openscap.sce.xccdf.ScriptDataType;

import org.joval.scap.oval.CollectException;

/**
 * The interface for implementing a jOVAL SCE provider.  A provider knows how to execute a script and return a result.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IProvider {
    /**
     * Execute the specified script on the target, and return the result.
     *
     * @param exports variable exports for the script
     * @param source the source datatype containing the script content
     *
     * @throws Exception if there was a problem obtaining a result
     */
    public SceResultsType exec(Map<String, String> exports, ScriptDataType source) throws Exception;
}
