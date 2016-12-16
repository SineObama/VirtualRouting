package selforganized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import selforganized.router.struct.Node;

public class ObjectUtil {
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T clone(T obj) {
		if (obj == null)
			return null;
		T clonedObj = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.close();

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			clonedObj = (T) ois.readObject();
			ois.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return clonedObj;
	}

	public static void send(Node node, Object obj) throws UnknownHostException, IOException {
		Socket socket = new Socket(node.addr, node.port);
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		objectOutputStream.writeObject(obj);
		socket.close();
	}

	public static Object receive(ServerSocket serverSocket) throws IOException, ClassNotFoundException {
		Socket socket = serverSocket.accept();
		ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
		Object obj = objectInputStream.readObject();
		socket.close();
		return obj;
	}
}
