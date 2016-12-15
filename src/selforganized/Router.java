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

		// ��ȡ��̬·�ɵ�ַ�˿ں;���
		// �ļ���ʽ��ÿһ�У�[��ַ:�˿�] [���루���ۣ�]������Ϊ0��ʾ�Լ�������Ϊ-1��ʾ���ھ�
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String lineTxt;

			// ���ļ���ȡ���нڵ㣬��ʼ������Ϊ����
			// FIXME ÿ�����ж������Լ����Լ�����Ŀ����������
			final RouteInfo infinite = new RouteInfo(null, Integer.MAX_VALUE);
			TreeMap<Node, RouteInfo> initMap = new TreeMap<>();
			while ((lineTxt = bufferedReader.readLine()) != null) {
				String[] strings = lineTxt.split(" ");
				if (strings.length != 2) {
					bufferedReader.close();
					throw new MyException("�ļ���ʽ����");
				}
				Node des = new Node(strings[0]);
				int distance = Integer.parseInt(strings[1]);
				if (distance == 0) { // �Լ�
					me = new Node(des);
				} else if (distance != -1) { // �ھ�
					dvs.put(des, new DV(des));
					cost.put(des, distance);
				}
				initMap.put(des, new RouteInfo(infinite));
			}
			bufferedReader.close();

			// ���ھӵľ�����������ʼ�����нڵ�Ϊ����
			for (DV dv : dvs.values())
				dv.infos = ObjectUtil.clone(initMap);

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
			}
			dvs.put(me, mydv);

			debug("��ʼ�����");

			// ���������߳�
			sender = new Sender(this);
			sender.start();

			// ��ʼ����
			ServerSocket serverSocket = new ServerSocket(me.port);
			while (true) {
				Object obj = ObjectUtil.receive(serverSocket);
				if (obj instanceof DV) {
					DV dv = (DV) obj;
					// debug("�յ�����" + dv.node + "�ľ�������");
					synchronized (sender) {
						refresh(dv);
					}
				} else if (obj instanceof Message) {
					Message message = (Message) obj;
					if (message.dst.equals(me)) {
						debug("�յ�����" + message.src + "�ı��ģ�" + message.text);
						continue;
					}
					debug("�յ�����" + message.sender + "ת���ı���");
					Node next = getDV().infos.get(message.dst).next;
					if (next == null) {
						debug("·�ɱ���û�нڵ�" + message.dst + "����Ϣ���޷�����");
						continue;
					}
					message.sender = me;
					ObjectUtil.send(next, message);
					debug("�ɹ�ת������" + message.sender + "�ı��ĵ�" + next);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		debug("·������ֹͣ");
	}

	/**
	 * ������Ϣ����һ���ڵ�
	 * 
	 * @param node
	 *            ���սڵ�
	 * @param msg
	 *            ��Ϣ
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public void send(Node node, String msg) throws UnknownHostException, IOException {
		Node next = getDV().infos.get(node).next;
		if (next == null)
			debug("·�ɱ���û�д˽ڵ���Ϣ���޷�����");
		Message message = new Message();
		message.src = me;
		message.dst = node;
		message.sender = me;
		message.text = msg;
		ObjectUtil.send(next, message);
		debug("�ɹ����ͱ��ģ���һ�ڵ㣺" + next);
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

					changed = true;
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
						changed = true;
				}

			}
		}

		// ·�ɱ�ı��������������·�ɱ���ھ�
		if (changed)
			sender.notify();
	}

	Set<Node> getNeibours() {
		return dvs.keySet();
	}

	/**
	 * ��ȡ·�����Լ��ľ�������
	 * 
	 * @return ·�����Լ��ľ�������
	 */
	DV getDV() {
		return dvs.get(me);
	}

	/**
	 * ��ȡ·�����Ľڵ���Ϣ
	 * 
	 * @return ·�����Ľڵ���Ϣ
	 */
	Node getMe() {
		return me;
	}

	/**
	 * ���·����������Ϣ
	 * 
	 * @param s
	 *            Ҫ����ĵ�����Ϣ
	 */
	void debug(String s) {
		System.out.println(df.format(new Date()) + "\t" + me + "\t" + s);
	}

}
