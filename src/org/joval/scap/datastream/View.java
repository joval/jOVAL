// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.datastream;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import scap.xccdf.ObjectFactory;
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
import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.oval.DefinitionFilter;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.JOVALMsg;

/**
 * Implementation of an IView.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class View implements IView {
    private static final ObjectFactory FACTORY = new ObjectFactory();

    private String benchmarkId;
    private ProfileType profile;
    private Datastream stream;
    private IBenchmark benchmark;
    private BenchmarkType bt;
    private HashSet<RuleType> rules;
    private Map<String, Map<String, Collection<String>>> platforms;
    private Map<String, Collection<String>> values = null;

    /**
     * Create an XCCDF profile view. If profileId == null, then defaults are selected. If there is no profile with the given
     * name, a NoSuchElementException is thrown.
     */
    View(Datastream stream, IBenchmark benchmark, ProfileType profile) throws XccdfException {
	this.stream = stream;
	this.benchmark = benchmark;
	bt = benchmark.getBenchmark();
	this.profile = resolve(profile);

	platforms = new HashMap<String, Map<String, Collection<String>>>();
	// Add Benchmark-wide platforms
	for (CPE2IdrefType cpe : bt.getPlatform()) {
	    addPlatform(cpe.getIdref());
	}

	values = new HashMap<String, Collection<String>>();
	rules = new HashSet<RuleType>();

	//
	// If a named profile is specified, then gather all the selections and values associated with it.
	//
	Map<String, Boolean> selections = new HashMap<String, Boolean>();
	Map<String, String> valueSelectors = new HashMap<String, String>();
	Map<String, ProfileRefineRuleType> refinements = new HashMap<String, ProfileRefineRuleType>();
	if (profile != null) {
	    for (OverrideableCPE2IdrefType cpe : profile.getPlatform()) {
		// Add profile platforms
		addPlatform(cpe.getIdref());
	    }
	    for (Object obj : profile.getSelectOrSetComplexValueOrSetValue()) {
		if (obj instanceof ProfileSelectType) {
		    ProfileSelectType select = (ProfileSelectType)obj;
		    if (select.isSelected()) {
			selections.put(select.getIdref(), Boolean.TRUE);
		    } else {
			selections.put(select.getIdref(), Boolean.FALSE);
		    }
		} else if (obj instanceof ProfileSetValueType) {
		    ProfileSetValueType set = (ProfileSetValueType)obj;
		    values.put(set.getIdref(), Arrays.asList(set.getValue()));
		} else if (obj instanceof ProfileSetComplexValueType) {
		    ProfileSetComplexValueType complex = (ProfileSetComplexValueType)obj;
		    values.put(complex.getIdref(), complex.getItem());
		} else if (obj instanceof ProfileRefineValueType) {
		    ProfileRefineValueType refine = (ProfileRefineValueType)obj;
		    valueSelectors.put(refine.getIdref(), refine.getSelector());
		} else if (obj instanceof ProfileRefineRuleType) {
		    ProfileRefineRuleType rule = (ProfileRefineRuleType)obj;
		    refinements.put(rule.getIdref(), rule);
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
		for (ValueType base : ((GroupType)item).getValue()) {
		    ValueType value = resolve(base);
		    if (!value.getAbstract()) {
			vals.add(value);
		    }
		}
	    } else if (item instanceof RuleType) {
		RuleType rule = resolve((RuleType)item, refinements);
		if (!rule.getAbstract()) {
		    rules.add(rule);
		}
	    }
	}

	//
	// Set all the selected values
	//
	for (ValueType val : vals) {
	    if (values.containsKey(val.getId())) {
		StringBuffer sb = new StringBuffer();
		for (String s : values.get(val.getId())) {
		    if (sb.length() > 0) {
			sb.append(", ");
		    }
		    sb.append(s);
		}
		String selector = valueSelectors.get(val.getId());
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_VALUE, val.getId(), selector, sb.toString());
		throw new XccdfException(msg);
	    } else {
		values.put(val.getId(), getValue(val, valueSelectors.get(val.getId())));
	    }
	}
    }

    // Implement IView

    public IBenchmark getBenchmark() {
	return benchmark;
    }

    public ProfileType getProfile() {
	return profile;
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

    public Map<String, Collection<String>> getValues() {
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
	    if (!selections.containsKey(id)) {
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
     * Given a CPE platform name, add the corresponding OVAL definition IDs to the platforms map (idempotent).
     *
     * @throws NoSuchElementException if no OVAL definitions corresponding to the CPE name were found in the stream.
     */
    private void addPlatform(String cpeName) throws NoSuchElementException {
	if (!platforms.containsKey(cpeName)) {
	    Map<String, Collection<String>> ovalMap = new HashMap<String, Collection<String>>();
	    IDictionary dictionary = stream.getDictionary();
	    boolean found = false;
	    if (dictionary == null) {
		throw new NoSuchElementException("CPE Dictionary");
	    }
	    ItemType cpeItem = dictionary.getItem(cpeName);
	    if (cpeItem.isSetCheck()) {
		for (CheckType check : cpeItem.getCheck()) {
		    if (SystemEnumeration.OVAL.namespace().equals(check.getSystem()) && check.isSetHref()) {
			String href = check.getHref();
			if (!ovalMap.containsKey(href)) {
			    ovalMap.put(href, new ArrayList<String>());
			}
			ovalMap.get(href).add(check.getValue());
		    }
		}
	    }
	    if (ovalMap.size() > 0) {
		platforms.put(cpeName, ovalMap);
	    } else {
		throw new NoSuchElementException(cpeName);
	    }
	}
    }

    /**
     * Returns the value(s) corresponding to the specified selector.
     *
     * If selector is null, returns the default value. The default value is the value with no selector, or if there is no
     * value without a selector, it is the first value that appears.
     *
     * @throws NoSuchElementException if the selector is not null, and is not found in the ValueType.
     */
    private List<String> getValue(ValueType val, String selector) throws NoSuchElementException {
	for (Object obj : val.getValueOrComplexValue()) {
	    if (obj instanceof SelStringType) {
		SelStringType sel = (SelStringType)obj;
		if (selector != null) {
		    if (selector.equals(sel.getSelector())) {
			return Arrays.asList(sel.getValue());
		    }
		} else if (!sel.isSetSelector()) {
		    return Arrays.asList(sel.getValue());
		}
	    } else if (obj instanceof SelComplexValueType) {
		SelComplexValueType sel = (SelComplexValueType)obj;
		if (selector != null) {
		    if (selector.equals(sel.getSelector())) {
			return sel.getItem();
		    }
		} else if (!sel.isSetSelector()) {
		    return sel.getItem();
		}
	    }
	}
	if (selector != null) {
	    throw new NoSuchElementException(selector);
	} else if (val.getValueOrComplexValue().size() > 0) {
	    Object obj = val.getValueOrComplexValue().get(0);
	    if (obj instanceof SelStringType) {
		SelStringType sel = (SelStringType)obj;
		return Arrays.asList(sel.getValue());
	    } else if (obj instanceof SelComplexValueType) {
		SelComplexValueType sel = (SelComplexValueType)obj;
		return sel.getItem();
	    }
	}
	@SuppressWarnings("unchecked")
	List<String> empty = (List<String>)Collections.EMPTY_LIST;
	return empty;
    }

    /**
     * Return a ProfileType with all inherited properties and elements incorporated therein.
     */
    private ProfileType resolve(ProfileType base) {
	ProfileType profile = FACTORY.createProfileType();

	//
	// Inheritance
	//
	if (base.getExtends() != null) {
	    ProfileType parent = resolve(benchmark.getProfile(base.getExtends()));
	    profile.getPlatform().addAll(parent.getPlatform());
	    profile.getSelectOrSetComplexValueOrSetValue().addAll(parent.getSelectOrSetComplexValueOrSetValue());
	}

	//
	// Model: None
	//
	profile.setProfileId(base.getProfileId());
	profile.setAbstract(base.getAbstract());

	//
	// Model: Append
	//
	profile.getPlatform().addAll(base.getPlatform());
	profile.getSelectOrSetComplexValueOrSetValue().addAll(base.getSelectOrSetComplexValueOrSetValue());

	return profile;
    }

    /**
     * Return a RuleType with all inherited properties and refinements incorporated therein.
     */
    private RuleType resolve(RuleType base, Map<String, ProfileRefineRuleType> refinements) {
	String ruleId = base.getId();
	RuleType rule = FACTORY.createRuleType();

	//
	// Inheritance
	//
	if (base.isSetExtends()) {
	    RuleType parent = resolve((RuleType)benchmark.getItem(base.getExtends()), refinements);
	    if (parent.isSetProhibitChanges()) {
		rule.setProhibitChanges(parent.getProhibitChanges());
	    }
	    rule.getCheck().addAll(parent.getCheck());
	    rule.setComplexCheck(parent.getComplexCheck());
	    rule.setWeight(parent.getWeight());
	    rule.getPlatform().addAll(parent.getPlatform());
	}

	//
	// Model: None
	//
	rule.setId(ruleId);
	rule.setAbstract(base.getAbstract());

	//
	// Model: Replace
	//
	if (base.isSetCheck()) {
	    rule.unsetCheck();
	    rule.getCheck().addAll(base.getCheck());
	}
	if (base.isSetComplexCheck()) {
	    rule.setComplexCheck(base.getComplexCheck());
	}
	if (base.isSetWeight()) {
	    rule.setWeight(base.getWeight());
	}

	//
	// Model: Override
	//
	if (base.isSetPlatform() && base.getPlatform().size() > 0) {
	    if (base.getPlatform().get(0).getOverride()) {
		rule.unsetPlatform();
	    }
	    for (OverrideableCPE2IdrefType cpe : base.getPlatform()) {
		rule.getPlatform().add(cpe);
		addPlatform(cpe.getIdref());
	    }
	}

	if (!(base.isSetProhibitChanges() && base.getProhibitChanges()) &&
	    !(rule.isSetProhibitChanges() && rule.getProhibitChanges()) &&
	    refinements.containsKey(ruleId)) {

	    ProfileRefineRuleType refinement = refinements.get(ruleId);
	    if (refinement.isSetWeight()) {
		rule.setWeight(refinement.getWeight());
	    }
	}

	return rule;
    }

    /**
     * Return a ValueType with all inherited properties and elements incorporated therein.
     */
    private ValueType resolve(ValueType base) {
	ValueType value = FACTORY.createValueType();

	//
	// Inheritance
	//
	if (base.isSetExtends()) {
	    ValueType parent = resolve((ValueType)benchmark.getItem(base.getExtends()));
	    if (parent.isSetProhibitChanges()) {
		value.setProhibitChanges(parent.getProhibitChanges());
	    }
	    value.getValueOrComplexValue().addAll(parent.getValueOrComplexValue());
	}

	//
	// Model: None
	//
	value.setId(base.getId());
	value.setAbstract(base.getAbstract());

	//
	// Model: Append
	//
	value.getValueOrComplexValue().addAll(base.getValueOrComplexValue());

	return value;
    }
}
