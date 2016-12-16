package selforganized.router;

import selforganized.router.struct.Node;

public interface IRouter {
	String send(Node dst, String msg);
	String change(Node neibour, int dis);
}
