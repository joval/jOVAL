// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.sce;

import java.util.Map;

import jsaf.intf.system.ISession;

import org.openscap.sce.results.SceResultsType;
import org.openscap.sce.xccdf.LangEnumeration;
import org.openscap.sce.xccdf.ScriptDataType;
import scap.datastream.ExtendedComponent;

import org.joval.intf.xml.ITransformable;

/**
 * A representation of a single SCE script.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IScript extends ITransformable {
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
     * Get the underlying script data.
     */
    byte[] getData();

    /**
     * Get the href of the script component source (if any).
     */
    String getHref();

    /**
     * Get the script language.
     */
    LangEnumeration getLanguage();

    /**
     * Execute the script and return the result. The (last) result will also be furnished as the JAXB root element for the
     * ITransformable.
     *
     * @param exports a map of variable-value pairs for the script runtime environment.
     * @param session the jSAF session on which to execute the script.
     */
    SceResultsType exec(Map<String, String> exports, ISession session) throws Exception;
}
