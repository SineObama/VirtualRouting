package selforganized.router.struct;

import java.io.Serializable;

import selforganized.ObjectUtil;

/**
 * 路由信息。包括下一跳和距离
 * 
 * @author Sine
 *
 */
public class RouteInfo implements Serializable, Comparable<RouteInfo> {
	private static final long serialVersionUID = 1944450231426532349L;
	public Node next;
	public Distance dis;

	public RouteInfo(Node next, int dis) {
		this.next = ObjectUtil.clone(next);
		this.dis = new Distance(dis);
	}

	public RouteInfo(Node next, Distance dis) {
		this.next = ObjectUtil.clone(next);
		this.dis = ObjectUtil.clone(dis);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof RouteInfo) {
			RouteInfo o = (RouteInfo) obj;
			if (next == null)
				return o.next == null && dis.equals(o.dis);
			return next.equals(o.next) && dis.equals(o.dis);
		}
		return false;
	}

	@Override
	public String toString() {
		return "(" + next + "," + dis + ")";
	}

	public static RouteInfo getUnreachable() {
		return new RouteInfo(null, Distance.getUnreachable());
	}

	@Override
	public int compareTo(RouteInfo o) {
		return dis.compareTo(o.dis);
	}

	public boolean isUnreachable() {
		return dis.isUnreachable();
	}
}
