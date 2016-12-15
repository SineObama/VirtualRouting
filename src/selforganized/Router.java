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
					if (message.dst.equals(me)) {
						Client.sysout("收到来自" + message.src + "的报文：\n" + message.text);
						continue;
					}
					debug("收到来自" + message.sender + "转发的报文");
					Node next = getDV().infos.get(message.dst).next;
					if (next == null) {
						debug("路由表中没有节点" + message.dst + "的信息，无法发送");
						continue;
					}
					message.sender = me;
					ObjectUtil.send(next, message);
					debug("成功转发来自" + message.sender + "的报文到" + next);
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
	 * @param node
	 *            接收节点
	 * @param msg
	 *            信息
	 * @return 反馈信息
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public String send(Node node, String msg) throws UnknownHostException, IOException {
		RouteInfo info = getDV().infos.get(node);
		Node next = info.next;
		if (next == null)
			return "路由表中没有此节点信息，无法发送";
		if (info.dis == Integer.MAX_VALUE)
			return "此节点不可达";
		Message message = new Message();
		message.src = me;
		message.dst = node;
		message.sender = me;
		message.text = msg;
		ObjectUtil.send(next, message);
		return "成功发送报文到下一节点：" + next;
	}

	void refresh(DV dv) throws MyException {
		final Node neibour = dv.node;
		final int disToNeibour = cost.get(neibour);
		DV myDv = dvs.get(me);
		DV neibourDV = dvs.get(neibour);
		boolean changed = false;

		for (final Entry<Node, RouteInfo> entry : dv.infos.entrySet()) {
			final Node des = entry.getKey();
			if (des.equals(neibour))
				continue;
			final RouteInfo newHis = entry.getValue();
			final int newDis = newHis.dis;

			// 单独处理邻居到自己的代价更新
			if (des.equals(me)) {
				if (disToNeibour != newDis) {
					// FIXME Integer值的修改？
					cost.remove(neibour);
					cost.put(neibour, newDis);

					final RouteInfo go = myDv.infos.get(neibour);
					go.dis = newDis;

					final RouteInfo back = neibourDV.infos.get(me);
					back.dis = newDis;
					back.next = new Node(newHis.next);

					changed = true;
					debug("更新邻居" + neibour + "到自己的代价为" + newDis);
				}
				continue;
			}

			// 更新邻居的距离向量
			final RouteInfo his = neibourDV.infos.get(des);
			if (his == null) { // 添加
				neibourDV.infos.put(des, new RouteInfo(newHis));
				debug("添加来自" + neibour + "到" + des + "的代价为" + newDis);
			} else if (!his.equals(newHis)) { // 更改

				his.dis = newDis;
				his.next = new Node(newHis.next);
				debug("更新邻居" + neibour + "到" + des + "的代价为" + newDis);
				// 再检查自身的距离向量是否需要更新
				final RouteInfo my = myDv.infos.get(des);

				int totalDis = disToNeibour + newDis;
				if (totalDis < 0) // 溢出表明不可达
					totalDis = Integer.MAX_VALUE;

				if (my.dis > totalDis) { // 代价变小
					my.dis = totalDis;
					my.next = new Node(neibour);
					debug("更新经由" + my.next + "到" + des + "的代价为" + my.dis);
				} else if (my.next.equals(neibour) && my.dis < totalDis) { // 原路径代价增加
					int src = my.dis;
					my.dis = totalDis;
					for (Entry<Node, Integer> n : cost.entrySet()) { // 遍历邻居
						DV curDV = dvs.get(n.getKey());
						int curDis = n.getValue() + curDV.infos.get(des).dis;
						if (curDis < 0) // 溢出表明不可达
							curDis = Integer.MAX_VALUE;
						if (my.dis > curDis) {
							my.dis = curDis;
							my.next = new Node(n.getKey());
						}
					}
					debug("更新经由" + my.next + "到" + des + "的代价为" + my.dis);
					if (src != my.dis) // 需要更新距离向量
						changed = true;
				}

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

}
