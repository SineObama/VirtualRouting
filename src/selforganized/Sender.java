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
			// ��ʼ����·�ɱ�
			synchronized (router.table) {
				for (Info info : router.neibours) {
					try {
						Socket socket = new Socket(info.address, info.port);
						ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
						objectOutputStream.writeObject(router.table);
						socket.close();
					} catch (IOException e) {
						System.out.println(router.name + "����·�ɱ�" + info.address + ":" + info.port + "ʧ��: " + e);
					}
				}
			}
		}

	}
}
