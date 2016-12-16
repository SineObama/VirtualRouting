package selforganized.router.struct;

import java.io.Serializable;

/**
 * ±¨ÎÄ
 * 
 * @author Sine
 *
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 2861019874243079729L;
	public Node src;
	public Node dst;
	public Node sender;
	public String text;
}
