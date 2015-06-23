// author: DHL brnpoem@gmail.com

package dcd.el.feature;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SingleIntFeature extends Feature {

	@Override
	public boolean fromFile(RandomAccessFile raf) {
		try {
			if (raf.getFilePointer() >= raf.length())
				return false;

			value = raf.readInt();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean fromFile(DataInputStream dis) {
		try {
			if (dis.available() <= 0)
				return false;

			value = dis.readInt();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void toFile(DataOutputStream dos) {
		try {
			dos.writeInt(value);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toFile(RandomAccessFile raf) {
		try {
			raf.writeInt(value);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getValue() {
		return value;
	}

	public int value;
}
