package selforganized;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import selforganized.exception.MyException;
import selforganized.router.Router;
import selforganized.router.struct.Node;

public class Client {

	static final String prefix = "client";
	static final String postfix = ".txt";
	static final String sendFormat = "send [address]:[port] [text]";
	static final String setFormat = "set [neibour address]:[port] [positive integer]|-1";

	public static void main(String[] args) throws NumberFormatException, IOException, MyException {
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
					if (tokens.length < 2) {
						sysout("指令格式：" + sendFormat);
						break;
					}
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
					if (tokens.length < 2) {
						sysout("指令格式：" + setFormat);
						break;
					}
					tokens = tokens[1].split(" ");
					if (tokens.length != 2) {
						sysout("指令格式：" + setFormat);
						break;
					}
					Node neibour = new Node(tokens[0]);
					int dis = Integer.parseInt(tokens[1]);
					String msg2 = router.change(neibour, dis);
					sysout(msg2);
					break;

				case "?":
					sysout("指令：");
					sysout(sendFormat);
					sysout(setFormat);
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
