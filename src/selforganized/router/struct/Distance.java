package selforganized.router.struct;

import java.io.Serializable;

public class Distance implements Serializable, Comparable<Distance> {
	private static final long serialVersionUID = 7134243725393718049L;
	private int dis = 0;

	public Distance(int dis) {
		if (dis >= 0)
			this.dis = dis;
		else
			this.dis = Integer.MAX_VALUE;
	}

	public Distance(Distance dis) {
		this.dis = dis.dis;
	}

	public boolean isUnreachable() {
		return dis == Integer.MAX_VALUE;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Distance) {
			Distance o = (Distance) obj;
			return this.dis == o.dis;
		}
		return false;
	}

	@Override
	public int compareTo(Distance o) {
		return dis - o.dis;
	}

	@Override
	public String toString() {
		return isUnreachable() ? "����" : Integer.toString(dis);
	}

	public static Distance add(Distance dis1, Distance dis2) {
		int total = dis1.dis + dis2.dis;
		if (total < 0) // ������Ϊ����һ��Ϊ���ɴ�������Ҳ���ɴ�
			total = Integer.MAX_VALUE;
		return new Distance(total);
	}
	
	public static Distance getUnreachable() {
		return new Distance(Integer.MAX_VALUE);
	}
}
