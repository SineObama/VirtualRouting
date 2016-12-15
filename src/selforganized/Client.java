package selforganized;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

	static final String prefix = "client";
	static final String postfix = ".txt";

	public static void main(String[] args) {
		// �Ӳ�����ȡ�����ļ�����׺
		int i = 0;
		if (args.length == 1)
			i = Integer.parseInt(args[0]);
		File file = new File(prefix + i + postfix);
		if (!file.exists() || !file.isFile())
			throw new RuntimeException("�Ҳ���·����Ϣ�ļ�");

		// ����·����
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
					String msg = null;
					if (tokens.length == 2)
						msg = tokens[1];
					else
						msg = "";
					Node node = new Node(tokens[0]);
					router.send(node, msg);
					break;

				default:
					break;
				}
			} catch (MyException e) {
				System.out.println(e.getMessage());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
