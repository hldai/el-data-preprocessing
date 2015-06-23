// author: DHL brnpoem@gmail.com

package dcd.el.wiki;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dcd.el.utils.CommonUtils;
import dcd.el.io.IOUtils;
import dcd.el.io.Item;
import dcd.el.io.ItemReader;
import dcd.el.io.ItemWriter;

public class WikiTools {
	public static void checkDumpSymbols(String wikiFileName) {
		BufferedReader reader = IOUtils.getGZIPBufReader(wikiFileName);

		try {
			String line = null;

			long cnt = 0;
			while ((line = reader.readLine()) != null) {
				if (line.contains("&apos;&apos;")
						|| line.contains("&#39;&#39;")) {
					System.out.println("double apos! " + line);
					break;
				}
				if (line.contains("&#91;&#91;")) {
					System.out.println("double [! " + line);
					break;
				}

				++cnt;
				// System.out.println(line);
				// if (cnt == 10) break;
			}

			reader.close();

			System.out.println(cnt + " lines checked.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void extractXMLPages(String gzWikiFileName, String dstFileName) {
		BufferedReader reader = IOUtils.getGZIPBufReader(gzWikiFileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);

		try {
			// String page = WikiReaderXML.nextPage(reader);
			int cnt = 0;
			String page = null;
			while ((page = WikiReaderXML.nextPage(reader)) != null) {
				writer.write(page);
				++cnt;

				// if (cnt == 10) break;
				if (cnt % 100000 == 0) {
					System.out.println(cnt);
				}
			}

			reader.close();
			writer.close();

			System.out.println(cnt + " pages read and written.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void filterRedirect(String gzWikiFileName,
			String dstFileName, String redListFileName, String weirdPageFileName) {
		// BufferedReader reader = IOUtils.getGZIPBufReader(gzWikiFileName);
		BufferedReader reader = IOUtils.getUTF8BufReader(gzWikiFileName);
		BufferedWriter nredWikiWriter = IOUtils
				.getUTF8BufWriter(dstFileName);
		BufferedWriter redListWriter = IOUtils
				.getUTF8BufWriter(redListFileName);
		BufferedWriter wpWriter = IOUtils
				.getUTF8BufWriter(weirdPageFileName);

		if (nredWikiWriter == null || redListWriter == null || wpWriter == null)
			return;

		try {
			Pattern p = Pattern.compile("<redirect\\s*title=\"(.*?)\"\\s*/>");
			Matcher m = null;
			String page = null;
			String title = null;
			long cnt = 0, redCnt = 0;
			while ((page = WikiReaderXML.nextPage(reader)) != null) {
				title = getTitle(page);
				if (title == null) {
					System.out.println("Failed to find title!");
					wpWriter.write(page);
				}

				m = p.matcher(page);
				if (m.find()) {
					redListWriter.write(title + "\t" + m.group(1) + "\n");
					++redCnt;
					// System.out.println(m.group(1));
				} else {
					nredWikiWriter.write(page);
				}

				++cnt;
				// if (cnt == 10) break;
				if (cnt % 100000 == 0) {
					System.out.println(cnt);
				}
			}

			reader.close();
			nredWikiWriter.close();
			redListWriter.close();
			wpWriter.close();

			System.out.println(cnt + " pages processed.");
			System.out.println(redCnt + " redirect pages.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void stripWikiXML(String xmlWikiFileName, String dstFileName,
			String weirdPageFileName) {
		stripWikiXML(xmlWikiFileName, dstFileName, weirdPageFileName, false);
	}
	
	public static void searchWikiXMLPageById(String fileName, String wid, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(fileName);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		if (writer == null)
			return ;
		
		try {
			String page = null;
			Matcher m = null;
			boolean endLoop = false;
			while (!endLoop && (page = WikiReaderXML.nextPage(reader)) != null) {
				m = matchPage(page);
				if (m.find()) {
					if (m.group(2).trim().equals(wid)) {
						endLoop = true;
					}
				}
			}
			
			reader.close();
			
			// article is found.
			if (endLoop) {
				writer.write(page);
			}
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void stripWikiXML(String xmlWikiFileName, String dstFileName,
			String weirdPageFileName, boolean withId) {
		BufferedReader reader = IOUtils.getUTF8BufReader(xmlWikiFileName);
		BufferedWriter dstWriter = IOUtils.getUTF8BufWriter(dstFileName);
		BufferedWriter wpWriter = IOUtils
				.getUTF8BufWriter(weirdPageFileName);

		if (dstWriter == null || wpWriter == null) {
			return;
		}

		try {
			String page = null;
			long cnt = 0, matchedCnt = 0;
			Matcher m = null;
			int numTitleLines = 0, numTextLines = 0;
			String title = null, text = null;
			while ((page = WikiReaderXML.nextPage(reader)) != null) {
				m = matchPage(page);
				if (m.find()) {
					if (withId) {
						String id = m.group(2).trim();

						if (!idOK(id)) {
							System.out.println(id + " #id not OK");
							wpWriter.write(page);
						} else {
							dstWriter.write("ID 1\n");
							dstWriter.write(id + "\n");
						}
					}

					title = m.group(1).trim();
					text = m.group(5).trim();

					numTitleLines = CommonUtils.countLines(title);
					if (numTitleLines != 0) {
						System.out.println("title line larger than 0");
						wpWriter.write(page);
						continue;
					}

					dstWriter.write("TITLE 1\n");
					dstWriter.write(title + "\n");

					// will add a '\n' at the end
					numTextLines = CommonUtils.countLines(text) + 1;
					dstWriter.write("TEXT " + numTextLines + "\n");
					dstWriter.write(text + "\n");

					++matchedCnt;
				} else {
					System.out.println("Unmatched page!");
					wpWriter.write(page);
				}

				++cnt;

				// if (cnt == 15) break;
				if (cnt % 100000 == 0)
					System.out.println(cnt);
			}

			reader.close();
			dstWriter.close();
			wpWriter.close();

			System.out.println(cnt + " pages processed.\n" + matchedCnt
					+ " pages matched and written.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void genAnchorList(String wikiFilePath,
			String anchorListFilePath) {
		WikiReaderStripped wrs = new WikiReaderStripped();
		wrs.open(wikiFilePath, false, true);

		ItemWriter writer = new ItemWriter();
		writer.open(anchorListFilePath);

		Item anchorListItem = new Item();
		anchorListItem.key = "ANCHOR";
		int cnt = 0;
		while (wrs.nextPage()) {
			// System.out.println(wrs.getTitleItem().value);
			writer.writeItem(wrs.getIdItem());
			writer.writeItem(wrs.getTitleItem());

			anchorListItem.value = getAnchorList(wrs.getTextItem().value);
			writer.writeItem(anchorListItem);

			++cnt;
			// if (cnt == 2) break;
			if (cnt % 100000 == 0)
				System.out.println(cnt);
		}

		wrs.close();
		writer.close();

		System.out.println(cnt + " pages processed.");
	}

	public static void genAnchorAliasList(String anchorListFilePath,
			String dstFilePath) {
		ItemReader reader = new ItemReader();
		reader.open(anchorListFilePath, false);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFilePath);

		Item anchorListItem = null;
		int cnt = 0, wcnt = 0;
		try {
			while (reader.readNextItem() != null) {
				reader.readNextItem();
				anchorListItem = reader.readNextItem();

				String[] lines = anchorListItem.value.split("\n");
				for (String line : lines) {
					if (line.contains("\t")) {
						String[] vals = line.split("\t");
						if (vals.length > 2) {
							System.out.println("More than two \\t!");
						} else {
							writer.write(vals[1] + "\t" + vals[0] + "\n");
							++wcnt;
						}
					}
				}

				++cnt;
				// if (cnt == 10) break;
				if (cnt % 100000 == 0)
					System.out.println(cnt);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		reader.close();
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(cnt + " pages processed.");
		System.out.println(wcnt + " lines written.");
	}
	
	public static void mergeCharListFiles(String file0, String file1, String dstFileName) {
		BufferedReader reader = IOUtils.getUTF8BufReader(file0);
		BufferedWriter writer = IOUtils.getUTF8BufWriter(dstFileName);
		
		int cnt = 0;
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				writer.write(line + "\n");
				
				++cnt;
			}
			System.out.println(cnt + " lines processed.");
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		reader = IOUtils.getUTF8BufReader(file1);
		
		try {
			cnt = 0;
			while ((line = reader.readLine()) != null) {
				String[] vals = line.split("\t");
				if (vals.length == 3) {
					writer.write(vals[2] + "\t" + vals[0] + "\n");
				}
				
				++cnt;
			}
			System.out.println(cnt + " lines processed.");
			
			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getAnchorList(String text) {
		StringBuilder sb = new StringBuilder();

		// Pattern p = Pattern
		// .compile("\\[\\[([^\\[\\]]*?)\\|([^\\[\\]]*?)\\]\\]");
		Pattern p = Pattern.compile("\\[\\[([^\\[\\]]*?)\\]\\]");
		Matcher m = p.matcher(text);
		boolean isFirst = true;
		while (m.find()) {
			String mstr = m.group(1);
			int vbarPos = 0;
			while (vbarPos < mstr.length() && mstr.charAt(vbarPos) != '|')
				++vbarPos;

			String target = mstr.substring(0, vbarPos).trim();
			// System.out.println(target);
			if (legalName(target) && properTarget(target)) {
				if (isFirst)
					isFirst = false;
				else
					sb.append("\n");

				sb.append(target);

				if (vbarPos == mstr.length())
					continue;

				String name = mstr.substring(vbarPos + 1, mstr.length()).trim();
				if (legalName(name)) {
					sb.append("\t").append(name);
				}
			}
		}

		return new String(sb);
	}

	private static boolean idOK(String id) {
		for (int i = 0; i < id.length(); ++i) {
			if (id.charAt(i) < '0' || id.charAt(i) > '9') {
				return false;
			}
		}

		return true;
	}

	private static String getTitle(String page) {
		Pattern p = Pattern.compile("<title>(.*?)</title>");
		Matcher m = p.matcher(page);
		if (m.find()) {
			return m.group(1);
		}

		return null;
	}

	private static Matcher matchPage(String page) {
		Pattern p = Pattern.compile("\\s*<page>.*?" + "<title>(.*?)</title>.*?"
				+ "<id>(.*?)</id>.*?" + "<revision>\\s*" + "<id>(.*?)</id>.*?"
				+ "<timestamp>(.*?)</timestamp>.*?"
				+ "<text\\s*xml:space=\"preserve\">(.*?)</text>.*?"
				+ "<sha1>(.*?)</sha1>.*?" + "</page>", Pattern.DOTALL);
		return p.matcher(page);
	}

	private static boolean legalName(String name) {
		if (name.length() == 0)
			return false;

		char ch;
		for (int i = 0; i < name.length(); ++i) {
			ch = name.charAt(i);
			if (ch == '\t' || ch == '\n')
				return false;
		}

		return true;
	}

	// this method is not fully implemented
	// but it shall be fine
	private static boolean properTarget(String target) {
		String beg = target.substring(0, Integer.min(10, target.length()));
		beg = beg.toLowerCase();

		return !(beg.charAt(0) == ':' || beg.startsWith("file:")
				|| beg.startsWith("category:") || beg.startsWith("wikt:"));
	}
}
