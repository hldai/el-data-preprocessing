// author: DHL brnpoem@gmail.com

package dcd.el.io;

import java.io.FileInputStream;
import java.io.IOException;

public class ByteLineReader {
	public static final int MAX_BUF_LEN = 1024 * 1024;
	public static byte ASCII_LF = 10;

	public void open(String fileName) {
		curBuf = buf0;
		nextBuf = buf1;
		
		try {
			fis = new FileInputStream(fileName);
			curBufLen = fis.read(curBuf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String nextLine() {
		if (curBufLen == -1)
			return null;

		int lineLen = nextLFPos(curBuf, curBufLen, lineStartPos);

		try {
			if (lineLen > -1) {
				curLinePos = curBufPos + lineStartPos;
				String line = new String(curBuf, lineStartPos, lineLen, "UTF-8");
				lineStartPos += lineLen + 1;
				checkLineStartPos();
				return line;
			}

			int nlen = fis.read(nextBuf);
			if (nlen == -1) {
				String line = new String(curBuf, lineStartPos, curBufLen
						- lineStartPos, "UTF-8");
				curLinePos = curBufPos + lineStartPos;
				curBufLen = -1;
				return line;
			}

			lineLen = nextLFPos(nextBuf, nlen, 0);
			if (lineLen < 0) {
				if (nlen == MAX_BUF_LEN) // TODO use fis.available?
					System.out.println("Line may be too long!");

				String sl = new String(curBuf, lineStartPos, curBufLen
						- lineStartPos, "UTF-8"), sr = new String(nextBuf, 0,
						nlen, "UTF-8");
				curLinePos = curBufPos + lineStartPos;
				return sl + sr;
			}

			String sl = new String(curBuf, lineStartPos, curBufLen
					- lineStartPos, "UTF-8"), sr = new String(nextBuf, 0,
					lineLen, "UTF-8");
			curLinePos = curBufPos + lineStartPos;

			lineStartPos = lineLen + 1;
			curBufPos += curBufLen;
			curBufLen = nlen;
			// swap
			byte[] tmp = curBuf;
			curBuf = nextBuf;
			nextBuf = tmp;

			checkLineStartPos();
			return sl + sr;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public void close() {
		try {
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public long getCurLinePos() {
		return curLinePos;
	}

	private void checkLineStartPos() {
		if (lineStartPos >= curBufLen) {
			curBufPos += curBufLen;

			try {
				curBufLen = fis.read(curBuf);
			} catch (IOException e) {
				e.printStackTrace();
			}
			lineStartPos = 0;
		}
	}

	private static int nextLFPos(byte[] buf, int bufLen, int begPos) {
		int len = 0;
		while (begPos + len < bufLen && buf[begPos + len] != ASCII_LF)
			++len;

		return begPos + len < bufLen ? len : -1;
	}

	private byte[] buf0 = new byte[MAX_BUF_LEN], buf1 = new byte[MAX_BUF_LEN];
	private byte[] curBuf = null, nextBuf = null;
	private int curBufLen = -1;
	private long curBufPos = 0;
	private int lineStartPos = 0;
	private long curLinePos = 0;

	private FileInputStream fis = null;
}
