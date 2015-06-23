// author: DHL brnpoem@gmail.com

package dcd.el.tools;


public class MidPopularityPair implements Comparable<MidPopularityPair> {
	public String getMid() {
		return mid;
	}
	public void setMid(String mid) {
		this.mid = mid;
	}
	public int getPopularity() {
		return popularity;
	}
	public void setPopularity(int popularity) {
		this.popularity = popularity;
	}
	
	@Override
	public int compareTo(MidPopularityPair arg0) {
		MidPopularityPair mpp0 = (MidPopularityPair)arg0;
		return this.getMid().compareTo(mpp0.getMid());
	}
	
	private String mid;
	private int popularity;
	
}
