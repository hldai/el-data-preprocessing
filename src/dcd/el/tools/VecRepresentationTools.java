package dcd.el.tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import dcd.el.ELConsts;
import dcd.el.dict.SimpleWikiDict;
import dcd.el.io.IOUtils;
import dcd.el.objects.ByteArrayString;
import dcd.el.objects.Span;
import dcd.el.utils.CommonUtils;
import dcd.el.utils.TokenizeUtils;
import dcd.el.utils.TokenizeUtils.IndexSentenceWithMentions;
import dcd.el.utils.TupleFileTools;
import dcd.word2vec.WordVectorSet;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class VecRepresentationTools {
	public static class WordVectorPair implements Comparable<WordVectorPair> {
		public ByteArrayString word;
		public float[] vector;
		
		@Override
		public int compareTo(WordVectorPair wvpr) {
			return this.word.compareTo(wvpr.word);
		}
	}
	
	public static int MAX_NUM_CANDIDATES = 10;
	
	public static void genEntityRepresentation(String widNotableForFileName,
			String widTitleFileName, String wordVecFileName, String midWidFileName,
			String dstFileName) {
		WordVectorSet wordVectorSet = new WordVectorSet(wordVecFileName);
		BufferedReader reader0 = IOUtils.getUTF8BufReader(widTitleFileName);
		BufferedReader reader1 = IOUtils.getUTF8BufReader(widNotableForFileName);
		BufferedReader reader2 = IOUtils.getUTF8BufReader(midWidFileName);
//		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			int cnt = 0;
			String line0 = null, line1 = null, line2 = null;
			String[] vals0 = null, vals1 = null;
			int wid0 = 0, wid1 = 0, wid2 = 0;
			while ((line2 = reader2.readLine()) != null) {
				wid2 = Integer.valueOf(CommonUtils.getFieldFromLine(line2, 1));
				if (wid0 != -1) {
					while (wid0 < wid2 && (line0 = reader0.readLine()) != null) {
						vals0 = line0.split("\t");
						wid0 = Integer.valueOf(vals0[0]);
					}
					if (line0 == null)
						wid0 = -1;
				}
				if (wid1 != -1) {
					while (wid1 < wid2 && (line1 = reader1.readLine()) != null) {
						vals1 = line1.split("\t");
						wid1 = Integer.valueOf(vals1[0]);
					}
					if (line1 == null)
						wid1 = -1;
				}
				
//				if (wid1 == wid2 && wid0 != wid2) {
//					System.out.println(wid2 + " not good");
//				}
				
				if (wid0 == wid2) {
//					writer.write(wid2 + "\t" + vals0[1]);
//					if (wid1 == wid2) {
//						writer.write("\t" + vals1[1]);
//					}
//					writer.write("\n");
					
					String notableFor = wid1 == wid2 ? vals1[1] : null;
					float[] entityVec = getEntityVector(vals0[1], notableFor, 
							wordVectorSet);
					if (entityVec != null) {
						dos.writeInt(wid2);
						for (float f : entityVec) {
							dos.writeFloat(f);
						}
						++cnt;
					}	
				}
				
//				if (cnt == 3)
//					break;
				if (cnt % 100000 == 0)
					System.out.println(cnt);
			}
			
			reader1.close();
			reader0.close();
			dos.close();
			
			System.out.println(cnt + " wids.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static float[] getEntityVector(String title, String notableFor,
			WordVectorSet wordVectorSet) {
		float[] vec = null;
		
		int wordCnt = 0;
		boolean isFirst = true;
		title = title.replace('/', ' ');
		title = title.replaceAll("[\\(\\)]", " ");
		StringReader sr = new StringReader(title);
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(sr,
	              new CoreLabelTokenFactory(), "untokenizable=noneKeep");
		List<CoreLabel> labels = ptbt.tokenize();
		sr.close();
		for (CoreLabel label : labels) {
			String word = label.toString();
			float[] wvec = wordVectorSet.getVector(word);
			if (wvec == null) {
				wvec = wordVectorSet.getVector(word.toLowerCase());
			}
			
			if (wvec != null) {
				if (isFirst) {
					isFirst = false;
					vec = wvec.clone();
				} else {
					for (int i = 0; i < vec.length; ++i) {
						vec[i] += wvec[i];
					}
				}
				++wordCnt;
			}
		}
		
		if (notableFor != null) {
			notableFor = notableFor.replace('/', ' ');
			notableFor = notableFor.replaceAll("[\\(\\)]", " ");
			sr = new StringReader(notableFor);
			ptbt = new PTBTokenizer<>(sr, new CoreLabelTokenFactory(),
					"untokenizable=noneKeep");
			labels = ptbt.tokenize();
			sr.close();
			for (CoreLabel label : labels) {
				// use lower case as default
				String word = label.toString();
				float[] wvec = wordVectorSet.getVector(word.toLowerCase());
				if (wvec == null) {
					wvec = wordVectorSet.getVector(word);
				}

				if (wvec != null) {
					if (isFirst) {
						isFirst = false;
						vec = wvec.clone();
					} else {
						for (int i = 0; i < vec.length; ++i) {
							vec[i] += wvec[i];
						}
					}
					++wordCnt;
				}
			}
		}
		
		if (vec != null) {
			for (int i = 0; i < vec.length; ++i) {
				vec[i] /= wordCnt;
			}
		}
		
		return vec;
	}
	
	public static void genWikiTrainingDataWordVec(
			String wikiDataFileName, String dictAliasFileName,
			String dictWidFileName, String wordVecFileName, String dstTrainFileName,
			String dstEvalFileName, String dstTestFileName) {
		SimpleWikiDict simpleWikiDict = new SimpleWikiDict(dictAliasFileName, dictWidFileName);
		WordVectorSet wordVectorSet = new WordVectorSet(wordVecFileName);
		
		DataOutputStream dosTrain = IOUtils.getBufferedDataOutputStream(dstTrainFileName);
		DataOutputStream dosEval = IOUtils.getBufferedDataOutputStream(dstEvalFileName);
		DataOutputStream dosTest = IOUtils.getBufferedDataOutputStream(dstTestFileName);
		BufferedReader reader = IOUtils.getUTF8BufReader(wikiDataFileName);
		String line = null;
		long textCnt = 0, mentionCnt = 0;
		Random random = new Random();
		int rndCnt = 0, nextRnd = random.nextInt() % 10;
		boolean toEval = true;
		try {
			while ((line = reader.readLine()) != null) {
				int numMentions = Integer.valueOf(reader.readLine());
				mentionCnt += numMentions;
				Span[] mentionSpans = new Span[numMentions];
				int[] trueWids = new int[numMentions];
				for (int i = 0; i < numMentions; ++i) {
					trueWids[i] = Integer.valueOf(reader.readLine());
					
					String spanText = reader.readLine();
					String[] vals = spanText.split("\t");
					Span span = new Span();
					span.beg = Integer.valueOf(vals[0]);
					span.end = Integer.valueOf(vals[1]);
					mentionSpans[i] = span;
				}
				
				IndexSentenceWithMentions iswm = TokenizeUtils.indexWords(line, mentionSpans,
						wordVectorSet);
				
				if (mentionSpans.length != iswm.mentionSpans.length) {
					System.out.println("mention span length not equal.");
					continue;
				}
				
//				System.out.println(rndCnt + " " + nextRnd);
				if (rndCnt == nextRnd) {
					if (toEval) {
						writeData(dosEval, line, iswm, mentionSpans, simpleWikiDict, trueWids);
						toEval = false;
					} else {
						writeData(dosTest, line, iswm, mentionSpans, simpleWikiDict, trueWids);
						toEval = true;
					}
					rndCnt = -1;
					nextRnd = random.nextInt(10);
				} else {
					writeData(dosTrain, line, iswm, mentionSpans, simpleWikiDict, trueWids);
				}
				
				++rndCnt;
				
				++textCnt;
//				if (textCnt == 200)
//					break;
				if (textCnt % 100000 == 0) {
					System.out.println(textCnt);
				}
			}
			
			reader.close();
			dosTrain.close();
			dosEval.close();
			dosTest.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(textCnt + " paragraphs.");
		System.out.println(mentionCnt + " mentions.");
	}
	
	private static void writeData(DataOutputStream dos, String line, IndexSentenceWithMentions iswm,
			Span[] mentionSpans, SimpleWikiDict simpleWikiDict, int[] trueWids) throws IOException {
		dos.writeInt(iswm.wordIndices.length);
		for (int wordIndex : iswm.wordIndices) {
			dos.writeInt(wordIndex);
		}
		

		dos.writeInt(mentionSpans.length);
		
		for (int i = 0; i < mentionSpans.length; ++i) {
			dos.writeInt(iswm.mentionSpans[i].beg);
			dos.writeInt(iswm.mentionSpans[i].end);
			
			Span span = mentionSpans[i];
			
//			System.out.println(line.substring(span.beg, span.end + 1));
			int[] wids = simpleWikiDict.getCandidates(line.substring(span.beg, span.end + 1));
			
			if (wids == null) {
				dos.writeInt(1);
				dos.writeInt(trueWids[i]);
			} else if (wids.length < 11) {
				int numCandidates = wids.length + 1;
				for (int wid : wids) {
					if (wid == trueWids[i]) {
						--numCandidates;
						break;
					}
				}
				
				dos.writeInt(numCandidates);
				dos.writeInt(trueWids[i]);
				for (int wid : wids) {
					if (wid != trueWids[i]) {
						dos.writeInt(wid);
					}
				}
			} else {
				int[] rndIndices = CommonUtils.genNonRepeatingRandom(wids.length, 
						MAX_NUM_CANDIDATES);
				int numCandidates = MAX_NUM_CANDIDATES + 1;
				for (int rndIdx : rndIndices) {
					if (wids[rndIdx] == trueWids[i]) {
						--numCandidates;
						break;
					}
				}
				
				dos.writeInt(numCandidates);
				dos.writeInt(trueWids[i]);
				for (int rndIdx : rndIndices) {
					if (wids[rndIdx] != trueWids[i]) {
						dos.writeInt(wids[rndIdx]);
					}
				}
			}
		}
	}

	public static void genJavaFriendlyWordVectorFile(String srcWordVecFileName,
			String dstFileName) {
		WordVectorPair[] wordVectorPairs = loadWordVectors(srcWordVecFileName);

		Arrays.sort(wordVectorPairs);
		
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			System.out.println("writing " + wordVectorPairs.length + " words.");
			dos.writeInt(wordVectorPairs.length);
			dos.writeInt(wordVectorPairs[0].vector.length);
			
			for (WordVectorPair wvp : wordVectorPairs) {
				wvp.word.toFileWithByteLen(dos);
				for (float f : wvp.vector) {
					dos.writeFloat(f);
				}
			}
			
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static WordVectorPair[] loadWordVectors(String fileName) {
		WordVectorPair[] wordVectorPairs = null;
		int wordCnt = 0;
		try {
			BufferedInputStream bis = new BufferedInputStream(
					new FileInputStream(fileName));
			byte[] intBytes = new byte[Integer.BYTES];
			bis.read(intBytes);
			int numWords = CommonUtils.getLittleEndianInt(intBytes);
			wordVectorPairs = new WordVectorPair[(int) numWords];
			System.out.println(numWords);

			bis.read(intBytes);
			int vecLen = CommonUtils.getLittleEndianInt(intBytes);
			System.out.println(vecLen);

			for (int i = 0; i < numWords; ++i) {
				int len = bis.read();
				ByteArrayString word = new ByteArrayString();
				word.bytes = new byte[len];
				bis.read(word.bytes);

				byte[] vecBytes = new byte[Float.BYTES * vecLen];
				bis.read(vecBytes);
				float[] vec = CommonUtils.getLittleEndianFloatArray(vecBytes);
				
				String sword = word.toString();
				if (!sword.contains("_")) {
					wordVectorPairs[wordCnt] = new WordVectorPair();
					wordVectorPairs[wordCnt].word = word;
					wordVectorPairs[wordCnt].vector = vec;
					++wordCnt;
				}

				// if (i == 10) break;
				// System.out.println(i);
				if (i % 100000 == 0)
					System.out.println(i);
			}

			// bos.close();
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return Arrays.copyOf(wordVectorPairs, wordCnt);
//		return wordVectorPairs;
	}

	public static void filterTrainingData(String wikiTrainingDataFileName,
			String widNotableForFileName, String dstFileName) {
		BufferedReader reader = IOUtils
				.getUTF8BufReader(wikiTrainingDataFileName);

		int numWids = IOUtils.getNumLinesFor(widNotableForFileName);
		int[] wids = new int[numWids];
		loadWidNotableForAttributes(widNotableForFileName, wids, null);

		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);

		long textCnt = 0, mentionCnt = 0, writtenTextCnt = 0, writtenMentionCnt = 0;
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				++textCnt;
				// if (textCnt == 20)
				// break;
				if (textCnt % 100000 == 0) {
					System.out.println(textCnt);
				}

				int numMentions = Integer.valueOf(reader.readLine());
				mentionCnt += numMentions;
				int curMentionCnt = 0;
				StringBuilder mentionListBuilder = new StringBuilder();
				for (int i = 0; i < numMentions; ++i) {
					int curWid = Integer.valueOf(reader.readLine());
					// reader.readLine();
					String span = reader.readLine();

					if (Arrays.binarySearch(wids, curWid) > -1) {
						mentionListBuilder.append(curWid + "\n" + span + "\n");
						++curMentionCnt;
					}
				}

				writtenMentionCnt += curMentionCnt;

				if (curMentionCnt > 0) {
					++writtenTextCnt;
					writer.write(line + "\n");
					writer.write(curMentionCnt + "\n");
					writer.write(new String(mentionListBuilder));
				}
			}

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("text: " + textCnt);
		System.out.println("written text: " + writtenTextCnt);
		System.out.println("mentions: " + mentionCnt);
		System.out.println("written mentions: " + writtenMentionCnt);
	}

	public static void genMidNameForVecRepresentation(String midNameFileName,
			String midToWidFileName, String widTitleFileName,
			String dstMidNameForVecRepFileName,
			String dstMidNameForVecRepWikiOnlyFileName) {
		genMidNameForVecRepresentationWikiOnly(midToWidFileName,
				widTitleFileName, dstMidNameForVecRepWikiOnlyFileName);

		BufferedReader reader0 = IOUtils.getUTF8BufReader(midNameFileName), reader1 = IOUtils
				.getUTF8BufReader(dstMidNameForVecRepWikiOnlyFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(
				dstMidNameForVecRepFileName, false);

		try {
			String line0 = reader0.readLine(), line1 = reader1.readLine();
			String mid0 = CommonUtils.getFieldFromLine(line0, 0), mid1 = CommonUtils
					.getFieldFromLine(line1, 0);
			while (line0 != null && line1 != null) {
				int cmp = mid0.compareTo(mid1);
				if (cmp == 0) {
					writer.write(mid0 + "\t"
							+ CommonUtils.getFieldFromLine(line1, 1) + "\n");
					line0 = reader0.readLine();
					line1 = reader1.readLine();
					if (line0 != null)
						mid0 = CommonUtils.getFieldFromLine(line0, 0);
					if (line1 != null)
						mid1 = CommonUtils.getFieldFromLine(line1, 0);
				} else if (cmp < 0) {
					writer.write(line0 + "\n");
					line0 = reader0.readLine();
					if (line0 != null)
						mid0 = CommonUtils.getFieldFromLine(line0, 0);
				} else {
					writer.write(line1 + "\n");
					line1 = reader1.readLine();
					if (line1 != null)
						mid1 = CommonUtils.getFieldFromLine(line1, 0);
				}
			}

			while (line0 != null) {
				writer.write(line0 + "\n");
				line0 = reader0.readLine();
			}

			while (line1 != null) {
				writer.write(line1 + "\n");
				line1 = reader1.readLine();
			}

			reader0.close();
			reader1.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void genMidNameForVecRepresentationWikiOnly(
			String midToWidFileName, String widTitleFileName, String dstFileName) {
		String tmpFileName = Paths.get(ELConsts.TMP_FILE_PATH,
				"mid_name_tmp.txt").toString();
		TupleFileTools.join(midToWidFileName, widTitleFileName, 1, 0,
				tmpFileName, new CommonUtils.StringToIntComparator());
		TupleFileTools.sort(tmpFileName, dstFileName,
				new TupleFileTools.SingleFieldComparator(0));
	}

	private static void loadWidNotableForAttributes(String fileName,
			int[] wids, String[] nfAttributes) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		try {
			for (int i = 0; i < wids.length; ++i) {
				String line = reader.readLine();
				String[] vals = line.split("\t");
				wids[i] = Integer.valueOf(vals[0]);

				if (nfAttributes != null)
					nfAttributes[i] = vals[1];
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
