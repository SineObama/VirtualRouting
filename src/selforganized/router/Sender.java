package selforganized.router;

import java.io.IOException;

import selforganized.ObjectUtil;
import selforganized.router.struct.Node;

/**
 * 路由器发送距离向量的线程
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
				wait(); // 用于提前创建线程，但与路由器一同开始
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
				// 开始发送路由表
				for (Node node : dao.getNeibour()) {
					try {
						if (node.equals(router.me))
							continue;
						ObjectUtil.send(node, dao.getDV(router.me));
					} catch (IOException e) {
						router.debug("发送距离向量到" + node + "失败: " + e);
						// TODO 修改路由表？
					}
				}
			}
		}

	}

}
