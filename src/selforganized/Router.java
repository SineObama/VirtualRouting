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

		// ��ȡ��̬·�ɵ�ַ�˿ں;���
		// �ļ���ʽ��ÿһ���ǵ�ַ���˿ں;��룬����Ϊ0��ʾ�Լ�������Ϊ-1��ʾ���ھ�
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream(file));
			BufferedReader bufferedReader = new BufferedReader(read);
			String lineTxt;

			// ��ʼ������Ϊ������ļ���ȡ���нڵ�
			// FIXME ÿ�����ж������Լ����Լ�����Ŀ����������
			final RouteInfo infinite = new RouteInfo(null, Integer.MAX_VALUE);
			TreeMap<Node, RouteInfo> initMap = new TreeMap<>();
			while ((lineTxt = bufferedReader.readLine()) != null) {
				String[] strings = lineTxt.split(" ");
				Node des = new Node(strings[0], Integer.parseInt(strings[1]));
				int distance = Integer.parseInt(strings[2]);
				if (distance == 0) { // �Լ�
					me = new Node(des);
				} else if (distance != -1) { // �ھ�
					dvs.put(des, new DV(des));
					cost.put(des, distance);
				}
				initMap.put(des, new RouteInfo(infinite));
			}

			// ���ھӵľ�����������ʼ�����нڵ�Ϊ����
			for (DV dv : dvs.values())
				dv.infos = ObjectUtil.clone(initMap);
			// for (Entry<Node, DV> dv2 : dvs.entrySet())
			// debugdv(dv2);
			// debug("1");

			// �Լ��ľ��������������ھӵľ���
			DV mydv = new DV(me);
			mydv.infos = new TreeMap<>(initMap);
			for (Entry<Node, RouteInfo> entry : mydv.infos.entrySet()) {
				Node des = entry.getKey();
				Integer distance = cost.get(des);
				if (distance != null) { // ���ھ�
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
			System.out.println(getName() + "\t����");
			return;
		}

		debug("��ʼ�����");

		// ���������߳�
		sender = new Sender(this);
		sender.start();

		// ��ʼ����
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(me.port);
			while (true) {
				Socket socket = serverSocket.accept();
				ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
				Object obj = objectInputStream.readObject();
				if (obj instanceof DV) {
					DV dv = (DV) obj;
//					debug("�յ�����" + dv.node + "�ľ�������");
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

			// ���������ھӵ��Լ��Ĵ��۸���
			if (des.equals(me)) {
				if (disToNeibour != newDis) {
					// FIXME Integerֵ���޸ģ�
					cost.remove(neibour);
					cost.put(neibour, newDis);

					final RouteInfo go = myDv.infos.get(neibour);
					go.dis = newDis;

					final RouteInfo back = neibourDV.infos.get(me);
					back.dis = newDis;
					back.next = new Node(newHis.next);

					newDV.infos.put(des, new RouteInfo(go));
					debug("�����ھ�" + neibour + "���Լ��Ĵ���Ϊ" + newDis);
				}
				continue;
			}

			// �����ھӵľ�������
			final RouteInfo his = neibourDV.infos.get(des);
			if (his == null) { // ���
				neibourDV.infos.put(des, new RouteInfo(newHis));
				debug("�������" + neibour + "��" + des + "�Ĵ���Ϊ" + newDis);
			} else if (!his.equals(newHis)) { // ����

				his.dis = newDis;
				his.next = new Node(newHis.next);
				debug("�����ھ�" + neibour + "��" + des + "�Ĵ���Ϊ" + newDis);
				// �ټ������ľ��������Ƿ���Ҫ����
				final RouteInfo my = myDv.infos.get(des);

				int totalDis = disToNeibour + newDis;
				if (totalDis < 0) // ����������ɴ�
					totalDis = Integer.MAX_VALUE;

				if (my.dis > totalDis) { // ���۱�С
					my.dis = totalDis;
					my.next = new Node(neibour);
					debug("���¾���" + my.next + "��" + des + "�Ĵ���Ϊ" + my.dis);
				} else if (my.next.equals(neibour) && my.dis < totalDis) { // ԭ·����������
					int src = my.dis;
					my.dis = totalDis;
					for (Entry<Node, Integer> n : cost.entrySet()) { // �����ھ�
						DV curDV = dvs.get(n.getKey());
						int curDis = n.getValue() + curDV.infos.get(des).dis;
						if (curDis < 0) // ����������ɴ�
							curDis = Integer.MAX_VALUE;
						if (my.dis > curDis) {
							my.dis = curDis;
							my.next = new Node(n.getKey());
						}
					}
					debug("���¾���" + my.next + "��" + des + "�Ĵ���Ϊ" + my.dis);
					if (src != my.dis) // ��Ҫ���¾�������
						newDV.infos.put(des, new RouteInfo(my));
				}

			}
		}

		// ·�ɱ�ı������������·�߸��ھ�
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
