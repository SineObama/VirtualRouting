package selforganized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Router extends Thread {

	Map<Node, Integer> cost = new TreeMap<>();
	Node me;
	Map<Node, DV> dvs = new TreeMap<>();

	File file;
	Sender sender;
	final static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	public Router(File file) {
		this.file = file;
		// TODO final Sender ?
	}

	@Override
	public void run() {

		// 读取静态路由地址端口和距离
		// 文件格式：每一行是地址、端口和距离，距离为0表示自己，距离为-1表示非邻居
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream(file));
			BufferedReader bufferedReader = new BufferedReader(read);
			String lineTxt;

			// 初始化距离为无穷。从文件读取所有节点
			// FIXME 每个表中都含有自己到自己的条目，代价无穷
			final RouteInfo infinite = new RouteInfo(null, Integer.MAX_VALUE);
			TreeMap<Node, RouteInfo> initMap = new TreeMap<>();
			while ((lineTxt = bufferedReader.readLine()) != null) {
				String[] strings = lineTxt.split(" ");
				Node des = new Node(strings[0], Integer.parseInt(strings[1]));
				int distance = Integer.parseInt(strings[2]);
				if (distance == 0) { // 自己
					me = new Node(des);
				} else if (distance != -1) { // 邻居
					dvs.put(des, new DV(des));
					cost.put(des, distance);
				}
				initMap.put(des, new RouteInfo(infinite));
			}

			// 对邻居的距离向量，初始化所有节点为无穷
			for (DV dv : dvs.values())
				dv.infos = ObjectUtil.clone(initMap);
			// for (Entry<Node, DV> dv2 : dvs.entrySet())
			// debugdv(dv2);
			// debug("1");

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
				// debug(des + " " + entry.getValue());
			}

			dvs.put(me, mydv);
			// for (Entry<Node, DV> dv2 : dvs.entrySet())
			// debugdv(dv2);
			// debug("2");

			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(getName() + "\t结束");
			return;
		}

		debug("初始化完成");

		// 创建发送线程
		sender = new Sender(this);
		sender.start();

		// 开始监听
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(me.port);
			while (true) {
				Socket socket = serverSocket.accept();
				ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
				Object obj = objectInputStream.readObject();
				if (obj instanceof DV) {
					DV dv = (DV) obj;
//					debug("收到来自" + dv.node + "的距离向量");
					synchronized (dvs) {
						refresh(dv);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public String send(Node info, String msg) {
		return "not OK " + msg;
	}

	void refresh(DV dv) {
		final Node neibour = dv.node;
		final int disToNeibour = cost.get(neibour);
		DV newDV = new DV(me);
		DV myDv = dvs.get(me);
		DV neibourDV = dvs.get(neibour);

		for (final Entry<Node, RouteInfo> entry : dv.infos.entrySet()) {
			// for (Entry<Node, DV> dv2 : dvs.entrySet())
			// debugdv(dv2);
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

					newDV.infos.put(des, new RouteInfo(go));
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
						newDV.infos.put(des, new RouteInfo(my));
				}

			}
		}

		// 路由表改变后立即发送新路边给邻居
		if (!newDV.infos.isEmpty()) {
			synchronized (sender) {
				sender.dv = newDV;
				sender.notify();
			}
		}
	}

	void debug(String s) {
		System.out.println(df.format(new Date()) + "\t" + me + "\t" + s);
	}

	void debugdv(Entry<Node, DV> entry0) {
		for (Entry<Node, RouteInfo> entry : entry0.getValue().infos.entrySet()) {
			debug(entry0.getKey().port % 23400 + " " + entry.getKey().port % 23400 + " " + entry.getValue().dis);
		}
	}

}
