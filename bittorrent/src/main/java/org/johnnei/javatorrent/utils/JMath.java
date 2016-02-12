package org.johnnei.javatorrent.utils;

public class JMath {
	
	public static int ceilDivision(int value, int dividor) {
		return (value + (dividor - 1)) / dividor;
	}
	
	public static long ceilDivision(long value, int dividor) {
		return (value + (dividor - 1)) / dividor;
	}
	
	public static int diff(int a, int b) {
		return Math.abs(a - b);
	}

}
