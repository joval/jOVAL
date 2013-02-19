// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.datastream;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import scap.cpe.language.LogicalTestType;
import scap.xccdf.ProfileType;
import scap.xccdf.RuleType;

import org.joval.intf.scap.xccdf.IBenchmark;

/**
 * A view of a datastream resulting from the selection of a stream, benchmark and profile.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IView {
    /**
     * Get the benchmark document that was selected for the view.
     */
    IBenchmark getBenchmark();

    /**
     * Get the fully-resolved XCCDF profile that was selected for the view.
     */
    ProfileType getProfile();

    /**
     * Get the parent Datastream document.
     */
    IDatastream getStream();

    /**
     * Get a map of CPE idrefs relevant for the view, and their corresponding CPE LogicalTestTypes.
     */
    Map<String, LogicalTestType> getCpeTests();

    /**
     * Return a Map of all the fully-resolved values selected and defined in this view.
     */
    Map<String, Collection<String>> getValues();

    /**
     * Return a Map of all the fully-resolved rules selected by this Profile.
     */
    Map<String, RuleType> getSelectedRules();
}
