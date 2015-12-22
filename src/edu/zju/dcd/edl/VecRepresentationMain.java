package edu.zju.dcd.edl;

import edu.zju.dcd.edl.cg.CandidatesRetriever;
import edu.zju.dcd.edl.cg.IndexedAliasDictWithPse;
import edu.zju.dcd.edl.config.ConfigUtils;
import edu.zju.dcd.edl.config.IniFile;
import edu.zju.dcd.edl.tac.MidToEidMapper;
import edu.zju.dcd.edl.tools.CnnContextModelTools;
import edu.zju.dcd.edl.tools.EntityVecTools;
import edu.zju.dcd.edl.tools.VecRepresentationTools;

public class VecRepresentationMain {
	private static void genCnnWikiTrainingData(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_cnn_training_data");
		String wikiDataFileName = sect.getValue("wiki_data_file"),
				dictAliasFileName = sect.getValue("dict_alias_file"),
				dictWidFileName = sect.getValue("dict_wid_file"),
				wordVecFileName = sect.getValue("word_vec_file"),
				dstTrainFileName = sect.getValue("dst_train_file"),
				dstEvalFileName = sect.getValue("dst_val_file"),
				dstTestFileName = sect.getValue("dst_test_file");
		VecRepresentationTools.genWikiTrainingDataWordVec(wikiDataFileName, 
				dictAliasFileName, dictWidFileName, wordVecFileName, dstTrainFileName,
				dstEvalFileName, dstTestFileName);
	}
	
	private static void genCnnTacTrainingData(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_cnn_training_data_tac");
		String textDataFileName = sect.getValue("text_data_file"),
				wordVecFileName = sect.getValue("word_vec_file"),
				dstFileName = sect.getValue("dst_file");
		CnnContextModelTools.genCnnTrainingDataTac(textDataFileName, wordVecFileName, dstFileName);
	}
	
	private static void genJavaFriendlyWordVectorFile(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_word_vec_file_for_java");
		String srcWordVecFileName = sect.getValue("src_word_vec_file"),
				dstFileName = sect.getValue("dst_file");
		VecRepresentationTools.genJavaFriendlyWordVectorFile(srcWordVecFileName, dstFileName);
	}
	
	private static void entityRepresentationToUnitVectors(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_entity_rep_to_unit_vec");
		String fileName = sect.getValue("file"),
				dstFileName = sect.getValue("dst_file");
		VecRepresentationTools.entityRepresentationToUnitVectors(fileName, dstFileName);
	}
	
	private static void genEntityRepresentation(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_entity_rep");
		String widNotableForFileName = sect.getValue("wid_notable_for_file"),
				widTitleFileName = sect.getValue("wid_title_file"),
				wordVecFileName = sect.getValue("word_vec_file"),
				midWidFileName = sect.getValue("mid_wid_file"),
				dstFileName = sect.getValue("dst_file");
		VecRepresentationTools.genEntityRepresentation(widNotableForFileName,
				widTitleFileName, wordVecFileName, midWidFileName, dstFileName);
	}
	
	private static void genEntityRepresentationWordIdxVec(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_entity_rep_word_indices");
		String widNotableForFileName = sect.getValue("wid_notable_for_file"),
				widTitleFileName = sect.getValue("wid_title_file"),
				wordVecFileName = sect.getValue("word_vec_file"),
				midWidFileName = sect.getValue("mid_wid_file"),
				widKeywordsFileName = sect.getValue("wid_keywords_file"),
				dstFileName = sect.getValue("dst_file");
		VecRepresentationTools.genEntityRepresentationWordIdxVec(widNotableForFileName, widTitleFileName, 
				wordVecFileName, midWidFileName, widKeywordsFileName, dstFileName, 50, 1, 0);
	}
	
	private static void genWordVecTrainingData(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_word_vec_training_data");
		String dataFileName = sect.getValue("data_file"),
				dstFileName = sect.getValue("dst_file"),
				filterWordsFileName = sect.getValue("filter_words_file");
		boolean preserveWid = sect.getIntValue("preserve_wid") == 1;
		VecRepresentationTools.genWordVecTrainingData(dataFileName, dstFileName, filterWordsFileName, preserveWid);
	}
	
	private static void filterTrainingData(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_filter_training_data");
		String wikiTrainingDataFileName = sect.getValue("training_data_file"),
				widEntityRepFileName = sect.getValue("wid_entity_rep_file"),
				dstFileName = sect.getValue("dst_file");
		VecRepresentationTools.filterTrainingData(wikiTrainingDataFileName, widEntityRepFileName, dstFileName);
	}
	
