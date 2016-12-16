package selforganized.router;

import selforganized.router.struct.Node;

public interface IRouter {
	String send(Node dst, String msg);
	void change(Node neibour, int dis);
}
