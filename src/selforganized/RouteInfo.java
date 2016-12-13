package selforganized;

import java.io.Serializable;

/**
 * 包括下一跳和距离
 */
@SuppressWarnings("serial")
public class RouteInfo implements Serializable {
	public Info next;
	public int distance;

	public RouteInfo(Info next, int distance) {
		this.next = new Info(next);
		this.distance = distance;
	}
}
