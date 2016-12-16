package selforganized.router;

import java.io.IOException;

import selforganized.ObjectUtil;
import selforganized.router.struct.Node;

/**
 * ·�������;����������߳�
 * 
 * @author Sine
 *
 */
public class Sender extends Thread {

	private final DVdao dao = DVdao.getInstance();
	private final Router router;

	public Sender(Router router) {
		this.router = router;
	}

	@Override
	public void run() {
		synchronized (this) {
			try {
				wait(); // ������ǰ�����̣߳�����·����һͬ��ʼ
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			while (true) {
				try {
					wait(2000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// ��ʼ����·�ɱ�
				for (Node node : dao.getNeibour()) {
					try {
						if (node.equals(router.me))
							continue;
						ObjectUtil.send(node, dao.getDV(router.me));
					} catch (IOException e) {
						router.debug("���;���������" + node + "ʧ��: " + e);
						// TODO �޸�·�ɱ�
					}
				}
			}
		}

	}

}
