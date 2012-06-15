package org.joval.os.unix.io;

import java.io.DataInput;
import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.io.fs.CacheFileSerializer;

class UnixCacheFileSerializer extends CacheFileSerializer {
    private transient UnixFilesystem fs;

    UnixCacheFileSerializer(UnixFilesystem fs) {
	super(fs);
	this.fs = fs;
    }

    @Override
    public IFile deserialize(DataInput in) throws IOException {
	String path = in.readUTF();
	return new UnixFile(fs, new UnixFileInfo(in), path);
    }
}
