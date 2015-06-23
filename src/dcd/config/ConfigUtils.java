// author: DHL brnpoem@gmail.com

package dcd.config;

import dcd.el.dict.AliasDictWithIndex;
import dcd.el.tac.MidToEidMapper;

public class ConfigUtils {
	public static AliasDictWithIndex getAliasDict(IniFile.Section sect) {
		if (sect == null)
			return null;
		
		String dictAliasFileName = sect.getValue("alias_file"),
				dictAliasIndexFileName = sect.getValue("alias_index_file"),
				dictMidFileName = sect.getValue("mid_file");

		return new AliasDictWithIndex(dictAliasFileName,
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
