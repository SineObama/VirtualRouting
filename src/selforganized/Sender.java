package selforganized;

import java.io.IOException;

/**
 * ·�������;����������߳�
 * 
 * @author Sine
 *
 */
public class Sender extends Thread {
	private final Router router;

	public Sender(Router router) {
		this.router = router;
	}

	@Override
	public void run() {
		synchronized (this) {
			while (true) {
				try {
					wait(2000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// ��ʼ����·�ɱ�
				for (Node node : router.getNeibours()) {
					try {
						if (node.equals(router.getMe()))
							continue;
						ObjectUtil.send(node, router.getDV());
//						router.debug("���;���������" + info + "�ɹ�");
					} catch (IOException e) {
						router.debug("���;���������" + node + "ʧ��: " + e);
					}
				}
			}
		}

	}
}
