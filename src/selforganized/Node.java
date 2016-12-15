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

	/**
	 * 以字符串创建节点。
	 * 
	 * @param s
	 *            格式为[IPv4地址:端口号]。如"127.0.0.1:8080"
	 * @throws MyException
	 */
	public Node(String s) throws MyException {
		String[] strings = s.split(":");
		if (strings.length != 2)
			throw new MyException("节点格式错误");
		addr = strings[0];
		port = Integer.parseInt(strings[1]);
	}

	/**
	 * 创建节点。
	 * @param address 字符串地址。如"127.0.0.1"
	 * @param port 端口号。
	 */
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
		if (o == null)
			return 1;
		int i = addr.compareTo(o.addr);
		if (i != 0)
			return i;
		return new Integer(port).compareTo(o.port);
	}

	public boolean equals(Node o) {
		if (o == null)
			return false;
		return addr.equals(o.addr) && port == o.port;
	}

	@Override
	public String toString() {
		return addr + ":" + port;
	}
}
