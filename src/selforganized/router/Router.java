package selforganized.router;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Map.Entry;

import selforganized.Client;
import selforganized.ObjectUtil;
import selforganized.exception.MyException;
import selforganized.router.struct.DCMessage;
import selforganized.router.struct.DVMessage;
import selforganized.router.struct.Message;
import selforganized.router.struct.Node;
import selforganized.router.struct.RouteInfo;

public class Router extends Thread {

	private final Sender sender;
	private final static DVService service = DVService.getInstance();

	public Router(File file) throws NumberFormatException, IOException, MyException {
		service.init(file);
		// ���������߳�
		sender = new Sender();
		sender.start();
	}

	public void shutdown() {
		service.shutdown();
		synchronized (sender) {
			sender.notify();
		}
	}

	@Override
	public void run() {
		synchronized (sender) {
			sender.notify();
		}

		// ��ʼ����
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(service.getMe().port);
			while (true) {
				Object obj = ObjectUtil.receive(serverSocket);
				if (obj instanceof DVMessage) {
					DVMessage message = (DVMessage) obj;
					// debug("�յ�����" + message.sender + "�ľ�������");
					boolean changed = false; // ����Լ���·�ɱ��Ƿ�ı�
					for (final Entry<Node, RouteInfo> entry : message.dv.entrySet())
						changed |= service.refresh(message.sender, entry);
					if (changed)// ·�ɱ�ı��������������·�ɱ���ھ�
						synchronized (sender) {
							sender.notify();
						}
				} else if (obj instanceof Message) {
					Message message = (Message) obj;
					service.debug("�յ���" + message.sender + "ת���ı���");
					if (message.dst.equals(service.getMe())) {
						Client.sysout("�յ�����" + message.src + "�ı��ģ�\n" + message.text);
						continue;
					}
					message.sender = service.getMe();
					service.debug(forward(message));
				} else if (obj instanceof DCMessage) {
					DCMessage message = (DCMessage) obj;
					service.setDis(message.sender, message.distance);
				}
			}
		} catch (Exception e) {
			service.debug("����: " + e);
		} finally {
			if (serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			service.debug("·������ֹͣ");
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
	public String send(Node dst, String msg) {
		Message message = new Message();
		message.src = service.getMe();
		message.dst = dst;
		message.text = msg;
		return forward(message);
	}

	private String forward(Message message) {
		try {
			Node neighbor = null;
			RouteInfo info = service.get(message.dst);
			if (info == null || info.isUnreachable())
				return "�˽ڵ�Ŀǰ���ɴ����ȡ������";
			message.sender = service.getMe();
			neighbor = info.next;
			ObjectUtil.send(neighbor, message);
			return "�ɹ����ͱ��ĵ���һ�ڵ㣺" + neighbor;
		} catch (IOException e) {
			e.printStackTrace();
			return "����������ʧ�ܣ���ȷ��Ŀ��·�����������У�";
		}
	}

	public String change(Node neighbor, int dis) throws MyException {
		if (!service.isNeighbor(neighbor))
			return "�˽ڵ㲻���ھӣ������޸ľ���";
		if (dis <= 0 && dis != -1)
			throw new MyException("�ھӾ��������������-1��ʾ����");
		if (service.setDis(neighbor, dis)) {
			try {
				ObjectUtil.send(neighbor, new DCMessage(service.getMe(), dis));
			} catch (IOException e) {
				return "������ȷ��Ŀ��·�����������У�: " + e;
			}
			synchronized (sender) {
				sender.notify();
			}
		}
		return "�޸ĳɹ�";
	}

}