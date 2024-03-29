// author: DHL brnpoem@gmail.com

package edu.zju.dcd.edl.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPInputStream;

import edu.zju.dcd.edl.ELConsts;

public class IOUtils {
	public static String readTextFile(String fileName) {
		BufferedReader reader = getUTF8BufReader(fileName);
		try {
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			reader.close();
			
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void writeIntAtFileBeg(String fileName, int val) {
		try {
			RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
			raf.writeInt(val);
			raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static int readLittleEndianInt(FileChannel fc) {
		ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		try {
			fc.read(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
		buf.rewind();
		return buf.asIntBuffer().get();
	}
	
	public static long readLittleEndianLong(FileChannel fc) {
		ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		try {
			fc.read(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
		buf.rewind();
		return buf.asLongBuffer().get();
	}
	
	public static String readMidInByteArr(DataInputStream dis) {
		byte[] tmp = new byte[ELConsts.MID_BYTE_LEN];
		try {
			dis.read(tmp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return (new String(tmp)).trim();
	}
	
	public static void writeMidAsByteArr(DataOutputStream dos, String mid) {
		byte[] tmp = mid.getBytes();
		try {
			dos.write(tmp);
			int cnt = ELConsts.MID_BYTE_LEN - tmp.length;
			while (cnt-- > 0)
				dos.write(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeMidAsByteArr(BufferedOutputStream bos, String mid) {
		byte[] tmp = mid.getBytes();
		try {
			bos.write(tmp);
			int cnt = ELConsts.MID_BYTE_LEN - tmp.length;
			while (cnt-- > 0)
				bos.write(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String addSuffixToFileName(String fileName, String suffix) {
		StringBuilder sb = new StringBuilder();
		
		int pos = fileName.lastIndexOf('.');
		if (pos < 0)
			sb.append(fileName).append(suffix);
			
		sb.append(fileName.substring(0, pos)).append(suffix);
		sb.append(fileName.substring(pos, fileName.length()));
		
		return new String(sb);
	}

	// do not forget to close!
	public static BufferedReader getUTF8BufReader(String fileName) {
		try {
			FileInputStream inStream = new FileInputStream(fileName);
			InputStreamReader isr = new InputStreamReader(inStream, "UTF8");
			BufferedReader reader = new BufferedReader(isr);
			
			return reader;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static BufferedWriter getUTF8BufWriter(String fileName) {
		return getUTF8BufWriter(fileName, true);
	}

	// do not forget to close!
	public static BufferedWriter getUTF8BufWriter(String fileName, boolean checkExistance) {
		try {
			File f = new File(fileName);
			if (checkExistance && f.exists()) {
				System.out.println(fileName + " already exits!");
				return null;
			}
			
			FileOutputStream outStream = new FileOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter(outStream, "UTF8");
			BufferedWriter writer = new BufferedWriter(osw);
			
			return writer;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	// do not forget to close!
	public static BufferedReader getGZIPBufReader(String filePath) {
		try {
			FileInputStream fileStream = new FileInputStream(filePath);
			GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
			InputStreamReader decoder = new InputStreamReader(gzipStream,
					"UTF8");
			BufferedReader bufReader = new BufferedReader(decoder);

			return bufReader;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static DataOutputStream getBufferedDataOutputStream(String fileName) {
		try {
			return new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(fileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static DataInputStream getBufferedDataInputStream(String fileName) {
		try {
			return new DataInputStream(new BufferedInputStream(
					new FileInputStream(fileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void writeNumLinesFileFor(String srcFileName, long numLines) {
		String fileName = addSuffixToFileName(srcFileName,
				ELConsts.NUM_LINES_FILE_SUFFIX);
		writeIntValueFile(fileName, numLines);
	}

	public static void writeIntValueFile(String fileName, long val) {
		try {
			OutputStreamWriter writer = new OutputStreamWriter(
					new FileOutputStream(fileName), "UTF8");

			writer.write(String.valueOf(val));

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int getNumLinesFor(String srcFileName) {
		return getIntValueFromFile(getNumLinesFileName(srcFileName));
	}

	public static String getNumLinesFileName(String srcFileName) {
		return addSuffixToFileName(srcFileName,
				ELConsts.NUM_LINES_FILE_SUFFIX);
	}

	public static int getIntValueFromFile(String fileName) {
		int val = -1;
		try {
			InputStreamReader reader = new InputStreamReader(
					new FileInputStream(fileName), "UTF8");

			char[] buf = new char[256];
			int len = reader.read(buf);
			String str = new String(buf, 0, len).trim();
			// System.out.println("to int " + str);
			val = Integer.valueOf(str);

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return val;
	}

	public static String[] getMidList(String midListFile) {
		BufferedReader reader = getUTF8BufReader(midListFile);

		String[] mids = null;
		try {
			int numMids = Integer.valueOf(reader.readLine());
			mids = new String[numMids];

			for (int i = 0; i < numMids; ++i) {
				mids[i] = reader.readLine().trim();
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return mids;
	}
	
	
	public static int countLinesInFile(String fileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);

		int cnt = 0;
		try {
			while (reader.readLine() != null) {
				++cnt;
				
				if (cnt % 1000000 == 0)
					System.out.println(cnt);
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(cnt + " lines in " + fileName);
		return cnt;
	}
	
	public static String readLines(BufferedReader reader, int numLines) {
		String line = null;
		StringBuilder sb = new StringBuilder();
		int cnt = 0;
		try {
			while (cnt < numLines && (line = reader.readLine()) != null) {
				sb.append(line);
				if (cnt < numLines - 1)
					sb.append("\n");
				++cnt;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new String(sb);
	}
}
