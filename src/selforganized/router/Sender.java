package selforganized.router;

import java.io.IOException;

import selforganized.ObjectUtil;
import selforganized.router.struct.DVMessage;
import selforganized.router.struct.Node;

/**
 * ·�������;����������߳�
 * 
 * @author Sine
 *
 */
public class Sender extends Thread {

	private final DVService service = DVService.getInstance();
	public Sender() {
	}

	@Override
	public void run() {
		synchronized (this) {
			try {
				wait(); // ������ǰ�����̣߳�����·����һͬ��ʼ
			} catch (InterruptedException e1) {
				// TODO InterruptedException ��֪��ʱ�ᷢ����notify��ʱ�򲢲���
				e1.printStackTrace();
			}
			while (true) {
				try {
					wait(2000);
				} catch (InterruptedException e1) {
					// TODO InterruptedException ��֪��ʱ�ᷢ����notify��ʱ�򲢲���
					e1.printStackTrace();
				}
				// ��ʼ����·�ɱ�
				for (Node neibour : service.getNeighbors()) {
					try {
						DVMessage message = new DVMessage();
						message.sender = service.getMe();
						message.dv = service.getDV();
						ObjectUtil.send(neibour, message);
						// service.debug("���;���������" + neibour + "�ɹ�");
					} catch (IOException e) {
						service.debug("���;���������" + neibour + "ʧ��: " + e);
						// TODO �޸�·�ɱ�
					}
				}
			}
		}

	}

}
