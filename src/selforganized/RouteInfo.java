package selforganized;

import java.io.Serializable;

/**
 * ������һ���;���
 * 
 * @author Sine
 *
 */
@SuppressWarnings("serial")
public class RouteInfo implements Serializable {
	public Node next;
	public int dis;

	public RouteInfo(RouteInfo o) throws MyException {
		if (o == null)
			throw new MyException("·����Ϣ����Ϊ��");
		next = new Node(o.next);
		dis = o.dis;
	}

	public RouteInfo(Node next, int distance) {
		this.next = new Node(next);
		this.dis = distance;
	}

	public boolean equals(RouteInfo o) {
		if (o == null)
			return false;
		return next.equals(o.next) && dis == o.dis;
	}

	@Override
	public String toString() {
		return "(" + next + "," + dis + ")";
	}
}
