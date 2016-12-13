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

		// ��ȡ�ļ�
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream(file));
			BufferedReader bufferedReader = new BufferedReader(read);
			String lineTxt = null;
			if ((lineTxt = bufferedReader.readLine()) == null) {
				bufferedReader.close();
				throw new RuntimeException("�޷���ȡ·����Ϣ");
			}
			String[] strings = lineTxt.split(" ");
			table = new RouteTable();
			table.me = new Info(strings[0], Integer.parseInt(strings[1]));
			while ((lineTxt = bufferedReader.readLine()) != null) {
				strings = lineTxt.split(" ");
				// �ӽ�·�ɱ�
				Info des = new Info(strings[0], Integer.parseInt(strings[1]));
				RouteInfo info = new RouteInfo(des, Integer.parseInt(strings[2]));
				table.infos.put(des, info);
				// �ӽ��ھӱ�
				neibours.add(des);
			}
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(getName() + "\t����");
			return;
		}

		name = table.me.toString();
		debug(name + "�������");

		// ���������߳�
		sender = new Sender(this);
		sender.start();

		// ��ʼ����
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(table.me.port);
			while (true) {
				Socket socket = serverSocket.accept();
				ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
				Object obj = objectInputStream.readObject();
				if (obj instanceof RouteTable) {
					RouteTable o = (RouteTable) obj;
//					debug(name + "�յ�����" + o.me + "��·�ɱ�");
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
				debug(name + "��Ӿ���" + o.me + "��" + info + "��" + newInfo.distance);
			} else {
				if (oldInfo.distance > distance + newInfo.distance
						|| (oldInfo.next.equals(o.me) && oldInfo.distance < distance + newInfo.distance)) {
					oldInfo.distance = distance + newInfo.distance;
					oldInfo.next = o.me;
					debug(name + "���¾���" + o.me + "��" + info + "��" + oldInfo.distance);
					modified = true;
				}
			}
		}
		// ·�ɱ�ı������������·�߸��ھ�
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
