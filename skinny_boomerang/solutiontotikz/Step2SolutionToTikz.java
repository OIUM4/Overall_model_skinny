package boomerangsearch.solutiontotikz;

import boomerangsearch.step2.Step2Solution;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Step2SolutionToTikz {
  private final Step2Solution step2Solution;

  /** Pick a Step2 solution by flat index in the JSON array. */
  public Step2SolutionToTikz(final String filename, final int solutionNumber, final boolean best) {
    this(selectByFlatIndex(Step2Solution.fromFile(filename), solutionNumber, best));
  }

  /**
   * Pick a Step2 solution for overlay on a specific Step1 solution.
   * If bestForStep1 is true, choose the highest probaExponent among Step2 results
   * tagged with step1SolutionIndex; legacy files without tags fall back to flat index.
   */
  public Step2SolutionToTikz(final String filename, final int step1SolutionNumber, final int step2SolutionNumber, final boolean bestForStep1) {
    this(selectForStep1(Step2Solution.fromFile(filename), step1SolutionNumber, step2SolutionNumber, bestForStep1));
  }

  public Step2SolutionToTikz(final Step2Solution step2Solution) {
    this.step2Solution = step2Solution;
  }

  private static Step2Solution selectByFlatIndex(final List<Step2Solution> allSolutions, final int solutionNumber, final boolean best) {
    if (allSolutions.isEmpty())
      throw new IllegalArgumentException("No Step2 solutions in input file.");
    if (!best)
      return allSolutions.get(solutionNumber);
    return allSolutions.stream()
      .max(Comparator.comparingDouble(s -> s.probaExponent))
      .orElse(allSolutions.get(0));
  }

  private static Step2Solution selectForStep1(final List<Step2Solution> allSolutions, final int step1SolutionNumber, final int step2SolutionNumber, final boolean bestForStep1) {
    if (allSolutions.isEmpty())
      throw new IllegalArgumentException("No Step2 solutions in input file.");

    boolean hasStep1Tags = allSolutions.stream().anyMatch(s -> s.step1SolutionIndex >= 0);
    List<Step2Solution> candidates = hasStep1Tags
      ? allSolutions.stream().filter(s -> s.step1SolutionIndex == step1SolutionNumber).collect(Collectors.toList())
      : allSolutions;

    if (candidates.isEmpty())
      throw new IllegalArgumentException(
        "No Step2 solution tagged for Step1 index " + step1SolutionNumber
        + ". Re-run Step2 (after recompiling) so step1SolutionIndex is saved in the JSON.");

    if (bestForStep1)
      return candidates.stream()
        .max(Comparator.comparingDouble(s -> s.probaExponent))
        .orElse(candidates.get(0));

    if (step2SolutionNumber < candidates.size())
      return candidates.get(step2SolutionNumber);

    return selectByFlatIndex(allSolutions, step2SolutionNumber, false);
  }

  public String generate() {
    System.err.println("Step2 overlay: probaExponent=" + step2Solution.probaExponent
      + ", probaClusters=" + step2Solution.probaClusters
      + ", step1SolutionIndex=" + step2Solution.step1SolutionIndex
      + ", step1Objective=" + step2Solution.step1Objective);
    String output = "";

    // -------- Eb-last round SB output (the concrete green region) --------
    if (step2Solution.nExtbRounds > 0 && step2Solution.dSBupper_EbLast != null) {
      int lastEbRoundAbs = step2Solution.nExtbRounds - 1;
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          if (step2Solution.dSBupper_EbLast[i][j] != 0)
            output += "\\node[align=center] at (" + (10 * lastEbRoundAbs + j + 5.5) + "," + (11.5 - i) + ") {\\Large $" + step2Solution.dSBupper_EbLast[i][j] + "$};\n";
        }
      // -------- Eb-last round TK values (ART box, same anchor as Ed TK cells) --------
      if (step2Solution.dTKupper_EbLast != null)
        for (int i = 0; i < 2; i++)
          for (int j = 0; j < 4; j++)
            if (step2Solution.dTKupper_EbLast[i][j] != 0)
              output += "\\node[align=center] at (" + (7.5 + 10 * lastEbRoundAbs + j + 0.5) + "," + (13.5 - i + 0.5) + ") {\\Large $" + step2Solution.dTKupper_EbLast[i][j] + "$};\n";
      output += "\n";
    }

    // dX and dSB — coordinates aligned with Step1SolutionToTikz cell centers
    for (int round = step2Solution.nExtbRounds; round < step2Solution.nbRounds + step2Solution.nExtbRounds; round++) {
      int r = round - step2Solution.nExtbRounds;
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          if (step2Solution.dXupper[r][i][j] != 0)
            output += "\\node[align=center] at (" + (10 * round + j + 0.5) + "," + (11.5 - i) + ") {\\Large $" + step2Solution.dXupper[r][i][j] + "$};\n";
          if (step2Solution.dSBupper[r][i][j] != 0)
            output += "\\node[align=center] at (" + (10 * round + j + 5.5) + "," + (11.5 - i) + ") {\\Large $" + step2Solution.dSBupper[r][i][j] + "$};\n";
          if (step2Solution.dXlower[r][i][j] != 0)
            output += "\\node[align=center] at (" + (10 * round + j + 0.5) + "," + (6.5 - i) + ") {\\Large $" + step2Solution.dXlower[r][i][j] + "$};\n";
          if (step2Solution.dSBlower[r][i][j] != 0)
            output += "\\node[align=center] at (" + (10 * round + j + 5.5) + "," + (6.5 - i) + ") {\\Large $" + step2Solution.dSBlower[r][i][j] + "$};\n";
        }
      output += "\n";
    }
    // dTK — same anchor as Step1SolutionToTikz: (7.5+10*round+j, 13.5-i) cell → center (+0.5)
    if (step2Solution.dTKupper != null)
      for (int round = step2Solution.nExtbRounds; round < step2Solution.nbRounds + step2Solution.nExtbRounds; round++) {
        int r = round - step2Solution.nExtbRounds;
        for (int i = 0; i < 2; i++)
          for (int j = 0; j < 4; j++) {
            if (step2Solution.dTKupper.dTK[r][i][j] != 0)
              output += "\\node[align=center] at (" + (7.5 + 10 * round + j + 0.5) + "," + (13.5 - i + 0.5) + ") {\\Large $" + step2Solution.dTKupper.dTK[r][i][j] + "$};\n";
            if (step2Solution.dTKlower.dTK[r][i][j] != 0)
              output += "\\node[align=center] at (" + (7.5 + 10 * round + j + 0.5) + "," + (1.5 - i + 0.5) + ") {\\Large $" + step2Solution.dTKlower.dTK[r][i][j] + "$};\n";
          }
      }

    return output;
  }
}
