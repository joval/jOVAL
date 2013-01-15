// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import cpe.schemas.dictionary.CheckType;
import cpe.schemas.dictionary.ItemType;
import cpe.schemas.dictionary.ListType;
import xccdf.schemas.core.ProfileType;

import xccdf.schemas.core.RuleType;

import org.joval.intf.oval.IDefinitionFilter;

/**
 * A view of a datastream resulting from the selection of a stream, benchmark and profile.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IView {
    /**
     * Get the XCCDF benchmark ID for the view.
     */
    String getBenchmark();

    /**
     * Get the XCCDF profile ID for the view.
     */
    String getProfile();

    /**
     * Get the containing Datastream.
     */
    IDatastream getStream();

    /**
     * Return a list of all the applicable CPE platform IDs for this view.
     */
    Collection<String> getCpePlatforms();

    /**
     * Get the Map of OVAL definition component HREFs and corresponding OVAL definition ID filters for a given CPE ID.
     *
     * @throws NoSuchElementException if there is no CPE dictionary entry with the specified ID.
     */
    Map<String, IDefinitionFilter> getCpeOval(String cpeId) throws NoSuchElementException;

    /**
     * Return a Map of all the values selected and defined in this view.
     */
    Map<String, String> getValues();

    /**
     * Return a list of all the rules selected by this Profile.
     */
    Collection<RuleType> getSelectedRules();
}
