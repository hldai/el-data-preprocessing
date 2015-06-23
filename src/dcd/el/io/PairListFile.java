// author: DHL brnpoem@gmail.com

package dcd.el.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class PairListFile {
	public static class StringIntArray {
		public String[] keys = null;
		public int[] values = null;
	}

	public static class StringDoubleArray {
		public String[] keys = null;
		public double[] values = null;
	}
	
	public static StringDoubleArray loadStringDoublePairFile(String fileName) {
		int numLines = IOUtils.getNumLinesFor(fileName);
		
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		StringDoubleArray sda = new StringDoubleArray();
		sda.keys = new String[numLines];
		sda.values = new double[numLines];
		String line = null;
		try {
			for (int i = 0; i < numLines; ++i) {
				line = reader.readLine();

				String[] vals = line.split("\t");
				sda.keys[i] = vals[0];
				sda.values[i] = Double.valueOf(vals[1]);
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return sda;
	}

	public static StringIntArray loadStringIntPairFile(String fileName) {
		int numLines = IOUtils.getNumLinesFor(fileName);

		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		StringIntArray sia = new StringIntArray();
		sia.keys = new String[numLines];
		sia.values = new int[numLines];
		String line = null;
		try {
			for (int i = 0; i < numLines; ++i) {
				line = reader.readLine();

				String[] vals = line.split("\t");
				sia.keys[i] = vals[0];
				sia.values[i] = Integer.valueOf(vals[1]);
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sia;
	}

	// TODO remove
	public static void mergePairListFiles(String[] srcFileNames,
			String dstFileName) {
		BufferedReader reader = null;
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		try {
			for (String fileName : srcFileNames) {
				System.out.println("processing " + fileName);

				reader = IOUtils.getUTF8BufReader(fileName);

				String line = null;
				while ((line = reader.readLine()) != null) {
					String[] vals = line.split("\t");
					if (vals.length == 2) {
						// writer.write(line + "\n");
						writer.write(vals[0].trim() + "\t" + vals[1].trim()
								+ "\n");
					}
				}

				reader.close();
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void swap(String fileName, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");

				writer.write(vals[1] + "\t" + vals[0] + "\n");
			}

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// e.g. idxCmp0 = 1, idxCmp1 = 0. if file0 has a b, file1 has b c, then
	// dstFile has a c
	public static void pairListFileMap(String fileName0, int idxCmp0,
			String fileName1, int idxCmp1, boolean filterUnMatched,
			String dstFileName) {
		BufferedReader reader0 = IOUtils.getUTF8BufReader(fileName0), reader1 = IOUtils
				.getUTF8BufReader(fileName1);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		String line0 = null, line1 = null;
		try {
			line1 = reader1.readLine();
			String[] vals1 = line1.split("\t");
			int cnt = 0;
			while ((line0 = reader0.readLine()) != null) {
				String[] vals0 = line0.split("\t");

				if (line1 == null) {
					if (filterUnMatched)
						break;
					writer.write(vals0[1 - idxCmp0] + "\t" + vals0[idxCmp0]
							+ "\n");
					continue;
				}

				// System.out.println(vals0[idxCmp0] + "%" + vals1[idxCmp1]);
				// int rcnt = 0;
				int cmpVal = 0;
				while (line1 != null
						&& (cmpVal = vals0[idxCmp0].compareTo(vals1[idxCmp1])) > 0) {
					line1 = reader1.readLine();
					// ++rcnt;
					if (line1 != null)
						vals1 = line1.split("\t");
				}
				// System.out.println(rcnt);

				if (cmpVal == 0) {
					writer.write(vals0[1 - idxCmp0] + "\t" + vals1[1 - idxCmp1]
							+ "\n");
				} else if (!filterUnMatched) {
					writer.write(vals0[1 - idxCmp0] + "\t" + vals0[idxCmp0]
							+ "\n");
				}

				++cnt;

				// if (cnt == 100) break;
			}

			reader0.close();
			reader1.close();
			writer.close();

			System.out.println(cnt + " lines processed.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void toLowerCase(String srcFileName, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(srcFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				writer.write(line.toLowerCase() + "\n");
			}

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void check(String fileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		System.out.println("Checking " + fileName);

		String line = null;

		try {
			long cnt = 0;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");

				if (vals.length != 2) {
					System.out.println(line);
					break;
				}

				++cnt;
				if (cnt % 100000000 == 0)
					System.out.println(cnt);
			}

			reader.close();
			System.out.println(cnt + " lines checked.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
