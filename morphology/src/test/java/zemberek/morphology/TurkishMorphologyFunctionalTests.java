package zemberek.morphology;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.RuleBasedAnalyzer;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class TurkishMorphologyFunctionalTests {

  private TurkishMorphology getEmptyTurkishMorphology() {
    return TurkishMorphology
        .builder()
        .disableCache()
        .build();
  }

  private TurkishMorphology getMorphology(String... lines) {
    return TurkishMorphology
        .builder()
        .setLexicon(lines)
        .disableCache()
        .build();
  }

  private TurkishMorphology getAsciiTolerantMorphology(String... lines) {
    RootLexicon lexicon = TurkishDictionaryLoader.load(lines);
    return TurkishMorphology
        .builder()
        .setLexicon(lines)
        .setLexicon(lexicon)
        .ignoreDiacriticsInAnalysis()
        .disableCache()
        .build();
  }

  @Test
  public void testWordsWithCircumflex() {
    TurkishMorphology morphology = getMorphology("zekâ");
    WordAnalysis result = morphology.analyze("zekâ");
    Assert.assertEquals(1, result.analysisCount());
  }

  @Test
  public void test2() {
    TurkishMorphology morphology = getMorphology("Air");
    Assert.assertEquals(0, morphology.analyze("Air'rrr").analysisCount());
    Assert.assertEquals(1, morphology.analyze("Air").analysisCount());
  }

  @Test
  public void testWordsWithDot() {
    TurkishMorphology morphology = getMorphology("Dr [P:Abbrv]");
    WordAnalysis result = morphology.analyze("Dr.");
    Assert.assertEquals(1, result.analysisCount());
  }

  @Test
  public void testRomanNumeral() {
    TurkishMorphology morphology = getMorphology("dört [P:Num,Card;A:Voicing]");
    WordAnalysis result = morphology.analyze("IV");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.RomanNumeral,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testRomanNumeral2() {
    // Instance with no dictionary item.
    TurkishMorphology morphology = getMorphology("dördüncü [P:Num,Ord]");
    WordAnalysis result = morphology.analyze("XXIV.");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.RomanNumeral,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testRomanNumeral3() {
    TurkishMorphology morphology = getMorphology("dört [P:Num,Card;A:Voicing]");
    WordAnalysis result = morphology.analyze("XXIV'ten");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.RomanNumeral,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testDate() {
    TurkishMorphology morphology = getMorphology("dört [P:Num,Card;A:Voicing]");
    WordAnalysis result = morphology.analyze("1.1.2014");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Date,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testTime() {
    TurkishMorphology morphology = getMorphology("otuz [P:Num,Card]");
    WordAnalysis result = morphology.analyze("20:30'da");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Clock,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testRatio() {
    TurkishMorphology morphology = getMorphology("iki [P:Num,Card]");
    WordAnalysis result = morphology.analyze("1/2");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Ratio,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testPercent() {
    TurkishMorphology morphology = getMorphology("iki [P:Num,Card]");
    String[] correct = {"%2", "%2'si", "%2.2'si", "%2,2'si"};
    for (String s : correct) {
      WordAnalysis result = morphology.analyze(s);
      Assert.assertEquals("Failed for " + s, 1, result.analysisCount());
      Assert.assertEquals("Failed for " + s,
          SecondaryPos.Percentage,
          result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
    }
  }


  @Test
  public void testEmoticon() {
    TurkishMorphology morphology = getEmptyTurkishMorphology();
    WordAnalysis result = morphology.analyze(":)");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Emoticon,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testWordsWithDash() {
    // Instance with no dictionary item.
    TurkishMorphology morphology = getEmptyTurkishMorphology();
    WordAnalysis result = morphology.analyze("Blah-Foo'ya");
    Assert.assertEquals(1, result.analysisCount());
  }

  @Test
  public void testAbbreviationVoicing_Issue_183() {
    // Instance with no dictionary item.
    TurkishMorphology morphology = getMorphology("Tübitak [P:Abbrv]");
    WordAnalysis result = morphology.analyze("Tübitak'a");
    Assert.assertEquals(1, result.analysisCount());
    result = morphology.analyze("Tübitaka");
    Assert.assertEquals(1, result.analysisCount());
    result = morphology.analyze("Tübitağa");
    Assert.assertEquals(0, result.analysisCount());
  }

  @Test
  public void testAsciiTolerantMorphology() {
    // Instance with no dictionary item.
    TurkishMorphology morphology = getAsciiTolerantMorphology(
        "sıra", "şıra", "armut", "kazan", "ekonomik [P:Adj]", "insan");
    RuleBasedAnalyzer analyzer = morphology.getAnalyzer();
    List<SingleAnalysis> result;
    result = analyzer.analyze("ekonomık");
    Assert.assertTrue(containsAllDictionaryLemma(result, "ekonomik"));
    result = analyzer.analyze("sira");
    Assert.assertEquals(2, result.size());
    Assert.assertTrue(containsAllDictionaryLemma(result, "sıra", "şıra"));
    result = analyzer.analyze("siraci");
    Assert.assertTrue(containsAllDictionaryLemma(result, "sıra", "şıra"));
    result = analyzer.analyze("armutcuga");
    Assert.assertTrue(containsAllDictionaryLemma(result, "armut"));
    result = analyzer.analyze("kazancıga");
    Assert.assertTrue(containsAllDictionaryLemma(result, "kazan"));
    result = analyzer.analyze("kazanciga");
    Assert.assertTrue(containsAllDictionaryLemma(result, "kazan"));
    result = analyzer.analyze("kazançiğimizdan");
    Assert.assertTrue(containsAllDictionaryLemma(result, "kazan"));
    result = analyzer.analyze("ınsanların");
    Assert.assertTrue(containsAllDictionaryLemma(result, "insan"));
  }

  private boolean containsAllDictionaryLemma(List<SingleAnalysis> analyses, String... item) {

    for (String i : item) {
      boolean fail = true;
      for (SingleAnalysis s : analyses) {
        if (s.getDictionaryItem().lemma.contains(i)) {
          fail = false;
          break;
        }
      }
      if (fail) {
        Log.info("Failed to find item %s", i);
        return false;
      }
    }
    return true;
  }

}
