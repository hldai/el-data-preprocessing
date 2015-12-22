package edu.zju.dcd.edl.tools;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import edu.zju.dcd.edl.io.IOUtils;
import edu.zju.dcd.edl.utils.MathUtils;

public class DivisorFileGenThread extends Thread {
	public static void initOutputVecs(String outputVecFileName) {
		try {
			FileInputStream fis = new FileInputStream(outputVecFileName);
			FileChannel fc = fis.getChannel();
			long numVecs = IOUtils.readLittleEndianLong(fc),
					vecLen = IOUtils.readLittleEndianLong(fc);
			System.out.println(numVecs + "\t" + vecLen);
			outputVecs = new float[(int) numVecs][];
			ByteBuffer buf = ByteBuffer.allocate((int) (vecLen * Float.BYTES));
			buf.order(ByteOrder.LITTLE_ENDIAN);
			for (int i = 0; i < numVecs; ++i) {
				outputVecs[i] = new float[(int) vecLen];
				fc.read(buf);
				buf.rewind();
				buf.asFloatBuffer().get(outputVecs[i]);
			}
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public DivisorFileGenThread(String entityVecFileName, String dstFileName) {
		this.entityVecFileName = entityVecFileName;
		this.dstFileName = dstFileName;
	}
	
	public void run() {
		System.out.println(entityVecFileName + "\t" + dstFileName);
		try {
			FileInputStream fis = new FileInputStream(entityVecFileName);
			FileChannel fc = fis.getChannel();
			int numWids = IOUtils.readLittleEndianInt(fc);
			int vecLen = IOUtils.readLittleEndianInt(fc);
			
			DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
			
			float[] entityVec = new float[vecLen];
			ByteBuffer buf = ByteBuffer
					.allocate((int) (vecLen * Float.BYTES));
			buf.order(ByteOrder.LITTLE_ENDIAN);
			for (int i = 0; i < numWids; ++i) {
				int wid = IOUtils.readLittleEndianInt(fc);
				fc.read(buf);
				buf.rewind();
				buf.asFloatBuffer().get(entityVec);
				
				double result = 0;
				for (int j = 0; j < outputVecs.length; ++j) {
					result += Math.exp(MathUtils.dotProduct(outputVecs[j], entityVec));
				}
				
				dos.writeInt(wid);
				dos.writeDouble(result);
				
				System.out.println("wid: " + wid + "\t" + result);

				if (i == 10)
					break;
				if ((i + 1) % 10000 == 0)
					System.out.println(i + 1);
			}
			
			fis.close();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	String entityVecFileName = null;
	String dstFileName = null;
	
	private static float[][] outputVecs = null;
}
