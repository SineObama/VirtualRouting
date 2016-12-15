package selforganized;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

	static final String prefix = "client";
	static final String postfix = ".txt";

	public static void main(String[] args) {
		int i = 0;
		if (args.length == 1)
			i = Integer.parseInt(args[0]);
		File file = new File(prefix + i + postfix);
		if (!file.exists() || !file.isFile())
			throw new RuntimeException("找不到路由信息文件");

		// 启动路由器
		Router router = new Router(file);
		router.start();

		// 开始读取指令
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);
		while (true) {
			try {
				String line = in.nextLine();
				String[] tokens = line.split(" ");
				Node node = new Node(tokens[0], Integer.parseInt(tokens[1]));
				String msg = line.substring(tokens[0].length() + tokens[1].length() + 2);
				String info;
				info = router.send(node, msg);
				System.out.println(info);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
