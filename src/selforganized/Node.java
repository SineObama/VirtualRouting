package selforganized;

import java.io.Serializable;

/**
 * ��ַ�Ͷ˿ڣ��൱�ڱ�ʶ����
 * 
 * @author Sine
 *
 */
@SuppressWarnings("serial")
public class Node implements Comparable<Node>, Serializable {
	public String addr;
	public int port;

	/**
	 * ���ַ��������ڵ㡣
	 * 
	 * @param s
	 *            ��ʽΪ[IPv4��ַ:�˿ں�]����"127.0.0.1:8080"
	 * @throws MyException
	 */
	public Node(String s) throws MyException {
		String[] strings = s.split(":");
		if (strings.length != 2)
			throw new MyException("�ڵ��ʽ����");
		addr = strings[0];
		port = Integer.parseInt(strings[1]);
	}

	/**
	 * �����ڵ㡣
	 * @param address �ַ�����ַ����"127.0.0.1"
	 * @param port �˿ںš�
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
