// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.Collection;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.AuditeventpolicysubcategoriesObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.AuditeventpolicysubcategoriesItem;
import oval.schemas.systemcharacteristics.windows.EntityItemAuditType;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Retrieves the unary windows:auditeventpolicysubcategories_item.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class AuditeventpolicysubcategoriesAdapter implements IAdapter {
    protected IWindowsSession session;
    private Collection<AuditeventpolicysubcategoriesItem> items = null;
    private CollectException error = null;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(AuditeventpolicysubcategoriesObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (error != null) {
	    throw error;
	} else if (items == null) {
	    makeItem();
	}
	return items;
    }

    // Private

    enum AuditType {
	NONE("No Auditing", "AUDIT_NONE"),
	SUCCESS("Success", "AUDIT_SUCCESS"),
	SUCCESS_AND_FAILURE("Success and Failure", "AUDIT_SUCCESS_FAILURE"), // NB: precedes Failure!
	FAILURE("Failure", "AUDIT_FAILURE");

	private String key;
	private EntityItemAuditType entity;

	private AuditType(String key, String value) {
	    this.key = key;
	    entity = Factories.sc.windows.createEntityItemAuditType();
	    entity.setValue(value);
	}

	String key() {
	    return key;
	}

	EntityItemAuditType entity() {
	    return entity;
	}

	static AuditType fromLine(String line) throws IllegalArgumentException {
	    if (line.startsWith("  ")) {
		for (AuditType type : values()) {
		    if (line.trim().endsWith(type.key())) {
			return type;
		    }
		}
	    }
	    throw new IllegalArgumentException(line);
	}
    }

    private void makeItem() throws CollectException {
	try {
	    long timeout = session.getTimeout(IBaseSession.Timeout.M);
	    String[] env = session.getEnvironment().toArray();
	    SafeCLI.ExecData data = SafeCLI.execData("AuditPol /get /category:*", env, session, timeout);
	    int code = data.getExitCode();
	    switch(code) {
	      case 0: // success
		items = new Vector<AuditeventpolicysubcategoriesItem>();
		AuditeventpolicysubcategoriesItem item = Factories.sc.windows.createAuditeventpolicysubcategoriesItem();
		for (String line : data.getLines()) {
		    if (line.startsWith("  ")) { // skip category lines
			try {
			    AuditType type = AuditType.fromLine(line);
			    line = line.trim();
			    String subcategory = line.substring(0, line.length() - type.key().length()).trim();
			    if ("Security System Extension".equals(subcategory)) {
				item.setSecuritySystemExtension(type.entity());
			    } else if ("System Integrity".equals(subcategory)) {
				item.setSystemIntegrity(type.entity());
			    } else if ("IPsec Driver".equals(subcategory)) {
				item.setIpsecDriver(type.entity());
			    } else if ("Other System Events".equals(subcategory)) {
				item.setOtherSystemEvents(type.entity());
			    } else if ("Security State Change".equals(subcategory)) {
				item.setSecurityStateChange(type.entity());
			    } else if ("Logon".equals(subcategory)) {
				item.setLogon(type.entity());
			    } else if ("Logoff".equals(subcategory)) {
				item.setLogoff(type.entity());
			    } else if ("Account Lockout".equals(subcategory)) {
				item.setAccountLockout(type.entity());
			    } else if ("IPsec Main Mode".equals(subcategory)) {
				item.setIpsecMainMode(type.entity());
			    } else if ("IPsec Quick Mode".equals(subcategory)) {
				item.setIpsecQuickMode(type.entity());
			    } else if ("IPsec Extended Mode".equals(subcategory)) {
				item.setIpsecExtendedMode(type.entity());
			    } else if ("Special Logon".equals(subcategory)) {
				item.setSpecialLogon(type.entity());
			    } else if ("Other Logon/Logoff Events".equals(subcategory)) {
				item.setOtherLogonLogoffEvents(type.entity());
			    } else if ("Network Policy Server".equals(subcategory)) {
				item.setNetworkPolicyServer(type.entity());
			    } else if ("File System".equals(subcategory)) {
				item.setFileSystem(type.entity());
			    } else if ("Registry".equals(subcategory)) {
				item.setRegistry(type.entity());
			    } else if ("Kernel Object".equals(subcategory)) {
				item.setKernelObject(type.entity());
			    } else if ("SAM".equals(subcategory)) {
				item.setSam(type.entity());
			    } else if ("Certification Services".equals(subcategory)) {
				item.setCertificationServices(type.entity());
			    } else if ("Application Generated".equals(subcategory)) {
				item.setApplicationGenerated(type.entity());
			    } else if ("Handle Manipulation".equals(subcategory)) {
				item.setHandleManipulation(type.entity());
			    } else if ("File Share".equals(subcategory)) {
				item.setFileShare(type.entity());
			    } else if ("Filtering Platform Packet Drop".equals(subcategory)) {
				item.setFilteringPlatformPacketDrop(type.entity());
			    } else if ("Filtering Platform Connection".equals(subcategory)) {
				item.setFilteringPlatformConnection(type.entity());
			    } else if ("Other Object Access Events".equals(subcategory)) {
				item.setOtherObjectAccessEvents(type.entity());
			    } else if ("Detailed File Share".equals(subcategory)) {
				item.setDetailedFileShare(type.entity());
			    } else if ("Sensitive Privilege Use".equals(subcategory)) {
				item.setSensitivePrivilegeUse(type.entity());
			    } else if ("Non Sensitive Privilege Use".equals(subcategory)) {
				item.setNonSensitivePrivilegeUse(type.entity());
			    } else if ("Other Privilege Use Events".equals(subcategory)) {
				item.setOtherPrivilegeUseEvents(type.entity());
			    } else if ("Process Termination".equals(subcategory)) {
				item.setProcessTermination(type.entity());
			    } else if ("DPAPI Activity".equals(subcategory)) {
				item.setDpapiActivity(type.entity());
			    } else if ("RPC Events".equals(subcategory)) {
				item.setRpcEvents(type.entity());
			    } else if ("Process Creation".equals(subcategory)) {
				item.setProcessCreation(type.entity());
			    } else if ("Audit Policy Change".equals(subcategory)) {
				item.setAuditPolicyChange(type.entity());
			    } else if ("Authentication Policy Change".equals(subcategory)) {
				item.setAuthenticationPolicyChange(type.entity());
			    } else if ("Authorization Policy Change".equals(subcategory)) {
				item.setAuthorizationPolicyChange(type.entity());
			    } else if ("MPSSVC Rule-Level Policy Change".equals(subcategory)) {
				item.setMpssvcRuleLevelPolicyChange(type.entity());
			    } else if ("Filtering Platform Policy Change".equals(subcategory)) {
				item.setFilteringPlatformPolicyChange(type.entity());
			    } else if ("Other Policy Change Events".equals(subcategory)) {
				item.setOtherPolicyChangeEvents(type.entity());
			    } else if ("User Account Management".equals(subcategory)) {
				item.setUserAccountManagement(type.entity());
			    } else if ("Computer Account Management".equals(subcategory)) {
				item.setComputerAccountManagement(type.entity());
			    } else if ("Security Group Management".equals(subcategory)) {
				item.setSecurityGroupManagement(type.entity());
			    } else if ("Distribution Group Management".equals(subcategory)) {
				item.setDistributionGroupManagement(type.entity());
			    } else if ("Application Group Management".equals(subcategory)) {
				item.setApplicationGroupManagement(type.entity());
			    } else if ("Other Account Management Events".equals(subcategory)) {
				item.setOtherAccountManagementEvents(type.entity());
			    } else if ("Directory Service Changes".equals(subcategory)) {
				item.setDirectoryServiceChanges(type.entity());
			    } else if ("Directory Service Replication".equals(subcategory)) {
				item.setDirectoryServiceReplication(type.entity());
			    } else if ("Detailed Directory Service Replication".equals(subcategory)) {
				item.setDetailedDirectoryServiceReplication(type.entity());
			    } else if ("Directory Service Access".equals(subcategory)) {
				item.setDirectoryServiceAccess(type.entity());
			    } else if ("Kerberos Service Ticket Operations".equals(subcategory)) {
				item.setKerberosServiceTicketOperations(type.entity());
			    } else if ("Kerberos Ticket Events".equals(subcategory)) {
				item.setKerberosTicketEvents(type.entity());
			    } else if ("Other Account Logon Events".equals(subcategory)) {
				item.setOtherAccountLogonEvents(type.entity());
			    } else if ("Kerberos Authentication Service".equals(subcategory)) {
				item.setKerberosAuthenticationService(type.entity());
			    } else if ("Credential Validation".equals(subcategory)) {
				item.setCredentialValidation(type.entity());
			    } else {
				session.getLogger().warn(JOVALMsg.ERROR_WIN_AUDITPOL_SUBCATEGORY, subcategory);
			    }
			} catch (IllegalArgumentException e) {
			    session.getLogger().warn(JOVALMsg.ERROR_WIN_AUDITPOL_SETTING, line);
			}
		    }
		}
		items.add(item);
		break;

	      default:
		String output = new String(data.getData());
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_AUDITPOL_CODE, Integer.toString(code), output);
		throw new Exception(msg);
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_PROCESS_CREATE, e.getMessage());
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }
}
