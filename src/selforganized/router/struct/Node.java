package selforganized.router.struct;

import java.io.Serializable;

import selforganized.exception.MyException;
import selforganized.exception.FormatException;

/**
 * �����ַ�Ͷ˿ڣ��൱�ڱ�ʶ����
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
	 * ���ַ��������ڵ㡣
	 * 
	 * @param s
	 *            ��ʽΪ[IPv4��ַ:�˿ں�]����"127.0.0.1:8080"
	 * @throws MyException
	 */
	public Node(String s) throws FormatException {
		String[] strings = s.split(":");
		if (strings.length != 2)
			throw new FormatException("�ڵ��ʽ������ð��");
		if (strings[0].length() == 0)// ʡ�Ե�ַ��Ĭ�ϱ���
			this.addr = localhostAddr;
		else
			this.addr = strings[0];
		this.port = Integer.parseInt(strings[1]);
	}

	/**
	 * �����ڵ㡣
	 * 
	 * @param address
	 *            �ַ�����ַ����"127.0.0.1"
	 * @param port
	 *            �˿ںš�
	 */
	public Node(String address, int port) {
		if (address.length() == 0)// ʡ�Ե�ַ��Ĭ�ϱ���
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
