// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.types;

import org.joval.intf.scap.oval.IType;
import org.joval.util.JOVALMsg;

/**
 * IOS version type.
 * @see http://www.cisco.com/web/about/security/intelligence/ios-ref.html
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IosVersionType extends AbstractType {
    private static final String OPEN_PEREN = "(";
    private static final String CLOSE_PEREN = ")";

    String data;

    private int majorVersion = 0, minorVersion = 0, release = 0, rebuild = 0;
    private String mainlineRebuild = null, trainId = null, subrebuild = null;

    public IosVersionType(String data) throws IllegalArgumentException {
	this.data = data;
	int begin = 0;
	int end = data.indexOf(".");
	if (end != -1) {
	    majorVersion = Integer.parseInt(data.substring(begin, end));
	}
	begin = end + 1;
	end = data.indexOf(OPEN_PEREN);
	if (end == -1) {
	    end = data.indexOf(".", begin);
	}
	if (begin > 0 && end > begin) {
	    minorVersion = Integer.parseInt(data.substring(begin, end));
	}
	begin = end + 1;
	end = data.indexOf(CLOSE_PEREN);
	if (begin > 0 && end > begin) {
	    StringBuffer sb = new StringBuffer();
	    for (int i=begin; i < end; i++) {
		char ch = data.charAt(i);
		if (isNum(ch) || ch == '.') {
		    sb.append(ch);
		} else {
		    break;
		}
	    }
	    if (sb.length() > 0) {
		release = Integer.parseInt(sb.toString());

		begin += sb.length();
		if (begin < end) {
		    mainlineRebuild = data.substring(begin, end);
		}
	    }
	}
	if (end > 0) {
	    begin = end + 1;
	    end = data.length();
	    StringBuffer sb = new StringBuffer();
	    for (int i=begin; i < end; i++) {
		char ch = data.charAt(i);
		if (isNum(ch)) {
		    break;
		} else {
		    sb.append(ch);
		}
	    }
	    if (sb.length() > 0) {
		trainId = sb.toString();

		begin += sb.length();
		if (begin < end) {
		    sb = new StringBuffer();
		    for (int i=begin; i < end; i++) {
			char ch = data.charAt(i);
			if (isNum(ch)) {
			    sb.append(ch);
			} else {
			    break;
			}
		    }
		    if (sb.length() > 0) {
			rebuild = Integer.parseInt(sb.toString());
		    }

		    begin += sb.length();
		    if (begin < end) {
			subrebuild = data.substring(begin, end);
		    }
		}
	    }
	} else {
	    end = data.length();
	    rebuild = Integer.parseInt(data.substring(begin, end));
	}
    }

    public String getData() {
	return data;
    }

    public int getMajorVersion() {
	return majorVersion;
    }

    public int getMinorVersion() {
	return minorVersion;
    }

    public int getRelease() {
	return release;
    }

    public int getRebuild() {
	return rebuild;
    }

    public String getMainlineRebuild() {
	return mainlineRebuild;
    }

    public String getTrainIdentifier() {
	return trainId;
    }

    public String getSubrebuild() {
	return subrebuild;
    }

    // Implement IType

    public Type getType() {
	return Type.IOS_VERSION;
    }

    public String getString() {
	StringBuffer sb = new StringBuffer();
	sb.append(Integer.toString(majorVersion));
	sb.append(".");
	sb.append(Integer.toString(minorVersion));
	if (release == 0 && mainlineRebuild == null) {
	    sb.append(".").append(rebuild);
	} else {
	    sb.append(OPEN_PEREN);
	    sb.append(Integer.toString(release));
	    if (mainlineRebuild != null) {
		sb.append(mainlineRebuild);
	    }
	    sb.append(CLOSE_PEREN);
	    if (trainId != null) {
		sb.append(trainId);
		if (rebuild > 0) {
		    sb.append(Integer.toString(rebuild));
		    if (subrebuild != null) {
			sb.append(subrebuild);
		    }
		}
	    }
	}
	return sb.toString();
    }

    // Implement Comparable

    public int compareTo(IType t) {
	IosVersionType other = null;
	try {
	    other = (IosVersionType)t.cast(getType());
	    if (trainId == null) {
		if (other.trainId != null) {
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_TRAIN_COMPARISON, trainId, other.trainId);
		    throw new IllegalArgumentException(msg);
		}
	    } else if (!trainId.equals(other.trainId)) {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_TRAIN_COMPARISON, trainId, other.trainId);
		throw new IllegalArgumentException(msg);
	    }
	    if (majorVersion != other.majorVersion) {
		return majorVersion - other.majorVersion;
	    }
	    if (minorVersion != other.minorVersion) {
		return minorVersion - other.minorVersion;
	    }
	    if (release != other.release) {
		return release - other.release;
	    }
	    if (mainlineRebuild != null) {
		if (other.mainlineRebuild == null) {
		    return -1;
		} else {
		    int i = mainlineRebuild.compareTo(other.mainlineRebuild);
		    if (i != 0) return i;
		}
	    } else if (other.mainlineRebuild != null) {
		return 1;
	    }
	    if (rebuild != other.rebuild) {
		return rebuild - other.rebuild;
	    }
	    if (subrebuild != null) {
		if (other.subrebuild == null) {
		    return -1;
		} else {
		    int i = subrebuild.compareTo(other.subrebuild);
		    if (i != 0) return i;
		}
	    } else if (other.subrebuild != null) {
		return 1;
	    }
	    return 0;
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
    }

    // Private

    private boolean isNum(char ch) {
	return ch >= '0' && ch <= '9';
    }
}
