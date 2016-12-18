package selforganized.router;

import java.io.IOException;

import selforganized.ObjectUtil;
import selforganized.router.struct.DVMessage;
import selforganized.router.struct.Node;

/**
 * 路由器发送距离向量的线程
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
				wait(); // 用于提前创建线程，但与路由器一同开始
			} catch (InterruptedException e1) {
				// TODO InterruptedException 不知何时会发生。notify的时候并不会
				e1.printStackTrace();
			}
			while (true) {
				try {
					wait(2000);
				} catch (InterruptedException e1) {
					// TODO InterruptedException 不知何时会发生。notify的时候并不会
					e1.printStackTrace();
				}
				// 开始发送路由表
				for (Node neibour : service.getNeighbors()) {
					try {
						DVMessage message = new DVMessage();
						message.sender = service.getMe();
						message.dv = service.getDV();
						ObjectUtil.send(neibour, message);
						// service.debug("发送距离向量到" + neibour + "成功");
					} catch (IOException e) {
						service.debug("发送距离向量到" + neibour + "失败: " + e);
						// TODO 修改路由表？
					}
				}
			}
		}

	}

}
