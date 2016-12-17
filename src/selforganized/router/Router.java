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
import java.util.Map.Entry;

import selforganized.Client;
import selforganized.ObjectUtil;
import selforganized.exception.FormatException;
import selforganized.exception.MyException;
import selforganized.router.struct.DV;
import selforganized.router.struct.DVMessage;
import selforganized.router.struct.Distance;
import selforganized.router.struct.Message;
import selforganized.router.struct.Node;
import selforganized.router.struct.RouteInfo;

import java.util.TreeMap;

public class Router extends Thread implements IRouter {

	private final Sender sender;
	final static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
	private final static DVService dao = DVService.getInstance();

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
		myDV.replace(me, new RouteInfo(me, 0));
		dao.setDV(myDV);
		// ���������߳�
		sender = new Sender(this);
		sender.start();
		debug("��ʼ�����");
	}

	@Override
	public void run() {
		synchronized (sender) {
			sender.notify();
		}

		// ��ʼ����
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(dao.getMe().port);
			while (true) {
				Object obj = ObjectUtil.receive(serverSocket);
				if (obj instanceof DVMessage) {
					DVMessage message = (DVMessage) obj;
					// debug("�յ�����" + message.sender + "�ľ�������");
					synchronized (sender) {
						refresh(message.sender, message.dv);
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
		for (Node neibour : dao.getNeibours()) { // �����ھ�
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
		boolean changed = false; // ����Լ���·�ɱ��Ƿ�ı�
		for (final Entry<Node, RouteInfo> entry : dv.entrySet()) {
			final Node dst = entry.getKey();
			final RouteInfo newInfo = entry.getValue();
			final Distance dis = newInfo.dis;
			// if (dst.equals(neibour)) // �����ھӵ��ھ��������Ŀ
			// continue;

			final RouteInfo oldInfo = neibourDV.get(dst);
			if (oldInfo == null || !oldInfo.equals(newInfo)) {
				dao.replace(neibour, dst, newInfo);
				if (!dst.equals(dao.getMe())) {
					debug("���´��ھ�" + neibour + "��" + dst + "�Ĵ���Ϊ" + dis);
				} else if (disToNeibour.compareTo(dis) > 0) {
					debug(dst);
					dao.replace(neibour, new RouteInfo(dst, dis));
					changed = true;
					debug("�����ھ�" + neibour + "���Լ�֮��Ĵ���Ϊ" + dis);
				}
			}

			// // ���������ھӵ��Լ��Ĵ��۸���
			// if (dst.equals(dao.getMe())) {
			// if (disToNeibour.compareTo(dis) > 0) {
			// dao.replace(neibour, dst, newInfo);
			// dao.replace(neibour, dis);
			// changed = true;
			// debug("�����ھ�" + neibour + "���Լ�֮��Ĵ���Ϊ" + dis);
			// }
			// continue;
			// }
			//
			// // �����ھӵľ�������
			// final RouteInfo oldInfo = neibourDV.get(dst);
			// if (oldInfo != null && oldInfo.equals(newInfo))
			// continue;
			//
			// // ��ӻ����
			// dao.replace(neibour, dst, newInfo);
			// debug("���´��ھ�" + neibour + "��" + dst + "�Ĵ���Ϊ" + dis);

			// �������ľ��������Ƿ���Ҫ���¡���2�����
			final RouteInfo myInfo = myDv.get(dst);
			Distance totalDis = Distance.add(disToNeibour, dis);
			if (myInfo.dis.compareTo(totalDis) > 0) { // ���۱�С
				dao.replace(dst, new RouteInfo(neibour, totalDis));
				debug("�����Լ�����" + neibour + "��" + dst + "�Ĵ���Ϊ" + totalDis);
			} else if (myInfo.next.equals(neibour) && myInfo.dis.compareTo(totalDis) < 0) { // ԭ·����������
				RouteInfo minInfo = getMin(dst);
				if (!minInfo.dis.equals(myInfo.dis)) // ��Ҫ���¾�������
					changed = true;
				dao.replace(dst, minInfo);
				debug("�����Լ�����" + minInfo.next + "��" + dst + "�Ĵ���Ϊ" + minInfo.dis);
			}
		}

		// ·�ɱ�ı��������������·�ɱ���ھ�
		if (changed)
			sender.notify();
	}

	@Override
	public String change(Node neibour, int dis) {
		if (!dao.getNeibours().contains(neibour))
			return "�˽ڵ㲻���ھӣ������޸ľ���";
		dao.replace(neibour, new Distance(dis));
		synchronized (sender) {
			sender.notify();
		}
		return "�޸ĳɹ�";
	}

	// helpers

	/**
	 * ���·����������Ϣ
	 * 
	 * @param s
	 *            Ҫ����ĵ�����Ϣ
	 */
	void debug(Object s) {
		System.out.println(df.format(new Date()) + "\t" + dao.getMe() + "\t" + s);
	}

}