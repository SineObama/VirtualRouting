package selforganized;

import java.io.File;
import java.util.Map.Entry;
import java.util.Scanner;

import selforganized.exception.MyException;
import selforganized.router.Router;
import selforganized.router.struct.DV;
import selforganized.router.struct.Node;
import selforganized.router.struct.RouteInfo;

public class Client {

	static final String sendFormat = "send [address]:[port] [text]";
	static final String setFormat = "set [neibour address]:[port] [positive integer]|-1";

	public static void main(String[] args) throws MyException {
		// 从参数读取配置文件全路径
		// 找到配置文件，启动路由器
		File file = new File(args[0]);
		Router router = null;
		try {
			if (!file.exists() || !file.isFile())
				throw new MyException("找不到路由信息文件");
			router = new Router(file);
			router.start();
		} catch (Exception e) {
			throw new MyException("初始化出错: " + e);
		}

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

				case "show":
					DV dv = router.getDV();
					sysout("目的地\t\t下一跳\t\t总距离");
					for (Entry<Node, RouteInfo> entry : dv.entrySet())
						sysout(entry.getKey() + "\t" + entry.getValue().next + "\t" + entry.getValue().dis);
					sysout("");
					break;

				case "shutdown":
					router.shutdown();
					break;

				case "?":
					sysout("指令：");
					sysout(sendFormat);
					sysout(setFormat);
					sysout("show");
					sysout("shutdown");
					break;

				default:
					if (line.length() != 0)
						sysout("指令错误");
					break;
				}
			} catch (MyException e) {
				sysout(e.getMessage());
			} catch (Exception e) {
				sysout("出错: " + e);
			}
		}
	}

	public static void sysout(String msg) {
		System.out.println(msg);
	}
}
