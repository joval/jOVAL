// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

namespace jOVAL.Metabase {
    using System;
    using System.Runtime.InteropServices;
    using System.Diagnostics;
    using System.Collections;
    using System.Collections.Generic;
    using System.Text;

    [GuidAttribute("479ED58D-2833-4078-9401-7535B182B6A8")]
    [ProgId("IIS.MSAdminBase")]
    public class Probe {
	private const uint METADATA_MAX_NAME_LEN = 256;
	private const uint METADATA_PERMISSION_READ = 0x00000001;
	private const uint METADATA_PERMISSION_WRITE = 0x00000002;

	private static IntPtr METADATA_MASTER_ROOT_HANDLE;
	private static IMSAdminBase baseInterface;

	static Probe() {
	    METADATA_MASTER_ROOT_HANDLE = new IntPtr(0);
	    MSAdminBase adminBase = new MSAdminBase();
	    baseInterface = (IMSAdminBase)adminBase;
	}

	public static List<String> ListSubkeys(String keyPath) {
	    List<String> subkeys = new List<String>();
	    try {
		UInt32 index = 0;
		while(true) {
		    StringBuilder name = new StringBuilder((int)METADATA_MAX_NAME_LEN);
		    baseInterface.EnumKeys(METADATA_MASTER_ROOT_HANDLE, keyPath, name, index++);
		    subkeys.Add(name.ToString());
		}
	    } catch (COMException e) {
		switch((UInt32)e.ErrorCode) {
		  case 0x80070103: // All done
		    break;
		  default:
		    throw e;
		}
	    }
	    return subkeys;
	}

	public enum DataField {
	    DATA_ID,
	    USER_TYPE,
	    DATA_TYPE,
	    VALUE
	}

	public static List<Dictionary<DataField, String>> ListData(String keyPath) {
	    List<Dictionary<DataField, String>> data = new List<Dictionary<DataField, String>>();
	    UInt32 index = 0;
	    bool done = false;
	    while(!done) {
		METADATA_RECORD metaDataRecord = new METADATA_RECORD();
		UInt32 len = 0;
		try {
		    baseInterface.EnumData(METADATA_MASTER_ROOT_HANDLE, keyPath, ref metaDataRecord, index, out len);
		} catch (COMException e) {
		    switch((UInt32)e.ErrorCode) {
		      case 0x8007007A: // Length too small
			if (len == 0) {
			    len = 1024; // DAS: there's an apparent bug where len isn't getting set!
			}
			metaDataRecord.pbMDData = Marshal.AllocCoTaskMem((int)len);
			if (metaDataRecord.pbMDData == IntPtr.Zero) {
			    throw new ExternalException("Unable to allocate memory for Metabase data buffer.");
			}
			metaDataRecord.dwMDDataLen = len;
			baseInterface.EnumData(METADATA_MASTER_ROOT_HANDLE, keyPath, ref metaDataRecord, index, out len);
			data.Add(ToData(metaDataRecord));
			break;
		      case 0x80070103: // All done
			done = true;
			break;
		      default:
			throw e;
		    }
		} finally {
		    if (metaDataRecord.pbMDData != IntPtr.Zero) {
			Marshal.FreeCoTaskMem(metaDataRecord.pbMDData);
		    }
		}
		index++;
	    }
	    return data;
	}

	public static Dictionary<DataField, String> GetData(String keyPath, UInt32 dataId) {
	    METADATA_RECORD metaDataRecord = new METADATA_RECORD();
	    metaDataRecord.dwMDIdentifier = dataId;
	    metaDataRecord.dwMDAttributes = (UInt32)METADATA_ATTRIBUTES.METADATA_INHERIT;
	    metaDataRecord.dwMDUserType = (UInt32)METADATA_USER_TYPE.IIS_MD_UT_SERVER;
	    metaDataRecord.dwMDDataType = (UInt32)METADATA_TYPES.ALL_METADATA;
	    try {
		UInt32 len = 0;
		try {
		    baseInterface.GetData(METADATA_MASTER_ROOT_HANDLE, keyPath, ref metaDataRecord, out len);
		} catch (COMException e) {
		    switch((UInt32)e.ErrorCode) {
		      case 0x8007007A: // Length too small
			if (len == 0) {
			    len = 1024; // DAS: there's an apparent bug where len isn't getting set!
			}
			metaDataRecord.pbMDData = Marshal.AllocCoTaskMem((int)len);
			if (metaDataRecord.pbMDData == IntPtr.Zero) {
			    throw new ExternalException("Unable to allocate memory for Metabase data buffer.");
			}
			metaDataRecord.dwMDDataLen = len;
			baseInterface.GetData(METADATA_MASTER_ROOT_HANDLE, keyPath, ref metaDataRecord, out len);
			break;
		      default:
			throw e;
		    }
		}
		return ToData(metaDataRecord);
	    } finally {
		if (metaDataRecord.pbMDData != IntPtr.Zero) {
		    Marshal.FreeCoTaskMem(metaDataRecord.pbMDData);
		}
	    }
	}

