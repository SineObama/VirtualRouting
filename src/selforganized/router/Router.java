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

		// ��ȡ��̬·����Ϣ
		// �ļ���ʽ��ÿһ�У�[��ַ:�˿�] [���루���ۣ�]������Ϊ0��ʾ�Լ�������Ϊ-1��ʾ���ھӣ���ǰ���ɴ
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String lineTxt;

		// ���ļ���ȡ���нڵ㣬��ʼ������Ϊ����
		// FIXME ÿ�����ж������Լ����Լ�����Ŀ����������
		Map<Node, Integer> cost = new TreeMap<>();
		DV initDV = new DV();
		Node me = null;
		while ((lineTxt = bufferedReader.readLine()) != null) {
			String[] strings = lineTxt.split(" ");
			if (strings.length != 2) {
				bufferedReader.close();
				throw new FormatException("�ļ���ʽ�����޿ո�ָ�");
			}
			Node dst = new Node(strings[0]);
			int distance = Integer.parseInt(strings[1]);
			if (distance == 0) { // �Լ�
				me = ObjectUtil.clone(dst);
			} else if (distance != -1) { // �ھ�
				// ���ھӵľ�����������ʼ�����нڵ�Ϊ����
				cost.put(dst, distance);
			}
			initDV.put(dst, RouteInfo.getUnreachable());
		}
		bufferedReader.close();
		for (Node neibour : cost.keySet())
			dao.add(neibour, ObjectUtil.clone(initDV));
		dao.setMe(me);

		// �Լ��ľ��������������ھӵľ���
		DV myDV = ObjectUtil.clone(initDV);
		for (Entry<Node, Integer> entry : cost.entrySet())
			myDV.replace(entry.getKey(), new RouteInfo(entry.getKey(), entry.getValue()));
		dao.add(me, myDV);
		// ���������߳�
		sender = new Sender(this);
		sender.start();
		debug("��ʼ�����");
	}

	@Override
	public void run() {
		sender.notify();

		// ��ʼ����
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(dao.getMe().port);
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
					if (message.dst.equals(dao.getMe())) {
						Client.sysout("�յ�����" + message.src + "�ı��ģ�\n" + message.text);
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
			debug("·������ֹͣ");
		}
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
					return "�˽ڵ�Ŀǰ���ɴ����ȡ������";
				neibour = info.next;
				ObjectUtil.send(neibour, message);
				// } catch (UnknownHostException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
			} catch (IOException e) {
				debug("�ھ�" + neibour + "��Ϊ���ɴ����������·��");
				RouteInfo info = getMin(message.dst);
				neibour = info.next;
				dao.replace(message.dst, info);
				continue;
			}
			break;
		}
		return "�ɹ����ͱ��ĵ���һ�ڵ㣺" + neibour;
	}

	/**
	 * DV�㷨�м��㵽Ŀ�Ľڵ����·�ɵķ�����
	 * 
	 * @param dst
	 *            Ŀ�Ľڵ�
	 * @return ���·����Ϣ���ڵ㲻�ɴ�ʱ���ؾ���Ϊ���ֵ����Ϣ��
	 */
	private RouteInfo getMin(Node dst) {
		RouteInfo minInfo = RouteInfo.getUnreachable();
		for (Node neibour : dao.getNeibour()) { // �����ھ�
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
			if (dst.equals(neibour)) // �����ھӵ��ھ��������Ŀ
				continue;

			// ���������ھӵ��Լ��Ĵ��۸���
			if (dst.equals(dao.getMe())) {
				if (disToNeibour.equals(dis)) {
					dao.replace(neibour, dst, newInfo);
					dao.replace(neibour, dis);
					changed = true;
					debug("�����ھ�" + neibour + "���Լ�֮��Ĵ���Ϊ" + dis));
				}
				continue;
			}

			// �����ھӵľ�������
			final RouteInfo oldInfo = neibourDV.get(dst);
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

	@Override
	public void change(Node neibour, int dis) {
		// TODO Auto-generated method stub

	}

	// helpers

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