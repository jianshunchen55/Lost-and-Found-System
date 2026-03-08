package com.campus.lostfound.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchUtilTest {
  @Test
  void testLevenshtein() {
    double s = MatchUtil.levenshtein("abc", "abd");
    assertTrue(s > 0.6);
  }
  @Test
  void testHaversine() {
    double d = MatchUtil.haversine(0,0,0.001,0.001);
    assertTrue(d > 0);
  }
}
