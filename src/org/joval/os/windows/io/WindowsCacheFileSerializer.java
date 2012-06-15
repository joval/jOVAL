package org.joval.os.windows.io;

import java.io.DataInput;
import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.io.fs.CacheFileSerializer;

class WindowsCacheFileSerializer extends CacheFileSerializer {
    private transient WindowsFilesystem fs;

    WindowsCacheFileSerializer(WindowsFilesystem fs) {
	super(fs);
	this.fs = fs;
    }

    @Override
    public IFile deserialize(DataInput in) throws IOException {
	String path = in.readUTF();
	return new WindowsFile(fs, new WindowsFileInfo(in), path);
    }
}
