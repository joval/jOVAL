// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.datastream;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import scap.cpe.dictionary.CheckType;
import scap.cpe.dictionary.ItemType;
import scap.cpe.dictionary.ListType;
import scap.xccdf.BenchmarkType;
import scap.xccdf.CPE2IdrefType;
import scap.xccdf.GroupType;
import scap.xccdf.OverrideableCPE2IdrefType;
import scap.xccdf.ProfileType;
import scap.xccdf.ProfileRefineRuleType;
import scap.xccdf.ProfileRefineValueType;
import scap.xccdf.ProfileSetComplexValueType;
import scap.xccdf.ProfileSetValueType;
import scap.xccdf.ProfileSelectType;
import scap.xccdf.RuleType;
import scap.xccdf.SelectableItemType;
import scap.xccdf.SelComplexValueType;
import scap.xccdf.SelStringType;
import scap.xccdf.ValueType;

import org.joval.intf.scap.cpe.IDictionary;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.datastream.IView;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.oval.DefinitionFilter;

/**
 * Implementation of an IView.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class View implements IView {
    private String benchmarkId, profileId;
    private Datastream stream;
    private BenchmarkType bt;
    private HashSet<RuleType> rules;
    private Map<String, Map<String, Collection<String>>> platforms;
    private Map<String, String> values = null;

    /**
     * Create an XCCDF profile view. If profileId == null, then defaults are selected. If there is no profile with the given
     * name, a NoSuchElementException is thrown.
     */
    View(String benchmarkId, String profileId, Datastream stream, BenchmarkType bt) throws NoSuchElementException {
	this.benchmarkId = benchmarkId;
	this.profileId = profileId;
	this.stream = stream;
	this.bt = bt;
	platforms = new HashMap<String, Map<String, Collection<String>>>();
	values = new HashMap<String, String>();
	rules = new HashSet<RuleType>();

	//
	// Set Benchmark-wide platforms
	//
	for (CPE2IdrefType platform : bt.getPlatform()) {
	    addPlatform(platform.getIdref());
	}

	//
	// If a named profile is specified, then gather all the selections and values associated with it.
	//
	Map<String, Boolean> selections = null;
	Map<String, String> refinements = null;
	if (profileId != null) {
	    ProfileType prof = null;
	    for (ProfileType pt : bt.getProfile()) {
		if (profileId.equals(pt.getProfileId())) {
		    prof = pt;
		    break;
		}
	    }
	    if (prof == null) {
		throw new NoSuchElementException(profileId);
	    } else {
		for (OverrideableCPE2IdrefType platform : prof.getPlatform()) {
		    addPlatform(platform.getIdref());
		}

		selections = new HashMap<String, Boolean>();
		refinements = new HashMap<String, String>();
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
	vals.addAll(bt.getValue());
	for (SelectableItemType item : getSelected(bt.getGroupOrRule(), selections)) {
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
		    // DAS: TBD
		}
	    }
	}
    }

    // Implement IView

    public String getBenchmark() {
	return benchmarkId;
    }

    public String getProfile() {
	return profileId;
    }

    public IDatastream getStream() {
	return stream;
    }

    public Collection<String> getCpePlatforms() {
	return platforms.keySet();
    }

    public Map<String, IDefinitionFilter> getCpeOval(String cpeId) throws NoSuchElementException {
	if (platforms.containsKey(cpeId)) {
	    Map<String, IDefinitionFilter> result = new HashMap<String, IDefinitionFilter>();
	    for (Map.Entry<String, Collection<String>> entry : platforms.get(cpeId).entrySet()) {
		result.put(entry.getKey(), new DefinitionFilter(entry.getValue()));
	    }
	    return result;
	}
	throw new NoSuchElementException(cpeId);
    }

    public Map<String, String> getValues() {
	return values;
    }

    public Collection<RuleType> getSelectedRules() {
	return rules;
    }

    // Private

    /**
     * Recursively find all the selected items, using selections gathered from a Profile (or null for defaults).
     */
    private Collection<SelectableItemType> getSelected(List<SelectableItemType> items, Map<String, Boolean> selections) {
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
     *
     * @throws NoSuchElementException if no OVAL definitions corresponding to the CPE name were found in the stream.
     */
    private void addPlatform(String cpeName) throws NoSuchElementException {
	Map<String, Collection<String>> ovalMap = null;
	if (platforms.containsKey(cpeName)) {
	    ovalMap = platforms.get(cpeName);
	} else {
	    ovalMap = new HashMap<String, Collection<String>>();
	    platforms.put(cpeName, ovalMap);
	}
	IDictionary dictionary = stream.getDictionary();
	boolean found = false;
	if (dictionary != null) {
	    ItemType cpeItem = dictionary.getItem(cpeName);
	    if (cpeItem != null && cpeItem.isSetCheck()) {
		for (CheckType check : cpeItem.getCheck()) {
		    if (SystemEnumeration.OVAL.namespace().equals(check.getSystem()) && check.isSetHref()) {
			String href = check.getHref();
			if (!ovalMap.containsKey(href)) {
			    ovalMap.put(href, new ArrayList<String>());
			}
			ovalMap.get(href).add(check.getValue());
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
