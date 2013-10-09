// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.registry.IDwordValue;
import jsaf.intf.windows.registry.IKey;
import jsaf.intf.windows.registry.IRegistry;
import jsaf.intf.windows.registry.IValue;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.UacObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.UacItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates UacTest OVAL tests.
 *
 * @see http://technet.microsoft.com/en-us/library/dd835564%28v=ws.10%29.aspx#BKMK_RegistryKeys
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UacAdapter implements IAdapter {
    private IWindowsSession session;
    private UacItem item = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(UacObject.class);
	} else {
	    notapplicable.add(UacObject.class);
	}
	return classes;
    }

    public Collection<UacItem> getItems(ObjectType obj, IRequestContext rc) {
	Collection<UacItem> items = new ArrayList<UacItem>();
	try {
	    if (item == null) {
		//
		// Get the UAC registry values
		//
		IRegistry reg = session.getRegistry(session.getNativeView());
		IKey key = reg.getKey(IRegistry.Hive.HKLM, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\System");
		Map<String, IDwordValue> values = new HashMap<String, IDwordValue>();
		for (IValue value : reg.enumValues(key)) {
		    switch(value.getType()) {
		      case REG_DWORD:
			values.put(value.getName(), (IDwordValue)value);
			break;
		    }
		}
		item = Factories.sc.windows.createUacItem();

		EntityItemBoolType adminApprovalMode = Factories.sc.core.createEntityItemBoolType();
		adminApprovalMode.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		if (values.containsKey("FilterAdministratorToken")) {
		    IDwordValue value = values.get("FilterAdministratorToken");
		    switch(value.getData().intValue()) {
		      case 0:
			adminApprovalMode.setValue("0");
			break;
		      case 1:
			adminApprovalMode.setValue("1");
			break;
		      default:
			adminApprovalMode.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_UAC, value.toString()));
			rc.addMessage(msg);
			break;
		    }
		} else {
		    adminApprovalMode.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setAdminApprovalMode(adminApprovalMode);

		EntityItemStringType elevationPromptAdmin = Factories.sc.core.createEntityItemStringType();
		if (values.containsKey("ConsentPromptBehaviorAdmin")) {
		    IDwordValue value = values.get("ConsentPromptBehaviorAdmin");
		    switch(value.getData().intValue()) {
		      case 0:
			elevationPromptAdmin.setValue("Elevate without prompting");
			break;
		      case 1:
			elevationPromptAdmin.setValue("Prompt for credentials on the secure desktop");
			break;
		      case 2:
			elevationPromptAdmin.setValue("Prompt for consent on the secure desktop");
			break;
		      case 3:
			elevationPromptAdmin.setValue("Prompt for credentials");
			break;
		      case 4:
			elevationPromptAdmin.setValue("Prompt for consent");
			break;
		      case 5:
			elevationPromptAdmin.setValue("Prompt for consent for non-Windows binaries");
			break;
		      default:
			elevationPromptAdmin.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_UAC, value.toString()));
			rc.addMessage(msg);
			break;
		    }
		} else {
		    elevationPromptAdmin.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setElevationPromptAdmin(elevationPromptAdmin);

		EntityItemStringType elevationPromptStandard = Factories.sc.core.createEntityItemStringType();
		if (values.containsKey("ConsentPromptBehaviorUser")) {
		    IDwordValue value = values.get("ConsentPromptBehaviorUser");
		    switch(value.getData().intValue()) {
		      case 0:
			elevationPromptStandard.setValue("Automatically deny elevation requests");
			break;
		      case 1:
		      case 3:
			elevationPromptStandard.setValue("Prompt for credentials on the secure desktop");
			break;
		      default:
			elevationPromptStandard.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_UAC, value.toString()));
			rc.addMessage(msg);
			break;
		    }
		} else {
		    elevationPromptStandard.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setElevationPromptStandard(elevationPromptStandard);

		EntityItemBoolType detectInstallations = Factories.sc.core.createEntityItemBoolType();
		detectInstallations.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		if (values.containsKey("EnableInstallerDetection")) {
		    IDwordValue value = values.get("EnableInstallerDetection");
		    switch(value.getData().intValue()) {
		      case 0:
			detectInstallations.setValue("0");
			break;
		      case 1:
			detectInstallations.setValue("1");
			break;
		      default:
			detectInstallations.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_UAC, value.toString()));
			rc.addMessage(msg);
			break;
		    }
		} else {
		    detectInstallations.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setDetectInstallations(detectInstallations);

		EntityItemBoolType elevateSignedExecutables = Factories.sc.core.createEntityItemBoolType();
		elevateSignedExecutables.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		if (values.containsKey("ValidateAdminCodeSignatures")) {
		    IDwordValue value = values.get("ValidateAdminCodeSignatures");
		    switch(value.getData().intValue()) {
		      case 0:
			elevateSignedExecutables.setValue("0");
			break;
		      case 1:
			elevateSignedExecutables.setValue("1");
			break;
		      default:
			elevateSignedExecutables.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_UAC, value.toString()));
			rc.addMessage(msg);
			break;
		    }
		} else {
		    elevateSignedExecutables.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setElevateSignedExecutables(elevateSignedExecutables);

		EntityItemBoolType elevateUiaccess = Factories.sc.core.createEntityItemBoolType();
		elevateUiaccess.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		if (values.containsKey("EnableSecureUIAPaths")) {
		    IDwordValue value = values.get("EnableSecureUIAPaths");
		    switch(value.getData().intValue()) {
		      case 0:
			elevateUiaccess.setValue("0");
			break;
		      case 1:
			elevateUiaccess.setValue("1");
			break;
		      default:
			elevateUiaccess.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_UAC, value.toString()));
			rc.addMessage(msg);
			break;
		    }
		} else {
		    elevateUiaccess.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setElevateUiaccess(elevateUiaccess);

		EntityItemBoolType runAdminsAam = Factories.sc.core.createEntityItemBoolType();
		runAdminsAam.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		if (values.containsKey("EnableLUA")) {
		    IDwordValue value = values.get("EnableLUA");
		    switch(value.getData().intValue()) {
		      case 0:
			runAdminsAam.setValue("0");
			break;
		      case 1:
			runAdminsAam.setValue("1");
			break;
		      default:
			runAdminsAam.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_UAC, value.toString()));
			rc.addMessage(msg);
			break;
		    }
		} else {
		    runAdminsAam.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setRunAdminsAam(runAdminsAam);

		EntityItemBoolType secureDesktop = Factories.sc.core.createEntityItemBoolType();
		secureDesktop.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		if (values.containsKey("PromptOnSecureDesktop")) {
		    IDwordValue value = values.get("PromptOnSecureDesktop");
		    switch(value.getData().intValue()) {
		      case 0:
			secureDesktop.setValue("0");
			break;
		      case 1:
			secureDesktop.setValue("1");
			break;
		      default:
			secureDesktop.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_UAC, value.toString()));
			rc.addMessage(msg);
			break;
		    }
		} else {
		    secureDesktop.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setSecureDesktop(secureDesktop);

		EntityItemBoolType virtualizeWriteFailures = Factories.sc.core.createEntityItemBoolType();
		virtualizeWriteFailures.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		if (values.containsKey("EnableVirtualization")) {
		    IDwordValue value = values.get("EnableVirtualization");
		    switch(value.getData().intValue()) {
		      case 0:
			virtualizeWriteFailures.setValue("0");
			break;
		      case 1:
			virtualizeWriteFailures.setValue("1");
			break;
		      default:
			virtualizeWriteFailures.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_UAC, value.toString()));
			rc.addMessage(msg);
			break;
		    }
		} else {
		    virtualizeWriteFailures.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setVirtualizeWriteFailures(virtualizeWriteFailures);
	    }
	    items.add(item);
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }
}
