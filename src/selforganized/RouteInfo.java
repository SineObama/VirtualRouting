package selforganized;

import java.io.Serializable;

/**
 * 包括下一跳和距离
 * 
 * @author Sine
 *
 */
@SuppressWarnings("serial")
public class RouteInfo implements Serializable {
	public Node next;
	public int dis;

	public RouteInfo(RouteInfo o) {
		next = new Node(o.next);
		dis = o.dis;
	}

	public RouteInfo(Node next, int distance) {
		this.next = new Node(next);
		this.dis = distance;
	}

	public boolean equals(RouteInfo o) {
		return next.equals(o.next) && dis == o.dis;
	}

	@Override
	public String toString() {
		return "(" + next + "," + dis + ")";
	}
}
