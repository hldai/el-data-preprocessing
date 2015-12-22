package edu.zju.dcd.edl.utils;

public class MathUtils {
	public static float dotProduct(float[] vec0, float[] vec1) {
		float rslt = 0;
		for (int i = 0; i < vec0.length; ++i) {
			rslt += vec0[i] * vec1[i];
		}
		return rslt;
	}
	
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
	
	public static void addTo(float[] mainVec, float[] nvec) {
		for (int i = 0; i < mainVec.length; ++i) {
			mainVec[i] += nvec[i];
		}
	}
	
	public static void divide(float[] vec, float val) {
		for (int i = 0; i < vec.length; ++i) {
			vec[i] /= val;
		}
	}
}
