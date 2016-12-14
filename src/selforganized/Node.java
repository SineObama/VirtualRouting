package selforganized;

import java.io.Serializable;

/**
 * 地址和端口，相当于标识符。
 * 
 * @author Sine
 *
 */
@SuppressWarnings("serial")
public class Node implements Comparable<Node>, Serializable {
	public String addr;
	public int port;

	public Node(String address, int port) {
		this.addr = address;
		this.port = port;
	}

	public Node(Node info) {
		if (info == null) {
			addr = "";
			port = 0;
		} else {
			addr = info.addr;
			port = info.port;
		}
	}

	@Override
	public int compareTo(Node o) {
		int i = addr.compareTo(o.addr);
		if (i != 0)
			return i;
		return new Integer(port).compareTo(o.port);
	}

	public boolean equals(Node o) {
		return addr.equals(o.addr) && port == o.port;
	}

	@Override
	public String toString() {
		return addr + ":" + port;
	}
}
