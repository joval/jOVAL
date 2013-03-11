// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import scap.cpe.language.LogicalTestType;
import scap.datastream.Component;
import scap.datastream.ExtendedComponent;
import scap.xccdf.ProfileType;
import scap.xccdf.RuleType;

import org.joval.intf.scap.cpe.IDictionary;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.sce.IScript;
import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.ScapException;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.oval.OvalException;
import org.joval.scap.sce.SceException;
import org.joval.scap.xccdf.XccdfException;

/**
 * Interface defining an XCCDF context, which is the combination of an XCCDF benchmark, a profile, and all the documents
 * required to execute them.  A context can be assembled using a datastream source or a bundle source.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IScapContext {
    /**
     * Get the CPE dictionary for the context.
     */
    IDictionary getDictionary();

    /**
     * Get the benchmark.
     */
    IBenchmark getBenchmark();

    /**
     * Get the fully-resolved XCCDF profile for the context.
     */
    ProfileType getProfile();

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

    /**
     * Get an OCIL checklist document with the specified component href.
     */
    IChecklist getOcil(String href) throws NoSuchElementException, OcilException;

    /**
     * Get an OVAL definitions document with the specified component href.
     */
    IDefinitions getOval(String href) throws NoSuchElementException, OvalException;

    /**
     * Get SCE script data with the specified component href.
     */
    IScript getSce(String href) throws NoSuchElementException, SceException;
}
