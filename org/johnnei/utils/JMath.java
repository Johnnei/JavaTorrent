package org.johnnei.utils;

public class JMath {
	
	public static int max(int i, int j) {
		return i > j ? i : j;
	}
	
	public static int min(int i, int j) {
		return i < j ? i : j;
	}
	
	public static int ceil(double d) {
		return (int)Math.ceil(d);
	}

}
