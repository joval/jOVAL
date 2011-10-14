// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.independent.EnvironmentvariableObject;
import oval.schemas.definitions.independent.Environmentvariable58Object;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.independent.EnvironmentvariableItem;
import oval.schemas.systemcharacteristics.independent.Environmentvariable58Item;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Evaluates Environmentvariable OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Environmentvariable58Adapter extends EnvironmentvariableAdapter {
    private ISession session;

    public Environmentvariable58Adapter(ISession session) {
	super(session);
	this.session = session;
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return Environmentvariable58Object.class;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {

	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	Environmentvariable58Object eObj = (Environmentvariable58Object)rc.getObject();

	if (eObj.isSetPid() && eObj.getPid().getValue() != null) {
	    //
	    // In the absence of a pid, just leverage the legacy adapter.
	    //
	    for (JAXBElement<? extends ItemType> elt : super.getItems(new EVRequestContext(rc))) {
		EnvironmentvariableItem item = (EnvironmentvariableItem)elt.getValue();
		Environmentvariable58Item newItem = JOVALSystem.factories.sc.independent.createEnvironmentvariable58Item();
		newItem.setName(item.getName());
		newItem.setValue(item.getValue());
		items.add(JOVALSystem.factories.sc.independent.createEnvironmentvariable58Item(newItem));
	    }
	} else {

/*

The implementation when given a PID is TBD.

In Solaris, there is the psargs command:
http://www.unix.com/hp-ux/112024-how-can-i-get-environment-running-process.html

On Linux hosts, you can cat /proc/$PID/environ, where $PID is the process id you are interested in.
http://prefetch.net/blog/index.php/2006/07/04/viewing-a-processes-environment-on-linux-and-solaris-hosts/

Also see:
http://yong321.freeshell.org/computer/ProcEnv.txt

*/
	    throw new OvalException("Not implemented!");

	}

	return items;
    }

    // Private

    class EVRequestContext implements IRequestContext {
        IRequestContext base;
        EnvironmentvariableObject object;

        EVRequestContext(IRequestContext base) {
            Environmentvariable58Object evo = (Environmentvariable58Object)base.getObject();
            object = JOVALSystem.factories.definitions.independent.createEnvironmentvariableObject();
            object.setName(evo.getName());
        }

        // Implement IRequestContext
        public ObjectType getObject() {
            return object;
        }

        public void addMessage(MessageType msg) {
            base.addMessage(msg);
        }

        public Collection<String> resolve(String variableId) throws NoSuchElementException, ResolveException, OvalException {
            return base.resolve(variableId);
        }
    }
}