	private static void genTextTrainingDataFromTac(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_training_data_tac_text");
		CandidatesRetriever candidatesRetriever = ConfigUtils.getCandidateRetriever(sect);
		
		MidToEidMapper midToEidMapper = ConfigUtils.getMidToEidMapper(sect);
		
		String midWidFileName = sect.getValue("mid_wid_file"),
				queryFileName = sect.getValue("query_file"),
				goldFileName = sect.getValue("gold_file"),
				docDir = sect.getValue("src_doc_path"),
				dstFileName = sect.getValue("dst_file");
		
		CnnContextModelTools.genTextTrainingDataFromTac(candidatesRetriever, midToEidMapper, 
				midWidFileName, queryFileName, goldFileName, docDir,
				dstFileName);
	}
	
	private static void splitEntityVecTrainingData(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_split_entity_vec_training_data");
		String fileName = sect.getValue("src_file"),
				dstPath = sect.getValue("dst_path");
		VecRepresentationTools.splitEntityVecTrainingData(fileName, dstPath);
	}
	
	private static void mergeEntityVecs(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_merge_entity_vecs");
		String dataPath = sect.getValue("path"),
				dstFileName = sect.getValue("dst_file");
		EntityVecTools.mergeEntityVecs(dataPath, dstFileName);
	}
	
	private static void genPredictBaseFile(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_predict_base");
		String entityVecFileName = sect.getValue("entity_vec_file"),
				outputVecFileName = sect.getValue("output_vec_file"),
				dstFileName = sect.getValue("dst_file");
		EntityVecTools.genPredictBaseFile(entityVecFileName, outputVecFileName, dstFileName);
	}
	
	private static void sortDivisorsFile(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_sort_divisors_file");
		String fileName = sect.getValue("divisors_file"),
				dstFileName = sect.getValue("dst_file");
		EntityVecTools.sortDivisorsFile(fileName, dstFileName);
	}
	
	private static void genMidNameVecs(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_mid_name_vec");
		String midNameFileName = sect.getValue("mid_name_file"),
				wordVecFileName = sect.getValue("word_vec_file"),
				dstFileName = sect.getValue("dst_file");
		VecRepresentationTools.genMidNameVecs(midNameFileName, wordVecFileName, dstFileName);
	}
	
	private static void genNameVecTypeData(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_name_type_data");
		String xmlFileName = sect.getValue("xml_file"),
				tabFileName = sect.getValue("tab_file"),
				wordVecFileName = sect.getValue("word_vec_file"),
				dstFileName = sect.getValue("dst_file");
		VecRepresentationTools.genNameVecTypeData(xmlFileName, tabFileName, wordVecFileName, dstFileName);
	}
	
	public static void run(IniFile config) {
		String job = config.getValue("main", "job");
		if (job.equals("vecrep_gen_word_vec_file_for_java"))
			genJavaFriendlyWordVectorFile(config);
		else if (job.equals("vecrep_gen_cnn_training_data"))
			genCnnWikiTrainingData(config);
		else if (job.equals("vecrep_gen_entity_rep"))
			genEntityRepresentation(config);
		else if (job.equals("vecrep_gen_word_vec_training_data"))
			genWordVecTrainingData(config);
		else if (job.equals("vecrep_filter_training_data"))
			filterTrainingData(config);
		else if (job.equals("vecrep_gen_training_data_tac_text"))
			genTextTrainingDataFromTac(config);
		else if (job.equals("vecrep_gen_cnn_training_data_tac"))
			genCnnTacTrainingData(config);
		else if (job.equals("vecrep_entity_rep_to_unit_vec"))
			entityRepresentationToUnitVectors(config);
		else if (job.equals("vecrep_gen_entity_rep_word_indices"))
			genEntityRepresentationWordIdxVec(config);
		else if (job.equals("vecrep_split_entity_vec_training_data"))
			splitEntityVecTrainingData(config);
		else if (job.equals("vecrep_merge_entity_vecs"))
			mergeEntityVecs(config);
		else if (job.equals("vecrep_gen_predict_base"))
			genPredictBaseFile(config);
		else if (job.equals("vecrep_sort_divisors_file"))
			sortDivisorsFile(config);
		else if (job.equals("vecrep_gen_mid_name_vec"))
			genMidNameVecs(config);
		else if (job.equals("vecrep_gen_name_type_data"))
			genNameVecTypeData(config);
	}
}
