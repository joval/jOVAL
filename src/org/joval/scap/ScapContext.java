// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.openscap.sce.xccdf.ScriptDataType;
import scap.cpe.dictionary.CheckType;
import scap.cpe.dictionary.ItemType;
import scap.cpe.dictionary.ListType;
import scap.cpe.language.CheckFactRefType;
import scap.cpe.language.LogicalTestType;
import scap.cpe.language.OperatorEnumeration;
import scap.cpe.language.PlatformType;
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
import scap.xccdf.XccdfBenchmark;

import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.cpe.IDictionary;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.cpe.CpeException;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.oval.OvalException;
import org.joval.scap.sce.SceException;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.JOVALMsg;

/**
 * Abstract base class for IScapContext implementations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class ScapContext implements IScapContext {
    private static final scap.cpe.language.ObjectFactory CPE = new scap.cpe.language.ObjectFactory();
    private static final scap.xccdf.ObjectFactory XCCDF = new scap.xccdf.ObjectFactory();

    private ProfileType profile;
    private IBenchmark benchmark;
    private IDictionary dictionary;
    private ITailoring tailoring;
    private XccdfBenchmark xb;
    private Map<String, RuleType> ruleMap;
    private List<RuleType> rules;
    private Map<String, LogicalTestType> platforms;
    private Map<String, Collection<String>> values = null;

    /**
     * Create an IScapContext from a benchmark and profile.
     *
     * @param profile The selected profile (which might originate from an external tailoring) if null, then
     *                defaults are selected
     */
    protected ScapContext(IBenchmark benchmark, IDictionary dictionary, ITailoring tailoring, String profileId)
		throws XccdfException {

	if (benchmark == null) {
	    throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_BENCHMARK));
	}
	xb = benchmark.getRootObject();
	this.benchmark = benchmark;
	this.dictionary = dictionary;
	if (profileId != null) {
	    if (tailoring == null) {
		profile = resolve(benchmark.getProfile(profileId));
	    } else {
		this.tailoring = tailoring;
		profile = resolve(tailoring.getProfile(profileId));
	    }
	}

	platforms = new HashMap<String, LogicalTestType>();
	// Add Benchmark-wide platforms
	for (CPE2IdrefType cpe : xb.getPlatform()) {
	    try {
		addPlatform(cpe.getIdref());
	    } catch (NoSuchElementException e) {
		throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_CPE_PLATFORM, e.getMessage()));
	    }
	}

	values = new HashMap<String, Collection<String>>();
	ruleMap = new HashMap<String, RuleType>();
	rules = new ArrayList<RuleType>();

	//
	// If a named profile is specified, then gather all the selections, values, refinements, etc. associated with it.
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
		    ProfileRefineRuleType refine = (ProfileRefineRuleType)obj;
		    refinements.put(refine.getIdref(), refine);
		}
	    }
	}

	//
	// Discover all the selected rules and values, and default platforms from groups.
	//
	Map<String, Collection<String>> platformDefaults = new HashMap<String, Collection<String>>();
	HashSet<ValueType> vals = new HashSet<ValueType>();
	visit(selections, platformDefaults, vals);

	//
	// Tailor the selected rules according to profile refinements
	//
	for (RuleType rule : rules) {
	    if (rule.getPlatform().size() == 0 && platformDefaults.containsKey(rule.getId())) {
		//
		// Borrow platforms from the nearest platform-defining container
		//
		for (String idref : platformDefaults.get(rule.getId())) {
		    OverrideableCPE2IdrefType cpe = XCCDF.createOverrideableCPE2IdrefType();
		    cpe.setIdref(idref);
		    rule.getPlatform().add(cpe);
		}
	    }
	    if (refinements.containsKey(rule.getId())) {
		ProfileRefineRuleType refinement = refinements.get(rule.getId());

		if (refinement.isSetWeight() && !rule.getProhibitChanges()) {
		    rule.setWeight(refinement.getWeight());
		}

		//
		// Remove checks that are not selected.
		//
		if (refinement.isSetSelector()) {
		    Iterator<scap.xccdf.CheckType> iter = rule.getCheck().iterator();
		    while(iter.hasNext()) {
			if (!refinement.getSelector().equals(iter.next().getSelector())) {
			    iter.remove();
			}
		    }
		}
	    }
	}

	//
	// Set all the selected values
	//
	for (ValueType val : vals) {
	    String valId = val.getId();
	    if (values.containsKey(val.getId()) && valueSelectors.containsKey(valId)) {
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

    // Implement IScapContext

    public IDictionary getDictionary() {
	return dictionary;
    }

    public IBenchmark getBenchmark() {
	return benchmark;
    }

    public ITailoring getTailoring() {
	return tailoring;
    }

    public ProfileType getProfile() {
	return profile;
    }

    public Map<String, LogicalTestType> getCpeTests() {
	return platforms;
    }

    public Map<String, Collection<String>> getValues() {
	return values;
    }

    public List<RuleType> getSelectedRules() {
	return rules;
    }

    public RuleType getRule(String ruleId) throws NoSuchElementException {
	if (ruleMap.containsKey(ruleId)) {
	    return ruleMap.get(ruleId);
	} else {
	    throw new NoSuchElementException(ruleId);
	}
    }

    // Private

    /**
     * Visit all the nodes in the benchmark, and populate the platforms map with default platforms, and the selected vals set.
     */
    private void visit(Map<String, Boolean> sel, Map<String, Collection<String>> platforms, HashSet<ValueType> vals) {
	Collection<String> benchmarkPlatforms = new ArrayList<String>();
	for (CPE2IdrefType cpe : xb.getPlatform()) {
	    benchmarkPlatforms.add(cpe.getIdref());
	}

	vals.addAll(xb.getValue());
	for (SelectableItemType item : getSelected(xb.getGroupOrRule(), sel, benchmarkPlatforms, platforms)) {
	    String id = null;
	    if (item instanceof GroupType) {
		for (ValueType base : ((GroupType)item).getValue()) {
		    ValueType value = resolve(base);
		    if (!value.getAbstract()) {
			vals.add(value);
		    }
		}
	    } else if (item instanceof RuleType) {
		RuleType rule = resolve((RuleType)item);
		if (!rule.getAbstract() && !ruleMap.containsKey(rule.getId())) {
		    ruleMap.put(rule.getId(), rule);
		    rules.add(rule);
		}
	    }
	}
    }

    /**
     * Recursively find all the selected items, using selections gathered from a Profile (or null for defaults).
     * Items are returned in document order.
     */
    private List<SelectableItemType> getSelected(List<SelectableItemType> items, Map<String, Boolean> selections,
		Collection<String> parentPlatforms, Map<String, Collection<String>> platforms) {

	List<SelectableItemType> results = new ArrayList<SelectableItemType>();
	for (SelectableItemType item : items) {
	    String cId = item.isSetClusterId() ? item.getClusterId() : null;
	    String id = null;
	    if (item instanceof GroupType) {
		id = ((GroupType)item).getId();
	    } else if (item instanceof RuleType) {
		id = ((RuleType)item).getId();
	    }

	    boolean selected =	(selections.containsKey(id) && selections.get(id).booleanValue()) ||
				(!selections.containsKey(id) && item.getSelected()) ||
				(cId != null && selections.containsKey(cId) && selections.get(cId).booleanValue());

	    if (selected) {
		results.add(item);
		if (item.getPlatform().size() == 0) {
		    platforms.put(id, parentPlatforms);
		} else {
		    Collection<String> p = new ArrayList<String>();
		    for (OverrideableCPE2IdrefType cpe : item.getPlatform()) {
			p.add(cpe.getIdref());
			addPlatform(cpe.getIdref());
		    }
		    platforms.put(id, p);
		}
		if (item instanceof GroupType) {
		    results.addAll(getSelected(((GroupType)item).getGroupOrRule(), selections, platforms.get(id), platforms));
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
	    if (cpeName.startsWith("cpe:")) {
		if (dictionary == null) {
		    throw new NoSuchElementException(cpeName);
		}
		ItemType cpeItem = dictionary.getItem(cpeName);
		if (cpeItem.isSetCheck()) {
		    for (CheckType check : cpeItem.getCheck()) {
			if (SystemEnumeration.OVAL.namespace().equals(check.getSystem()) && check.isSetHref()) {
			    LogicalTestType ltt = CPE.createLogicalTestType();
			    ltt.setNegate(false);
			    ltt.setOperator(OperatorEnumeration.AND);
			    CheckFactRefType cfrt = CPE.createCheckFactRefType();
			    cfrt.setSystem(check.getSystem());
			    cfrt.setHref(check.getHref());
			    cfrt.setIdRef(check.getValue());
			    ltt.getCheckFactRef().add(cfrt);
			    platforms.put(cpeName, ltt);
			    break;
			}
		    }
		}
	    } else if (cpeName.startsWith("#")) {
		String name = cpeName.substring(1);
		for (PlatformType pt : xb.getPlatformSpecification().getPlatform()) {
		    if (name.equals(pt.getId())) {
			platforms.put(cpeName, pt.getLogicalTest());
			break;
		    }
		}
	    }

	    if (!platforms.containsKey(cpeName)) {
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
	ProfileType profile = XCCDF.createProfileType();
	profile.getTitle().addAll(base.getTitle());

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
     * Return a RuleType with all inherited properties incorporated therein.
     */
    private RuleType resolve(RuleType base) {
	String ruleId = base.getId();
	RuleType rule = XCCDF.createRuleType();

	//
	// Inheritance
	//
	if (base.isSetExtends()) {
	    RuleType parent = resolve((RuleType)benchmark.getItem(base.getExtends()));
	    if (parent.isSetProhibitChanges()) {
		rule.setProhibitChanges(parent.getProhibitChanges());
	    }
	    rule.getCheck().addAll(parent.getCheck());
	    rule.setComplexCheck(parent.getComplexCheck());
	    rule.setWeight(parent.getWeight());
	    rule.setRole(parent.getRole());
	    rule.getPlatform().addAll(parent.getPlatform());
	}

	//
	// Model: None
	//
	rule.setId(ruleId);
	rule.setAbstract(base.getAbstract());

	//
	// Model: Append
	//
	if (base.isSetFix()) {
	    rule.getFix().addAll(base.getFix());
	}

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
	if (base.isSetRole()) {
	    rule.setRole(base.getRole());
	}
	if (base.isSetProhibitChanges()) {
	    rule.setProhibitChanges(base.getProhibitChanges());
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

	return rule;
    }

    /**
     * Return a ValueType with all inherited properties and elements incorporated therein.
     */
    private ValueType resolve(ValueType base) {
	ValueType value = XCCDF.createValueType();

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
