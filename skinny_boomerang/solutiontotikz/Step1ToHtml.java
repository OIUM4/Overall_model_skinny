package boomerangsearch.solutiontotikz;

import boomerangsearch.step1.Step1Solution;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Generates an interactive HTML visualization of Step1 solutions.
 * Usage: java -cp ".;newlibs/*" boomerangsearch.solutiontotikz.Step1ToHtml <input.json> [output.html] [solNumber]
 */
public class Step1ToHtml {

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.out.println("Usage: Step1ToHtml <step1.json> [output.html] [solNumber]");
      System.exit(1);
    }
    String inputFile = args[0];
    String outputFile = args.length > 1 ? args[1] : "output/step1_visual.html";
    int solNum = args.length > 2 ? Integer.parseInt(args[2]) : 0;

    List<Step1Solution> solutions = new ObjectMapper().readValue(new File(inputFile), new TypeReference<List<Step1Solution>>(){});
    if (solNum >= solutions.size()) {
      System.out.println("Error: only " + solutions.size() + " solutions, requested #" + solNum);
      System.exit(1);
    }
    Step1Solution sol = solutions.get(solNum);
    computeScalarBoundsIfMissing(sol);
    String html = generateHtml(sol, solNum);
    FileWriter fw = new FileWriter(outputFile);
    fw.write(html);
    fw.close();
    System.out.println("Generated: " + outputFile);
  }

  /**
   * Computes scalar bounds from array data if they were not stored in the JSON (old format).
   * Uses blocksize to determine weightmk (4 for blocksize=4, 8 for blocksize=8).
   * If blocksize is unknown (0), defaults to 8.
   */
  static void computeScalarBoundsIfMissing(Step1Solution sol) {
    // If already computed (new JSON format), skip
    if (sol.rb != 0 || sol.rf != 0 || sol.mb != 0 || sol.mf != 0) return;
    // If no array data available, skip
    if (sol.DXuppExt == null) return;

    int wmk = (sol.blocksize == 4) ? 4 : 8;

    // rb = wmk * sum(DXuppExt[1][i][j])
    if (sol.DXuppExt.length > 1) {
      int sum = 0;
      for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) sum += sol.DXuppExt[1][i][j];
      sol.rb = wmk * sum;
    }
    // rf = blocksize*16 - wmk * sum(DWFixedlowExt[nExtfRounds-1])
    if (sol.DWFixedlowExt != null) {
      int sum = 0;
      if (sol.nExtfRounds - 1 >= 0 && sol.nExtfRounds - 1 < sol.DWFixedlowExt.length) {
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) sum += sol.DWFixedlowExt[sol.nExtfRounds - 1][i][j];
      }
      int bs = (sol.blocksize == 4) ? 4 : 8;
      sol.rf = bs * 16 - wmk * sum;
    }
    // rb_prime = wmk * sum(DXisFilteruppExt + DYisFilteruppExt)
    if (sol.DXisFilteruppExt != null && sol.DYisFilteruppExt != null) {
      int sum = 0;
      int len = Math.min(sol.nExtbRounds, sol.DXisFilteruppExt.length);
      for (int r = 0; r < len; r++)
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++)
          sum += sol.DXisFilteruppExt[r][i][j] + sol.DYisFilteruppExt[r][i][j];
      sol.rb_prime = wmk * sum;
    }
    // rf_prime = wmk * sum(DXisFilterlowExt + DWisFilterlowExt)
    if (sol.DXisFilterlowExt != null && sol.DWisFilterlowExt != null) {
      int sum = 0;
      int len = Math.min(sol.nExtfRounds, sol.DXisFilterlowExt.length);
      for (int r = 0; r < len; r++)
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++)
          sum += sol.DXisFilterlowExt[r][i][j] + sol.DWisFilterlowExt[r][i][j];
      sol.rf_prime = wmk * sum;
    }
    // mb = wmk * sum(DKnownEncuppExt[r][i][j]), r in 0..nExtbRounds-2, i in 0..1
    if (sol.DKnownEncuppExt != null) {
      int sum = 0;
      int len = Math.min(sol.nExtbRounds - 1, sol.DKnownEncuppExt.length);
      for (int r = 0; r < len; r++)
        for (int i = 0; i < 2; i++) for (int j = 0; j < 4; j++)
          sum += sol.DKnownEncuppExt[r][i][j];
      sol.mb = wmk * sum;
    }
    // mb_prime = wmk * sum(DYGuessuppExt[r][i][j]), r in 0..nExtbRounds-2, i in 0..1
    if (sol.DYGuessuppExt != null) {
      int sum = 0;
      int len = Math.min(sol.nExtbRounds - 1, sol.DYGuessuppExt.length);
      for (int r = 0; r < len; r++)
        for (int i = 0; i < 2; i++) for (int j = 0; j < 4; j++)
          sum += sol.DYGuessuppExt[r][i][j];
      sol.mb_prime = wmk * sum;
    }
    // mf = wmk * sum(DKnownDeclowExt[r][i][j]), i in 0..1
    if (sol.DKnownDeclowExt != null) {
      int sum = 0;
      for (int r = 0; r < sol.DKnownDeclowExt.length; r++)
        for (int i = 0; i < 2; i++) for (int j = 0; j < 4; j++)
          sum += sol.DKnownDeclowExt[r][i][j];
      sol.mf = wmk * sum;
    }
    // mf_prime = wmk * sum(DXGuesslowExt[r][i][j]), i in 0..1
    if (sol.DXGuesslowExt != null) {
      int sum = 0;
      for (int r = 0; r < sol.DXGuesslowExt.length; r++)
        for (int i = 0; i < 2; i++) for (int j = 0; j < 4; j++)
          sum += sol.DXGuesslowExt[r][i][j];
      sol.mf_prime = wmk * sum;
    }
  }

  static String generateHtml(Step1Solution sol, int solNum) {
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Step1 Solution #").append(solNum).append("</title>\n");
    sb.append("<style>\n");
    sb.append("body{font-family:monospace;background:#1a1a2e;color:#e0e0e0;margin:20px}\n");
    sb.append("h1{color:#00d4ff} h2{color:#ffd700} h3{color:#aaa}\n");
    sb.append(".info{background:#16213e;padding:12px;border-radius:8px;margin:10px 0}\n");
    sb.append(".round{display:inline-block;margin:8px;vertical-align:top;background:#16213e;border-radius:8px;padding:8px}\n");
    sb.append(".round-title{text-align:center;font-weight:bold;padding:4px;margin-bottom:4px}\n");
    sb.append(".eb{border:2px solid #00ff88} .ef{border:2px solid #ff6b6b} .ed{border:2px solid #ffd700}\n");
    sb.append(".label{font-size:11px;color:#888;text-align:center}\n");
    sb.append("table{border-collapse:collapse;margin:2px auto}\n");
    sb.append("td{width:28px;height:28px;text-align:center;font-size:11px;border:1px solid #333}\n");
    sb.append(".c0{background:#2a2a3e} .c1{background:#ff4757;color:#fff} .c2{background:#2ed573;color:#000}\n");
    sb.append(".c3{background:#5352ed;color:#fff} .c4{background:#ffa502;color:#000}\n");
    sb.append(".cf{background:#57606f;color:#fff} .ca{background:#ff6348;color:#fff}\n");
    sb.append(".sep{width:12px}\n");
    sb.append(".arrow{text-align:center;font-size:20px;color:#ffd700;padding:4px}\n");
    sb.append(".tk-table td{width:22px;height:22px;font-size:10px}\n");
    sb.append(".tk1{background:#1e90ff;color:#fff} .tk0{background:#2a2a3e}\n");
    sb.append("legend{margin:10px 0} .leg{display:inline-block;width:20px;height:20px;margin:0 4px;vertical-align:middle;border-radius:3px}\n");
    sb.append("</style></head><body>\n");

    // Header
    sb.append("<h1>Step1 Solution #").append(solNum).append(" — Boomerang Visualization</h1>\n");
    sb.append("<div class='info'>");
    sb.append("<b>nbRounds</b>=").append(sol.nbRounds);
    sb.append(" | <b>nExtbRounds</b>=").append(sol.nExtbRounds);
    sb.append(" | <b>nExtfRounds</b>=").append(sol.nExtfRounds);
    sb.append(" | <b>regime</b>=").append(sol.regime);
    sb.append(" | <b>objective</b>=").append(sol.objective);
    sb.append("</div>\n");

    // Scalar bounds
    sb.append("<div class='info'>");
    sb.append("<h3 style='margin:0 0 6px 0;color:#ffd700'>Scalar Bounds</h3>");
    if (sol.blocksize != 0) {
      sb.append("<b>blocksize</b>=").append(sol.blocksize).append("<br>");
    }
    sb.append("<b>rb</b>=").append(sol.rb);
    sb.append(" | <b>rb'</b>=").append(sol.rb_prime);
    sb.append(" | <b>rf</b>=").append(sol.rf);
    sb.append(" | <b>rf'</b>=").append(sol.rf_prime);
    sb.append("<br>");
    sb.append("<b>mb</b>=").append(sol.mb);
    sb.append(" | <b>mb'</b>=").append(sol.mb_prime);
    sb.append(" | <b>mf</b>=").append(sol.mf);
    sb.append(" | <b>mf'</b>=").append(sol.mf_prime);
    sb.append("</div>\n");

    // Legend
    sb.append("<legend>");
    sb.append("<span class='leg ca'></span> Active (DX=1, fixed) ");
    sb.append("<span class='leg cf'></span> Free (DX=1, free) ");
    sb.append("<span class='leg c0'></span> Zero (DX=0) ");
    sb.append("<span class='leg c2'></span> Fixed=1 ");
    sb.append("<span class='leg c4'></span> Known=1 ");
    sb.append("<span class='leg' style='background:#1e90ff'></span> TK=1 ");
    sb.append("</legend>\n");

    int totalRounds = sol.nExtbRounds + sol.nbRounds + sol.nExtfRounds;

    // === Eb (Backward Extension) ===
    sb.append("<h2>Eb — Backward Extension (").append(sol.nExtbRounds).append(" rounds)</h2>\n");
    for (int r = 0; r < sol.nExtbRounds; r++) {
      sb.append("<div class='round eb'>\n");
      sb.append("<div class='round-title' style='color:#00ff88'>Eb-R").append(r).append("</div>\n");
      // DXuppExt
      sb.append("<div class='label'>DXuppExt</div>");
      appendGrid(sb, sol.DXuppExt[r], null);
      // DKnownEncuppExt
      if (sol.DKnownEncuppExt != null && r < sol.DKnownEncuppExt.length) {
        sb.append("<div class='label'>DKnownEnc</div>");
        appendKnownGrid(sb, sol.DKnownEncuppExt[r]);
      }
      sb.append("<div class='arrow'>→</div>\n");
      // Filter variables
      if (sol.DXFixeduppExt != null && r < sol.DXFixeduppExt.length) {
        sb.append("<div class='label'>DXFixed</div>");
        appendFixedGrid(sb, sol.DXFixeduppExt[r]);
        sb.append("<div class='label'>DYFixed</div>");
        appendFixedGrid(sb, sol.DYFixeduppExt[r]);
      }
      if (sol.DXFilteruppExt != null && r < sol.DXFilteruppExt.length) {
        sb.append("<div class='label'>DXFilter</div>");
        appendGrid(sb, sol.DXFilteruppExt[r], null);
        sb.append("<div class='label'>DYFilter</div>");
        appendGrid(sb, sol.DYFilteruppExt[r], null);
      }
      if (sol.DXisFilteruppExt != null && r < sol.DXisFilteruppExt.length) {
        sb.append("<div class='label'>DXisFilter</div>");
        appendGrid(sb, sol.DXisFilteruppExt[r], null);
        sb.append("<div class='label'>DYisFilter</div>");
        appendGrid(sb, sol.DYisFilteruppExt[r], null);
      }
      if (sol.DXGuessuppExt != null && r < sol.DXGuessuppExt.length) {
        sb.append("<div class='label'>DXGuess</div>");
        appendGrid(sb, sol.DXGuessuppExt[r], null);
        sb.append("<div class='label'>DYGuess</div>");
        appendGrid(sb, sol.DYGuessuppExt[r], null);
      }
      // TK
      if (sol.DSTKuppExt != null && r < sol.DSTKuppExt.length) {
        sb.append("<div class='label'>DSTKuppExt</div>");
        appendSmallGrid(sb, sol.DSTKuppExt[r]);
      }
      sb.append("</div>\n");
    }

    // Arrow
    sb.append("<div class='arrow' style='font-size:28px'>⟱</div>\n");

    // === E (Distinguisher) ===
    sb.append("<h2>E — Distinguisher (").append(sol.nbRounds).append(" rounds)</h2>\n");
    for (int r = 0; r < sol.nbRounds; r++) {
      sb.append("<div class='round ed'>\n");
      sb.append("<div class='round-title' style='color:#ffd700'>R").append(r + sol.nExtbRounds).append("</div>\n");
      // Upper path
      sb.append("<div class='label'>DXupper / freeXupper</div>");
      appendGrid(sb, sol.DXupper[r], sol.freeXupper[r]);
      sb.append("<div class='label'>DXupper(SB) / freeSBupper</div>");
      appendGrid(sb, sol.DXupper[r], sol.freeSBupper[r]);
      // Lower path
      sb.append("<div class='label'>DXlower / freeXlower</div>");
      appendGrid(sb, sol.DXlower[r], sol.freeXlower[r]);
      sb.append("<div class='label'>DXlower(SB) / freeSBlower</div>");
      appendGrid(sb, sol.DXlower[r], sol.freeSBlower[r]);
      // DDT2 / isTable
      sb.append("<div class='label'>isDDT2 / isTable</div>");
      appendDualGrid(sb, sol.isDDT2[r], sol.isTable[r]);
      // TK
      if (sol.DTKupper != null) {
        sb.append("<div class='label'>DTKupper</div>");
        appendSmallGrid(sb, sol.DTKupper.DTK[r]);
      }
      if (sol.DTKlower != null) {
        sb.append("<div class='label'>DTKlower</div>");
        appendSmallGrid(sb, sol.DTKlower.DTK[r]);
      }
      sb.append("</div>\n");
    }

    // Arrow
    sb.append("<div class='arrow' style='font-size:28px'>⟱</div>\n");

    // === Ef (Forward Extension) ===
    sb.append("<h2>Ef — Forward Extension (").append(sol.nExtfRounds).append(" rounds)</h2>\n");
    for (int r = 0; r < sol.nExtfRounds; r++) {
      sb.append("<div class='round ef'>\n");
      sb.append("<div class='round-title' style='color:#ff6b6b'>Ef-R").append(r).append("</div>\n");
      // DXlowExt
      sb.append("<div class='label'>DXlowExt</div>");
      appendGrid(sb, sol.DXlowExt[r], null);
      // DKnownDeclowExt
      if (sol.DKnownDeclowExt != null && r < sol.DKnownDeclowExt.length) {
        sb.append("<div class='label'>DKnownDec</div>");
        appendKnownGrid(sb, sol.DKnownDeclowExt[r]);
      }
      // Filter variables
      if (sol.DXFixedlowExt != null && r < sol.DXFixedlowExt.length) {
        sb.append("<div class='label'>DXFixed</div>");
        appendFixedGrid(sb, sol.DXFixedlowExt[r]);
        sb.append("<div class='label'>DWFixed</div>");
        appendFixedGrid(sb, sol.DWFixedlowExt[r]);
      }
      if (sol.DXFilterlowExt != null && r < sol.DXFilterlowExt.length) {
        sb.append("<div class='label'>DXFilter</div>");
        appendGrid(sb, sol.DXFilterlowExt[r], null);
        sb.append("<div class='label'>DWFilter</div>");
        appendGrid(sb, sol.DWFilterlowExt[r], null);
      }
      if (sol.DXisFilterlowExt != null && r < sol.DXisFilterlowExt.length) {
        sb.append("<div class='label'>DXisFilter</div>");
        appendGrid(sb, sol.DXisFilterlowExt[r], null);
        sb.append("<div class='label'>DWisFilter</div>");
        appendGrid(sb, sol.DWisFilterlowExt[r], null);
      }
      if (sol.DXGuesslowExt != null && r < sol.DXGuesslowExt.length) {
        sb.append("<div class='label'>DXGuess</div>");
        appendGrid(sb, sol.DXGuesslowExt[r], null);
        sb.append("<div class='label'>DWGuess</div>");
        appendGrid(sb, sol.DWGuesslowExt[r], null);
      }
      // TK
      if (sol.DSTKlowExt != null && r < sol.DSTKlowExt.length) {
        sb.append("<div class='label'>DSTKlowExt</div>");
        appendSmallGrid(sb, sol.DSTKlowExt[r]);
      }
      sb.append("</div>\n");
    }

    // Stats
    sb.append("<h2>Statistics</h2><div class='info'>");
    int activeUpper = 0, freeUpper = 0, activeLower = 0, freeLower = 0;
    for (int r = 0; r < sol.nbRounds; r++)
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          if (sol.DXupper[r][i][j] == 1) { if (sol.freeXupper[r][i][j] == 0) activeUpper++; else freeUpper++; }
          if (sol.DXlower[r][i][j] == 1) { if (sol.freeXlower[r][i][j] == 0) activeLower++; else freeLower++; }
        }
    sb.append("<b>Upper</b>: active=").append(activeUpper).append(", free=").append(freeUpper);
    sb.append(" | <b>Lower</b>: active=").append(activeLower).append(", free=").append(freeLower);
    int ebActive = 0;
    for (int r = 0; r < sol.nExtbRounds; r++)
      for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) if (sol.DXuppExt[r][i][j] != 0) ebActive++;
    int efActive = 0;
    for (int r = 0; r < sol.nExtfRounds; r++)
      for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) if (sol.DXlowExt[r][i][j] != 0) efActive++;
    sb.append("<br><b>Eb active</b>=").append(ebActive);
    sb.append(" | <b>Ef active</b>=").append(efActive);
    sb.append("</div>\n");

    sb.append("</body></html>");
    return sb.toString();
  }

  static void appendGrid(StringBuilder sb, int[][] data, int[][] freeMask) {
    sb.append("<table>");
    for (int i = 0; i < 4; i++) {
      sb.append("<tr>");
      for (int j = 0; j < 4; j++) {
        String cls;
        if (data[i][j] == 0) cls = "c0";
        else if (freeMask != null && freeMask[i][j] == 1) cls = "cf";
        else cls = "ca";
        sb.append("<td class='").append(cls).append("'>").append(data[i][j]).append("</td>");
      }
      sb.append("</tr>");
    }
    sb.append("</table>\n");
  }

  static void appendDualGrid(StringBuilder sb, int[][] d1, int[][] d2) {
    sb.append("<table>");
    for (int i = 0; i < 4; i++) {
      sb.append("<tr>");
      for (int j = 0; j < 4; j++) {
        String cls = "c0";
        if (d1[i][j] == 1 && d2[i][j] == 1) cls = "c3";
        else if (d2[i][j] == 1) cls = "c4";
        else if (d1[i][j] == 1) cls = "ca";
        sb.append("<td class='").append(cls).append("'>");
        sb.append(d1[i][j]).append("/").append(d2[i][j]);
        sb.append("</td>");
      }
      sb.append("</tr>");
    }
    sb.append("</table>\n");
  }

  static void appendFixedGrid(StringBuilder sb, int[][] data) {
    sb.append("<table>");
    for (int i = 0; i < 4; i++) {
      sb.append("<tr>");
      for (int j = 0; j < 4; j++) {
        String cls = data[i][j] != 0 ? "c2" : "c0";
        sb.append("<td class='").append(cls).append("'>").append(data[i][j]).append("</td>");
      }
      sb.append("</tr>");
    }
    sb.append("</table>\n");
  }

  static void appendKnownGrid(StringBuilder sb, int[][] data) {
    sb.append("<table>");
    for (int i = 0; i < 4; i++) {
      sb.append("<tr>");
      for (int j = 0; j < 4; j++) {
        String cls = data[i][j] != 0 ? "c4" : "c0";
        sb.append("<td class='").append(cls).append("'>").append(data[i][j]).append("</td>");
      }
      sb.append("</tr>");
    }
    sb.append("</table>\n");
  }

  static void appendSmallGrid(StringBuilder sb, int[][] data) {
    sb.append("<table class='tk-table'>");
    for (int i = 0; i < data.length; i++) {
      sb.append("<tr>");
      for (int j = 0; j < data[i].length; j++) {
        sb.append("<td class='").append(data[i][j] != 0 ? "tk1" : "tk0").append("'>").append(data[i][j]).append("</td>");
      }
      sb.append("</tr>");
    }
    sb.append("</table>\n");
  }
}
