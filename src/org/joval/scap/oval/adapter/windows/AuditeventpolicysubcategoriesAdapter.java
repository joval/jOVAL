// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.AuditeventpolicysubcategoriesObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.AuditeventpolicysubcategoriesItem;
import scap.oval.systemcharacteristics.windows.EntityItemAuditType;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

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

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(AuditeventpolicysubcategoriesObject.class);
	} else {
	    notapplicable.add(AuditeventpolicysubcategoriesObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (error != null) {
	    throw error;
	} else if (items == null) {
	    items = makeItems();
	}
	return items;
    }

    // Private

    private Collection<AuditeventpolicysubcategoriesItem> makeItems() throws CollectException {
	try {
            //
            // Get a runspace if there are any in the pool, or create a new one, and load the Get-AuditEventPolicies
            // Powershell module code.
            //
            IWindowsSession.View view = session.getNativeView();
            IRunspace runspace = null;
            for (IRunspace rs : session.getRunspacePool().enumerate()) {
                if (rs.getView() == view) {
                    runspace = rs;
                    break;
                }
            }
            if (runspace == null) {
                runspace = session.getRunspacePool().spawn(view);
            }
            if (runspace != null) {
                runspace.loadAssembly(getClass().getResourceAsStream("Auditeventpolicy.dll"));
                runspace.loadModule(getClass().getResourceAsStream("Auditeventpolicy.psm1"));
            }

	    AuditeventpolicysubcategoriesItem item = Factories.sc.windows.createAuditeventpolicysubcategoriesItem();
	    for (String line : runspace.invoke("Get-AuditEventSubcategoryPolicies").split("\r\n")) {
		if (line.startsWith("  ")) { // skip category lines
		    int ptr = line.indexOf(":");
		    if (ptr != -1) {
			String subcategory = line.substring(2,ptr);
			EntityItemAuditType value = Factories.sc.windows.createEntityItemAuditType();
			value.setValue(line.substring(ptr+1).trim());
			if ("SECURITY_SYSTEM_EXTENSION".equals(subcategory)) {
			    item.setSecuritySystemExtension(value);
			} else if ("SYSTEM_INTEGRITY".equals(subcategory)) {
			    item.setSystemIntegrity(value);
			} else if ("IPSEC_DRIVER".equals(subcategory)) {
			    item.setIpsecDriver(value);
			} else if ("OTHER_SYSTEM_EVENTS".equals(subcategory)) {
			    item.setOtherSystemEvents(value);
			} else if ("SECURITY_STATE_CHANGE".equals(subcategory)) {
			    item.setSecurityStateChange(value);
			} else if ("LOGON".equals(subcategory)) {
			    item.setLogon(value);
			} else if ("LOGOFF".equals(subcategory)) {
			    item.setLogoff(value);
			} else if ("ACCOUNT_LOCKOUT".equals(subcategory)) {
			    item.setAccountLockout(value);
			} else if ("IPSEC_MAIN_MODE".equals(subcategory)) {
			    item.setIpsecMainMode(value);
			} else if ("IPSEC_QUICK_MODE".equals(subcategory)) {
			    item.setIpsecQuickMode(value);
			} else if ("IPSEC_EXTENDED_MODE".equals(subcategory)) {
			    item.setIpsecExtendedMode(value);
			} else if ("SPECIAL_LOGON".equals(subcategory)) {
			    item.setSpecialLogon(value);
			} else if ("OTHER_LOGON_LOGOFF_EVENTS".equals(subcategory)) {
			    item.setOtherLogonLogoffEvents(value);
			} else if ("NETWORK_POLICY_SERVER".equals(subcategory)) {
			    item.setNetworkPolicyServer(value);
			} else if ("FILE_SYSTEM".equals(subcategory)) {
			    item.setFileSystem(value);
			} else if ("REGISTRY".equals(subcategory)) {
			    item.setRegistry(value);
			} else if ("KERNEL_OBJECT".equals(subcategory)) {
			    item.setKernelObject(value);
			} else if ("SAM".equals(subcategory)) {
			    item.setSam(value);
			} else if ("CERTIFICATION_SERVICES".equals(subcategory)) {
			    item.setCertificationServices(value);
			} else if ("APPLICATION_GENERATED".equals(subcategory)) {
			    item.setApplicationGenerated(value);
			} else if ("HANDLE_MANIPULATION".equals(subcategory)) {
			    item.setHandleManipulation(value);
			} else if ("FILE_SHARE".equals(subcategory)) {
			    item.setFileShare(value);
			} else if ("FILTERING_PLATFORM_PACKET_DROP".equals(subcategory)) {
			    item.setFilteringPlatformPacketDrop(value);
			} else if ("FILTERING_PLATFORM_CONNECTION".equals(subcategory)) {
			    item.setFilteringPlatformConnection(value);
			} else if ("OTHER_OBJECT_ACCESS_EVENTS".equals(subcategory)) {
			    item.setOtherObjectAccessEvents(value);
			} else if ("DETAILED_FILE_SHARE".equals(subcategory)) {
			    item.setDetailedFileShare(value);
			} else if ("SENSITIVE_PRIVILEGE_USE".equals(subcategory)) {
			    item.setSensitivePrivilegeUse(value);
			} else if ("NON_SENSITIVE_PRIVILEGE_USE".equals(subcategory)) {
			    item.setNonSensitivePrivilegeUse(value);
			} else if ("OTHER_PRIVILEGE_USE_EVENTS".equals(subcategory)) {
			    item.setOtherPrivilegeUseEvents(value);
			} else if ("PROCESS_TERMINATION".equals(subcategory)) {
			    item.setProcessTermination(value);
			} else if ("DPAPI_ACTIVITY".equals(subcategory)) {
			    item.setDpapiActivity(value);
			} else if ("RPC_EVENTS".equals(subcategory)) {
			    item.setRpcEvents(value);
			} else if ("PROCESS_CREATION".equals(subcategory)) {
			    item.setProcessCreation(value);
			} else if ("AUDIT_POLICY_CHANGE".equals(subcategory)) {
			    item.setAuditPolicyChange(value);
			} else if ("AUTHENTICATION_POLICY_CHANGE".equals(subcategory)) {
			    item.setAuthenticationPolicyChange(value);
			} else if ("AUTHORIZATION_POLICY_CHANGE".equals(subcategory)) {
			    item.setAuthorizationPolicyChange(value);
			} else if ("MPSSVC_RULE_LEVEL_POLICY_CHANGE".equals(subcategory)) {
			    item.setMpssvcRuleLevelPolicyChange(value);
			} else if ("FILTERING_PLATFORM_POLICY_CHANGE".equals(subcategory)) {
			    item.setFilteringPlatformPolicyChange(value);
			} else if ("OTHER_POLICY_CHANGE_EVENTS".equals(subcategory)) {
			    item.setOtherPolicyChangeEvents(value);
			} else if ("USER_ACCOUNT_MANAGEMENT".equals(subcategory)) {
			    item.setUserAccountManagement(value);
			} else if ("COMPUTER_ACCOUNT_MANAGEMENT".equals(subcategory)) {
			    item.setComputerAccountManagement(value);
			} else if ("SECURITY_GROUP_MANAGEMENT".equals(subcategory)) {
			    item.setSecurityGroupManagement(value);
			} else if ("DISTRIBUTION_GROUP_MANAGEMENT".equals(subcategory)) {
			    item.setDistributionGroupManagement(value);
			} else if ("APPLICATION_GROUP_MANAGEMENT".equals(subcategory)) {
			    item.setApplicationGroupManagement(value);
			} else if ("OTHER_ACCOUNT_MANAGEMENT_EVENTS".equals(subcategory)) {
			    item.setOtherAccountManagementEvents(value);
			} else if ("DIRECTORY_SERVICE_CHANGES".equals(subcategory)) {
			    item.setDirectoryServiceChanges(value);
			} else if ("DIRECTORY_SERVICE_REPLICATION".equals(subcategory)) {
			    item.setDirectoryServiceReplication(value);
			} else if ("DETAILED_DIRECTORY_SERVICE_REPLICATION".equals(subcategory)) {
			    item.setDetailedDirectoryServiceReplication(value);
			} else if ("DIRECTORY_SERVICE_ACCESS".equals(subcategory)) {
			    item.setDirectoryServiceAccess(value);
			} else if ("KERBEROS_SERVICE_TICKET_OPERATIONS".equals(subcategory)) {
			    item.setKerberosServiceTicketOperations(value);
			} else if ("KERBEROS_TICKET_EVENTS".equals(subcategory)) {
//DAS: there is no such audit event policy subcategory
			    item.setKerberosTicketEvents(value);
			} else if ("OTHER_ACCOUNT_LOGON_EVENTS".equals(subcategory)) {
			    item.setOtherAccountLogonEvents(value);
			} else if ("KERBEROS_AUTHENTICATION_SERVICE".equals(subcategory)) {
			    item.setKerberosAuthenticationService(value);
			} else if ("CREDENTIAL_VALIDATION".equals(subcategory)) {
			    item.setCredentialValidation(value);
			} else {
			    session.getLogger().warn(JOVALMsg.ERROR_WIN_AUDITPOL_SUBCATEGORY, subcategory);
			}
		    }
		}
	    }
	    return Arrays.asList(item);
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }
}