	private static Dictionary<DataField, String> ToData(METADATA_RECORD metaDataRecord) {
	    Dictionary<DataField, String> result = new Dictionary<DataField, String>();
	    result.Add(DataField.DATA_ID, metaDataRecord.dwMDIdentifier.ToString());
	    switch(metaDataRecord.dwMDUserType) {
	      case (UInt32)METADATA_USER_TYPE.IIS_MD_UT_SERVER:
		result.Add(DataField.USER_TYPE, "IIS_MD_UT_SERVER");
		break;
	      case (UInt32)METADATA_USER_TYPE.IIS_MD_UT_FILE:
		result.Add(DataField.USER_TYPE, "IIS_MD_UT_FILE");
		break;
	      case (UInt32)METADATA_USER_TYPE.IIS_MD_UT_WAM:
		result.Add(DataField.USER_TYPE, "IIS_MD_UT_WAM");
		break;
	      case (UInt32)METADATA_USER_TYPE.ASP_MD_UT_APP:
		result.Add(DataField.USER_TYPE, "ASP_MD_UT_APP");
		break;
	    }
	    switch(metaDataRecord.dwMDDataType) {
	      case (UInt32)METADATA_TYPES.BINARY_METADATA:
		result.Add(DataField.DATA_TYPE, "BINARY_METADATA");
		byte[] binaryData = new byte[metaDataRecord.dwMDDataLen];
		Marshal.Copy(metaDataRecord.pbMDData, binaryData, 0, binaryData.Length);
		result.Add(DataField.VALUE, ByteArrayToString(binaryData));
		break;
	      case (UInt32)METADATA_TYPES.STRING_METADATA:
		result.Add(DataField.DATA_TYPE, "STRING_METADATA");
		String stringData = Marshal.PtrToStringUni(metaDataRecord.pbMDData);
		result.Add(DataField.VALUE, stringData);
		break;
	      case (UInt32)METADATA_TYPES.DWORD_METADATA:
		result.Add(DataField.DATA_TYPE, "DWORD_METADATA");
		uint dwordData = (uint)Marshal.ReadInt32(metaDataRecord.pbMDData);
		result.Add(DataField.VALUE, dwordData.ToString());
		break;
	      case (UInt32)METADATA_TYPES.MULTISZ_METADATA:
		result.Add(DataField.DATA_TYPE, "MULTISZ_METADATA");
		byte[] multiSzData = new byte[metaDataRecord.dwMDDataLen];
		Marshal.Copy(metaDataRecord.pbMDData, multiSzData, 0, multiSzData.Length);
		// Trim Double Nulls
		string strings = Encoding.Unicode.GetString(multiSzData, 0, (int)(metaDataRecord.dwMDDataLen - 4));
		result.Add(DataField.VALUE, strings);
		break;
	    }
	    return result;
	}

	public static string ByteArrayToString(byte[] ba) {
	    StringBuilder hex = new StringBuilder(ba.Length * 2);
	    foreach (byte b in ba) {
	        hex.AppendFormat("{0:x2}", b);
	    }
	    return hex.ToString();
	}
    }

    [ComImport, Guid("a9e69610-b80d-11d0-b9b9-00a0c922e750")]
    public class MSAdminBase
    {
    }

