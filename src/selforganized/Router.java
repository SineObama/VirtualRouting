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
					debug("�յ���" + message.sender + "ת���ı���");
					if (message.dst.equals(me)) {
						Client.sysout("�յ�����" + message.src + "�ı��ģ�\n" + message.text);
						continue;
					}
					message.sender = new Node(me);
					debug(forward(message));
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
	 * @param dst
	 *            ���սڵ�
	 * @param msg
	 *            ��Ϣ
	 * @return ������Ϣ
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
					return "�˽ڵ�Ŀǰ���ɴ����ȡ������";
				neibour = info.next;
				ObjectUtil.send(neibour, message);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				cost.replace(neibour, Integer.MAX_VALUE);
				debug("�ھ�" + neibour + "��Ϊ���ɴ����������·��");
				RouteInfo minInfo = getMin(message.dst);
				try {
					getDV().infos.replace(message.dst, new RouteInfo(minInfo));
				} catch (MyException e1) {
					return "�ڲ����ش���";
				}
				continue;
			}
			break;
		}
		return "�ɹ����ͱ��ĵ���һ�ڵ㣺" + neibour;
	}

	private RouteInfo getMin(Node dst) {
		RouteInfo minInfo = new RouteInfo(null, Integer.MAX_VALUE);
		synchronized (cost) {
			for (Entry<Node, Integer> entry : cost.entrySet()) { // �����ھ�
				Node neibour = entry.getKey();
				DV neibourDV = dvs.get(neibour);
				int totalDis = entry.getValue() + neibourDV.infos.get(dst).dis;
				if (totalDis < 0) // ����������ɴ�
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
			if (dst.equals(neibour)) // �����ھӵ��ھ��������Ŀ
				continue;

			// ���������ھӵ��Լ��Ĵ��۸���
			if (dst.equals(me)) {
				if (disToNeibour != dis) {
					synchronized (cost) {
						cost.replace(neibour, dis);
					}
					myDv.infos.get(neibour).dis = dis;
					neibourDV.infos.replace(me, new RouteInfo(newInfo.next, dis));
					changed = true;
					debug("�����ھ�" + neibour + "���Լ�֮��Ĵ���Ϊ" + showCost(dis));
				}
				continue;
			}

			// �����ھӵľ�������
			final RouteInfo oldInfo = neibourDV.infos.get(dst);
			if (oldInfo != null && oldInfo.equals(newInfo))
				continue;

			// ��ӻ����
			neibourDV.infos.replace(dst, new RouteInfo(newInfo));
			debug("���´��ھ�" + neibour + "��" + dst + "�Ĵ���Ϊ" + showCost(dis));

			// �������ľ��������Ƿ���Ҫ���¡���2�����
			final RouteInfo myInfo = myDv.infos.get(dst);

			int totalDis = disToNeibour + dis;
			if (totalDis < 0) // ����������ɴ�
				totalDis = Integer.MAX_VALUE;

			if (myInfo.dis > totalDis) { // ���۱�С
				myInfo.dis = totalDis;
				myInfo.next = new Node(neibour);
				debug("�����Լ�����" + myInfo.next + "��" + dst + "�Ĵ���Ϊ" + showCost(myInfo.dis));
			} else if (myInfo.next.equals(neibour) && myInfo.dis < totalDis) { // ԭ·����������
				RouteInfo minInfo = getMin(dst);
				if (minInfo.dis != myInfo.dis) // ��Ҫ���¾�������
					changed = true;
				myInfo.dis = minInfo.dis;
				myInfo.next = new Node(minInfo.next);
				debug("�����Լ�����" + myInfo.next + "��" + dst + "�Ĵ���Ϊ" + myInfo.dis);
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

	private static String showCost(int cost) {
		return cost == Integer.MAX_VALUE ? "����(���ɴ�)" : Integer.toString(cost);
	}

}
