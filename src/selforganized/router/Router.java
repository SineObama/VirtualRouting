package selforganized.router;

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

import selforganized.Client;
import selforganized.ObjectUtil;
import selforganized.exception.FormatException;
import selforganized.exception.MyException;
import selforganized.router.struct.DV;
import selforganized.router.struct.Distance;
import selforganized.router.struct.Message;
import selforganized.router.struct.Node;
import selforganized.router.struct.RouteInfo;

import java.util.TreeMap;

public class Router extends Thread implements IRouter {

	private final Sender sender;
	final static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
	private final static DVdao dao = DVdao.getInstance();

	public Router(File file) throws NumberFormatException, FormatException, IOException {

		// 读取静态路由信息
		// 文件格式：每一行：[地址:端口] [距离（代价）]，距离为0表示自己，距离为-1表示非邻居（当前不可达）
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String lineTxt;

		// 从文件读取所有节点，初始化距离为无穷
		// FIXME 每个表中都含有自己到自己的条目，代价无穷
		Map<Node, Integer> cost = new TreeMap<>();
		DV initDV = new DV();
		Node me = null;
		while ((lineTxt = bufferedReader.readLine()) != null) {
			String[] strings = lineTxt.split(" ");
			if (strings.length != 2) {
				bufferedReader.close();
				throw new FormatException("文件格式错误：无空格分隔");
			}
			Node dst = new Node(strings[0]);
			int distance = Integer.parseInt(strings[1]);
			if (distance == 0) { // 自己
				me = ObjectUtil.clone(dst);
			} else if (distance != -1) { // 邻居
				// 对邻居的距离向量，初始化所有节点为无穷
				cost.put(dst, distance);
			}
			initDV.put(dst, RouteInfo.getUnreachable());
		}
		bufferedReader.close();
		for (Node neibour : cost.keySet())
			dao.add(neibour, ObjectUtil.clone(initDV));
		dao.setMe(me);

		// 自己的距离向量。更新邻居的距离
		DV myDV = ObjectUtil.clone(initDV);
		for (Entry<Node, Integer> entry : cost.entrySet())
			myDV.replace(entry.getKey(), new RouteInfo(entry.getKey(), entry.getValue()));
		dao.add(me, myDV);
		// 创建发送线程
		sender = new Sender(this);
		sender.start();
		debug("初始化完成");
	}

	@Override
	public void run() {
		sender.notify();

		// 开始监听
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(dao.getMe().port);
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
					if (message.dst.equals(dao.getMe())) {
						Client.sysout("收到来自" + message.src + "的报文：\n" + message.text);
						continue;
					}
					message.sender = dao.getMe();
					debug(forward(message));
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			debug("路由器已停止");
		}
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
	@Override
	public String send(Node dst, String msg) {
		Message message = new Message();
		message.src = dao.getMe();
		message.dst = dst;
		message.sender = dao.getMe();
		message.text = msg;
		return forward(message);
	}

	private String forward(Message message) {
		Node neibour = null;
		while (true) {
			try {
				RouteInfo info = dao.get(message.dst);
				if (info == null || info.isUnreachable())
					return "此节点目前不可达。报文取消发送";
				neibour = info.next;
				ObjectUtil.send(neibour, message);
				// } catch (UnknownHostException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
			} catch (IOException e) {
				debug("邻居" + neibour + "变为不可达。将查找其他路径");
				RouteInfo info = getMin(message.dst);
				neibour = info.next;
				dao.replace(message.dst, info);
				continue;
			}
			break;
		}
		return "成功发送报文到下一节点：" + neibour;
	}

	/**
	 * DV算法中计算到目的节点最短路由的方法。
	 * 
	 * @param dst
	 *            目的节点
	 * @return 最短路由信息。节点不可达时返回距离为最大值的信息。
	 */
	private RouteInfo getMin(Node dst) {
		RouteInfo minInfo = RouteInfo.getUnreachable();
		for (Node neibour : dao.getNeibour()) { // 遍历邻居
			Distance totalDis = Distance.add(dao.get(neibour).dis, dao.get(neibour, dst).dis);
			if (minInfo.dis.compareTo(totalDis) > 0)
				minInfo = new RouteInfo(neibour, totalDis);
		}
		return minInfo;
	}

	private void refresh(final Node neibour, final DV dv) throws MyException {
		final Distance disToNeibour = dao.get(neibour).dis;
		DV myDv = dao.getDV();
		DV neibourDV = dao.getDV(neibour);
		boolean changed = false;

		for (final Entry<Node, RouteInfo> entry : dv.entrySet()) {
			final Node dst = entry.getKey();
			final RouteInfo newInfo = entry.getValue();
			final Distance dis = newInfo.dis;
			if (dst.equals(neibour)) // 忽略邻居到邻居自身的条目
				continue;

			// 单独处理邻居到自己的代价更新
			if (dst.equals(dao.getMe())) {
				if (disToNeibour.equals(dis)) {
					dao.replace(neibour, dst, newInfo);
					dao.replace(neibour, dis);
					changed = true;
					debug("更新邻居" + neibour + "和自己之间的代价为" + dis));
				}
				continue;
			}

			// 更新邻居的距离向量
			final RouteInfo oldInfo = neibourDV.get(dst);
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

	@Override
	public void change(Node neibour, int dis) {
		// TODO Auto-generated method stub

	}

	// helpers

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