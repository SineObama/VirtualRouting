package selforganized.router.struct;

import java.io.Serializable;

import selforganized.exception.MyException;
import selforganized.exception.FormatException;

/**
 * 保存地址和端口，相当于标识符。
 * 
 * @author Sine
 *
 */
public class Node implements Comparable<Node>, Serializable {
	private static final long serialVersionUID = -5267795697773959702L;
	public final static String localhostAddr = "127.0.0.1";
	public String addr;
	public int port;

	/**
	 * 以字符串创建节点。
	 * 
	 * @param s
	 *            格式为[IPv4地址:端口号]。如"127.0.0.1:8080"
	 * @throws MyException
	 */
	public Node(String s) throws FormatException {
		String[] strings = s.split(":");
		if (strings.length != 2)
			throw new FormatException("节点格式错误：无冒号");
		if (strings[0].length() == 0)// 省略地址则默认本机
			this.addr = localhostAddr;
		else
			this.addr = strings[0];
		this.port = Integer.parseInt(strings[1]);
	}

	/**
	 * 创建节点。
	 * 
	 * @param address
	 *            字符串地址。如"127.0.0.1"
	 * @param port
	 *            端口号。
	 */
	public Node(String address, int port) {
		if (address.length() == 0)// 省略地址则默认本机
			this.addr = localhostAddr;
		else
			this.addr = address;
		this.port = port;
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Node) {
			Node o = (Node) obj;
			return addr.equals(o.addr) && port == o.port;
		}
		return false;
	}

	@Override
	public String toString() {
		return addr + ":" + port;
	}
}
