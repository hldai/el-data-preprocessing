package edu.zju.dcd.edl.tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.zju.dcd.edl.ELConsts;
import edu.zju.dcd.edl.cg.SimpleWikiDict;
import edu.zju.dcd.edl.io.IOUtils;
import edu.zju.dcd.edl.obj.ByteArrayString;
import dcd.el.objects.Span;
import dcd.el.utils.CommonUtils;
import dcd.el.utils.TokenizeUtils;
import dcd.el.utils.TokenizeUtils.IndexSentenceWithMentions;
import dcd.el.utils.TupleFileTools;
import edu.zju.dcd.edl.utils.MathUtils;
import edu.zju.dcd.edl.utils.StringUtils;
import edu.zju.dcd.edl.wordvec.WordVectorSet;
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

	public static final int MAX_NUM_CANDIDATES = 5;

	private static final String LINK_PATTERN_STR = "\\[\\[(.*?)\\|(.*?)\\]\\]";
	private static final int MIN_NUM_WORDS_IN_SENTENCE = 5;
	
	private static class MidNameVec implements Comparable<MidNameVec> {
		ByteArrayString mid;
		float[] vec;
		
		@Override
		public int compareTo(MidNameVec midNameVec) {
			return mid.compareTo(midNameVec.mid);
		}
	}
	
	private static int getTypeFlag(String type) {
		if (type.equals("PER")) {
			return 0;
		}
		if (type.equals("ORG")) {
			return 1;
		}
		if (type.equals("GPE")) {
			return 2;
		}
		if (type.equals("LOC")) {
			return 3;
		}
		if (type.equals("FAC")) {
			return 4;
		}
		return -1;
	}
	
	public static void genNameVecTypeData(String xmlFileName, String tabFileName, String wordVecFileName, String dstFileName) {
		String xmlText = IOUtils.readTextFile(xmlFileName);
		BufferedReader reader = IOUtils.getUTF8BufReader(tabFileName);
		WordVectorSet wordVectorSet = new WordVectorSet(wordVecFileName);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			dos.writeInt(0);
			dos.writeInt(wordVectorSet.getWordVecSize());
			
			Pattern mentionPattern = Pattern.compile(ELConsts.QUERY_PATTERN);
			Matcher m = mentionPattern.matcher(xmlText);
			String line = null;
			reader.readLine();
			int mcnt = 0;
			while (m.find()) {
				line = reader.readLine();
				String[] vals = line.split("\t");
				if (!vals[0].equals(m.group(1))) {
					System.out.println("qid not equal!");
					break;
				}
				
				String[] words = StringUtils.tokenize(m.group(2));
				float[] vec = new float[wordVectorSet.getWordVecSize()];
				int cnt = 0;
				for (String word : words) {
					float[] tmpVec = wordVectorSet.getVector(word);
					if (tmpVec != null) {
						++cnt;
						MathUtils.addTo(vec, tmpVec);
					}
				}
				if (cnt > 0)
					MathUtils.divide(vec, cnt);
				
				int typeFlg = getTypeFlag(vals[2]);
				if (typeFlg > -1) {
					for (float v : vec) {
						dos.writeFloat(v);
					}
					dos.writeInt(typeFlg);
					++mcnt;
				}
			}
			reader.close();
			dos.close();
			
			IOUtils.writeIntAtFileBeg(dstFileName, mcnt);
			System.out.println(mcnt + " names written.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genMidNameVecs(String midNameFileName, String wordVecFileName, String dstFileName) {
		WordVectorSet wordVectorSet = new WordVectorSet(wordVecFileName);
		int numMidNames = IOUtils.getNumLinesFor(midNameFileName);
		BufferedReader reader = IOUtils.getUTF8BufReader(midNameFileName);
		MidNameVec[] midNameVecs = new MidNameVec[numMidNames];
		try {
			for (int i = 0; i < numMidNames; ++i) {
				midNameVecs[i] = new MidNameVec();
				String line = reader.readLine();
				String vals[] = line.split("\t");
				midNameVecs[i].mid = new ByteArrayString(vals[0]);
				midNameVecs[i].vec = new float[wordVectorSet.getWordVecSize()];
				
				StringReader sr = new StringReader(vals[1]);
				List<CoreLabel> labels = null;
				PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(sr, new CoreLabelTokenFactory(),
						"ptb3Escaping=false,untokenizable=noneKeep");
				labels = ptbt.tokenize();
				sr.close();
				int hitWordCnt = 0;
				for (CoreLabel label : labels) {
					float[] vec = wordVectorSet.getVector(label.value().toLowerCase());
					if (vec != null) {
						for (int j = 0; j < vec.length; ++j) {
							midNameVecs[i].vec[j] += vec[j];
						}
						++hitWordCnt;
					}
				}
				
				if (hitWordCnt > 0) {
					for (int j = 0; j < midNameVecs[i].vec.length; ++j) {
						midNameVecs[i].vec[j] /= hitWordCnt;
					}

					MathUtils.toUnitVector(midNameVecs[i].vec);
				}
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Arrays.sort(midNameVecs);

		System.out.println(midNameVecs.length);
		
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			dos.writeInt(midNameVecs.length);
			dos.writeInt(wordVectorSet.getWordVecSize());
			for (MidNameVec midNameVec : midNameVecs) {
				midNameVec.mid.toFileWithFixedLen(dos, ELConsts.MID_BYTE_LEN);
				for (float v : midNameVec.vec) {
					dos.writeFloat(v);
				}
			}
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void splitEntityVecTrainingData(String fileName, String dstPath) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		try {
			int widCnt = 0;
			int partIdx = 0;
			String line = null, wid = null;
			BufferedWriter writer = null;
			while ((wid = reader.readLine()) != null) {
				if (widCnt % 500000 == 0) {
					if (writer != null) {
						writer.close();
					}
					String curDstFileName = Paths.get(dstPath, "wiki_entity_text_" + partIdx + ".txt").toString();
					writer = IOUtils.getUTF8BufWriter(curDstFileName, false);
					++partIdx;
				}
				
				int numLines = Integer.valueOf(reader.readLine());
				
				writer.write(wid + "\n");
				writer.write(numLines + "\n");
				
				for (int i = 0; i < numLines; ++i) {
					line = reader.readLine();
					writer.write(line + "\n");
				}
				++widCnt;
				
				if (widCnt % 100000 == 0)
					System.out.println(widCnt);
			}
			writer.close();
			System.out.println(widCnt);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	


	
	private static String[] loadFilterWords(String fileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		String[] words = null;
		try {
			int numLines = Integer.valueOf(reader.readLine());
			words = new String[numLines];
			for (int i = 0; i < numLines; ++i) {
				words[i] = reader.readLine().trim();
			}
			reader.close();
			
			Arrays.sort(words);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return words;
	}

	private static boolean toLowerCaseWords(String textLine,
			int minNumWords, String[] filterWords, StringBuilder dstStringBuilder) {
//		StringBuilder stringBuilder = new StringBuilder();
		StringReader sr = new StringReader(textLine);
		String word = null;
		PTBTokenizer<CoreLabel> ptbt = null;
		List<CoreLabel> labels = null;
		ptbt = new PTBTokenizer<>(sr, new CoreLabelTokenFactory(),
				"ptb3Escaping=false,untokenizable=noneKeep");
		labels = ptbt.tokenize();
		sr.close();
		int wordsCnt = 0, usedWordCnt = 0;
		for (CoreLabel label : labels) {
			if (CommonUtils.hasEnglishChar(label.value())) {
				word = label.value().toLowerCase();
				boolean flg = true;

				if (word.startsWith("http://") || word.startsWith("<")
						&& word.endsWith(">") || word.contains("\n")) {
					flg = false;
				} else {
					++wordsCnt;
				}
				
				if (filterWords != null) {
					int pos = Arrays.binarySearch(filterWords, word);
					if (pos > -1) {
						flg = false;
					}
				}

				if (flg) {
					if (usedWordCnt != 0) {
						dstStringBuilder.append(" ");
					}

					dstStringBuilder.append(word);
					++usedWordCnt;
				}
			}
		}

		return wordsCnt < minNumWords ? false : true;
	}

	// do tokenization, convert to lower case ...
	public static void genWordVecTrainingData(String dataFileName,
			String dstFileName, String filterWordsFileName, boolean preserveWid) {
//		TfIdfExtractor tfIdfExtractor = new TfIdfExtractor("d:/data/el/features/enwiki_idf.sd");
		
		BufferedReader reader = IOUtils.getUTF8BufReader(dataFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);

//		final String[] wordsToRemove = { "a", "to" };
		String[] filterWords = null;
		if (filterWordsFileName != null)
			filterWords = loadFilterWords(filterWordsFileName);

		try {
			long textCnt = 0;
			while (reader.readLine() != null) { // read title
				++textCnt;
				String wid = reader.readLine(); // read wid

				int numLines = Integer.valueOf(reader.readLine());
				String line = null;
				StringBuilder resultStringBuilder = new StringBuilder();
				for (int i = 0; i < numLines; ++i) {
					line = reader.readLine();
					line = line.replaceAll(LINK_PATTERN_STR, "$2").trim();
					if (CommonUtils.countWords(line) < MIN_NUM_WORDS_IN_SENTENCE) {
						continue;
					}

					// writer.write(line);
					StringBuilder tmpStringBuilder = new StringBuilder();
					boolean flg = toLowerCaseWords(line, MIN_NUM_WORDS_IN_SENTENCE, filterWords, 
							tmpStringBuilder);
					if (flg) {
						resultStringBuilder.append(tmpStringBuilder.toString());
						if (Character.isAlphabetic(line.charAt(line
										.length() - 1))) {
							resultStringBuilder.append(" ");
						} else {
							resultStringBuilder.append("\n");
						}
					}
				}
				String resultText = resultStringBuilder.toString().trim();
				if (resultText.equals("")) {
					continue;
				}
				
				int numLinesInText = CommonUtils.countLines(resultText) + 1;
				
				if (preserveWid) {
					writer.write(wid + "\n" + numLinesInText + "\n");
				}
				writer.write(resultText);
				writer.write("\n");

//				if (textCnt == 10)
//					break;
				if (textCnt % 10000 == 0)
					System.out.println(textCnt);
			}

			System.out.println(textCnt + " articles");

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void entityRepresentationToUnitVectors(String fileName,
			String dstFileName) {
		DataInputStream dis = IOUtils.getBufferedDataInputStream(fileName);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			int numEntities = dis.readInt();
			int vecLen = dis.readInt();
			
			System.out.println(numEntities + "\t" + vecLen);
			
			dos.writeInt(numEntities);
			dos.writeInt(vecLen);
			
			float[] vec = new float[vecLen];
			for (int i = 0; i < numEntities; ++i) {
				int wid = dis.readInt();
				dos.writeInt(wid);
				double sqrSum = 0;
				for (int j = 0; j < vecLen; ++j) {
					vec[j] = dis.readFloat();
					sqrSum += (double)vec[j] * vec[j];
				}
				
				sqrSum = Math.sqrt(sqrSum);
				for (int j = 0; j < vecLen; ++j) {
					vec[j] /= sqrSum;
					dos.writeFloat(vec[j]);
				}
			}
			
			dis.close();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genEntityRepresentation(String widNotableForFileName,
			String widTitleFileName, String wordVecFileName,
			String midWidFileName, String dstFileName) {
		WordVectorSet wordVectorSet = new WordVectorSet(wordVecFileName);
		BufferedReader reader0 = IOUtils.getUTF8BufReader(widTitleFileName);
		BufferedReader reader1 = IOUtils
				.getUTF8BufReader(widNotableForFileName);
		BufferedReader reader2 = IOUtils.getUTF8BufReader(midWidFileName);
		// BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName, false);
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		try {
			dos.writeInt(0); // number of entities, will be fixed later
			dos.writeInt(wordVectorSet.getWordVecSize() * 2);

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

				// if (wid1 == wid2 && wid0 != wid2) {
				// System.out.println(wid2 + " not good");
				// }

				if (wid0 == wid2) {
					// writer.write(wid2 + "\t" + vals0[1]);
					// if (wid1 == wid2) {
					// writer.write("\t" + vals1[1]);
					// }
					// writer.write("\n");

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

				// if (cnt == 3)
				// break;
				if (cnt % 100000 == 0)
					System.out.println(cnt);
			}

			reader1.close();
			reader0.close();
			dos.close();

			// fix the number of entities
			RandomAccessFile raf = new RandomAccessFile(dstFileName, "rw");
			raf.writeInt(cnt);
			raf.close();

			System.out.println(cnt + " wids.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void genEntityRepresentationWordIdxVec(String widNotableForFileName,
			String widTitleFileName, String wordVecFileName,
			String midWidFileName, String widKeywordsFileName, String dstFileName, int fixedLen,
			int padLen, int maxNumKeywords) {
		WordVectorSet wordVectorSet = new WordVectorSet(wordVecFileName);
		
		BufferedReader reader0 = IOUtils.getUTF8BufReader(midWidFileName);
		BufferedReader reader1 = IOUtils.getUTF8BufReader(widTitleFileName);
		BufferedReader reader2 = IOUtils.getUTF8BufReader(widNotableForFileName);
		BufferedReader reader3 = maxNumKeywords > 0 ? IOUtils.getUTF8BufReader(widKeywordsFileName) : null;
		
		DataOutputStream dos = IOUtils.getBufferedDataOutputStream(dstFileName);
		
		try {
			dos.writeInt(0); // number of entities, will be fixed later
			boolean isLenFixed = fixedLen > -1;
			if (isLenFixed) {
				dos.writeInt(fixedLen + 2 * padLen);
			}

			int cnt = 0;
			String line0 = null, line1 = null, line2 = null, line3 = null;
			String[] vals1 = null, vals2 = null, vals3 = null;
			int wid0 = 0, wid1 = 0, wid2 = 0, wid3 = maxNumKeywords > 0 ? 0 : -1;
			int maxNumIndices = 0;
			while ((line0 = reader0.readLine()) != null) {
				wid0 = Integer.valueOf(CommonUtils.getFieldFromLine(line0, 1));
				if (wid1 != -1) {
					while (wid1 < wid0 && (line1 = reader1.readLine()) != null) {
						vals1 = line1.split("\t");
						wid1 = Integer.valueOf(vals1[0]);
					}
					if (line1 == null)
						wid1 = -1;
				}
				if (wid2 != -1) {
					while (wid2 < wid0 && (line2 = reader2.readLine()) != null) {
						vals2 = line2.split("\t");
						wid2 = Integer.valueOf(vals2[0]);
					}
					if (line2 == null)
						wid2 = -1;
				}
				if (wid3 != -1) {
					while (wid3 < wid0 && (line3 = reader3.readLine()) != null) {
						wid3 = Integer.valueOf(line3);
						vals3 = reader3.readLine().split(" ");
					}
				}

				if (wid0 == wid1) {
					String notableFor = wid2 == wid0 ? vals2[1] : null;
					int[] titleWordIndices = getWordVectorIndices(wordVectorSet, vals1[1], isLenFixed);
					int[] notableForWordIndices = getWordVectorIndices(wordVectorSet, notableFor, isLenFixed);
					int[] keywordsIndices = wid3 == wid0 ? getWordVectorIndices(wordVectorSet, vals3, isLenFixed) : null;
					
					int numIndices = countLegalWordIndices(titleWordIndices);
					numIndices += countLegalWordIndices(notableForWordIndices);
					
					int usedNumKeywordIndices = keywordsIndices == null ? 0 : filterKeywordsIndices(keywordsIndices,
									titleWordIndices, notableForWordIndices);
					if (usedNumKeywordIndices > maxNumKeywords)
						usedNumKeywordIndices = maxNumKeywords;
					numIndices += usedNumKeywordIndices;
					
					if (numIndices > maxNumIndices) {
						maxNumIndices = numIndices;
					}
					
					if (numIndices > 0) {
//						System.out.println(wid0);
						dos.writeInt(wid0);
						if (fixedLen < 0)
							dos.writeInt(numIndices);
						else {
							for (int i = 0; i < padLen; ++i)
								dos.writeInt(0);
						}
						
						int numWordsLeft = fixedLen;
						int writtenCnt = writeLegalWordIndices(dos, titleWordIndices, numWordsLeft);
						numWordsLeft -= writtenCnt;
						writtenCnt += writeLegalWordIndices(dos, notableForWordIndices, numWordsLeft);
						numWordsLeft = fixedLen - writtenCnt;
//						for (int idx : titleWordIndices) {
//							if (idx > -1) {
//								System.out.print(wordVectorSet.getWord(idx) + " ");
//							}
//						}
//						if (notableForWordIndices != null) {
//							for (int idx : notableForWordIndices) {
//								if (idx > -1) {
//									System.out.print(wordVectorSet.getWord(idx) + " ");
//								}
//							}
//						}
//						System.out.println(usedNumKeywordIndices);
						if (usedNumKeywordIndices > 0 && numWordsLeft != 0) {
							for (int i = 0; i < usedNumKeywordIndices; ++i) {
								dos.writeInt(keywordsIndices[i]);
								++writtenCnt;
								if (writtenCnt == fixedLen)
									break;
//								System.out.print(wordVectorSet.getWord(keywordsIndices[i]) + " ");
							}
						}
						numWordsLeft = fixedLen - writtenCnt;
//						System.out.println();

						if (fixedLen > -1) {
							if (numWordsLeft < 0)
								System.out.println("numWordsLeft < 0");
							for (int i = 0; i < numWordsLeft + padLen; ++i)
								dos.writeInt(0);
						}
						++cnt;
					}
				}

//				if (cnt == 10)
//					break;
				if (cnt % 100000 == 0)
					System.out.println(cnt);
			}

			reader0.close();
			reader1.close();
			reader2.close();
			if (reader3 != null)
				reader3.close();
			dos.close();

			// fix the number of entities
			RandomAccessFile raf = new RandomAccessFile(dstFileName, "rw");
			raf.writeInt(cnt);
			raf.close();

			System.out.println("max num indices: " + maxNumIndices);
			System.out.println(cnt + " wids.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int filterKeywordsIndices(int[] keywordsIndices, int[] titleWordIndices,
			int[] notableForWordIndices) {
		int usedNumKeywordIndices = 0;
		for (int i = 0; i < keywordsIndices.length; ++i) {
			int idx = keywordsIndices[i];
			if (idx < 0)
				continue;
			
			boolean flg = true;
			for (int j = 0; j < titleWordIndices.length && flg; ++j) {
				if (titleWordIndices[j] == idx) {
					flg = false;
				}
			}
			
			if (notableForWordIndices != null) {
				for (int j = 0; j < notableForWordIndices.length && flg; ++j) {
					if (notableForWordIndices[j] == idx) {
						flg = false;
					}
				}
			}
			
			if (flg) {
				keywordsIndices[usedNumKeywordIndices++] = idx;
			}
		}
		
		return usedNumKeywordIndices;
	}
	
	private static int countLegalWordIndices(int[] wordIndices) {
		if (wordIndices == null)
			return 0;
		
		int cnt = 0;
		for (int idx : wordIndices) {
			if (idx > -1)
				++cnt;
		}
		return cnt;
	}
	
	private static int writeLegalWordIndices(DataOutputStream dos, int[] wordIndices, int maxLen) throws IOException {
		if (wordIndices == null || maxLen == 0)
			return 0;

		int cnt = 0;
		for (int idx : wordIndices) {
			if (idx > -1) {
				dos.writeInt(idx);
				++cnt;
				if (cnt == maxLen)
					break;
			}
		}
		return cnt;
	}
	
	private static int[] getWordVectorIndices(WordVectorSet wordVectorSet, String[] words, boolean incIndexByOne) {
		if (words == null)
			return null;
		
		int[] indices = new int[words.length];
		int pos = 0;
		for (String word : words) {
			indices[pos] = wordVectorSet.getWordIndex(word);
			if (indices[pos] > -1 && incIndexByOne)
				++indices[pos];
			++pos;
		}
		
		return indices;
	}
	
	private static int[] getWordVectorIndices(WordVectorSet wordVectorSet, String words, boolean incIndexByOne) {
		if (words == null)
			return null;

		words = words.replace('/', ' ');
		words = words.replaceAll("[\\(\\)]", " ");
		
		StringReader sr = new StringReader(words);
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(sr,
				new CoreLabelTokenFactory(), "ptb3Escaping=false,untokenizable=noneKeep");
		List<CoreLabel> labels = ptbt.tokenize();
		int[] indices = new int[labels.size()];
		sr.close();
		int pos = 0;
		for (CoreLabel label : labels) {
			String word = label.toString();
			indices[pos] = wordVectorSet.getWordIndex(word.toLowerCase());
			if (indices[pos] > -1 && incIndexByOne)
				++indices[pos];
			++pos;
		}
		
		return indices;
	}

	private static float[] getEntityVector(String title, String notableFor,
			WordVectorSet wordVectorSet) {
		int wordVecLen = wordVectorSet.getWordVecSize();
		float[] vec = new float[wordVecLen * 2];

		int wordCnt = 0;
		boolean hasValue = false;
		title = title.replace('/', ' ');
		title = title.replaceAll("[\\(\\)]", " ");
		StringReader sr = new StringReader(title);
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(sr,
				new CoreLabelTokenFactory(), "ptb3Escaping=false,untokenizable=noneKeep");
		List<CoreLabel> labels = ptbt.tokenize();
		sr.close();
		for (CoreLabel label : labels) {
			String word = label.toString();
//			float[] wvec = wordVectorSet.getVector(word.toLowerCase());
//			if (wvec == null) {
//				wvec = wordVectorSet.getVector(word);
//			}
			float[] wvec = wordVectorSet.getVector(word.toLowerCase());

			if (wvec != null) {
				for (int i = 0; i < wvec.length; ++i) {
					vec[i] += wvec[i];
				}
				++wordCnt;
			}
		}
		
		if (wordCnt > 0) {
			hasValue = true;
			for (int i = 0; i < wordVecLen; ++i) {
				vec[i] /= wordCnt;
			}
		}

		if (notableFor != null) {
			notableFor = notableFor.replace('/', ' ');
			notableFor = notableFor.replaceAll("[\\(\\)]", " ");
			sr = new StringReader(notableFor);
			ptbt = new PTBTokenizer<>(sr, new CoreLabelTokenFactory(),
					"ptb3Escaping=false,untokenizable=noneKeep");
			labels = ptbt.tokenize();
			sr.close();
			wordCnt = 0;
			for (CoreLabel label : labels) {
				// use lower case as default
				String word = label.toString();
//				float[] wvec = wordVectorSet.getVector(word.toLowerCase());
//				if (wvec == null) {
//					wvec = wordVectorSet.getVector(word);
//				}
				float[] wvec = wordVectorSet.getVector(word.toLowerCase());

				if (wvec != null) {
						for (int i = 0; i < wvec.length; ++i) {
							vec[i + wordVecLen] += wvec[i];
						}
					++wordCnt;
				}
			}
			
			if (wordCnt > 0) {
				hasValue = true;
				for (int i = wordVecLen; i < vec.length; ++i) {
					vec[i] /= wordCnt;
				}
			}
		}

		return hasValue ? vec : null;
	}

	public static void genWikiTrainingDataWordVec(String wikiDataFileName,
			String dictAliasFileName, String dictWidFileName,
			String wordVecFileName, String dstTrainFileName,
			String dstEvalFileName, String dstTestFileName) {
		SimpleWikiDict simpleWikiDict = new SimpleWikiDict(dictAliasFileName,
				dictWidFileName);
		// if (simpleWikiDict != null)
		// return ;
		WordVectorSet wordVectorSet = new WordVectorSet(wordVecFileName);

		DataOutputStream dosTrain = IOUtils
				.getBufferedDataOutputStream(dstTrainFileName);
		DataOutputStream dosEval = IOUtils
				.getBufferedDataOutputStream(dstEvalFileName);
		DataOutputStream dosTest = IOUtils
				.getBufferedDataOutputStream(dstTestFileName);
		BufferedReader reader = IOUtils.getUTF8BufReader(wikiDataFileName);
		String line = null;
		long textCnt = 0, mentionCnt = 0;
		Random random = new Random();
		final int randomSkipWidth = 10;
		int rndCnt = 0, nextRnd = random.nextInt(randomSkipWidth);
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
					// if (span.beg > span.end) {
					// System.out.println(textCnt + "\t" + span.beg + "\t" +
					// span.end);
					// }
					mentionSpans[i] = span;
				}

				IndexSentenceWithMentions iswm = TokenizeUtils.indexWords(line,
						mentionSpans, wordVectorSet);

				if (mentionSpans.length != iswm.mentionSpans.length) {
					System.out.println("mention span length not equal.");
					continue;
				}

				// System.out.println(rndCnt + " " + nextRnd);
				if (rndCnt == nextRnd) {
					if (toEval) {
						writeData(dosEval, line, iswm, mentionSpans,
								simpleWikiDict, trueWids);
						toEval = false;
					} else {
						writeData(dosTest, line, iswm, mentionSpans,
								simpleWikiDict, trueWids);
						toEval = true;
					}
					rndCnt = -1;
					nextRnd = random.nextInt(randomSkipWidth);
				} else {
					writeData(dosTrain, line, iswm, mentionSpans,
							simpleWikiDict, trueWids);
				}

				++rndCnt;

				++textCnt;
				// dbgCnt = textCnt;
				// if (textCnt == 150000)
				// break;
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

	// private static long dbgCnt = 0; // TODO remove

	private static void writeData(DataOutputStream dos, String line,
			IndexSentenceWithMentions iswm, Span[] mentionSpans,
			SimpleWikiDict simpleWikiDict, int[] trueWids) throws IOException {
		dos.writeInt(iswm.wordIndices.length);
		for (int wordIndex : iswm.wordIndices) {
			dos.writeInt(wordIndex);
		}

		dos.writeInt(mentionSpans.length);

		for (int i = 0; i < mentionSpans.length; ++i) {
			dos.writeInt(iswm.mentionSpans[i].beg);
			dos.writeInt(iswm.mentionSpans[i].end);

			Span span = mentionSpans[i];

			// System.out.println(line.substring(span.beg, span.end + 1));
			int[] wids = simpleWikiDict.getCandidates(line.substring(span.beg,
					span.end + 1));

			if (wids == null) {
				dos.writeInt(1);
				dos.writeInt(trueWids[i]);
			} else if (wids.length <= MAX_NUM_CANDIDATES) {
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
				int[] rndIndices = CommonUtils.genNonRepeatingRandom(
						wids.length, MAX_NUM_CANDIDATES);
				// int[] rndIndices = { 0, 1, 2, 3, 4 };
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
					} else {
						// if (dbgCnt == 10996)
						// System.out.println("true " + rndIdx + "\t" +
						// wids[rndIdx]);
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
		// return wordVectorPairs;
	}

	private static int[] loadWidsFromEntityRepFile(String fileName) {
		System.out.println("loading wids...");
		DataInputStream dis = IOUtils.getBufferedDataInputStream(fileName);
		int[] wids = null;
		try {
			int numWids = dis.readInt(), wordVecLen = dis.readInt();
			System.out.println(numWids + "\t" + wordVecLen);
			wids = new int[numWids];
			for (int i = 0; i < numWids; ++i) {
				wids[i] = dis.readInt();
				if (wids[i] < 0) {
					System.err.println("wid smaller than 0!");
					return null;
				}
				for (int j = 0; j < wordVecLen; ++j) {
					dis.readFloat();
				}

				if (i % 100000 == 0)
					System.out.println(i);
			}

			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done.");
		return wids;
	}

	// filter those without notable_for attributes
	public static void filterTrainingData(String wikiTrainingDataFileName,
			String widEntityRepFileName, String dstFileName) {
		BufferedReader reader = IOUtils
				.getUTF8BufReader(wikiTrainingDataFileName);

		// int numWids = IOUtils.getNumLinesFor(widNotableForFileName);
		int[] wids = loadWidsFromEntityRepFile(widEntityRepFileName);
		// loadWidNotableForAttributes(widNotableForFileName, wids, null);

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
}
