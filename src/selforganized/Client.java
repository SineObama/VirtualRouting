package selforganized;

import java.io.File;
import java.util.Scanner;

public class Client {

	static final String prefix = "client";
	static final String postfix = ".txt";

	public static void main(String[] args) {
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
					if (tokens.length < 2)
						throw new MyException("sendָ���ʽ����");
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
