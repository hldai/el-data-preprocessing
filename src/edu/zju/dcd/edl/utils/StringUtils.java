package edu.zju.dcd.edl.utils;

import java.io.StringReader;
import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class StringUtils {
	public static String[] tokenize(String text) {
		StringReader sr = new StringReader(text);
		List<CoreLabel> labels = null;
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(sr, new CoreLabelTokenFactory(),
				"ptb3Escaping=false,untokenizable=noneKeep");
		labels = ptbt.tokenize();
		sr.close();
		
		String[] words = new String[labels.size()];
		int i = 0;
		for (CoreLabel label : labels) {
			words[i++] = label.value();
		}
		
		return words;
	}
}
