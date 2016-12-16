package selforganized.router;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import selforganized.ObjectUtil;
import selforganized.router.struct.DV;
import selforganized.router.struct.Distance;
import selforganized.router.struct.Node;
import selforganized.router.struct.RouteInfo;

public class DVdao {

	private Node me = null;
	private boolean hadSet = false;
	private DV myDv;
	private Map<Node, DV> neibourDVs = new TreeMap<>();
	private static DVdao instance = new DVdao();

	public static DVdao getInstance() {
		return instance;
	}

	public void setMe(Node node) {
		if (!hadSet) {
			hadSet = true;
			this.me = node;
		}
	}

	public Node getMe() {
		return ObjectUtil.clone(me);
	}

	public synchronized void add(Node neibour, DV dv) {
		neibourDVs.put(neibour, dv);
	}

	public synchronized DV getDV() {
		return ObjectUtil.clone(myDv);
	}

	public synchronized void setDV(DV dv) {
		myDv = dv;
	}

	public synchronized DV getDV(Node node) {
		return ObjectUtil.clone(neibourDVs.get(node));
	}

	public synchronized RouteInfo get(Node dst) {
		return ObjectUtil.clone(myDv.get(dst));
	}

	public synchronized RouteInfo get(Node neibour, Node dst) {
		return ObjectUtil.clone(neibourDVs.get(neibour).get(dst));
	}

	public synchronized void replace(Node dst, Distance dis) {
		myDv.get(dst).dis = dis;
	}

	public synchronized void replace(Node dst, RouteInfo info) {
		myDv.replace(dst, info);
	}

	public synchronized void replace(Node neibour, Node dst, RouteInfo info) {
		neibourDVs.get(neibour).replace(dst, info);
	}

	public synchronized Set<Node> getNeibours() {
		return neibourDVs.keySet();
	}
}
