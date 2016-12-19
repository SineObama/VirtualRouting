package selforganized.router.struct;

import java.io.Serializable;

public class DCMessage implements Serializable {
	private static final long serialVersionUID = 5448188632945575068L;
	public Node sender;
	public Distance distance;

	public DCMessage(Node sender, Distance dis) {
		this.sender = sender;
		this.distance = dis;
	}
}
