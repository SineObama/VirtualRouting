package selforganized.router;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import selforganized.ObjectUtil;
import selforganized.exception.FormatException;
import selforganized.exception.MyException;
import selforganized.router.struct.DV;
import selforganized.router.struct.Distance;
import selforganized.router.struct.Node;
import selforganized.router.struct.RouteInfo;

public class DVService {

	private Node me = null;
	private DV myDV;
	private Map<Node, DV> neighborDVs = new TreeMap<>();
	private Map<Node, Distance> cost = new TreeMap<>();
	private final static DVService instance = new DVService();
	private final static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	public static DVService getInstance() {
		return instance;
	}

	public synchronized void init(File file) throws NumberFormatException, IOException, MyException {
		// TODO ��ʼ���ж�
		// ��ȡ��̬·����Ϣ
		// �ļ���ʽ��ÿһ�У�[��ַ:�˿�] [���루���ۣ�]������Ϊ0��ʾ�Լ�������Ϊ-1��ʾ���ھӣ���ǰ���ɴ
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String lineTxt;

		// ���ļ���ȡ���нڵ㣬��¼�Լ����ھӣ���ʼ������Ϊ����
		final DV initDV = new DV();
		while ((lineTxt = bufferedReader.readLine()) != null) {
			String[] tokens = lineTxt.split(" ");
			if (tokens.length != 2) {
				bufferedReader.close();
				throw new FormatException("�ļ���ʽ�����޿ո�ָ�");
			}
			Node dst = new Node(tokens[0]);
			int dis = Integer.parseInt(tokens[1]);
			if (dis == 0) { // �Լ�
				me = ObjectUtil.clone(dst);
			} else if (dis != -1) { // �ھ�
				// ���ھӵľ�����������ʼ�����нڵ�Ϊ����
				cost.put(dst, new Distance(dis));
			}
			initDV.put(dst, RouteInfo.getUnreachable());
		}
		bufferedReader.close();
		if (me == null)
			throw new MyException("�����ļ�����û�б��ؽڵ�");
		for (Node neighbor : cost.keySet()) {
			DV dv = ObjectUtil.clone(initDV);
			dv.replace(neighbor, new RouteInfo(neighbor, new Distance(0)));
			neighborDVs.put(neighbor, dv);
		}

		// �Լ��ľ��������������ھӵľ���
		myDV = ObjectUtil.clone(initDV);
		for (Entry<Node, Distance> entry : cost.entrySet())
			myDV.replace(entry.getKey(), new RouteInfo(entry.getKey(), entry.getValue()));
		myDV.replace(me, new RouteInfo(me, 0));
		debug("��ʼ�����");
	}

	public Node getMe() {
		return ObjectUtil.clone(me);
	}

	public synchronized DV getDV() {
		return ObjectUtil.clone(myDV);
	}

	public synchronized RouteInfo get(Node dst) {
		return ObjectUtil.clone(myDV.get(dst));
	}

	// private synchronized void replace(Node neibour, Node dst, RouteInfo info)
	// {
	// neighborDVs.get(neibour).replace(dst, info);
	// }

	public boolean isNeighbor(Node node) {
		return cost.containsKey(node);
	}

	public Set<Node> getNeighbors() {
		return cost.keySet();
	}

	/**
	 * ���ϵ��ӽ��޸��Լ����ھӵľ���
	 * 
	 * @param neighbor
	 *            �ھӽڵ�
	 * @param dis
	 *            ����
	 * @return ����ľ��������Ƿ���
	 * @throws MyException
	 */
	public synchronized boolean setDis(Node neighbor, int dis) throws MyException {
		if (!isNeighbor(neighbor))
			throw new MyException("�����ھӣ��޷��޸�");
		if (dis <= 0 && dis != -1)
			throw new MyException("�ھӾ��������������-1��ʾ����");
		Distance newDis = new Distance(dis);
		cost.replace(neighbor, newDis);
		debug("�����Լ����ھ�" + neighbor + "ֱ�Ӿ���Ϊ" + newDis);
		RouteInfo oldInfo = myDV.get(neighbor);
		RouteInfo minInfo = getMin(neighbor);
		if (oldInfo.equals(minInfo))
			return false;
		myDV.replace(neighbor, minInfo);
		debug("�����Լ�����" + minInfo.next + "��" + neighbor + "�Ĵ���Ϊ" + minInfo.dis);
		return true;
	}

	public synchronized boolean refresh(Node neighbor, Entry<Node, RouteInfo> entry) throws MyException {
		Node dst = entry.getKey();
		RouteInfo newInfo = entry.getValue();
		if (!isNeighbor(neighbor))
			throw new MyException("�����ھӣ��޷�����");
		if (dst.equals(neighbor)) // �����ھӵ��ھ��������Ŀ
			return false;
		// boolean changed = false;
		final DV neighborDV = neighborDVs.get(neighbor);
		final Distance dis = newInfo.dis;

		final RouteInfo oldInfo = neighborDV.get(dst);
		if (oldInfo == null || !oldInfo.equals(newInfo)) {
			neighborDVs.get(neighbor).replace(dst, newInfo);
			debug("���´��ھ�" + neighbor + "��" + dst + "�Ĵ���Ϊ" + dis);
		}

		boolean changed = false;
		final RouteInfo myInfo = myDV.get(dst);
		final Distance totalDis = Distance.add(myDV.get(neighbor).dis, dis);
		if (myInfo.dis.compareTo(totalDis) > 0) { // ���۱�С
			myDV.replace(dst, new RouteInfo(neighbor, totalDis));
			debug("�����Լ�����" + neighbor + "��" + dst + "�Ĵ���Ϊ" + totalDis);
		} else if (neighbor.equals(myInfo.next) && myInfo.dis.compareTo(totalDis) < 0) { // ԭ·����������
			RouteInfo minInfo = getMin(dst);
			if (!minInfo.dis.equals(myInfo.dis)) // ��Ҫ���¾�������
				changed = true;
			myDV.replace(dst, minInfo);
			debug("�����Լ�����" + minInfo.next + "��" + dst + "�Ĵ���Ϊ" + minInfo.dis);
		}
		return changed;
	}

	/**
	 * DV�㷨�м��㵽Ŀ�Ľڵ����·�ɵķ�����
	 * 
	 * @param dst
	 *            Ŀ�Ľڵ�
	 * @return ���·����Ϣ���ڵ㲻�ɴ�ʱ���ؾ���Ϊ���ֵ����Ϣ��
	 */
	private synchronized RouteInfo getMin(Node dst) {
		RouteInfo minInfo = RouteInfo.getUnreachable();
		for (Node neibour : getNeighbors()) { // �����ھ�
			Distance totalDis = Distance.add(cost.get(neibour), neighborDVs.get(neibour).get(dst).dis);
			if (minInfo.dis.compareTo(totalDis) > 0)
				minInfo = new RouteInfo(neibour, totalDis);
		}
		return minInfo;
	}

	// helpers

	/**
	 * ���·����������Ϣ
	 * 
	 * @param s
	 *            Ҫ����ĵ�����Ϣ
	 */
	void debug(Object s) {
		System.out.println(df.format(new Date()) + "\t" + me + "\t" + s);
	}

}
