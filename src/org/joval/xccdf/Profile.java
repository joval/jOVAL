// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf;

import java.util.Collection;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import xccdf.schemas.core.GroupType;
import xccdf.schemas.core.ProfileType;
import xccdf.schemas.core.ProfileRefineValueType;
import xccdf.schemas.core.ProfileSetValueType;
import xccdf.schemas.core.ProfileSelectType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.SelectableItemType;
import xccdf.schemas.core.SelStringType;
import xccdf.schemas.core.URIidrefType;
import xccdf.schemas.core.ValueType;

import org.joval.xccdf.XccdfBundle;

/**
 * Convenience class for an XCCDF Profile, which is like a view on an XCCDF Benchmark.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Profile {
    private XccdfBundle xccdf;
    private String name;
    private HashSet<RuleType> rules;
    private HashSet<String> platforms;
    private Hashtable<String, String> values = null;

    public Profile(XccdfBundle xccdf, String name) throws NoSuchElementException {
	this.xccdf = xccdf;

	values = new Hashtable<String, String>();
	rules = new HashSet<RuleType>();
	platforms = new HashSet<String>();
	for (URIidrefType platform : xccdf.getBenchmark().getPlatform()) {
	    platforms.add(xccdf.getDictionary().getOvalDefinitionId(platform.getIdref()));
	}

	Hashtable<String, Boolean> selections = null;
	Hashtable<String, String> refinements = null;
	if (name != null) {
	    this.name = name;
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
		for (URIidrefType platform : prof.getPlatform()) {
		    platforms.add(xccdf.getDictionary().getOvalDefinitionId(platform.getIdref()));
		}

		selections = new Hashtable<String, Boolean>();
		refinements = new Hashtable<String, String>();
		for (Object obj : prof.getSelectOrSetValueOrRefineValue()) {
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
		    }
		}
	    }
	}

	HashSet<ValueType> vals = new HashSet<ValueType>();
	vals.addAll(xccdf.getBenchmark().getValue());
	for (SelectableItemType item : getSelected(xccdf.getBenchmark().getGroupOrRule(), selections)) {
	    if (item instanceof GroupType) {
		vals.addAll(((GroupType)item).getValue());
	    } else if (item instanceof RuleType) {
		rules.add((RuleType)item);
	    }
	}
	for (ValueType val : vals) {
	    for (SelStringType sel : val.getValue()) {
		if (values.containsKey(val.getItemId())) {
		    // already set ... DAS throw an exception?
		} else if (refinements == null || refinements.get(val.getItemId()) == null) {
		    if (!sel.isSetSelector()) {
			values.put(val.getItemId(), sel.getValue());
		    }
		} else if (refinements.get(val.getItemId()).equals(sel.getSelector())) {
		    values.put(val.getItemId(), sel.getValue());
		}
	    }
	}
    }

    public Collection<String> getPlatformDefinitionIds() {
	return platforms;
    }

    public Hashtable<String, String> getValues() {
	return values;
    }

    public Collection<RuleType> getSelectedRules() {
	return rules;
    }

    // Private

    private Collection<SelectableItemType> getSelected(List<SelectableItemType> items, Hashtable<String, Boolean> selections) {
	Collection<SelectableItemType> results = new HashSet<SelectableItemType>();
	for (SelectableItemType item : items) {
	    if (selections == null || selections.get(item.getItemId()) == null) {
		if (item.isSelected()) {
		    results.add(item);
		    if (item instanceof GroupType) {
			results.addAll(getSelected(((GroupType)item).getGroupOrRule(), selections));
		    }
		}
	    } else if (selections.get(item.getItemId()).booleanValue()) {
		results.add(item);
		if (item instanceof GroupType) {
		    results.addAll(getSelected(((GroupType)item).getGroupOrRule(), selections));
		}
	    }
	}
	return results;
    }
}
