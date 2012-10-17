// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import cpe.schemas.dictionary.CheckType;
import cpe.schemas.dictionary.ItemType;
import cpe.schemas.dictionary.ListType;

import xccdf.schemas.core.CPE2IdrefType;
import xccdf.schemas.core.GroupType;
import xccdf.schemas.core.OverrideableCPE2IdrefType;
import xccdf.schemas.core.ProfileType;
import xccdf.schemas.core.ProfileRefineRuleType;
import xccdf.schemas.core.ProfileRefineValueType;
import xccdf.schemas.core.ProfileSetComplexValueType;
import xccdf.schemas.core.ProfileSetValueType;
import xccdf.schemas.core.ProfileSelectType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.SelectableItemType;
import xccdf.schemas.core.SelComplexValueType;
import xccdf.schemas.core.SelStringType;
import xccdf.schemas.core.ValueType;

import org.joval.intf.oval.IDefinitions;
import org.joval.scap.cpe.CpeException;
import org.joval.scap.cpe.Dictionary;
import org.joval.scap.oval.OvalException;
import org.joval.scap.xccdf.Benchmark;
import org.joval.scap.xccdf.handler.OVALHandler;

/**
 * Convenience class for an XCCDF Profile, which is like a view on an XCCDF Benchmark.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Profile {
    private Benchmark xccdf;
    private String name;
    private HashSet<RuleType> rules;
    private Hashtable<String, List<String>> platforms;
    private List<String> cpePlatforms;
    private Hashtable<String, String> values = null;

    /**
     * Create an XCCDF profile. If name == null, then defaults are selected. If there is no profile with the given name,
     * a NoSuchElementException is thrown.
     */
    public Profile(Benchmark xccdf, String name) throws NoSuchElementException {
	this.xccdf = xccdf;
	this.name = name;
	platforms = new Hashtable<String, List<String>>();
	cpePlatforms = new Vector<String>();
	values = new Hashtable<String, String>();
	rules = new HashSet<RuleType>();

	//
	// Set Benchmark-wide platforms
	//
	for (CPE2IdrefType platform : xccdf.getBenchmark().getPlatform()) {
	    addPlatform(platform.getIdref());
	}

	//
	// If a named profile is specified, then gather all the selections and values associated with it.
	//
	Hashtable<String, Boolean> selections = null;
	Hashtable<String, String> refinements = null;
	if (name != null) {
	    ProfileType prof = null;
	    for (ProfileType pt : xccdf.getBenchmark().getProfile()) {
		if (name.equals(pt.getProfileId())) {
		    prof = pt;
		    break;
		}
	    }
	    if (prof == null) {
		throw new NoSuchElementException(name);
	    } else {
		for (OverrideableCPE2IdrefType platform : prof.getPlatform()) {
		    addPlatform(platform.getIdref());
		}

		selections = new Hashtable<String, Boolean>();
		refinements = new Hashtable<String, String>();
		for (Object obj : prof.getSelectOrSetComplexValueOrSetValue()) {
		    if (obj instanceof ProfileSelectType) {
			ProfileSelectType select = (ProfileSelectType)obj;
			if (select.isSelected()) {
			    selections.put(select.getIdref(), Boolean.TRUE);
			} else {
			    selections.put(select.getIdref(), Boolean.FALSE);
			}
		    } else if (obj instanceof ProfileSetValueType) {
			ProfileSetValueType set = (ProfileSetValueType)obj;
			values.put(set.getIdref(), set.getValue());
		    } else if (obj instanceof ProfileRefineValueType) {
			ProfileRefineValueType refine = (ProfileRefineValueType)obj;
			refinements.put(refine.getIdref(), refine.getSelector());
		    } else if (obj instanceof ProfileRefineRuleType) {
			ProfileRefineRuleType rule = (ProfileRefineRuleType)obj;
			//TBD
		    } else if (obj instanceof ProfileSetComplexValueType) {
			ProfileSetComplexValueType complex = (ProfileSetComplexValueType)obj;
			//TBD
		    }
		}
	    }
	}

	//
	// Discover all the selected rules and values
	//
	HashSet<ValueType> vals = new HashSet<ValueType>();
	vals.addAll(xccdf.getBenchmark().getValue());
	for (SelectableItemType item : getSelected(xccdf.getBenchmark().getGroupOrRule(), selections)) {
	    if (item instanceof GroupType) {
		vals.addAll(((GroupType)item).getValue());
	    } else if (item instanceof RuleType) {
		rules.add((RuleType)item);
	    }
	}

	//
	// Set all the selected values
	//
	for (ValueType val : vals) {
	    for (Object obj : val.getValueOrComplexValue()) {
		if (obj instanceof SelStringType) {
		    SelStringType sel = (SelStringType)obj;
		    if (values.containsKey(val.getId())) {
			// already set ... DAS throw an exception?
		    } else if (refinements == null || refinements.get(val.getId()) == null) {
			if (!sel.isSetSelector()) {
			    values.put(val.getId(), sel.getValue());
			}
		    } else if (refinements.get(val.getId()).equals(sel.getSelector())) {
			values.put(val.getId(), sel.getValue());
		    }
		} else if (obj instanceof SelComplexValueType) {
		    // TBD
		}
	    }
	}
    }

    /**
     * Returns the profile Idref.
     */
    public String getName() {
	return name;
    }

    /**
     * Return the hrefs to all the checks relevant to the profile.
     */
    public Collection<String> getPlatformDefinitionHrefs() {
	return platforms.keySet();
    }

    public IDefinitions getDefinitions(String href) throws NoSuchElementException, OvalException {
	return xccdf.getDefinitions(href);
    }

    /**
     * Return a list of all the CPE platform IDs for this profile.
     */
    public List<String> getCpePlatforms() {
	return cpePlatforms;
    }

    /**
     * Return all of the OVAL definition IDs associated with the specified href.
     */
    public List<String> getPlatformDefinitionIds(String href) {
	return platforms.get(href);
    }

    /**
     * Return a Hashtable of all the values selected/defined by this Profile.
     */
    public Hashtable<String, String> getValues() {
	return values;
    }

    /**
     * Return a list of all the rules selected by this Profile.
     */
    public Collection<RuleType> getSelectedRules() {
	return rules;
    }

    // Private

    /**
     * Recursively find all the selected items, using selections gathered from a Profile (or null for defaults).
     */
    private Collection<SelectableItemType> getSelected(List<SelectableItemType> items, Hashtable<String, Boolean> selections) {
	Collection<SelectableItemType> results = new HashSet<SelectableItemType>();
	for (SelectableItemType item : items) {
	    String id = null;
	    if (item instanceof GroupType) {
		id = ((GroupType)item).getId();
	    } else if (item instanceof RuleType) {
		id = ((RuleType)item).getId();
	    } else {
		throw new RuntimeException("Not a group or rule: " + item.getClass().getName());
	    }
	    if (selections == null || selections.get(id) == null) {
		if (item.getSelected()) {
		    results.add(item);
		    if (item instanceof GroupType) {
			results.addAll(getSelected(((GroupType)item).getGroupOrRule(), selections));
		    }
		}
	    } else if (selections.get(id).booleanValue()) {
		results.add(item);
		if (item instanceof GroupType) {
		    results.addAll(getSelected(((GroupType)item).getGroupOrRule(), selections));
		}
	    }
	}
	return results;
    }

    /**
     * Given a CPE platform name, add the corresponding OVAL definition IDs to the platforms list.
     */
    private void addPlatform(String cpeName) throws IllegalStateException, NoSuchElementException {
	cpePlatforms.add(cpeName);
	Dictionary dictionary = xccdf.getDictionary();
	boolean found = false;
	if (dictionary != null) {
	    ItemType cpeItem = xccdf.getDictionary().getItem(cpeName);
	    if (cpeItem != null && cpeItem.isSetCheck()) {
		for (CheckType check : cpeItem.getCheck()) {
		    if (OVALHandler.NAMESPACE.equals(check.getSystem()) && check.isSetHref()) {
			String href = check.getHref();
			if (!platforms.containsKey(href)) {
			    platforms.put(href, new Vector<String>());
			}
			platforms.get(href).add(check.getValue());
			found = true;
		    }
		}
	    }
	}
	if (!found) {
	    throw new NoSuchElementException(cpeName);
	}
    }
}
