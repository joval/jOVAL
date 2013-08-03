// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL {
    using System;
    using System.Collections.Generic;
    using System.Runtime.InteropServices;
    using System.Security.AccessControl;
    using System.Security.Principal;
    using System.Text;

    namespace EffectiveRights {
	public class Probe {
	    public enum MULTIPLE_TRUSTEE_OPERATION {
		NO_MULTIPLE_TRUSTEE,
		TRUSTEE_IS_IMPERSONATE
	    }

	    public enum TRUSTEE_FORM {
		TRUSTEE_IS_SID,
		TRUSTEE_IS_NAME,
		TRUSTEE_BAD_FORM,
		TRUSTEE_IS_OBJECTS_AND_SID,
		TRUSTEE_IS_OBJECTS_AND_NAME
	    }

	    public enum TRUSTEE_TYPE {
		TRUSTEE_IS_UNKNOWN,
		TRUSTEE_IS_USER,
		TRUSTEE_IS_GROUP,
		TRUSTEE_IS_DOMAIN,
		TRUSTEE_IS_ALIAS,
		TRUSTEE_IS_WELL_KNOWN_GROUP,
		TRUSTEE_IS_DELETED,
		TRUSTEE_IS_INVALID,
		TRUSTEE_IS_COMPUTER
	    }

	    public struct TRUSTEE {
		public IntPtr pMultipleTrustee; public MULTIPLE_TRUSTEE_OPERATION MultipleTrusteeOperation;
		public TRUSTEE_FORM TrusteeForm; public TRUSTEE_TYPE TrusteeType; public IntPtr ptstrName;
	    }

	    public enum SE_OBJECT_TYPE {
		SE_UNKNOWN_OBJECT_TYPE = 0,
		SE_FILE_OBJECT,
		SE_SERVICE,
		SE_PRINTER,
		SE_REGISTRY_KEY,
		SE_LMSHARE,
		SE_KERNEL_OBJECT,
		SE_WINDOW_OBJECT,
		SE_DS_OBJECT,
		SE_DS_OBJECT_ALL,
		SE_PROVIDER_DEFINED_OBJECT,
		SE_WMIGUID_OBJECT,
		SE_REGISTRY_WOW64_32KEY
	    }

	    [Flags]
	    public enum SECURITY_INFORMATION : uint {
		OWNER_SECURITY_INFORMATION = 0x00000001,
		GROUP_SECURITY_INFORMATION = 0x00000002,
		DACL_SECURITY_INFORMATION = 0x00000004,
		SACL_SECURITY_INFORMATION = 0x00000008,
		UNPROTECTED_SACL_SECURITY_INFORMATION = 0x10000000,
		UNPROTECTED_DACL_SECURITY_INFORMATION = 0x20000000,
		PROTECTED_SACL_SECURITY_INFORMATION = 0x40000000,
		PROTECTED_DACL_SECURITY_INFORMATION = 0x80000000
	    }

	    [DllImport("advapi32.dll", CharSet=CharSet.Auto)]
	    static extern uint GetNamedSecurityInfo
		(String pObjectName, SE_OBJECT_TYPE ObjectType, SECURITY_INFORMATION SecurityInfo, out IntPtr pSidOwner,
		 out IntPtr pSidGroup, out IntPtr pDacl, out IntPtr pSacl, out IntPtr pSecurityDescriptor);

	    [DllImport("advapi32.dll", SetLastError = true)]
	    public static extern void BuildTrusteeWithSid(ref TRUSTEE pTrustee, byte[] sid);

	    [DllImport("advapi32.dll")]
	    public static extern uint GetEffectiveRightsFromAcl(IntPtr pacl, ref TRUSTEE pTrustee, ref uint pAccessRights);

	    public static uint GetRegKeyEffectiveRights(String name, String sidString) {
		return GetEffectiveRights(SE_OBJECT_TYPE.SE_REGISTRY_KEY, name, sidString);
	    }

	    public static uint GetFileEffectiveRights(String name, String sidString) { 
		return GetEffectiveRights(SE_OBJECT_TYPE.SE_FILE_OBJECT, name, sidString);
	    }

	    public static uint GetServiceEffectiveRights(String name, String sidString) { 
		return GetEffectiveRights(SE_OBJECT_TYPE.SE_SERVICE, name, sidString);
	    }

	    static uint GetEffectiveRights(SE_OBJECT_TYPE type, String name, String sidString) {
		SecurityIdentifier sid = new SecurityIdentifier(sidString);

		IntPtr pOwner = IntPtr.Zero; // pSID
		IntPtr pGroup = IntPtr.Zero; // pSID
		IntPtr pSacl = IntPtr.Zero;
		IntPtr pDacl = IntPtr.Zero;
		IntPtr pSD = IntPtr.Zero; // pSECURITY_DESCRIPTOR
		uint result = GetNamedSecurityInfo(name, type, SECURITY_INFORMATION.DACL_SECURITY_INFORMATION, out pOwner,
						   out pGroup, out pDacl, out pSacl, out pSD);
		if (result != 0) {
		    throw new System.ComponentModel.Win32Exception((int)result);
		}

		byte[] sidBuffer = new byte[sid.BinaryLength];
		sid.GetBinaryForm(sidBuffer, 0);

		TRUSTEE t = new TRUSTEE();
		BuildTrusteeWithSid(ref t, sidBuffer);

		uint access = 0;
		uint hr = GetEffectiveRightsFromAcl(pDacl, ref t, ref access);
		int i = Marshal.Release(t.ptstrName);

		return access;
	    }
	}
    }
}
