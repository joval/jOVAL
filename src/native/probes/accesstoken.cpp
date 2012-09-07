// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

#pragma once

#include <stdio.h>
#include <malloc.h>
#include <Windows.h>
#include <Ntsecapi.h>

HRESULT GetSid(LPCSTR szAccName, PSID *ppSid);

/**
 * Return Codes:
 *
 * Int	Win32 Code		Meaning
 * ---- ----------------------- ----------------------------------------------------
 *    0	ERROR_SUCCESS		Account rights were listed successfully.
 *    2	ERROR_FILE_NOT_FOUND	No rights are defined for the account.
 *    8	ERROR_NOT_ENOUGH_MEMORY	The computer failed to allocate memory for a buffer.
 *   16	ERROR_INVALID_DATA	The SID returned for the account was not valid.
 *  160	ERROR_BAD_ARGUMENTS	Improper arguments were passed into the program.
 * 1332	ERROR_NODE_MAPPED	No such account was found on this machine.
 *
 * Any other return code will correspond to an underlying Win32 code that was returned by
 * a Win32 API call.
 */
int main(int argc, char* argv[]) {
    if (argc == 2) {
	if (strcmp(argv[1], "-about") == 0) {
	    fprintf(stdout, "jOVAL.org Windows:AccessToken Probe\n");
	    fprintf(stdout, "Copyright (C) 2012, jOVAL.org.  All rights reserved.\n");
	    return 0;
	}
    } else {
	fprintf(stdout, "Usage: %s [Security Principal]\n", argv[0]);
	return ERROR_BAD_ARGUMENTS;
    }

    PSID sid;
    DWORD code = HRESULT_CODE(GetSid(argv[1], &sid));
    switch(code) {
      case ERROR_SUCCESS:
	LSA_HANDLE hPolicy;
	LSA_OBJECT_ATTRIBUTES attrs;
	ZeroMemory(&attrs, sizeof(attrs));
	code = HRESULT_CODE(LsaOpenPolicy(NULL, &attrs, POLICY_LOOKUP_NAMES, &hPolicy));
	switch(code) {
	  case ERROR_SUCCESS: {
	    PLSA_UNICODE_STRING rights = NULL;
	    ULONG count = 0;
	    code = LsaNtStatusToWinError(LsaEnumerateAccountRights(hPolicy, sid, &rights, &count));
	    switch(code) {
	      case ERROR_SUCCESS:
		for (int i=0; i < count; i++) {
		    wprintf(L"%s\n", rights[i].Buffer);
		}
		LsaFreeMemory(rights);
		break;

	      case ERROR_FILE_NOT_FOUND:
		fprintf(stdout, "No account rights were found for user %s\n", argv[1]);
		break;

	      default:
		fprintf(stdout, "Failed to enumerate account rights: %d", code);
		break;
	    }
	    LsaClose(&hPolicy);
	    break;
	  }

	  default:
	    fprintf(stdout, "Failed to open policy: %d", code);
	    break;
	}
	break;

      case ERROR_NONE_MAPPED:
	fprintf(stdout, "Account name %s was not found\n", argv[1]);
	break;

      default:
	fprintf(stdout, "Error retrieving sid: %d", code);
	break;
    }

    return code;
}

/**
 * Get a PSID corresponding to an account name.
 */
HRESULT GetSid(LPCSTR szAccName, PSID *ppSid) {
    // Validate the input parameters.
    if (szAccName == NULL || ppSid == NULL) {
	return ERROR_INVALID_PARAMETER;
    }

    // Create buffers that may be large enough.
    // If a buffer is too small, the count parameter will be set to the size needed.
    const DWORD INITIAL_SIZE = 32;
    DWORD cbSid = 0;
    DWORD dwSidBufferSize = INITIAL_SIZE;
    DWORD cchDomainName = 0;
    DWORD dwDomainBufferSize = INITIAL_SIZE;
    TCHAR *szDomainName = NULL;
    SID_NAME_USE eSidType;
    DWORD dwErrorCode = 0;
    HRESULT hr = ERROR_SUCCESS;

    // Create buffers for the SID and the domain name.
    *ppSid = (PSID) new BYTE[dwSidBufferSize];
    if (*ppSid == NULL) {
	return ERROR_NOT_ENOUGH_MEMORY;
    }
    ZeroMemory(*ppSid, dwSidBufferSize);
    szDomainName = new TCHAR[dwDomainBufferSize];
    if (szDomainName == NULL) {
	return ERROR_NOT_ENOUGH_MEMORY;
    }
    ZeroMemory(szDomainName, dwDomainBufferSize*sizeof(TCHAR));

    // Obtain the SID for the account name passed.
    while(TRUE) {
	// Set the count variables to the buffer sizes and retrieve the SID.
	cbSid = dwSidBufferSize;
	cchDomainName = dwDomainBufferSize;
	if (LookupAccountName(NULL, szAccName, *ppSid, &cbSid, szDomainName, &cchDomainName, &eSidType)) {
	    if (IsValidSid(*ppSid) == FALSE) {
		 dwErrorCode = ERROR_INVALID_DATA;
	    }
	    break;
	}
	dwErrorCode = GetLastError();

	// Check if one of the buffers was too small.
	if (dwErrorCode == ERROR_INSUFFICIENT_BUFFER) {
	    if (cbSid > dwSidBufferSize) {
		// Reallocate memory for the SID buffer.
		FreeSid(*ppSid);
		*ppSid = (PSID) new BYTE[cbSid];
		if (*ppSid == NULL) {
		    return ERROR_NOT_ENOUGH_MEMORY;
		}
		ZeroMemory(*ppSid, cbSid);
		dwSidBufferSize = cbSid;
	    }
	    if (cchDomainName > dwDomainBufferSize) {
		// Reallocate memory for the domain name buffer.
		delete [] szDomainName;
		szDomainName = new TCHAR[cchDomainName];
		if (szDomainName == NULL) {
		    return ERROR_NOT_ENOUGH_MEMORY;
		}
		ZeroMemory(szDomainName, cchDomainName*sizeof(WCHAR));
		dwDomainBufferSize = cchDomainName;
	    }
	} else {
	    hr = HRESULT_FROM_WIN32(dwErrorCode);
	    break;
	}
    }

    delete [] szDomainName;
    return hr; 
}

