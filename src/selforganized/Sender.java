package selforganized;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Sender extends Thread {
	Router router;

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
			synchronized (router.table) {
				for (Info info : router.neibours) {
					try {
						Socket socket = new Socket(info.address, info.port);
						ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
						objectOutputStream.writeObject(router.table);
						socket.close();
					} catch (IOException e) {
						System.out.println(router.name + "发送路由表" + info.address + ":" + info.port + "失败: " + e);
					}
				}
			}
		}

	}
}
