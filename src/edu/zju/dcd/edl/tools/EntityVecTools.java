package edu.zju.dcd.edl.tools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

import edu.zju.dcd.edl.ELConsts;
import edu.zju.dcd.edl.io.IOUtils;

public class EntityVecTools {
	private static class WidDivisor implements Comparable<WidDivisor> {
		int wid;
		double divisor;
		
		@Override
		public int compareTo(WidDivisor widDivisor) {
			return this.wid - widDivisor.wid;
		}
	}
	
	public static void sortDivisorsFile(String fileName, String dstFileName) {
		DataInputStream dis = IOUtils.getBufferedDataInputStream(fileName);
		try {
			int numWids = dis.readInt();
			System.out.println(numWids + " wids.");
			WidDivisor[] widDivisors = new WidDivisor[numWids];
			for (int i = 0; i < numWids; ++i) {
				widDivisors[i] = new WidDivisor();
				widDivisors[i].wid = dis.readInt();
				widDivisors[i].divisor = dis.readDouble();
			}
			
			dis.close();
			
			Arrays.sort(widDivisors);

			DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
			dos.writeInt(numWids);
			for (WidDivisor widDivisor : widDivisors) {
				dos.writeInt(widDivisor.wid);
				dos.writeDouble(widDivisor.divisor);
			}
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void mergeEntityVecs(String dataPath, String dstFileName) {
		File dirFile = new File(dataPath);
		File[] files = dirFile.listFiles();
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		int numEntities = 0, vecLen = 0;
		try {
			dos.writeInt(numEntities);
			dos.writeInt(vecLen);
			
			for (File f : files) {
				if (!f.isFile()) {
					continue;
				}
				System.out.println(f.getAbsolutePath());

				FileInputStream fis = new FileInputStream(
						f.getAbsolutePath());
				FileChannel fc = fis.getChannel();
				int numWids = IOUtils.readLittleEndianInt(fc);
				vecLen = IOUtils.readLittleEndianInt(fc);
				System.out.println(numWids + "\t" + vecLen);
				numEntities += numWids;
				float[] entityVec = new float[vecLen];
				ByteBuffer buf = ByteBuffer
						.allocate((int) (vecLen * Float.BYTES));
				buf.order(ByteOrder.LITTLE_ENDIAN);
				for (int i = 0; i < numWids; ++i) {
					int wid = IOUtils.readLittleEndianInt(fc);
					fc.read(buf);
					buf.rewind();
					buf.asFloatBuffer().get(entityVec);
					
					dos.writeInt(wid);
					for (float v : entityVec) {
						dos.writeFloat(v);
					}

//					if (i == 5)
//						break;
					if (i % 100000 == 0)
						System.out.println(i);
				}
				fis.close();
			}
			dos.close();
			
			RandomAccessFile raf = new RandomAccessFile(dstFileName, "rw");
			raf.writeInt(numEntities);
			raf.writeInt(vecLen);
			raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genPredictBaseFile(String entityVecFileDir, String outputVecFileName,
			String dstFileName) {
		DivisorFileGenThread.initOutputVecs(outputVecFileName);
		File dirFile = new File(entityVecFileDir);
		File[] files = dirFile.listFiles();
		LinkedList<DivisorFileGenThread> divisorFileGenThreads = new LinkedList<DivisorFileGenThread>();
		LinkedList<String> tempFileNames = new LinkedList<String>();
		int cnt = 0;
		for (File f : files) {
			if (!f.isFile()) {
				continue;
			}
			
			String tmpDstFileName = Paths.get(ELConsts.TMP_FILE_PATH, "divisors_" + cnt + ".bin").toString();
			tempFileNames.add(tmpDstFileName);
			
			DivisorFileGenThread thread = new DivisorFileGenThread(f.getAbsolutePath(), tmpDstFileName);
			divisorFileGenThreads.add(thread);
			thread.start();
			++cnt;
		}
		try {
			for (DivisorFileGenThread thread : divisorFileGenThreads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("done.");
		
		mergeTempDivisorFiles(tempFileNames, dstFileName);
	}
	
	private static void mergeTempDivisorFiles(LinkedList<String> fileNames, String dstFileName) {
		int numWids = 0;
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			dos.writeInt(numWids);
			
			for (String fileName : fileNames) {
				DataInputStream dis = IOUtils.getBufferedDataInputStream(fileName);
				while (dis.available() > 0) {
					int wid = dis.readInt();
					double divisor = dis.readDouble();
					dos.writeInt(wid);
					dos.writeDouble(divisor);
					++numWids;
				}
				dis.close();
			}
			dos.close();
			
			RandomAccessFile raf = new RandomAccessFile(dstFileName, "rw");
			raf.writeInt(numWids);
			raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
