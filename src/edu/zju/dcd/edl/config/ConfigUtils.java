// author: DHL brnpoem@gmail.com

package edu.zju.dcd.edl.config;

import edu.zju.dcd.edl.cg.AliasDictWithIndex;
import edu.zju.dcd.edl.cg.CandidatesRetriever;
import edu.zju.dcd.edl.cg.IndexedAliasDictWithPse;
import edu.zju.dcd.edl.tac.MidToEidMapper;

public class ConfigUtils {	
	public static CandidatesRetriever getCandidateRetriever(IniFile.Section sect) {
		if (sect == null)
			return null;
		
		IndexedAliasDictWithPse indexedAliasDictWithPse = getAliasDictWithPse(sect);
		String midPopularityFileName = sect.getValue("mid_popularity_file"),
				personListFileName = sect.getValue("person_list_file");
		return new CandidatesRetriever(indexedAliasDictWithPse, midPopularityFileName, personListFileName);
	}
	
	public static AliasDictWithIndex getAliasDict(IniFile.Section sect) {
		if (sect == null)
			return null;
		
		String dictAliasFileName = sect.getValue("alias_file"),
				dictAliasIndexFileName = sect.getValue("alias_index_file"),
				dictMidFileName = sect.getValue("mid_file");

		return new AliasDictWithIndex(dictAliasFileName,
				dictAliasIndexFileName, dictMidFileName);
	}

	public static IndexedAliasDictWithPse getAliasDictWithPse(IniFile.Section sect) {
		if (sect == null)
			return null;
		
		String dictAliasFileName = sect.getValue("alias_file"),
				dictAliasIndexFileName = sect.getValue("alias_index_file"),
				dictMidFileName = sect.getValue("mid_file");

		return new IndexedAliasDictWithPse(dictAliasFileName,
				dictAliasIndexFileName, dictMidFileName);
	}
	
	public static MidToEidMapper getMidToEidMapper(IniFile.Section sect) {
		if (sect == null)
			return null;
		
		String midToEidFileName = sect.getValue("mid_to_eid_file");
		MidToEidMapper mapper = new MidToEidMapper(midToEidFileName);
		return mapper;
	}
}