    [ComImport, Guid("70B51430-B6CA-11d0-B9B9-00A0C922E750"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    public interface IMSAdminBase {
	void AddKey(IntPtr hMDHandle, [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath);
	void DeleteKey(IntPtr hMDHandle, [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath);
	void DeleteChildKeys(IntPtr hMDHandle, [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath);
	void EnumKeys(IntPtr hMDHandle,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath,
	    StringBuilder name,
	    UInt32 dwMDEnumKeyIndex);
	void CopyKey(IntPtr hMDSourceHandle,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDSourcePath,
	    IntPtr hMDDestHandle,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDDestPath,
	    bool bMDOverwriteFlag,
	    bool bMDCopyFlag);
	void RenameKey(IntPtr hMDHandle,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDNewName);
	void SetData(IntPtr hMDHandle,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath,
	    ref METADATA_RECORD pmdrMDData);
	void GetData(IntPtr hMDHandle,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath,
	    [MarshalAs(UnmanagedType.Struct)] ref METADATA_RECORD pmdrMDData,
	    out UInt32 pdwMDRequiredDataLen);
	void DeleteData(IntPtr hMDHandle,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath,
	    UInt32 dwMDIdentifier,
	    UInt32 dwMDDataType);
	void EnumData(IntPtr hMDHandle,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath,
	    [MarshalAs(UnmanagedType.Struct)] ref METADATA_RECORD pmdrMDData,
	    UInt32 dwMDEnumDataIndex,
	    out UInt32 pdwMDRequiredDataLen);
	void GetAllData();
	void DeleteAllData();
	void CopyData();
	void GetDataPaths();
	void OpenKey(IntPtr hMDHandle,
	    [MarshalAs(UnmanagedType.LPWStr)] String pszMDPath,
	    UInt32 dwMDAccessRequested,
	    UInt32 dwMDTimeOut,
	    out IntPtr phMDNewHandle);
	void CloseKey(IntPtr hMDHandle);
	void ChangePermissions(IntPtr hMDHandle,
	    UInt32 dwMDTimeOut,
	    UInt32 dwMDAccessRequested);
	void SaveData();
	void GetHandleInfo();
	void GetSystemChangeNumber();
	void GetDataSetNumber();
	void SetLastChangeTime();
	void GetLastChangeTime();
	void KeyExchangePhase1();
	void KeyExchangePhase2();
	void Backup();
	void Restore();
	void EnumBackups();
	void DeleteBackup();
	void UnmarshalInterface();
	void GetServerGuid();
    }

    public struct METADATA_RECORD {
	public UInt32 dwMDIdentifier;
	public UInt32 dwMDAttributes;
	public UInt32 dwMDUserType;
	public UInt32 dwMDDataType;
	public UInt32 dwMDDataLen;
	public IntPtr pbMDData;
	public UInt32 dwMDDataTag;
    }

    public struct METADATA_HANDLE_INFO {
	public UInt32 dwMDPermissions;
	public UInt32 dwMDSystemChangeNumber;
    }

    public enum METADATA_TYPES : uint {
	ALL_METADATA = 0,
	DWORD_METADATA = ALL_METADATA + 1,
	STRING_METADATA = DWORD_METADATA + 1,
	BINARY_METADATA = STRING_METADATA + 1,
	EXPANDSZ_METADATA = BINARY_METADATA + 1,
	MULTISZ_METADATA = EXPANDSZ_METADATA + 1,
	INVALID_END_METADATA = MULTISZ_METADATA + 1
    }

    public enum METADATA_USER_TYPE : uint {
	IIS_MD_UT_SERVER = 1,
	IIS_MD_UT_FILE = 2,
	IIS_MD_UT_WAM = 100,
	ASP_MD_UT_APP = 101,
	IIS_MD_UT_END_RESERVED = 2000
    }

    public enum METADATA_ATTRIBUTES : uint {
	METADATA_NO_ATTRIBUTES = 0,
	METADATA_INHERIT = 0x00000001,
	METADATA_PARTIAL_PATH = 0x00000002,
	METADATA_SECURE = 0x00000004,
	METADATA_REFERENCE = 0x00000008,
	METADATA_VOLATILE = 0x00000010,
	METADATA_ISINHERITED = 0x00000020,
	METADATA_INSERT_PATH = 0x00000040,
	METADATA_LOCAL_MACHINE_ONLY = 0x00000080,
	METADATA_NON_SECURE_ONLY = 0x00000100
    }
}
