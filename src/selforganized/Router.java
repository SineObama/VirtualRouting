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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Router extends Thread {

	File file;
	RouteTable table;
	List<Info> neibours = new ArrayList<>();
	String name;
	Sender sender;
	SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	public Router(File file) {
		this.file = file;
	}

	@Override
	public void run() {

		// 读取文件
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream(file));
			BufferedReader bufferedReader = new BufferedReader(read);
			String lineTxt = null;
			if ((lineTxt = bufferedReader.readLine()) == null) {
				bufferedReader.close();
				throw new RuntimeException("无法读取路由信息");
			}
			String[] strings = lineTxt.split(" ");
			table = new RouteTable();
			table.me = new Info(strings[0], Integer.parseInt(strings[1]));
			while ((lineTxt = bufferedReader.readLine()) != null) {
				strings = lineTxt.split(" ");
				// 加进路由表
				Info des = new Info(strings[0], Integer.parseInt(strings[1]));
				RouteInfo info = new RouteInfo(des, Integer.parseInt(strings[2]));
				table.infos.put(des, info);
				// 加进邻居表
				neibours.add(des);
			}
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(getName() + "\t结束");
			return;
		}

		name = table.me.toString();
		debug(name + "启动完成");

		// 创建发送线程
		sender = new Sender(this);
		sender.start();

		// 开始监听
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(table.me.port);
			while (true) {
				Socket socket = serverSocket.accept();
				ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
				Object obj = objectInputStream.readObject();
				if (obj instanceof RouteTable) {
					RouteTable o = (RouteTable) obj;
//					debug(name + "收到来自" + o.me + "的路由表");
					synchronized (table) {
						refresh(o);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	void refresh(RouteTable o) {
		int distance = table.infos.get(o.me).distance;
		boolean modified = false;
		for (Info info : o.infos.keySet()) {
			if (info.equals(table.me))
				continue;
			RouteInfo oldInfo = table.infos.get(info);
			RouteInfo newInfo = o.infos.get(info);
			if (oldInfo == null) {
				table.infos.put(info, new RouteInfo(o.me, distance + newInfo.distance));
				debug(name + "添加经由" + o.me + "到" + info + "的" + newInfo.distance);
			} else {
				if (oldInfo.distance > distance + newInfo.distance
						|| (oldInfo.next.equals(o.me) && oldInfo.distance < distance + newInfo.distance)) {
					oldInfo.distance = distance + newInfo.distance;
					oldInfo.next = o.me;
					debug(name + "更新经由" + o.me + "到" + info + "的" + oldInfo.distance);
					modified = true;
				}
			}
		}
		// 路由表改变后立即发送新路边给邻居
		if (modified) {
			synchronized (sender) {
				sender.notify();
			}
		}
	}

	void debug(String s) {
		System.out.println(df.format(new Date()) + " " + name + " " + s);
	}

}
