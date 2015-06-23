// author: DHL brnpoem@gmail.com

package dcd.el.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;

import dcd.el.io.IOUtils;

public class Trie {
	public class Node {
		public Node() {
			ch = ' ';
			val = 0;
		}
		
		public Node(char ch, char val) {
			this.ch = ch;
			this.val = val;
		}
		
		char ch;
		char val;
		
		LinkedList<Node> childNodes = new LinkedList<Node>();
	}
	
	public Trie() {
	}
	
	public void insert(String str) {
		Node curNode = root;
		for (int i = 0; i < str.length(); ++i) {
			char ch = str.charAt(i);
			
			boolean flg = false;
			for (Node n : curNode.childNodes) {
				if (ch == n.ch) {
					curNode = n;
					flg = true;
					break;
				}
			}
			
			if (!flg) {
				Node nn = new Node(ch, (char)0);
				curNode.childNodes.add(nn);
				curNode = nn;
			}
		}
		
		if (curNode.val == 0)
			++numStrings;
		
		++curNode.val;
		++sumVal;
	}
	
	public int check(String str) {
		Node curNode = root;
		
		boolean flg;
		for (int i = 0; i < str.length(); ++i) {
			char ch = str.charAt(i);
			
			flg = false;
			for (Node n : curNode.childNodes) {
				if (n.ch == ch) {
					curNode = n;
					flg = true;
				}
			}
			
			if (!flg)
				return 0;
		}
		
		return curNode.val;
	}
	
	public void store(String fileName) {
		System.out.println("Writing trie to " + fileName + '\n');
		
		BufferedWriter writer = IOUtils.getUTF8BufWriter(fileName);
		
		String str = new String();
		traverseStore(root, str, writer);
		
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Done.");
	}
	
	public void load(String fileName) {
		
	}
	
	public void clearValues() {
		clearValues(root);
	}
	
	public int getSumVal() {
		return sumVal;
	}
	
	public int getNumStrings() {
		return numStrings;
	}
	
	private void traverseStore(Node n, String curStr, BufferedWriter writer) {
		try {
			for (Node chn : n.childNodes) {
				curStr += chn.ch;
				
				if (chn.val > 0) {
					writer.write(curStr + " " 
						+ String.valueOf(chn.val) + "\n");
				}
				
				traverseStore(chn, curStr, writer);
				curStr = curStr.substring(0, curStr.length() - 1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void clearValues(Node n) {
		for (Node chn : n.childNodes) {
			clearValues(chn);
		}
		
		n.val = 0;
	}
	
	private Node root = new Node();
	
	private int sumVal = 0;
	private int numStrings = 0;
}
