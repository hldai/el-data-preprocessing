package dcd.el;

import dcd.config.IniFile;
import dcd.el.tools.VecRepresentationTools;

public class VecRepresentationMain {
	private static void genWikiTrainingDataWordVec(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_training_data_word_vec");
		String wikiDataFileName = sect.getValue("wiki_data_file"),
				dictAliasFileName = sect.getValue("dict_alias_file"),
				dictWidFileName = sect.getValue("dict_wid_file"),
				wordVecFileName = sect.getValue("word_vec_file"),
				dstTrainFileName = sect.getValue("dst_train_file"),
				dstEvalFileName = sect.getValue("dst_eval_file"),
				dstTestFileName = sect.getValue("dst_test_file");
		VecRepresentationTools.genWikiTrainingDataWordVec(wikiDataFileName, 
				dictAliasFileName, dictWidFileName, wordVecFileName, dstTrainFileName,
				dstEvalFileName, dstTestFileName);
	}
	
	private static void genJavaFriendlyWordVectorFile(IniFile config) {
		IniFile.Section sect = config.getSection("vecrep_gen_word_vec_file_for_java");
		String srcWordVecFileName = sect.getValue("src_word_vec_file"),
				dstFileName = sect.getValue("dst_file");
		VecRepresentationTools.genJavaFriendlyWordVectorFile(srcWordVecFileName, dstFileName);
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
	
	public static void run(IniFile config) {
		String job = config.getValue("main", "job");
		if (job.equals("vecrep_gen_word_vec_file_for_java"))
			genJavaFriendlyWordVectorFile(config);
		else if (job.equals("vecrep_gen_training_data_word_vec"))
			genWikiTrainingDataWordVec(config);
		else if (job.equals("vecrep_gen_entity_rep"))
			genEntityRepresentation(config);
	}
}
