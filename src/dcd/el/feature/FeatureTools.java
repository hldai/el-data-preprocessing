// author: DHL brnpoem@gmail.com

package dcd.el.feature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import edu.zju.dcd.edl.ELConsts;
import dcd.el.documents.NewsDocument;
import edu.zju.dcd.edl.io.IOUtils;
import edu.zju.dcd.edl.obj.ByteArrayString;
import edu.zju.dcd.edl.utils.WidMidMapper;

public class FeatureTools {
	public static class FeaturePos implements Comparable<FeaturePos> {
		@Override
		public int compareTo(FeaturePos fp) {
			return mid.compareTo(fp.mid);
		}
		
		public ByteArrayString mid = null;
		public long filePointer = 0;
	}
	
	private static class MidPopularityEntry implements Comparable<MidPopularityEntry> {
		@Override
		public int compareTo(MidPopularityEntry entry) {
			return mid.compareTo(entry.mid);
		}
		
		public ByteArrayString mid;
		public float popularity;
	}

	public static void test() {		
//		String idfFileName = "d:/data/el/features/enwiki_idf.txt";
		String idfFileName = "d:/data/el/features/enwiki_idf.sd";
		String featFileName = "d:/data/el/features/enwiki_pop_tfidf.feat";
		String featIndexFileName = "d:/data/el/features/enwiki_pop_tfidf_index.txt";
		
//		TfIdfExtractor tfidf = new TfIdfExtractor(idfFileName);
		TfIdfExtractor tfidf = new TfIdfExtractor(idfFileName);
		
		FeatureLoader featLoad = new FeatureLoader(featFileName, featIndexFileName);
//		WikiFeaturesPack feats = featLoad.loadFeatures("01008_51");
//		System.out.println(feats.popularity.wid + "\t" + feats.popularity.getValue());
		System.out.println("foo");
		String tmpDocFileName0 = "d:/data/el/tmpdoc0.txt",
				tmpDocFileName1 = "d:/data/el/tmpdoc1.txt";
		NewsDocument doc0 = new NewsDocument(),
				doc1 = new NewsDocument();
		doc0.load(tmpDocFileName0);
		doc1.load(tmpDocFileName1);

		TfIdfFeature feat0 = tfidf.getTfIdf(doc0.getText()),
				feat1 = tfidf.getTfIdf(doc1.getText());
		FeaturePack feats = featLoad.loadFeatures("026djs");
		System.out.println(TfIdfFeature.similarity(feat0, feats.tfidf));
		System.out.println(TfIdfFeature.similarity(feat1, feats.tfidf));
		
		feats = featLoad.loadFeatures("0573h5");
		System.out.println(TfIdfFeature.similarity(feat0, feats.tfidf));
		System.out.println(TfIdfFeature.similarity(feat1, feats.tfidf));
//		String[] texts = { "a it fold",
//				"a it the" };
//		for (String text : texts) {
//			TfIdfFeature feat = tfidf.getTfIdf(text);
//			for (int i = 0; i < feat.termIndices.length; ++i) {
//				System.out.println(feat.termIndices[i] + "\t" + feat.values[i]);
//			}
//			System.out.println();
//		}
	}
	
