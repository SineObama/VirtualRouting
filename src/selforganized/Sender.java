package selforganized;

import java.io.IOException;

/**
 * 路由器发送距离向量的线程
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
				// 开始发送路由表
				for (Node node : router.getNeibours()) {
					try {
						if (node.equals(router.getMe()))
							continue;
						ObjectUtil.send(node, router.getDV());
//						router.debug("发送距离向量到" + info + "成功");
					} catch (IOException e) {
						router.debug("发送距离向量到" + node + "失败: " + e);
					}
				}
			}
		}

	}
}
