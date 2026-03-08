package com.campus.lostfound.util;

public class MatchUtil {
  public static double levenshtein(String a, String b) {
    int[][] dp = new int[a.length()+1][b.length()+1];
    for (int i=0;i<=a.length();i++) dp[i][0]=i;
    for (int j=0;j<=b.length();j++) dp[0][j]=j;
    for (int i=1;i<=a.length();i++) {
      for (int j=1;j<=b.length();j++) {
        int cost = a.charAt(i-1)==b.charAt(j-1)?0:1;
        dp[i][j]=Math.min(Math.min(dp[i-1][j]+1, dp[i][j-1]+1), dp[i-1][j-1]+cost);
      }
    }
    int max = Math.max(a.length(), b.length());
    return max==0?1.0:1.0 - (double)dp[a.length()][b.length()]/max;
  }
  public static double haversine(double lat1,double lon1,double lat2,double lon2) {
    double R=6371e3;
    double dLat=Math.toRadians(lat2-lat1);
    double dLon=Math.toRadians(lon2-lon1);
    double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
    double c=2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    return R*c;
  }
  public static double score(String cat1,String cat2,long t1,long t2,double lat1,double lon1,double lat2,double lon2,String d1,String d2) {
    // Adjusted weights to prioritize text similarity and reduce false positives from default time/location
    // Previous: Type=0.3, Time=0.25, Loc=0.25, Text=0.2
    // New: Type=0.25, Time=0.15, Loc=0.15, Text=0.45
    double wType=0.25, wTime=0.15, wLoc=0.15, wText=0.45;
    double typeScore = cat1!=null && cat1.equals(cat2) ? 1.0 : 0.0;
    long diff = Math.abs(t1 - t2);
    double timeScore = diff < 48*3600_000L ? 1.0 : diff < 7*24*3600_000L ? 0.5 : 0.0;
    double dist = haversine(lat1, lon1, lat2, lon2);
    double locScore = dist < 200 ? 1.0 : dist < 1000 ? 0.6 : 0.0;
    double textScore = levenshtein(d1==null?"":d1, d2==null?"":d2);
    
    // If text is completely different, penalize heavily
    if (textScore < 0.1) {
        return 0.0;
    }
    
    return typeScore*wType + timeScore*wTime + locScore*wLoc + textScore*wText;
  }
}
