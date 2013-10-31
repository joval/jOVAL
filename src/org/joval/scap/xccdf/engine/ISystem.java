// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import scap.xccdf.CheckType;
import scap.xccdf.InstanceResultType;
import scap.xccdf.MessageType;
import scap.xccdf.ResultEnumType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ScapException;

/**
 * XCCDF helper interface, which describes a "handler" that encapsulates the knowledge required to implement checks for
 * a particular XCCDF check system. Examples of check systems are OVAL, OCIL and SCE.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
interface ISystem {
    /**
     * Get the namespace processed by this ISystem.
     */
    String getNamespace();

    /**
     * Add a check, which should be processed when the exec method is called.
     */
    void add(CheckType check) throws Exception;

    /**
     * Execute the added checks using the specified plugin.
     *
     * @return reports, indexed by source document URI
     */
    Map<String, ? extends ITransformable> exec(IPlugin plugin) throws Exception;

    /**
     * Get the result for a rule.
     *
     * @param multi overrides CheckType.getMultiCheck().
     */
    IResult getResult(CheckType check, boolean multi) throws ScapException, IllegalArgumentException;

    /**
     * Interface definition for getting check result information. Since a check can be a regular single check, or a
     * muli-check, this interface defines two sub-types.
     */
    interface IResult {
	enum Type {
	    SINGLE, MULTI;
	}

	Type getType();

	Collection<MessageType> getMessages();

	/**
	 * For Type.SINGLE.
	 */
	ResultEnumType getResult() throws NoSuchElementException;

	/**
	 * For Type.SINGLE.
	 */
	CheckType getCheck() throws NoSuchElementException;

	/**
	 * For the children of Type.MULTI.
	 */
	InstanceResultType getInstance() throws NoSuchElementException;

	/**
	 * For Type.MULTI (for multi-check) - returns a list of Type.SINGLE-type IResult instances.
	 */
	Collection<IResult> getResults() throws NoSuchElementException;
    }
}
