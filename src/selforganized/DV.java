package selforganized;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 *  æ‡¿ÎœÚ¡ø
 * @author Sine
 *
 */
public class DV implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Node node;
	Map<Node, RouteInfo> infos = new TreeMap<>();

	public DV() {
	}

	public DV(Node node) {
		this.node = new Node(node);
	}
}
