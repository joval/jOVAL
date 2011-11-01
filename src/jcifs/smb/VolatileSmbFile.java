// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package jcifs.smb;

import java.net.MalformedURLException;

/**
 * This is just an SmbFile that doesn't cache information about its length or existence.  This is necessary for jOVAL's TailDashF
 * class to work properly.
 *
 * The alternative was to set the jCIFS property jcifs.smb.client.attrExpirationPeriod to 0, but that setting operates
 * statically on ALL instances of SmbFile, which is very undesirable.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VolatileSmbFile extends SmbFile {
    public VolatileSmbFile(String url, NtlmPasswordAuthentication auth) throws MalformedURLException {
	super(url, auth);
    }

    public long length() throws SmbException {
	long size = 0L;
        if (getType() == TYPE_SHARE) {
            Trans2QueryFSInformationResponse response;
            int level = Trans2QueryFSInformationResponse.SMB_INFO_ALLOCATION;

            response = new Trans2QueryFSInformationResponse(level);
            send(new Trans2QueryFSInformation(level), response);

            size = response.info.getCapacity();
        } else if (getUncPath0().length() > 1 && type != TYPE_NAMED_PIPE) {
            Info info = queryPath(getUncPath0(), Trans2QueryPathInformationResponse.SMB_QUERY_FILE_STANDARD_INFO);
            size = info.getSize();
        } else {
            size = 0L;
        }
        return size;
    }

    public boolean exists() throws SmbException {
        try {
            if (url.getHost().length() == 0) {
            } else if (getUncPath0().length() == 1) {
                connect0(); // treeConnect is good enough
            } else {
                queryPath(getUncPath0(), Trans2QueryPathInformationResponse.SMB_QUERY_FILE_BASIC_INFO);
            }

            return true;
        } catch (SmbException e) {
            switch (e.getNtStatus()) {
                case NtStatus.NT_STATUS_NO_SUCH_FILE:
                case NtStatus.NT_STATUS_OBJECT_NAME_INVALID:
                case NtStatus.NT_STATUS_OBJECT_NAME_NOT_FOUND:
                case NtStatus.NT_STATUS_OBJECT_PATH_NOT_FOUND:
                    break;
                default:
                    throw e;
            }
        }

        return false;
    }
}
