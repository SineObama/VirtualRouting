package selforganized;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("serial")
public class RouteTable implements Serializable {
	public Map<Info, RouteInfo> infos = new TreeMap<>();
	public Info me;
}
