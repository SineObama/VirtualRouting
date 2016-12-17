package selforganized.router;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Map.Entry;

import selforganized.Client;
import selforganized.ObjectUtil;
import selforganized.exception.MyException;
import selforganized.router.struct.DVMessage;
import selforganized.router.struct.Message;
import selforganized.router.struct.Node;
import selforganized.router.struct.RouteInfo;

public class Router extends Thread {

	private final Sender sender;
	private final static DVService service = DVService.getInstance();

	public Router(File file) throws NumberFormatException, IOException, MyException {
		service.init(file);
		// 创建发送线程
		sender = new Sender();
		sender.start();
	}

	@Override
	public void run() {
		synchronized (sender) {
			sender.notify();
		}

		// 开始监听
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(service.getMe().port);
			while (true) {
				Object obj = ObjectUtil.receive(serverSocket);
				if (obj instanceof DVMessage) {
					DVMessage message = (DVMessage) obj;
					// debug("收到来自" + message.sender + "的距离向量");
					boolean changed = false; // 标记自己的路由表是否改变
					for (final Entry<Node, RouteInfo> entry : message.dv.entrySet())
						changed |= service.refresh(message.sender, entry);
					if (changed)// 路由表改变后立即发送最新路由表给邻居
						synchronized (sender) {
							sender.notify();
						}
				} else if (obj instanceof Message) {
					Message message = (Message) obj;
					service.debug("收到由" + message.sender + "转发的报文");
					if (message.dst.equals(service.getMe())) {
						Client.sysout("收到来自" + message.src + "的报文：\n" + message.text);
						continue;
					}
					message.sender = service.getMe();
					service.debug(forward(message));
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
			System.out.println("路由器已停止");
		}
	}

	/**
	 * 发送信息到另一个节点
	 * 
	 * @param dst
	 *            接收节点
	 * @param msg
	 *            信息
	 * @return 反馈信息
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
				return "此节点目前不可达。报文取消发送";
			message.sender = service.getMe();
			neighbor = info.next;
			ObjectUtil.send(neighbor, message);
			return "成功发送报文到下一节点：" + neighbor;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "严重内部错误";
	}

	public String change(Node neighbor, int dis) throws MyException {
		if (!service.isNeighbor(neighbor))
			return "此节点不是邻居，不能修改距离";
		if (service.setDis(neighbor, dis))
			synchronized (sender) {
				sender.notify();
			}
		return "修改成功";
	}

}