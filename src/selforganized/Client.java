package selforganized;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Client {

	static final String prefix = "client";
	static final String postfix = ".txt";

	public static void main(String[] args) throws InterruptedException {
		int i = 0;
		if (args.length == 1)
			i = Integer.parseInt(args[0]);
		// 读取静态路由地址端口和距离
		// 文件格式：第一行是自己的地址和端口，后面若干行是邻居地址、端口和距离
		List<Thread> list = new ArrayList<>();
		for (;; i++) {
			File file = new File(prefix + i + postfix);
			if (!file.exists() || !file.isFile())
				break;
			Router router=new Router(file);
			list.add(router);
			router.start();
		}
		System.out.println("共找到" + list.size() + "个文件，线程已启动");
		for (Thread thread : list)
			thread.join();
	}

}
