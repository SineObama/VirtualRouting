package selforganized;

import java.io.File;
import java.util.Scanner;

public class Client {

	static final String prefix = "client";
	static final String postfix = ".txt";

	public static void main(String[] args) {
		// 从参数读取配置文件根目录与文件名后缀
		int i = 0;
		String path = "./";
		if (args.length > 0)
			i = Integer.parseInt(args[0]);
		if (args.length == 2)
			path = args[1];

		// 找到配置文件，启动路由器
		File file = new File(path + prefix + i + postfix);
		if (!file.exists() || !file.isFile())
			throw new RuntimeException("找不到路由信息文件");
		Router router = new Router(file);
		router.start();

		// 开始从命令行读取指令
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);
		while (true) {
			try {
				String line = in.nextLine();
				String[] tokens = line.split(" ", 2);
				switch (tokens[0]) {
				case "send":
					if (tokens.length < 2)
						throw new MyException("send指令格式错误");
					tokens = tokens[1].split(" ", 2);
					String text = null;
					if (tokens.length == 2)
						text = tokens[1];
					else
						text = "";
					Node node = new Node(tokens[0]);
					String msg = router.send(node, text);
					sysout(msg);
					break;

				case "set":
					break;

				default:
					if (line.length() != 0)
						sysout("指令错误");
					break;
				}
			} catch (MyException e) {
				sysout(e.getMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void sysout(String msg) {
		System.out.println(msg);
	}
}
