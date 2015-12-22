// author: DHL brnpoem@gmail.com

package edu.zju.dcd.edl;

import dcd.el.feature.FeatureTools;
import dcd.el.feature.PopularityGen;
import dcd.el.feature.TfIdfGen;
import edu.zju.dcd.edl.config.IniFile;
import edu.zju.dcd.edl.tools.VecRepresentationTools;

public class FeatureMain {
	public static void genFullWordCountList(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_word_cnt_full");
		if (sect == null)
			return;

		String wikiArticleWordCntFileName = sect.getValue("wiki_wc_file"), dstFileName = sect
				.getValue("dst_file");

		TfIdfGen.genFullWordCountFile(wikiArticleWordCntFileName, dstFileName);
	}

	public static void genWordCount(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_word_cnt");
		if (sect == null)
			return;

		String wikiTextFileName = sect.getValue("wiki_file"), dstFileName = sect
				.getValue("dst_file");

		TfIdfGen.genWikiWordCountFile(wikiTextFileName, dstFileName);
	}

	public static void genPopularityLen(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_popularity_len");
		if (sect == null)
			return;

		String wikiArticleWordCntFileName = sect
				.getValue("wiki_article_wc_file"), maxWordCountFileName = sect
				.getValue("max_word_count_file"), dstFileName = sect
				.getValue("dst_file");
		PopularityGen.genPopularityFile(wikiArticleWordCntFileName,
				maxWordCountFileName, dstFileName);
	}

	public static void genTfIdf(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_tfidf");
		if (sect == null)
			return;

		String wikiArticleWordCntFileName = sect
				.getValue("wiki_article_wc_file"), idfFileName = sect
				.getValue("idf_file"), dstFileName = sect.getValue("dst_file");
		TfIdfGen.genTfIdfFileMem(wikiArticleWordCntFileName, idfFileName,
				dstFileName);
	}

	public static void filterFullWordCnt(IniFile config) {
		IniFile.Section sect = config.getSection("feat_filter_word_cnt");
		if (sect == null)
			return;

		String wordCntFileName = sect.getValue("word_cnt_file"), dstFileName = sect
				.getValue("dst_file");

		TfIdfGen.filterFullWordCountFile(wordCntFileName, dstFileName);
	}

	public static void genIdf(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_idf");
		if (sect == null)
			return;

		String wordCntFileName = sect.getValue("full_wc_file"), wikiArticleWordCntFileName = sect
				.getValue("wiki_article_wc_file"), dstFileName = sect
				.getValue("dst_file");

		TfIdfGen.genIdfFile(wordCntFileName, wikiArticleWordCntFileName,
				dstFileName);
	}

	public static void mergePopTfIdfFeatures(IniFile config) {
		IniFile.Section sect = config.getSection("feat_merge_pop_tfidf");
		if (sect == null)
			return;

		String midToWidFileName = sect.getValue("mid_wid_file"), popFeatFileName = sect
				.getValue("pop_feat_file"), tfidfFeatFileName = sect
				.getValue("tfidf_feat_file"), dstFeatFileName = sect
				.getValue("dst_feat_file"), dstIdxFileName = sect
				.getValue("dst_idx_file");

		FeatureTools.mergePopTfIdfFeatures(midToWidFileName, popFeatFileName,
				tfidfFeatFileName, dstFeatFileName, dstIdxFileName);
	}

	private static void mapMidToFeatIndex(IniFile config) {
		IniFile.Section sect = config.getSection("feat_map_mid_to_feat_idx");
		if (sect == null)
			return;

		String midToWidFileName = sect.getValue("mid_to_wid_file"), widFeatIndexFileName = sect
				.getValue("wid_feat_index_file"), dstFileName = sect
				.getValue("dst_file");

		FeatureTools.mapMidToFeatIndex(midToWidFileName, widFeatIndexFileName,
				dstFileName);
	}

	// number of aliases of entities
	private static void genNumAliasesFile(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_num_aliases");
		String aliasListFileName = sect.getValue("alias_list_file"), dstFileName = sect
				.getValue("dst_file");
		FeatureTools.genEnityNumAliasesFile(aliasListFileName, dstFileName);
	}

	private static void genPopularityLink(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_popularity_link");
		String anchorCntFileName = sect.getValue("anchor_cnt_file"), maxRefCntFileName = sect
				.getValue("max_ref_cnt_file"), dstFileName = sect
				.getValue("dst_file");
		FeatureTools.genPopularityLink(anchorCntFileName, maxRefCntFileName,
				dstFileName);
	}
	
	private static void genMidName(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_mid_name");
		String midNameFileName = sect.getValue("mid_name_file"), 
				midToWidFileName = sect.getValue("mid_wid_file"),
				widTitleFileName = sect.getValue("wid_title_file"),
				dstMidNameForVecRepFileName = sect.getValue("dst_mid_name_file"),
				dstMidNameForVecRepWikiOnlyFileName = sect.getValue("dst_mid_name_wiki_only_file");
		VecRepresentationTools.genMidNameForVecRepresentation(midNameFileName, midToWidFileName, widTitleFileName,
				dstMidNameForVecRepFileName, dstMidNameForVecRepWikiOnlyFileName);
	}
	
	private static void genTfIdfIndex(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_tfidf_index");
		String midToWidFileName = sect.getValue("mid_wid_file"), 
				tfIdfFileName = sect.getValue("tfidf_feat_file"),
				dstIndexFileName = sect.getValue("dst_idx_file");
		FeatureTools.genTfIdfIndex(midToWidFileName, tfIdfFileName, dstIndexFileName);
	}
	
	private static void genMidPop(IniFile config) {
		IniFile.Section sect = config.getSection("feat_gen_mid_pop");
		String midToWidFileName = sect.getValue("mid_wid_file"), 
				widPopFileName = sect.getValue("wid_pop_file"),
				dstFileName = sect.getValue("dst_file");
		FeatureTools.genMidPop(midToWidFileName, widPopFileName, dstFileName);
	}

	public static void run(IniFile config) {
		String job = config.getValue("main", "job");
		if (job.equals("feat_gen_popularity_len")) {
			genPopularityLen(config);
		} else if (job.equals("feat_test")) {
			FeatureTools.test();
		} else if (job.equals("feat_gen_tfidf")) {
			genTfIdf(config);
		} else if (job.equals("feat_gen_word_cnt")) {
			genWordCount(config);
		} else if (job.equals("feat_gen_word_cnt_full")) {
			genFullWordCountList(config);
		} else if (job.equals("feat_filter_word_cnt")) {
			filterFullWordCnt(config);
		} else if (job.equals("feat_gen_idf")) {
			genIdf(config);
		} else if (job.equals("feat_merge_pop_tfidf"))
			mergePopTfIdfFeatures(config);
		else if (job.equals("feat_map_mid_to_feat_idx"))
			mapMidToFeatIndex(config);
		else if (job.equals("feat_gen_num_aliases"))
			genNumAliasesFile(config);
		else if (job.equals("feat_gen_popularity_link"))
			genPopularityLink(config);
		else if (job.equals("feat_gen_mid_name"))
			genMidName(config);
		else if (job.equals("feat_gen_tfidf_index"))
			genTfIdfIndex(config);
		else if (job.equals("feat_gen_mid_pop"))
			genMidPop(config);
	}
}
