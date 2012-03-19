// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.handler;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import xccdf.schemas.core.CheckContentRefType;
import xccdf.schemas.core.CheckType;
import xccdf.schemas.core.CheckExportType;
import xccdf.schemas.core.RuleType;

import org.joval.intf.system.ISession;
import org.joval.sce.SCEScript;
import org.joval.util.JOVALMsg;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;
import org.joval.xccdf.engine.XPERT;

/**
 * XCCDF helper class for SCE processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SCEHandler {
    public static final String NAMESPACE = "http://open-scap.org/page/SCE";

    private XccdfBundle xccdf;
    private Profile profile;
    private ISession session;
    private List<SCEScript> scripts;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile.
     */
    public SCEHandler(XccdfBundle xccdf, Profile profile, ISession session) {
	this.xccdf = xccdf;
	this.profile = profile;
	this.session = session;
    }

    /**
     * Create a list of SCE scripts that should be executed based on the profile.
     */
    public List<SCEScript> getScripts() {
	if (scripts == null) {
	    scripts = new Vector<SCEScript>();
	    Hashtable<String, String> values = profile.getValues();
	    Collection<RuleType> rules = profile.getSelectedRules();
	    for (RuleType rule : rules) {
		if (rule.isSetCheck()) {
		    for (CheckType check : rule.getCheck()) {
			if (check.isSetSystem() && check.getSystem().equals(NAMESPACE)) {
			    for (CheckContentRefType ref : check.getCheckContentRef()) {
				if (ref.isSetHref()) {
				    try {
					SCEScript sce = new SCEScript(rule.getItemId(), xccdf.getURL(ref.getHref()), session);
					for (CheckExportType export : check.getCheckExport()) {
					    String name = export.getExportName();
					    String valueId = export.getValueId();
					    sce.setenv(name, values.get(valueId));
					}
					scripts.add(sce);
				    } catch (MalformedURLException e) {
					xccdf.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	return scripts;
    }
}