	public static void genPopularityLink(String anchorCntFileName,
			String maxRefCntFileName, String dstFileName) {
		int maxRefCnt = IOUtils.getIntValueFromFile(maxRefCntFileName);
		BufferedReader reader = IOUtils.getUTF8BufReader(anchorCntFileName);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		
		try {
			String line = null;
			int cnt = 0;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				int wid = Integer.valueOf(vals[0]);
				int refCnt = Integer.valueOf(vals[1]);
				float pop = (refCnt + 1.0f) / (maxRefCnt + 1.0f); // scale to [0, 1]
				dos.writeInt(wid);
				dos.writeFloat(pop);
				
				if (cnt < 10)
					System.out.println(pop);
				++cnt;
			}
			System.out.println(cnt + " lines processed.");
			
			reader.close();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genEnityNumAliasesFile(String aliasListFileName,
			String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(aliasListFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		try {
			String line = null;
			String curMid = null;
			int curMidAliasCnt = 0;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				if (curMid == null || !curMid.equals(vals[0])) {
					if (curMid != null) {
						writer.write(curMid + "\t" + curMidAliasCnt + "\n");
					}
					
					curMidAliasCnt = 1;
					curMid = vals[0];
				} else {
					++curMidAliasCnt;
				}
			}
			writer.write(curMid + "\t" + curMidAliasCnt + "\n");
			
			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void mapMidToFeatIndex(String midToWidFileName,
			String widFeatIndexFileName, String dstFileName) {
		BufferedReader reader0 = IOUtils.getUTF8BufReader(midToWidFileName), reader1 = IOUtils
				.getUTF8BufReader(widFeatIndexFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		try {
			String line0 = null, line1 = null;
			int cnt = 0;
			while ((line0 = reader0.readLine()) != null) {
				line1 = reader1.readLine();

				String[] vals0 = line0.split("\t"), vals1 = line1.split("\t");

				if (!vals0[1].equals(vals1[0])) {
					System.err.println("wid not equal.");
					break;
				}

				writer.write(vals0[0] + "\t" + vals1[1] + "\n");
				++cnt;
			}

			reader0.close();
			reader1.close();
			writer.close();

			IOUtils.writeNumLinesFileFor(dstFileName, cnt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genMidPop(String midToWidFileName, String widPopFileName, String dstFileName) {
		WidMidMapper widToMidMapper = new WidMidMapper(midToWidFileName);
		DataInputStream dis = IOUtils.getBufferedDataInputStream(widPopFileName);
		
		LinkedList<MidPopularityEntry> midPopularityEntries = new LinkedList<MidPopularityEntry>();
		int widCnt = 0, midCnt = 0;
		try {
			while (dis.available() > 0) {
				++widCnt;
				int wid = dis.readInt();
				float popularity = dis.readFloat();
				String mid = widToMidMapper.getMid(wid);
				
				if (mid != null) {
					++midCnt;
					MidPopularityEntry entry = new MidPopularityEntry();
					entry.mid = new ByteArrayString(mid);
					entry.popularity = popularity;
					midPopularityEntries.add(entry);
				}
			}
			
			dis.close();
			System.out.println(widCnt + "\t" + midCnt);
			
			
			DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
			Collections.sort(midPopularityEntries);
			dos.writeInt(midPopularityEntries.size());
			for (MidPopularityEntry entry : midPopularityEntries) {
				entry.mid.toFileWithFixedLen(dos, ELConsts.MID_BYTE_LEN);
				dos.writeFloat(entry.popularity);
			}
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// can be used for more generized features
	public static void genTfIdfIndex(String midToWidFileName, String tfIdfFileName, String dstIndexFileName) {
		int numWids = IOUtils.getNumLinesFor(midToWidFileName);
		int[] wids = new int[numWids];
		String[] mids = new String[numWids];
		loadWidsMids(midToWidFileName, mids, wids);

		DataInputStream disTfidf = IOUtils
				.getBufferedDataInputStream(tfIdfFileName);

		FeaturePos[] featurePoses = new FeaturePos[wids.length];
		int featPosCnt = 0;
		try {
			TfIdfFeature tfidfFeature = new TfIdfFeature();
			RandomAccessFile raf = new RandomAccessFile(tfIdfFileName, "r");
			long curFeatFilePointer = raf.getFilePointer();
			long fileLen = raf.length();
			int curFeatWid = -1;
			for (int i = 0; i < wids.length; ++i) {				
				while (curFeatWid < wids[i] && raf.getFilePointer() < fileLen) {
					curFeatFilePointer = raf.getFilePointer();
					curFeatWid = raf.readInt();
					tfidfFeature.fromFile(raf);
				}
				
				if (curFeatWid == wids[i]) {
					featurePoses[featPosCnt] = new FeaturePos();
					featurePoses[featPosCnt].mid = new ByteArrayString(mids[i], ELConsts.MID_BYTE_LEN);
					featurePoses[featPosCnt].filePointer = curFeatFilePointer;
					++featPosCnt;
				}
			}
			
			disTfidf.close();
			raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(featPosCnt);
		Arrays.sort(featurePoses, 0, featPosCnt);
		writeFeatureIndices(featurePoses, featPosCnt, dstIndexFileName);
	}

	public static void mergePopTfIdfFeatures(String midToWidFileName,
			String popFeatFileName, String tfidfFeatFileName,
			String dstFeatFileName, String dstIdxFileName) {
		int numWids = IOUtils.getNumLinesFor(midToWidFileName);
		int[] wids = new int[numWids];
		String[] mids = new String[numWids];
		loadWidsMids(midToWidFileName, mids, wids);

		DataInputStream disPop = IOUtils
				.getBufferedDataInputStream(popFeatFileName);
		DataInputStream disTfidf = IOUtils
				.getBufferedDataInputStream(tfidfFeatFileName);

//		String tmpIdxFileName = Paths.get(ELConsts.TMP_FILE_PATH, "tmp_mid_feat_idx.txt").toString();
//		BufferedWriter writer = IOUtils.getUTF8BufWriter(tmpIdxFileName,
//				false);
//		if (writer == null)
//			return;

		// WikiSingleIntFeature popFeature = new WikiSingleIntFeature();
		// WikiTfIdfFeature tfidfFeature = new WikiTfIdfFeature();
//		SingleIntFeature popFeature = new SingleIntFeature();
		SingleFloatFeature popFeature = new SingleFloatFeature();
		TfIdfFeature tfidfFeature = new TfIdfFeature();
		FeaturePos[] featurePoses = new FeaturePos[wids.length]; 

		try {
			RandomAccessFile raf = new RandomAccessFile(dstFeatFileName, "rw");

			int popCnt = 0, tfidfCnt = 0;
			long preFilePointer = -1, curFilePointer;
			int curPopFeatWid = -1, curTfidfFeatWid = -1;
			for (int i = 0; i < wids.length; ++i) {
				curFilePointer = raf.getFilePointer();
				if (preFilePointer == curFilePointer) {
					System.err
							.println("pre file pointer equals cur file pointer!");
					break;
				}
				preFilePointer = curFilePointer;
				
				featurePoses[i] = new FeaturePos();
				featurePoses[i].mid = new ByteArrayString(mids[i], ELConsts.MID_BYTE_LEN);
				featurePoses[i].filePointer = curFilePointer;
//				writer.write(mids[i] + "\t" + curFilePointer + "\n");

				raf.writeInt(wids[i]);
				curPopFeatWid = writeFeatureOfWid(wids[i], disPop, curPopFeatWid, popFeature, raf);
				if (curPopFeatWid == wids[i])
					++popCnt;
				
				curTfidfFeatWid = writeFeatureOfWid(wids[i], disTfidf, curTfidfFeatWid, tfidfFeature, raf);
				if (curTfidfFeatWid == wids[i])
					++tfidfCnt;

				if (popCnt % 100000 == 0)
					System.out.println(popCnt);
			}

			disPop.close();
			disTfidf.close();
//			writer.close();
			raf.close();

			System.out.println(wids.length + " wids");
			System.out.println(popCnt + " wids have popularity. " + tfidfCnt
					+ " wids have TFIDF.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Arrays.sort(featurePoses);
		writeFeatureIndices(featurePoses, featurePoses.length, dstIdxFileName);

//		TupleFileTools.sort(tmpIdxFileName, dstIdxFileName, new TupleFileTools.SingleFieldComparator(0));
//		IOUtils.writeNumLinesFileFor(dstIdxFileName, numWids);
	}
	
	private static void writeFeatureIndices(FeaturePos[] featurePoses, int featurePosCnt, String dstFileName) {
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			System.out.println(featurePosCnt);
			dos.writeInt(featurePosCnt);
			for (int i = 0; i < featurePosCnt; ++i) {
				featurePoses[i].mid.toFileWithFixedLen(dos, ELConsts.MID_BYTE_LEN);
				dos.writeLong(featurePoses[i].filePointer);
			}
			
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadWidsMids(String midToWidFileName, String[] mids,
			int[] wids) {
		BufferedReader reader = IOUtils.getUTF8BufReader(midToWidFileName);
		try {
			String line = null;
			for (int i = 0; i < mids.length; ++i) {
				line = reader.readLine();
				String[] vals = line.split("\t");
				mids[i] = vals[0];
				wids[i] = Integer.valueOf(vals[1]);
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
//	private static void loadWidsMids(String midToWidFileName, ByteArrayString[] mids,
//			int[] wids) {
//		BufferedReader reader = IOUtils.getUTF8BufReader(midToWidFileName);
//		try {
//			String line = null;
//			for (int i = 0; i < mids.length; ++i) {
//				line = reader.readLine();
//				String[] vals = line.split("\t");
//				mids[i] = new ByteArrayString(vals[0], ELConsts.MID_BYTE_LEN);
//				wids[i] = Integer.valueOf(vals[1]);
//			}
//
//			reader.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//			return;
//		}
//	}

	private static int writeFeatureOfWid(int expectedWid, DataInputStream dis,
			int curFeatWid, Feature feat, RandomAccessFile dstFile) {
		if (curFeatWid < expectedWid) {
			try {
				if (dis.available() > 0) {
					boolean flg = true;
					while (flg && curFeatWid < expectedWid) {
						curFeatWid = dis.readInt();
						if (!feat.fromFile(dis)) {
							flg = false;
						}
					}
				} else {
					Feature.putEmptyFeature(dstFile);
					return curFeatWid;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (curFeatWid == expectedWid) {
			feat.toFile(dstFile);
			return curFeatWid;
		} else {
			Feature.putEmptyFeature(dstFile);
			return curFeatWid;
		}
	}
}
