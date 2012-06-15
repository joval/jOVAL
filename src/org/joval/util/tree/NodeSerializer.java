package org.joval.util.tree;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.HashSet;

import org.apache.jdbm.Serializer;

import org.joval.intf.util.tree.INode;
import org.joval.util.StringTools;

public class NodeSerializer implements Serializer<Node>, Serializable {
    private static final byte[] NULLBYTE = {0x00};
    private static final String NULL = new String(NULLBYTE, StringTools.ASCII);

    private transient Tree tree;

    NodeSerializer() {}

    void setTree(Tree tree) {
	this.tree = tree;
    }

    // Implement Serializer

    public Node deserialize(DataInput in) throws IOException {
	String temp = in.readUTF(); // name
	Node node = new Node(temp);
	node.type = INode.Type.fromVal(in.readShort());

	if (!NULL.equals(temp = in.readUTF())) {
	    node.parentPath = temp;
	}
	if (!NULL.equals(temp = in.readUTF())) {
	    node.path = temp;
	}
	if (!NULL.equals(temp = in.readUTF())) {
	    node.canonPath = temp;
	}
	node.isLinkChild = in.readBoolean();

	switch(node.type) {
	  case LINK:
	    node.linkPath = in.readUTF();
	    // fall-thru
	  case BRANCH:
	  case TREE:
	    if (in.readBoolean()) { // hasChildren
		node.children = new HashSet<String>();
		int numChildren = in.readInt();
		for (int i=0; i < numChildren; i++) {
		    node.children.add(in.readUTF());
		}
	    }
	    break;
	  case FOREST:
	    throw new IOException("FOREST: " + node.getName());
	}

	node.tree = tree;
	return node;
    }

    public void serialize(DataOutput out, Node node) throws IOException {
	out.writeUTF(node.name);
	out.writeShort(node.type.getVal());
	if (node.parentPath == null) {
	    out.writeUTF(NULL);
	} else {
	    out.writeUTF(node.parentPath);
	}
	if (node.path == null) {
	    out.writeUTF(NULL);
	} else {
	    out.writeUTF(node.path);
	}
	if (node.canonPath == null) {
	    out.writeUTF(NULL);
	} else {
	    out.writeUTF(node.canonPath);
	}
	out.writeBoolean(node.isLinkChild);
	switch(node.type) {
	  case UNRESOLVED:
	  case LEAF:
	    break;
	  case LINK:
	    out.writeUTF(node.linkPath);
	    // fall-thru
	  case BRANCH:
	  case TREE:
	    if (node.children == null) {
		out.writeBoolean(false);
	    } else {
		out.writeBoolean(true);
		out.writeInt(node.children.size());
		for (String child : node.children) {
		    out.writeUTF(child);
		}
	    }
	    break;
	  case FOREST:
	    throw new NotSerializableException(node.getClass().getName());
	}
    }
}
