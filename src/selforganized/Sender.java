package selforganized;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map.Entry;

/**
 * 路由器发送距离向量的线程
 * 
 * @author Sine
 *
 */
public class Sender extends Thread {
	Router router;
	DV dv;

	public Sender(Router router) {
		this.router = router;
	}

	@Override
	public void run() {
		while (true) {
			try {
				synchronized (this) {
					wait(2000);
				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// 开始发送路由表
			for (Node info : router.dvs.keySet()) {
				try {
					if (info.equals(router.me))
						continue;
					Socket socket = new Socket(info.addr, info.port);
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
					DV tem = router.dvs.get(router.me);
					objectOutputStream.writeObject(tem);
					socket.close();
//					router.debug("发送距离向量到" + info + "成功");
				} catch (IOException e) {
					router.debug("发送距离向量到" + info + "失败: " + e);
				}
			}
		}

	}
}
