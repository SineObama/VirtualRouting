package selforganized.router;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import selforganized.ObjectUtil;
import selforganized.exception.FormatException;
import selforganized.exception.MyException;
import selforganized.router.struct.DV;
import selforganized.router.struct.Distance;
import selforganized.router.struct.Node;
import selforganized.router.struct.RouteInfo;

public class DVService {

	private Node me = null;
	private DV myDV;
	private Map<Node, DV> neighborDVs = new TreeMap<>();
	private Map<Node, Distance> cost = new TreeMap<>();
	private final static DVService instance = new DVService();
	private final static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	public static DVService getInstance() {
		return instance;
	}

	public synchronized void init(File file) throws NumberFormatException, IOException, MyException {
		// TODO 初始化判断
		// 读取静态路由信息
		// 文件格式：每一行：[地址:端口] [距离（代价）]，距离为0表示自己，距离为-1表示非邻居（当前不可达）
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String lineTxt;

		// 从文件读取所有节点，记录自己合邻居，初始化距离为无穷
		final DV initDV = new DV();
		while ((lineTxt = bufferedReader.readLine()) != null) {
			String[] tokens = lineTxt.split(" ");
			if (tokens.length != 2) {
				bufferedReader.close();
				throw new FormatException("文件格式错误：无空格分隔");
			}
			Node dst = new Node(tokens[0]);
			int dis = Integer.parseInt(tokens[1]);
			if (dis == 0) { // 自己
				me = ObjectUtil.clone(dst);
			} else if (dis != -1) { // 邻居
				// 对邻居的距离向量，初始化所有节点为无穷
				cost.put(dst, new Distance(dis));
			}
			initDV.put(dst, RouteInfo.getUnreachable());
		}
		bufferedReader.close();
		if (me == null)
			throw new MyException("配置文件错误：没有本地节点");
		for (Node neighbor : cost.keySet()) {
			DV dv = ObjectUtil.clone(initDV);
			dv.replace(neighbor, new RouteInfo(neighbor, new Distance(0)));
			neighborDVs.put(neighbor, dv);
		}

		// 自己的距离向量。更新邻居的距离
		myDV = ObjectUtil.clone(initDV);
		for (Entry<Node, Distance> entry : cost.entrySet())
			myDV.replace(entry.getKey(), new RouteInfo(entry.getKey(), entry.getValue()));
		myDV.replace(me, new RouteInfo(me, 0));
		debug("初始化完成");
	}

	public Node getMe() {
		return ObjectUtil.clone(me);
	}

	public synchronized DV getDV() {
		return ObjectUtil.clone(myDV);
	}

	public synchronized RouteInfo get(Node dst) {
		return ObjectUtil.clone(myDV.get(dst));
	}

	// private synchronized void replace(Node neibour, Node dst, RouteInfo info)
	// {
	// neighborDVs.get(neibour).replace(dst, info);
	// }

	public boolean isNeighbor(Node node) {
		return cost.containsKey(node);
	}

	public Set<Node> getNeighbors() {
		return cost.keySet();
	}

	/**
	 * 从上帝视角修改自己到邻居的距离
	 * 
	 * @param neighbor
	 *            邻居节点
	 * @param dis
	 *            距离
	 * @return 自身的距离向量是否变更
	 * @throws MyException
	 */
	public synchronized boolean setDis(Node neighbor, int dis) throws MyException {
		if (!isNeighbor(neighbor))
			throw new MyException("不是邻居，无法修改");
		if (dis <= 0 && dis != -1)
			throw new MyException("邻居距离必须是正数或-1表示无穷");
		Distance newDis = new Distance(dis);
		cost.replace(neighbor, newDis);
		debug("更新自己到邻居" + neighbor + "直接距离为" + newDis);
		RouteInfo oldInfo = myDV.get(neighbor);
		RouteInfo minInfo = getMin(neighbor);
		if (oldInfo.equals(minInfo))
			return false;
		myDV.replace(neighbor, minInfo);
		debug("更新自己经由" + minInfo.next + "到" + neighbor + "的代价为" + minInfo.dis);
		return true;
	}

	public synchronized boolean refresh(Node neighbor, Entry<Node, RouteInfo> entry) throws MyException {
		Node dst = entry.getKey();
		RouteInfo newInfo = entry.getValue();
		if (!isNeighbor(neighbor))
			throw new MyException("不是邻居，无法更新");
		if (dst.equals(neighbor)) // 忽略邻居到邻居自身的条目
			return false;
		// boolean changed = false;
		final DV neighborDV = neighborDVs.get(neighbor);
		final Distance dis = newInfo.dis;

		final RouteInfo oldInfo = neighborDV.get(dst);
		if (oldInfo == null || !oldInfo.equals(newInfo)) {
			neighborDVs.get(neighbor).replace(dst, newInfo);
			debug("更新从邻居" + neighbor + "到" + dst + "的代价为" + dis);
		}

		boolean changed = false;
		final RouteInfo myInfo = myDV.get(dst);
		final Distance totalDis = Distance.add(myDV.get(neighbor).dis, dis);
		if (myInfo.dis.compareTo(totalDis) > 0) { // 代价变小
			myDV.replace(dst, new RouteInfo(neighbor, totalDis));
			debug("更新自己经由" + neighbor + "到" + dst + "的代价为" + totalDis);
		} else if (neighbor.equals(myInfo.next) && myInfo.dis.compareTo(totalDis) < 0) { // 原路径代价增加
			RouteInfo minInfo = getMin(dst);
			if (!minInfo.dis.equals(myInfo.dis)) // 需要更新距离向量
				changed = true;
			myDV.replace(dst, minInfo);
			debug("更新自己经由" + minInfo.next + "到" + dst + "的代价为" + minInfo.dis);
		}
		return changed;
	}

	/**
	 * DV算法中计算到目的节点最短路由的方法。
	 * 
	 * @param dst
	 *            目的节点
	 * @return 最短路由信息。节点不可达时返回距离为最大值的信息。
	 */
	private synchronized RouteInfo getMin(Node dst) {
		RouteInfo minInfo = RouteInfo.getUnreachable();
		for (Node neibour : getNeighbors()) { // 遍历邻居
			Distance totalDis = Distance.add(cost.get(neibour), neighborDVs.get(neibour).get(dst).dis);
			if (minInfo.dis.compareTo(totalDis) > 0)
				minInfo = new RouteInfo(neibour, totalDis);
		}
		return minInfo;
	}

	// helpers

	/**
	 * 输出路由器调试信息
	 * 
	 * @param s
	 *            要输出的调试信息
	 */
	void debug(Object s) {
		System.out.println(df.format(new Date()) + "\t" + me + "\t" + s);
	}

}
