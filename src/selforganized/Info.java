package selforganized;

import java.io.Serializable;

/**
 * 地址和端口，相当于标识符。
 */
@SuppressWarnings("serial")
public class Info implements Comparable<Info>, Serializable {
	public String address;
	public int port;

	public Info(String address, int port) {
		this.address = address;
		this.port = port;
	}

	public Info(Info info) {
		address = info.address;
		port = info.port;
	}

	@Override
	public int compareTo(Info o) {
		int i = address.compareTo(o.address);
		if (i != 0)
			return i;
		return new Integer(port).compareTo(o.port);
	}

	public boolean equals(Info o) {
		return address.equals(o.address) && port == o.port;
	}

	@Override
	public String toString() {
		return address + ":" + port;
	}
}
