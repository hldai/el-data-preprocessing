package edu.zju.dcd.utils;

public class MathUtils {
	public static void toUnitVector(float[] vec) {
		float sqrSum = 0;
		for (float v : vec) {
			sqrSum += v * v;
		}
		
		sqrSum = (float) Math.sqrt(sqrSum);
		
		for (int i = 0; i < vec.length; ++i) {
			vec[i] /= sqrSum;
		}
	}
}
