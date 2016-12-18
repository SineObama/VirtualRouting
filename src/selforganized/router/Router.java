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
		// 创建发送线程
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
				} else if (obj instanceof DCMessage) {
					DCMessage message = (DCMessage) obj;
					service.setDis(message.sender, message.distance);
				}
			}
		} catch (Exception e) {
			service.debug("出错: " + e);
		} finally {
			if (serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			service.debug("路由器已停止");
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
			e.printStackTrace();
			return "可能是连接失败（请确定目标路由器正常运行）";
		}
	}

	public String change(Node neighbor, int dis) throws MyException {
		if (!service.isNeighbor(neighbor))
			return "此节点不是邻居，不能修改距离";
		if (dis <= 0 && dis != -1)
			throw new MyException("邻居距离必须是正数或-1表示无穷");
		if (service.setDis(neighbor, dis)) {
			try {
				ObjectUtil.send(neighbor, new DCMessage(service.getMe(), dis));
			} catch (IOException e) {
				return "出错（请确定目标路由器正常运行）: " + e;
			}
			synchronized (sender) {
				sender.notify();
			}
		}
		return "修改成功";
	}

}