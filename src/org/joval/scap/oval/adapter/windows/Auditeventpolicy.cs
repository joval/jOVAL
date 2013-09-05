// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL.AuditEventPolicy {
    using System;
    using System.Collections.Generic;
    using System.Runtime.InteropServices;
    using System.Text;

    public static class Probe {
	public const int POLICY_VIEW_AUDIT_INFORMATION = 0x00000002;

	public enum POLICY_INFORMATION_CLASS {
	    PolicyAuditLogInformation = 1,
	    PolicyAuditEventsInformation,
	    PolicyPrimaryDomainInformation,
	    PolicyPdAccountInformation,
	    PolicyAccountDomainInformation,
	    PolicyLsaServerRoleInformation,
	    PolicyReplicaSourceInformation,
	    PolicyDefaultQuotaInformation,
	    PolicyModificationInformation,
	    PolicyAuditFullSetInformation,
	    PolicyAuditFullQueryInformation,
	    PolicyDnsDomainInformation
	}

	public enum POLICY_AUDIT_EVENT_TYPE {
	    AuditCategorySystem,
	    AuditCategoryLogon,
	    AuditCategoryObjectAccess,
	    AuditCategoryPrivilegeUse,
	    AuditCategoryDetailedTracking,
	    AuditCategoryPolicyChange,
	    AuditCategoryAccountManagement,
	    AuditCategoryDirectoryServiceAccess,
	    AuditCategoryAccountLogon
	}

	public enum AuditEventStatus {
	    AUDIT_NONE			= 0,
	    AUDIT_SUCCESS		= 1,
	    AUDIT_FAILURE		= 2,
	    AUDIT_SUCCESS_FAILURE	= 3,
	    EMPTY			= 8
	}

	public enum AuditEventPolicy {
	    SYSTEM = 0,
	    ACCOUNT_LOGON,
	    OBJECT_ACCESS,
	    PRIVILEGE_USE,
	    DETAILED_TRACKING,
	    POLICY_CHANGE,
	    ACCOUNT_MANAGEMENT,
	    DIRECTORY_SERVICE_ACCESS,
	    LOGON
	}

	/**
	 * Policies indexed by GUID.
	 *
	 * See http://msdn.microsoft.com/en-us/library/dd973928.aspx
	 */
	static Dictionary<String, String> AuditPolicySubcategories = new Dictionary<String, String>();
	static Probe() {
	    AuditPolicySubcategories.Add("0CCE9217-69AE-11D9-BED3-505054503030", "ACCOUNT_LOCKOUT");
	    AuditPolicySubcategories.Add("0CCE9222-69AE-11D9-BED3-505054503030", "APPLICATION_GENERATED");
	    AuditPolicySubcategories.Add("0CCE9239-69AE-11D9-BED3-505054503030", "APPLICATION_GROUP_MANAGEMENT");
	    AuditPolicySubcategories.Add("0CCE922F-69AE-11D9-BED3-505054503030", "AUDIT_POLICY_CHANGE");
	    AuditPolicySubcategories.Add("0CCE9230-69AE-11D9-BED3-505054503030", "AUTHENTICATION_POLICY_CHANGE");
	    AuditPolicySubcategories.Add("0CCE9231-69AE-11D9-BED3-505054503030", "AUTHORIZATION_POLICY_CHANGE");
	    AuditPolicySubcategories.Add("0CCE9221-69AE-11D9-BED3-505054503030", "CERTIFICATION_SERVICES");
	    AuditPolicySubcategories.Add("0CCE9246-69AE-11D9-BED3-505054503030", "CENTRAL_ACCESS_POLICY_STAGING");
	    AuditPolicySubcategories.Add("0CCE9236-69AE-11D9-BED3-505054503030", "COMPUTER_ACCOUNT_MANAGEMENT");
	    AuditPolicySubcategories.Add("0CCE923F-69AE-11D9-BED3-505054503030", "CREDENTIAL_VALIDATION");
	    AuditPolicySubcategories.Add("0CCE923E-69AE-11D9-BED3-505054503030", "DETAILED_DIRECTORY_SERVICE_REPLICATION");
	    AuditPolicySubcategories.Add("0CCE923B-69AE-11D9-BED3-505054503030", "DIRECTORY_SERVICE_ACCESS");
	    AuditPolicySubcategories.Add("0CCE923C-69AE-11D9-BED3-505054503030", "DIRECTORY_SERVICE_CHANGES");
	    AuditPolicySubcategories.Add("0CCE923D-69AE-11D9-BED3-505054503030", "DIRECTORY_SERVICE_REPLICATION");
	    AuditPolicySubcategories.Add("0CCE9238-69AE-11D9-BED3-505054503030", "DISTRIBUTION_GROUP_MANAGEMENT");
	    AuditPolicySubcategories.Add("0CCE922D-69AE-11D9-BED3-505054503030", "DPAPI_ACTIVITY");
	    AuditPolicySubcategories.Add("0CCE9224-69AE-11D9-BED3-505054503030", "FILE_SHARE");
	    AuditPolicySubcategories.Add("0CCE921D-69AE-11D9-BED3-505054503030", "FILE_SYSTEM");
	    AuditPolicySubcategories.Add("0CCE9226-69AE-11D9-BED3-505054503030", "FILTERING_PLATFORM_CONNECTION");
	    AuditPolicySubcategories.Add("0CCE9225-69AE-11D9-BED3-505054503030", "FILTERING_PLATFORM_PACKET_DROP");
	    AuditPolicySubcategories.Add("0CCE9233-69AE-11D9-BED3-505054503030", "FILTERING_PLATFORM_POLICY_CHANGE");
	    AuditPolicySubcategories.Add("0CCE9223-69AE-11D9-BED3-505054503030", "HANDLE_MANIPULATION");
	    AuditPolicySubcategories.Add("0CCE9213-69AE-11D9-BED3-505054503030", "IPSEC_DRIVER");
	    AuditPolicySubcategories.Add("0CCE921A-69AE-11D9-BED3-505054503030", "IPSEC_EXTENDED_MODE");
	    AuditPolicySubcategories.Add("0CCE9218-69AE-11D9-BED3-505054503030", "IPSEC_MAIN_MODE");
	    AuditPolicySubcategories.Add("0CCE9219-69AE-11D9-BED3-505054503030", "IPSEC_QUICK_MODE");
	    AuditPolicySubcategories.Add("0CCE921F-69AE-11D9-BED3-505054503030", "KERNEL_OBJECT");
	    AuditPolicySubcategories.Add("0CCE9216-69AE-11D9-BED3-505054503030", "LOGOFF");
	    AuditPolicySubcategories.Add("0CCE9215-69AE-11D9-BED3-505054503030", "LOGON");
	    AuditPolicySubcategories.Add("0CCE9232-69AE-11D9-BED3-505054503030", "MPSSVC_RULE_LEVEL_POLICY_CHANGE");
	    AuditPolicySubcategories.Add("0CCE9229-69AE-11D9-BED3-505054503030", "NON_SENSITIVE_PRIVILEGE_USE");
	    AuditPolicySubcategories.Add("0CCE9241-69AE-11D9-BED3-505054503030", "OTHER_ACCOUNT_LOGON_EVENTS");
	    AuditPolicySubcategories.Add("0CCE923A-69AE-11D9-BED3-505054503030", "OTHER_ACCOUNT_MANAGEMENT_EVENTS");
	    AuditPolicySubcategories.Add("0CCE921C-69AE-11D9-BED3-505054503030", "OTHER_LOGON_LOGOFF_EVENTS");
	    AuditPolicySubcategories.Add("0CCE9227-69AE-11D9-BED3-505054503030", "OTHER_OBJECT_ACCESS_EVENTS");
	    AuditPolicySubcategories.Add("0CCE9234-69AE-11D9-BED3-505054503030", "OTHER_POLICY_CHANGE_EVENTS");
	    AuditPolicySubcategories.Add("0CCE922A-69AE-11D9-BED3-505054503030", "OTHER_PRIVILEGE_USE_EVENTS");
	    AuditPolicySubcategories.Add("0CCE9214-69AE-11D9-BED3-505054503030", "OTHER_SYSTEM_EVENTS");
	    AuditPolicySubcategories.Add("0CCE922B-69AE-11D9-BED3-505054503030", "PROCESS_CREATION");
	    AuditPolicySubcategories.Add("0CCE922C-69AE-11D9-BED3-505054503030", "PROCESS_TERMINATION");
	    AuditPolicySubcategories.Add("0CCE921E-69AE-11D9-BED3-505054503030", "REGISTRY");
	    AuditPolicySubcategories.Add("0CCE9245-69AE-11D9-BED3-505054503030", "REMOVABLE_STORAGE");
	    AuditPolicySubcategories.Add("0CCE922E-69AE-11D9-BED3-505054503030", "RPC_EVENTS");
	    AuditPolicySubcategories.Add("0CCE9220-69AE-11D9-BED3-505054503030", "SAM");
	    AuditPolicySubcategories.Add("0CCE9237-69AE-11D9-BED3-505054503030", "SECURITY_GROUP_MANAGEMENT");
	    AuditPolicySubcategories.Add("0CCE9210-69AE-11D9-BED3-505054503030", "SECURITY_STATE_CHANGE");
	    AuditPolicySubcategories.Add("0CCE9211-69AE-11D9-BED3-505054503030", "SECURITY_SYSTEM_EXTENSION");
	    AuditPolicySubcategories.Add("0CCE9228-69AE-11D9-BED3-505054503030", "SENSITIVE_PRIVILEGE_USE");
	    AuditPolicySubcategories.Add("0CCE921B-69AE-11D9-BED3-505054503030", "SPECIAL_LOGON");
	    AuditPolicySubcategories.Add("0CCE9212-69AE-11D9-BED3-505054503030", "SYSTEM_INTEGRITY");
	    AuditPolicySubcategories.Add("0CCE9235-69AE-11D9-BED3-505054503030", "USER_ACCOUNT_MANAGEMENT");
	    AuditPolicySubcategories.Add("0CCE9244-69AE-11D9-BED3-505054503030", "DETAILED_FILE_SHARE");
	    AuditPolicySubcategories.Add("0CCE9243-69AE-11D9-BED3-505054503030", "NETWORK_POLICY_SERVER");
	    AuditPolicySubcategories.Add("0CCE9242-69AE-11D9-BED3-505054503030", "KERBEROS_AUTHENTICATION_SERVICE");
	    AuditPolicySubcategories.Add("0CCE9240-69AE-11D9-BED3-505054503030", "KERBEROS_SERVICE_TICKET_OPERATIONS");
	}

	[DllImport("kernel32.dll")]
	extern static int GetLastError();

	[DllImport("advapi32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
	public static extern UInt32 LsaNtStatusToWinError([In] UInt32 status);

	[DllImport("advapi32.dll", CharSet = CharSet.Unicode, PreserveSig = true)]
	public static extern UInt32 LsaOpenPolicy(String SystemName,
	    ref LSA_OBJECT_ATTRIBUTES ObjectAttributes, Int32 DesiredAccess, out IntPtr PolicyHandle);

	[DllImport("advapi32.dll", CharSet = CharSet.Unicode, PreserveSig = true)]
	public static extern UInt32 LsaClose(IntPtr PolicyHandle);

	[DllImport("advapi32.dll", CharSet = CharSet.Unicode, PreserveSig = true)]
	public static extern UInt32 LsaFreeMemory(IntPtr Buffer);

	[DllImport("advapi32.dll", CharSet = CharSet.Unicode, PreserveSig = true)]
	public static extern void AuditFree(IntPtr Buffer);

	[DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
	public static extern UInt32 LsaQueryInformationPolicy(IntPtr PolicyHandle, POLICY_INFORMATION_CLASS InformationClass,
	    out IntPtr Buffer);

	[DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
	public static extern bool AuditLookupCategoryGuidFromCategoryId(POLICY_AUDIT_EVENT_TYPE AuditCategoryId,
	    IntPtr pAuditCategoryGuid);

	[DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
	public static extern bool AuditEnumerateSubCategories(IntPtr pAuditCategoryGuid, bool bRetrieveAllSubCategories,
	    ref IntPtr pAuditSubCategoriesArray, out UInt32 pCountReturned);

	[DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
	public static extern bool AuditQuerySystemPolicy(IntPtr pSubCategoryGuids, UInt32 PolicyCount,
	    out IntPtr pAuditPolicies);

	[DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
        public static extern bool AuditLookupSubCategoryName(IntPtr pAuditSubCategoryGuid, ref String ppszSubCategoryName);

	[StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
	public struct LSA_UNICODE_STRING {
	    public UInt16 Length;
	    public UInt16 MaximumLength;
	    public IntPtr Buffer;
	}

	[StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
	public struct LSA_OBJECT_ATTRIBUTES {
	    public IntPtr RootDirectory;
	    public IntPtr SecurityDescriptor;
	    public IntPtr SecurityQualityOfService;
	    public LSA_UNICODE_STRING ObjectName;
	    public UInt32 Attributes;
	    public UInt32 Length;
	}

	[StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
	public struct POLICY_AUDIT_EVENTS_INFO {
	    public bool AuditingMode;
	    public IntPtr EventAuditingOptions;
	    public UInt32 MaximumAuditEventCount;
	}

	[StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
	public struct GUID {
	    public UInt32 Data1;
	    public UInt16 Data2;
	    public UInt16 Data3;
	    public Byte Data4a;
	    public Byte Data4b;
	    public Byte Data4c;
	    public Byte Data4d;
	    public Byte Data4e;
	    public Byte Data4f;
	    public Byte Data4g;
	    public Byte Data4h;

	    public override string ToString() {
		String s = Data1.ToString("x8") + "-" + Data2.ToString("x4") + "-" + Data3.ToString("x4") + "-"
		    + Data4a.ToString("x2") + Data4b.ToString("x2") + "-" + Data4c.ToString("x2")
		    + Data4d.ToString("x2") + Data4e.ToString("x2") + Data4f.ToString("x2") + Data4g.ToString("x2")
		    + Data4h.ToString("x2");
		return s.ToUpper();
	    }
	}

	[StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        public struct AUDIT_POLICY_INFORMATION {
            public GUID AuditSubCategoryGuid;
            public UInt32 AuditingInformation;
            public GUID AuditCategoryGuid;
        }

	public static Dictionary<AuditEventPolicy, AuditEventStatus> GetPolicies() {
	    Dictionary<AuditEventPolicy, AuditEventStatus> result = new Dictionary<AuditEventPolicy, AuditEventStatus>();

	    LSA_OBJECT_ATTRIBUTES objAttrs = new LSA_OBJECT_ATTRIBUTES();
	    IntPtr hPolicy = IntPtr.Zero;
	    IntPtr pInfo = IntPtr.Zero;
	    IntPtr pGuid = IntPtr.Zero;

	    UInt32 lrc = LsaOpenPolicy(null, ref objAttrs, POLICY_VIEW_AUDIT_INFORMATION, out hPolicy);
	    uint code = LsaNtStatusToWinError(lrc);
	    if (code != 0) {
		throw new System.ComponentModel.Win32Exception((int)code);
	    }
	    try {
		//
		// Query the policy
		//
		lrc = LsaQueryInformationPolicy(hPolicy, POLICY_INFORMATION_CLASS.PolicyAuditEventsInformation, out pInfo);
		code = LsaNtStatusToWinError(lrc);
		if (code != 0) {
		    throw new System.ComponentModel.Win32Exception((int)code);
		}
		POLICY_AUDIT_EVENTS_INFO info = new POLICY_AUDIT_EVENTS_INFO();
		info = (POLICY_AUDIT_EVENTS_INFO)Marshal.PtrToStructure(pInfo, info.GetType());

		//
		// Iterate through the event types
		//
		for (UInt32 eventType = 0; eventType < info.MaximumAuditEventCount; eventType++) {
		    pGuid = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(GUID)));
		    if (AuditLookupCategoryGuidFromCategoryId((POLICY_AUDIT_EVENT_TYPE)eventType, pGuid)) {
			IntPtr itemPtr = new IntPtr(info.EventAuditingOptions.ToInt64() +
				(Int64)eventType * (Int64)Marshal.SizeOf(typeof(UInt32)));
			UInt32 status = 0;
			status = (UInt32)Marshal.PtrToStructure(itemPtr, status.GetType());
			result.Add((AuditEventPolicy)eventType, (AuditEventStatus)(status & 0x3));
			Marshal.FreeHGlobal(pGuid);
			pGuid = IntPtr.Zero;
		    } else {
			throw new System.ComponentModel.Win32Exception(GetLastError());
		    }
		}
	    } finally {
		//
		// Cleanup
		//
		if (pInfo != IntPtr.Zero) {
		    LsaFreeMemory(pInfo);
		}
		if (pGuid != IntPtr.Zero) {
		    Marshal.FreeHGlobal(pGuid);
		}
		LsaClose(hPolicy);
	    }
	    return result;
	}

	public static Dictionary<AuditEventPolicy, Dictionary<String, AuditEventStatus>> GetSubcategoryPolicies() {
	    Dictionary<AuditEventPolicy, Dictionary<String, AuditEventStatus>> result = new Dictionary<AuditEventPolicy, Dictionary<String, AuditEventStatus>>();

	    LSA_OBJECT_ATTRIBUTES objAttrs = new LSA_OBJECT_ATTRIBUTES();
	    IntPtr hPolicy = IntPtr.Zero;
	    IntPtr pInfo = IntPtr.Zero;
	    IntPtr pGuid = IntPtr.Zero;
	    IntPtr pAuditPolicies = IntPtr.Zero;
	    IntPtr pSubcategories = IntPtr.Zero;

	    UInt32 lrc = LsaOpenPolicy(null, ref objAttrs, POLICY_VIEW_AUDIT_INFORMATION, out hPolicy);
	    uint code = LsaNtStatusToWinError(lrc);
	    if (code != 0) {
		throw new System.ComponentModel.Win32Exception((int)code);
	    }
	    try {
		//
		// Query the policy
		//
		lrc = LsaQueryInformationPolicy(hPolicy, POLICY_INFORMATION_CLASS.PolicyAuditEventsInformation, out pInfo);
		code = LsaNtStatusToWinError(lrc);
		if (code != 0) {
		    throw new System.ComponentModel.Win32Exception((int)code);
		}
		POLICY_AUDIT_EVENTS_INFO info = new POLICY_AUDIT_EVENTS_INFO();
		info = (POLICY_AUDIT_EVENTS_INFO)Marshal.PtrToStructure(pInfo, info.GetType());

		//
		// Iterate through the event types
		//
		for (UInt32 eventType = 0; eventType < info.MaximumAuditEventCount; eventType++) {
		    pGuid = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(GUID)));
		    if (!AuditLookupCategoryGuidFromCategoryId((POLICY_AUDIT_EVENT_TYPE)eventType, pGuid)) {
			throw new System.ComponentModel.Win32Exception(GetLastError());
		    }
		    UInt32 subcategoryCount = 0;
		    if (!AuditEnumerateSubCategories(pGuid, false, ref pSubcategories, out subcategoryCount)) {
			throw new System.ComponentModel.Win32Exception(GetLastError());
		    }
		    if (!AuditQuerySystemPolicy(pSubcategories, subcategoryCount, out pAuditPolicies)) {
			throw new System.ComponentModel.Win32Exception(GetLastError());
		    }

		    Dictionary<String, AuditEventStatus> dict = new Dictionary<String, AuditEventStatus>();
		    AUDIT_POLICY_INFORMATION policyInfo = new AUDIT_POLICY_INFORMATION();
		    for (UInt32 subcategoryIndex = 0; subcategoryIndex < subcategoryCount; subcategoryIndex++) {
			IntPtr itemPtr = new IntPtr(pAuditPolicies.ToInt64() +
				(long)subcategoryIndex * (Int64)Marshal.SizeOf(policyInfo));
			policyInfo = (AUDIT_POLICY_INFORMATION)Marshal.PtrToStructure(itemPtr, policyInfo.GetType());
			dict.Add(AuditPolicySubcategories[policyInfo.AuditSubCategoryGuid.ToString()],
				(AuditEventStatus)policyInfo.AuditingInformation);
		    }
		    result.Add((AuditEventPolicy)eventType, dict);

		    AuditFree(pAuditPolicies);
		    pAuditPolicies = IntPtr.Zero;
		    AuditFree(pSubcategories);
		    pSubcategories = IntPtr.Zero;
		    Marshal.FreeHGlobal(pGuid);
		    pGuid = IntPtr.Zero;
		}
	    } finally {
		//
		// Cleanup
		//
		if (pInfo != IntPtr.Zero) {
		    LsaFreeMemory(pInfo);
		}
		if (pGuid != IntPtr.Zero) {
		    Marshal.FreeHGlobal(pGuid);
		}
		if (pAuditPolicies != IntPtr.Zero) {
		    AuditFree(pAuditPolicies);
		}
		if (pSubcategories != IntPtr.Zero) {
		    AuditFree(pSubcategories);
		}
		LsaClose(hPolicy);
	    }
	    return result;
	}
    }
}
