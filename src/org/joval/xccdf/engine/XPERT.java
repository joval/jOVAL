// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

import cpe.schemas.dictionary.ListType;
import oval.schemas.common.GeneratorType;
import oval.schemas.definitions.core.OvalDefinitions;
import xccdf.schemas.core.Benchmark;
import xccdf.schemas.core.GroupType;
import xccdf.schemas.core.CheckType;
import xccdf.schemas.core.CheckExportType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.SelStringType;
import xccdf.schemas.core.SelectableItemType;
import xccdf.schemas.core.ValueType;

import org.joval.cpe.CpeException;
import org.joval.intf.oval.IDefinitions;
import org.joval.oval.OvalException;
import org.joval.oval.Variables;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPERT implements Runnable {
    static final GeneratorType getGenerator() {
	GeneratorType generator = JOVALSystem.factories.common.createGeneratorType();
	generator.setProductName("jOVAL XPERT");
	generator.setProductVersion(JOVALSystem.getSystemProperty("Alpha"));
	generator.setSchemaVersion("5.10.1");
	try {
	    generator.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
	} catch (DatatypeConfigurationException e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_TIMESTAMP);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return generator;
    }

    /**
     * Run from the command-line.
     */
    public static void main(String[] argv) {
	File f = null;
	if (argv.length == 1) {
	    //
	    // Use the specified directory
	    //
	    f = new File(argv[0]);
	}

	if (f == null) {
	    System.out.println("Usage: java org.joval.xccdf.XPERT [path]");
	    System.exit(1);
	}

	try {
	    XPERT engine = new XPERT(new XccdfBundle(f));
	    engine.run();
	} catch (CpeException e) {
	    e.printStackTrace();
	    System.exit(1);
	} catch (OvalException e) {
	    e.printStackTrace();
	    System.exit(1);
	} catch (XccdfException e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	System.exit(0);
    }

    private XccdfBundle xccdf;

    public XPERT(XccdfBundle xccdf) {
	this.xccdf = xccdf;
	System.out.println("Loaded document");
    }

    // Implement Runnable

    public void run() {

//Apparent order of things...
// Benchmark specifies what Rules/Groups are selected
// Groups contain rules, rules contain checks, checks export values...
//   values are specified at the root level; selected group/rule export values become OVAL external variables
//   checks -- pick the one whose system matches the OVAL definitions schema.  It will reference a definition.


	createVariables().writeXML(new File("variables.xml"));

    }

    // Private

    private Variables createVariables() {
	Variables variables = new Variables(getGenerator());

	//
	// Iterate through Value nodes and get the values without selectors.
	//
	Hashtable<String, String> settings = new Hashtable<String, String>();
	for (ValueType val : xccdf.getBenchmark().getValue()) {
	    for (SelStringType sel : val.getValue()) {
		if (!sel.isSetSelector()) {
		    settings.put(val.getItemId(), sel.getValue());
		}
	    }
	}

	//
	// Get every export from every rule, and add exported values to the OVAL variables.
	//
	for (SelectableItemType item : xccdf.getBenchmark().getGroupOrRule()) {
	    for (RuleType rule : getRules(item)) {
		for (CheckType check : rule.getCheck()) {
		    if (check.getSystem().equals("http://oval.mitre.org/XMLSchema/oval-definitions-5")) {
			for (CheckExportType export : check.getCheckExport()) {
			    String ovalVariableId = export.getExportName();
			    List<String> values = new Vector<String>();
			    values.add(settings.get(export.getValueId()));
			    variables.setValue(export.getExportName(), values);
			}
		    }
		}
	    }
	}
	return variables;
    }

    /**
     * Recursively list all rules within the SelectableItem.
     */
    private List<RuleType> getRules(SelectableItemType item) {
	List<RuleType> rules = new Vector<RuleType>();
	if (item instanceof RuleType) {
	    rules.add((RuleType)item);
	} else if (item instanceof GroupType) {
	    for (SelectableItemType child : ((GroupType)item).getGroupOrRule()) {
		rules.addAll(getRules(child));
	    }
	}
	return rules;
    }
}
