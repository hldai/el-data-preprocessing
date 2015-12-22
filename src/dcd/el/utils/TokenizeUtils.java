package dcd.el.utils;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.TreeMap;

import dcd.el.objects.Span;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.zju.dcd.edl.wordvec.WordVectorSet;

public class TokenizeUtils {
	// sentence with words represented as indices of a word vector set
	public static class IndexSentenceWithMentions {
		public int[] wordIndices = null;
		public Span[] mentionSpans = null;
	}
	
	public static class Words {
		public String words = null;
		public int numWords;
	}
	
	public static Words toWords(String text, boolean filterPunctuation) {
		Words words = new Words();

		StringReader sr = new StringReader(text);
		PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<>(sr,
	              new CoreLabelTokenFactory(), "ptb3Escaping=false,untokenizable=noneDelete");
		StringBuilder sb = new StringBuilder();
		words.numWords = 0;
		while (tokenizer.hasNext()) {
			CoreLabel label = tokenizer.next();
			if (!wordIllegal(label.value())) {
				if (filterPunctuation && !CommonUtils.hasEnglishChar(label.value())) {
					continue;
				}
				
				if (words.numWords > 0)
					sb.append(" ");
				sb.append(label.value().toLowerCase());
				++words.numWords;
			}
		}
		sr.close();
		
		words.words = new String(sb);
		
		return words;
	}
	
	public static IndexSentenceWithMentions indexWords(String text, Span[] mentionSpans,
			WordVectorSet wordVectorSet) {
		IndexSentenceWithMentions iswm = new IndexSentenceWithMentions();
		
		int lastPos = 0;
		LinkedList<Integer> wordIndices = new LinkedList<Integer>();
		LinkedList<Span> newSpans = new LinkedList<Span>();
		for (int i = 0; i < mentionSpans.length; ++i) {
			String textPart = text.substring(lastPos, mentionSpans[i].beg);
			if (i == 0)
				textPart = textPart.toLowerCase();
			tokenizeWordIndicesToList(textPart, wordVectorSet, wordIndices);

//			System.out.println("mention:");
			Span span = new Span();
			span.beg = wordIndices.size();
			tokenizeWordIndicesToList(text.substring(mentionSpans[i].beg, mentionSpans[i].end + 1), 
					wordVectorSet, wordIndices);
			span.end = wordIndices.size() - 1;
			newSpans.add(span);
//			System.out.println("mention_end");
			
			lastPos = mentionSpans[i].end + 1;
		}
		
		tokenizeWordIndicesToList(text.substring(lastPos, text.length()),
				wordVectorSet, wordIndices);
		
		iswm.wordIndices = new int[wordIndices.size()];
		int ix = 0;
		for (Integer index : wordIndices) {
			iswm.wordIndices[ix++] = index;
		}
		
		iswm.mentionSpans = newSpans.toArray(new Span[newSpans.size()]);
		
		return iswm;
	}
	
	private static void tokenizeWordIndicesToList(String text, WordVectorSet wordVectorSet,
			LinkedList<Integer> wordIndices) {
		StringReader sr = new StringReader(text);
		PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<>(sr,
	              new CoreLabelTokenFactory(), "ptb3Escaping=false,untokenizable=noneDelete");
		while (tokenizer.hasNext()) {
			CoreLabel label = tokenizer.next();
			int idx = wordVectorSet.getWordIndex(label.toString());
			if (idx < 0) {
				int lcIdx = wordVectorSet.getWordIndex(label.toString().toLowerCase());
				wordIndices.add(lcIdx);
			} else {
				wordIndices.add(idx);
			}
		}
		sr.close();
	}
	
	public static TreeMap<String, Integer> toBagOfWords(String text) {
		TreeMap<String, Integer> m = new TreeMap<String, Integer>();
		StringReader sr = new StringReader(text);
		PTBTokenizer<Word> tokenizer = PTBTokenizer.newPTBTokenizer(sr);
		while (tokenizer.hasNext()) {
			Word w = tokenizer.next();

			// TODO further process?
			String word = w.word().toLowerCase().trim();

			if (wordIllegal(word)) {
				continue;
			}

			Integer val = m.putIfAbsent(word, 1);
			if (val != null) {
				m.put(word, val + 1);
			}
		}
		
		sr.close();
		return m;
	}
	
	private static boolean wordIllegal(String word) {
		int len = word.length();
		return len == 0 || len > 25 || word.startsWith("http://")
				|| word.startsWith("https://") || word.contains("\n")
				|| word.contains("\t") || word.contains(" ")
				|| (word.charAt(0) == '<' && word.charAt(len - 1) == '>');
	}
}
