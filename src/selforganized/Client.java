package selforganized;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import selforganized.exception.FormatException;
import selforganized.exception.MyException;
import selforganized.router.Router;
import selforganized.router.struct.Node;

public class Client {

	static final String prefix = "client";
	static final String postfix = ".txt";

	public static void main(String[] args) throws NumberFormatException, FormatException, IOException {
		// �Ӳ�����ȡ�����ļ���Ŀ¼���ļ�����׺
		int i = 0;
		String path = "./";
		if (args.length > 0)
			i = Integer.parseInt(args[0]);
		if (args.length == 2)
			path = args[1];

		// �ҵ������ļ�������·����
		File file = new File(path + prefix + i + postfix);
		if (!file.exists() || !file.isFile())
			throw new RuntimeException("�Ҳ���·����Ϣ�ļ�");
		Router router = new Router(file);
		router.start();

		// ��ʼ�������ж�ȡָ��
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);
		while (true) {
			try {
				String line = in.nextLine();
				String[] tokens = line.split(" ", 2);
				switch (tokens[0]) {
				case "send":
					if (tokens.length < 2) {
						sysout("ָ���ʽ��send [address]:[port] [text]");
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
						sysout("ָ���ʽ��set [address]:[port] [positive integer]");
						break;
					}
					tokens = tokens[1].split(" ");
					if (tokens.length != 2) {
						sysout("ָ���ʽ��set [neibour address]:[port] [positive integer]");
						break;
					}
					Node neibour = new Node(tokens[0]);
					int dis = Integer.parseInt(tokens[1]);
					String msg2 = router.change(neibour, dis);
					sysout(msg2);
					break;

				case "?":
					sysout("ָ�");
					sysout("send [address]:[port] [text]");
					sysout("set [address]:[port] [positive integer]");
					break;

				default:
					if (line.length() != 0)
						sysout("ָ�����");
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
