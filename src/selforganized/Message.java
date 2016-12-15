package selforganized;

import java.io.Serializable;

public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Node src;
	public Node dst;
	public Node sender;
	public String text;
}
