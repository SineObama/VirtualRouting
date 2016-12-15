package selforganized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import java.util.TreeMap;

public class Router extends Thread {

	private Map<Node, Integer> cost = new TreeMap<>();
	private Node me;
	private Map<Node, DV> dvs = new TreeMap<>();

	private final File file;
	private Sender sender;
	private final static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	public Router(File file) {
		this.file = file;
		// TODO final Sender ?
	}

	@Override
	public void run() {

		// 读取静态路由地址端口和距离
		// 文件格式：每一行：[地址:端口] [距离（代价）]，距离为0表示自己，距离为-1表示非邻居
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String lineTxt;

			// 从文件读取所有节点，初始化距离为无穷
			// FIXME 每个表中都含有自己到自己的条目，代价无穷
			final RouteInfo infinite = new RouteInfo(null, Integer.MAX_VALUE);
			TreeMap<Node, RouteInfo> initMap = new TreeMap<>();
			while ((lineTxt = bufferedReader.readLine()) != null) {
				String[] strings = lineTxt.split(" ");
				if (strings.length != 2) {
					bufferedReader.close();
					throw new MyException("文件格式错误");
				}
				Node des = new Node(strings[0]);
				int distance = Integer.parseInt(strings[1]);
				if (distance == 0) { // 自己
					me = new Node(des);
				} else if (distance != -1) { // 邻居
					dvs.put(des, new DV(des));
					cost.put(des, distance);
				}
				initMap.put(des, new RouteInfo(infinite));
			}
			bufferedReader.close();

			// 对邻居的距离向量，初始化所有节点为无穷
			for (DV dv : dvs.values())
				dv.infos = ObjectUtil.clone(initMap);

			// 自己的距离向量。更新邻居的距离
			DV mydv = new DV(me);
			mydv.infos = new TreeMap<>(initMap);
			for (Entry<Node, RouteInfo> entry : mydv.infos.entrySet()) {
				Node des = entry.getKey();
				Integer distance = cost.get(des);
				if (distance != null) { // 是邻居
					RouteInfo info = entry.getValue();
					info.dis = distance;
					info.next = des;
				}
			}
			dvs.put(me, mydv);

			debug("初始化完成");

			// 创建发送线程
			sender = new Sender(this);
			sender.start();

			// 开始监听
			ServerSocket serverSocket = new ServerSocket(me.port);
			while (true) {
				Object obj = ObjectUtil.receive(serverSocket);
				if (obj instanceof DV) {
					DV dv = (DV) obj;
					// debug("收到来自" + dv.node + "的距离向量");
					synchronized (sender) {
						refresh(dv);
					}
				} else if (obj instanceof Message) {
					Message message = (Message) obj;
					debug("收到由" + message.sender + "转发的报文");
					if (message.dst.equals(me)) {
						Client.sysout("收到来自" + message.src + "的报文：\n" + message.text);
						continue;
					}
					message.sender = new Node(me);
					debug(forward(message));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		debug("路由器已停止");
	}

	/**
	 * 发送信息到另一个节点
	 * 
	 * @param dst
	 *            接收节点
	 * @param msg
	 *            信息
	 * @return 反馈信息
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public String send(Node dst, String msg) {
		Message message = new Message();
		message.src = me;
		message.dst = dst;
		message.sender = me;
		message.text = msg;
		return forward(message);
	}

	private String forward(Message message) {
		Node neibour = null;
		while (true) {
			try {
				RouteInfo info = getDV().infos.get(message.dst);
				if (info == null || info.dis == Integer.MAX_VALUE)
					return "此节点目前不可达。报文取消发送";
				neibour = info.next;
				ObjectUtil.send(neibour, message);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				cost.replace(neibour, Integer.MAX_VALUE);
				debug("邻居" + neibour + "变为不可达。将查找其他路径");
				RouteInfo minInfo = getMin(message.dst);
				try {
					getDV().infos.replace(message.dst, new RouteInfo(minInfo));
				} catch (MyException e1) {
					return "内部严重错误";
				}
				continue;
			}
			break;
		}
		return "成功发送报文到下一节点：" + neibour;
	}

	private RouteInfo getMin(Node dst) {
		RouteInfo minInfo = new RouteInfo(null, Integer.MAX_VALUE);
		synchronized (cost) {
			for (Entry<Node, Integer> entry : cost.entrySet()) { // 遍历邻居
				Node neibour = entry.getKey();
				DV neibourDV = dvs.get(neibour);
				int totalDis = entry.getValue() + neibourDV.infos.get(dst).dis;
				if (totalDis < 0) // 溢出表明不可达
					totalDis = Integer.MAX_VALUE;
				if (minInfo.dis > totalDis) {
					minInfo.dis = totalDis;
					minInfo.next = new Node(neibour);
				}
			}
		}
		return minInfo;
	}

	private void refresh(final DV dv) throws MyException {
		final Node neibour = dv.node;
		int tem;
		synchronized (cost) {
			tem = cost.get(neibour);
		}
		final int disToNeibour = tem;
		DV myDv = getDV();
		DV neibourDV = dvs.get(neibour);
		boolean changed = false;

		for (final Entry<Node, RouteInfo> entry : dv.infos.entrySet()) {
			final Node dst = entry.getKey();
			final RouteInfo newInfo = entry.getValue();
			final int dis = newInfo.dis;
			if (dst.equals(neibour)) // 忽略邻居到邻居自身的条目
				continue;

			// 单独处理邻居到自己的代价更新
			if (dst.equals(me)) {
				if (disToNeibour != dis) {
					synchronized (cost) {
						cost.replace(neibour, dis);
					}
					myDv.infos.get(neibour).dis = dis;
					neibourDV.infos.replace(me, new RouteInfo(newInfo.next, dis));
					changed = true;
					debug("更新邻居" + neibour + "和自己之间的代价为" + showCost(dis));
				}
				continue;
			}

			// 更新邻居的距离向量
			final RouteInfo oldInfo = neibourDV.infos.get(dst);
			if (oldInfo != null && oldInfo.equals(newInfo))
				continue;

			// 添加或更改
			neibourDV.infos.replace(dst, new RouteInfo(newInfo));
			debug("更新从邻居" + neibour + "到" + dst + "的代价为" + showCost(dis));

			// 检查自身的距离向量是否需要更新。分2种情况
			final RouteInfo myInfo = myDv.infos.get(dst);

			int totalDis = disToNeibour + dis;
			if (totalDis < 0) // 溢出表明不可达
				totalDis = Integer.MAX_VALUE;

			if (myInfo.dis > totalDis) { // 代价变小
				myInfo.dis = totalDis;
				myInfo.next = new Node(neibour);
				debug("更新自己经由" + myInfo.next + "到" + dst + "的代价为" + showCost(myInfo.dis));
			} else if (myInfo.next.equals(neibour) && myInfo.dis < totalDis) { // 原路径代价增加
				RouteInfo minInfo = getMin(dst);
				if (minInfo.dis != myInfo.dis) // 需要更新距离向量
					changed = true;
				myInfo.dis = minInfo.dis;
				myInfo.next = new Node(minInfo.next);
				debug("更新自己经由" + myInfo.next + "到" + dst + "的代价为" + myInfo.dis);
			}
		}

		// 路由表改变后立即发送最新路由表给邻居
		if (changed)
			sender.notify();
	}

	Set<Node> getNeibours() {
		return dvs.keySet();
	}

	/**
	 * 获取路由器自己的距离向量
	 * 
	 * @return 路由器自己的距离向量
	 */
	DV getDV() {
		return dvs.get(me);
	}

	/**
	 * 获取路由器的节点信息
	 * 
	 * @return 路由器的节点信息
	 */
	Node getMe() {
		return me;
	}

	/**
	 * 输出路由器调试信息
	 * 
	 * @param s
	 *            要输出的调试信息
	 */
	void debug(String s) {
		System.out.println(df.format(new Date()) + "\t" + me + "\t" + s);
	}

	private static String showCost(int cost) {
		return cost == Integer.MAX_VALUE ? "无穷(不可达)" : Integer.toString(cost);
	}

}
