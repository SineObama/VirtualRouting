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
		// ��ȡ��̬·�ɵ�ַ�˿ں;���
		// �ļ���ʽ����һ�����Լ��ĵ�ַ�Ͷ˿ڣ��������������ھӵ�ַ���˿ں;���
		List<Thread> list = new ArrayList<>();
		for (;; i++) {
			File file = new File(prefix + i + postfix);
			if (!file.exists() || !file.isFile())
				break;
			Router router=new Router(file);
			list.add(router);
			router.start();
		}
		System.out.println("���ҵ�" + list.size() + "���ļ����߳�������");
		for (Thread thread : list)
			thread.join();
	}

}
