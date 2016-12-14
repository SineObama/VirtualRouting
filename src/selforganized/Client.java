package selforganized;

import java.io.File;
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
			throw new RuntimeException("无法读取路由信息");
		Router router = new Router(file);
		router.start();
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);
		while (true) {
			String line = in.nextLine();
			String[] tokens = line.split(" ");
			Node info = new Node(tokens[0], Integer.parseInt(tokens[1]));
			String msg = line.substring(tokens[0].length() + tokens[1].length() + 2);
			String rtn = router.send(info, msg);
			System.out.println(rtn);
		}
	}

}
