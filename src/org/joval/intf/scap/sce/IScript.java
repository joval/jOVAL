// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.sce;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

import jsaf.intf.system.IComputerSystem;

import org.openscap.sce.xccdf.LangEnumeration;

/**
 * A representation of an SCE script.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IScript {
    int XCCDF_RESULT_PASS		= 101;
    int XCCDF_RESULT_FAIL		= 102;
    int XCCDF_RESULT_ERROR		= 103;
    int XCCDF_RESULT_UNKNOWN		= 104;
    int XCCDF_RESULT_NOT_APPLICABLE	= 105;
    int XCCDF_RESULT_NOT_CHECKED	= 106;
    int XCCDF_RESULT_NOT_SELECTED	= 107;
    int XCCDF_RESULT_INFORMATIONAL	= 108;
    int XCCDF_RESULT_FIXED		= 109;

    String ENV_VALUE_PREFIX	= "XCCDF_VALUE_";
    String ENV_TYPE_PREFIX	= "XCCDF_TYPE_";
    String ENV_OPERATOR_PREFIX	= "XCCDF_OPERATOR_";

    /**
     * Get a stream to the underlying script data.
     */
    InputStream getContent() throws IOException;

    /**
     * Get the href of the script component source (if any).
     */
    String getHref();

    /**
     * Get the script language.
     */
    LangEnumeration getLanguage();

    /**
     * Execute the script and return the result.
     *
     * @param exports a map of variable-value pairs for the script runtime environment.
     * @param session the jSAF session on which to execute the script.
     */
    IScriptResult exec(Map<String, String> exports, IComputerSystem session) throws Exception;
}
