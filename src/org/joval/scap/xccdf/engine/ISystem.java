// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.util.Collection;
import java.util.Map;

import scap.xccdf.CheckType;
import scap.xccdf.ResultEnumType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.xml.ITransformable;

/**
 * XCCDF helper interface.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISystem {
    /**
     * Get the namespace processed by this ISystem.
     */
    String getNamespace();

    /**
     * Add a check.
     */
    void add(CheckType check) throws Exception;

    /**
     * Execute the added checks using the specified plugin.
     *
     * @return an enumeration of reports
     */
    Collection<ITransformable> exec(IPlugin plugin) throws Exception;

    /**
     * Get the result for a rule.
     *
     * @return a ResultEnumType for a regular check, or a Collection<RuleResultType> if a multi-check.
     */
    Object getResult(CheckType check) throws Exception;
}
