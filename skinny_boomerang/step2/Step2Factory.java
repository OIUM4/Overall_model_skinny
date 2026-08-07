package boomerangsearch.step2;

import boomerangsearch.step1.Step1Solution;

import sandwichproba.sboxtables.SboxTables;

import org.javatuples.Triplet;
import org.javatuples.Quartet;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.constraints.extension.Tuples;

public class Step2Factory {
  public static int nbXors = 0;
  private static final int[] invPermutationTKS = new int[]{2, 0, 4, 7, 6, 3, 5, 1};
  public  static final int probaFactor = 10;
  private final int[] probaExponents;
  private final Model model;
  private final int tkVersion;
  private final SboxTables sboxTables;
  private static final Tuples tableXor = createRelationXor();
  private final Tuples tableDDT;
  private final Tuples tableDDT2;
  private final Tuples tableBCT;
  private final Tuples tableUBCT;
  private final Tuples tableLBCT;
  private final Tuples tableFBCT;
  private final Tuples tableDDTfeasibility;


  public Step2Factory(final Model model, final int tkVersion, final SboxTables sboxTables) {
    this.model = model;
    this.tkVersion = tkVersion;
    this.sboxTables = sboxTables;
    probaExponents = new int[sboxTables.getSboxSize()+1];
    for (int i = 0; i < sboxTables.getSboxSize()+1; i++) {
      probaExponents[i] = (int) Math.round(-probaFactor*Math.log((double) i/sboxTables.getSboxSize())/Math.log(2.0));
    }
    tableDDT = createRelationDDT(1);
    tableDDT2 = createRelationDDT(2);
    tableBCT = createRelationBCT();
    tableUBCT = createRelationUBCT();
    tableLBCT = createRelationLBCT();
    tableFBCT = createRelationFBCT();
    tableDDTfeasibility = createRelationDDTfeasibility();
  }
  
  // -------- Initialization --------
  public IntVar initializeAndPostSboxes(IntVar[][][] dXupper, IntVar[][][] dSBupper, IntVar[][][] dXlower, IntVar[][][] dSBlower, IntVar[][][] proba, Step1Solution step1) {
    ArExpression objective = model.intVar(0);
    int lowerBoundObjective = 0;
    int upperBoundObjective = 0;
    for (int round = 0; round < step1.nbRounds; round++)
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          // Initialization of useful variables
          if (step1.DXupper[round][i][j] == 1 && step1.freeXupper[round][i][j] == 0)
            dXupper[round][i][j] = model.intVar("dXupper"+round+i+j, 1, sboxTables.getSboxSize()-1, false);
          if (step1.DXupper[round][i][j] == 1 && step1.freeSBupper[round][i][j] == 0)
            dSBupper[round][i][j] = model.intVar("dSBupper"+round+i+j, 1, sboxTables.getSboxSize()-1, false);
          if (step1.DXlower[round][i][j] == 1 && step1.freeXlower[round][i][j] == 0)
            dXlower[round][i][j] = model.intVar("dXlower"+round+i+j, 1, sboxTables.getSboxSize()-1, false);
          if (step1.DXlower[round][i][j] == 1 && step1.freeSBlower[round][i][j] == 0)
            dSBlower[round][i][j] = model.intVar("dSBlower"+round+i+j, 1, sboxTables.getSboxSize()-1, false);
          // Sbox constraints
          if (step1.DXupper[round][i][j] == 0) {
            if (step1.DXlower[round][i][j] == 1 && step1.freeXlower[round][i][j] == 0) { // DDT
              proba[round][i][j] = model.intVar("proba"+round+i+j, 2*probaFactor, 7*probaFactor, false);
              model.table(new IntVar[]{dXlower[round][i][j], dSBlower[round][i][j], proba[round][i][j]}, tableDDT).post();
              objective = objective.add(proba[round][i][j]);
              lowerBoundObjective = lowerBoundObjective + 2*probaFactor;
              upperBoundObjective = upperBoundObjective + 7*probaFactor;
            }
          }
          else if (step1.DXlower[round][i][j] == 0) {
            if (step1.DXupper[round][i][j] == 1 && step1.freeSBupper[round][i][j] == 0) { // DDT
              proba[round][i][j] = model.intVar("proba"+round+i+j, 2*probaFactor, 7*probaFactor, false);
              model.table(new IntVar[]{dXupper[round][i][j], dSBupper[round][i][j], proba[round][i][j]}, tableDDT).post();
              objective = objective.add(proba[round][i][j]);
              lowerBoundObjective = lowerBoundObjective + 2*probaFactor;
              upperBoundObjective = upperBoundObjective + 7*probaFactor;
            }
          }
          else {
            if (step1.freeSBupper[round][i][j] == 0 && step1.freeXlower[round][i][j] == 0) { // FBCT
              proba[round][i][j] = model.intVar("proba"+round+i+j, 2*probaFactor, 7*probaFactor, false);
              model.table(new IntVar[]{dXupper[round][i][j], dSBupper[round][i][j], dXlower[round][i][j], dSBlower[round][i][j], proba[round][i][j]}, tableFBCT).post();
              objective = objective.add(proba[round][i][j]);
              lowerBoundObjective = lowerBoundObjective + 2*probaFactor;
              upperBoundObjective = upperBoundObjective + 7*probaFactor;
            }
            else if (step1.freeSBupper[round][i][j] == 0 && step1.freeSBlower[round][i][j] == 0) { // UBCT
              proba[round][i][j] = model.intVar("proba"+round+i+j, 2*probaFactor, 7*probaFactor, false);
              model.table(new IntVar[]{dXupper[round][i][j], dSBupper[round][i][j], dSBlower[round][i][j], proba[round][i][j]}, tableUBCT).post();
              objective = objective.add(proba[round][i][j]);
              lowerBoundObjective = lowerBoundObjective + 2*probaFactor;
              upperBoundObjective = upperBoundObjective + 7*probaFactor;
            }
            else if (step1.freeXlower[round][i][j] == 0 && step1.freeXupper[round][i][j] == 0) { // LBCT
              proba[round][i][j] = model.intVar("proba"+round+i+j, 2*probaFactor, 7*probaFactor, false);
              model.table(new IntVar[]{dXupper[round][i][j], dXlower[round][i][j], dSBlower[round][i][j], proba[round][i][j]}, tableLBCT).post();
              objective = objective.add(proba[round][i][j]);
              lowerBoundObjective = lowerBoundObjective + 2*probaFactor;
              upperBoundObjective = upperBoundObjective + 7*probaFactor;
            }
            else if (step1.freeSBupper[round][i][j] == 0) { // DDT2 upper
              proba[round][i][j] = model.intVar("proba"+round+i+j, 4*probaFactor, 14*probaFactor, false);
              model.table(new IntVar[]{dXupper[round][i][j], dSBupper[round][i][j], proba[round][i][j]}, tableDDT2).post();
              objective = objective.add(proba[round][i][j]);
              lowerBoundObjective = lowerBoundObjective + 4*probaFactor;
              upperBoundObjective = upperBoundObjective + 14*probaFactor;
            }
            else if (step1.freeXlower[round][i][j] == 0) { // DDT2 lower
              proba[round][i][j] = model.intVar("proba"+round+i+j, 4*probaFactor, 14*probaFactor, false);
              model.table(new IntVar[]{dXlower[round][i][j], dSBlower[round][i][j], proba[round][i][j]}, tableDDT2).post();
              objective = objective.add(proba[round][i][j]);
              lowerBoundObjective = lowerBoundObjective + 4*probaFactor;
              upperBoundObjective = upperBoundObjective + 14*probaFactor;
            }
            else if (step1.freeXupper[round][i][j] == 0 && step1.freeSBlower[round][i][j] == 0) { // BCT
              proba[round][i][j] = model.intVar("proba"+round+i+j, 0, 7*probaFactor, false);
              model.table(new IntVar[]{dXupper[round][i][j], dSBlower[round][i][j], proba[round][i][j]}, tableBCT).post();
              objective = objective.add(proba[round][i][j]);
              upperBoundObjective = upperBoundObjective + 7*probaFactor;
            }
          }
        }
    IntVar objectiveVar = model.intVar("objectiveVar", lowerBoundObjective, upperBoundObjective);
    objective.eq(objectiveVar).post();
    return objectiveVar;
  }
  
  public void initializeY(IntVar[][][] dY, int[][][] freeSB) {
    for (int round = 0; round < dY.length; round++)
      for (int i = 0; i < 2; i++)
        for (int j = 0; j < 4; j++)
          if (freeSB[round][i][j] == 0)
            dY[round][i][j] = model.intVar("dY", 0, 255);
  }

  // -------- Linear part of the cipher --------
  public void addRoundTweakey(IntVar[][][] dSB, int[][][] DX, IntVar[][][] dTK12, int[][][] DTK12,IntVar[][][] dY,int[][][] freeSB) {
    for (int round = 0; round < dY.length; round++)
      for (int i = 0; i < 2; i++)
        for (int j = 0; j < 4; j++)
          if (freeSB[round][i][j] == 0)
            postXOR(dSB[round][i][j],DX[round][i][j]==0,dTK12[round][i][j],DTK12[round][i][j]==0,dY[round][i][j]);
  }

  public void shiftrowsMC(IntVar[][][] dX, IntVar[][][] dSB, int[][][] DX, IntVar[][][] dY, int[][][] freeX, int[][][] freeSB) {
    for (int round = 0; round < dY.length; round++)
      for (int j = 0; j < 4; j++) {
        if (freeX[round+1][0][j] == 0 && freeSB[round][3][(j+1)%4] == 0 && freeX[round+1][3][j] == 0)
          postXOR(dSB[round][3][(j+1)%4], DX[round][3][(j+1)%4]==0, dX[round+1][0][j], DX[round+1][0][j]==0, dX[round+1][3][j], DX[round+1][3][j]==0);
        if (freeX[round+1][1][j] == 0 && freeSB[round][0][j] == 0) {
          if (DX[round+1][1][j] == 0)
            dY[round][0][j].eq(0).post();
          else
            dY[round][0][j].eq(dX[round+1][1][j]).post();
        }
        if (freeX[round+1][2][j] == 0 && freeSB[round][2][(j+2)%4] == 0 && freeSB[round][1][(j+3)%4] == 0)
          postXOR(dX[round+1][2][j], DX[round+1][2][j]==0, dSB[round][2][(j+2)%4], DX[round][2][(j+2)%4]==0, dY[round][1][(j+3)%4]);
        if (freeX[round+1][3][j] == 0 && freeSB[round][0][j] == 0 && freeSB[round][2][(j+2)%4] == 0)
          postXOR(dSB[round][2][(j+2)%4], DX[round][2][(j+2)%4]==0, dX[round+1][3][j], DX[round+1][3][j]==0, dY[round][0][j]);
      }
  }

  public void shiftrowsMCSK(IntVar[][][] dX, IntVar[][][] dSB, int[][][] DX, int[][][] freeX, int[][][] freeSB) {
    for (int round = 0; round < dX.length-1; round++)
      for (int j = 0; j < 4; j++) {
        if (freeX[round+1][0][j] == 0 && freeSB[round][3][(j+1)%4] == 0 && freeX[round+1][3][j] == 0)
          postXOR(dSB[round][3][(j+1)%4], DX[round][3][(j+1)%4]==0, dX[round+1][0][j], DX[round+1][0][j]==0, dX[round+1][3][j], DX[round+1][3][j]==0);
        if (freeX[round+1][1][j] == 0 && freeSB[round][0][j] == 0) {
          if (DX[round+1][1][j] != 0)
            dSB[round][0][j].eq(dX[round+1][1][j]).post();
        }
        if (freeX[round+1][2][j] == 0 && freeSB[round][2][(j+2)%4] == 0 && freeSB[round][1][(j+3)%4] == 0)
          postXOR(dX[round+1][2][j], DX[round+1][2][j]==0, dSB[round][2][(j+2)%4], DX[round][2][(j+2)%4]==0, dSB[round][1][(j+3)%4], DX[round][1][(j+3)%4]==0);
        if (freeX[round+1][3][j] == 0 && freeSB[round][0][j] == 0 && freeSB[round][2][(j+2)%4] == 0)
          postXOR(dSB[round][2][(j+2)%4], DX[round][2][(j+2)%4]==0, dX[round+1][3][j], DX[round+1][3][j]==0, dSB[round][0][j], DX[round][0][j]==0);
      }
  }

  // -------- Many versions of XOR with and without the knowledge of a zero, general one, and general one with 4 parameters --------
  public static void postXOR(Model model, IntVar a, IntVar b, IntVar c) {
    model.table(new IntVar[]{a, b, c}, tableXor).post();
    nbXors++;
  }
  
  public static void postXOR(Model model, IntVar a, IntVar b, IntVar c, IntVar d, int maxVal) { // a+b+c+d = 0
    IntVar tempVar = model.intVar("temp4Xor", 0, maxVal); // Need to create a temp variable
    model.table(new IntVar[]{a, b, tempVar}, tableXor).post();
    nbXors++;
    model.table(new IntVar[]{tempVar, c, d}, tableXor).post();
    nbXors++;
  }
  
  public void postXOR(IntVar a, IntVar b, IntVar c) {
    model.table(new IntVar[]{a, b, c}, tableXor).post();
    nbXors++;
  }

  private void postXOR(IntVar a, boolean a_zero, IntVar b, boolean b_zero, IntVar c) {
    if (a_zero) {
      if (b_zero)
        c.eq(0).post();
      else
        b.eq(c).post();
    }
    else if (b_zero) {
      if (a_zero)
        c.eq(0).post();
      else
        a.eq(c).post();
    }
    else
      postXOR(a, b, c);
  }

  private void postEqual(IntVar a, boolean a_zero, IntVar b, boolean b_zero) {
    if (a_zero) {
      if (!b_zero)
        b.eq(0).post();
    }
    else if (b_zero) {
      a.eq(0).post();
    }
    else {
      a.eq(b).post();
    }
  }

  private void postXOR(IntVar a, boolean a_zero, IntVar b, boolean b_zero, IntVar c, boolean c_zero) {
    if (a_zero && b_zero && c_zero)
      return;
    if (a_zero && b_zero) {
      c.eq(0).post();
      return;
    }
    if (a_zero && c_zero) {
      b.eq(0).post();
      return;
    }
    if (b_zero && c_zero) {
      a.eq(0).post();
      return;
    }
    if (a_zero) {
      b.eq(c).post();
      return;
    }
    if (b_zero) {
      a.eq(c).post();
      return;
    }
    if (c_zero) {
      a.eq(b).post();
      return;
    }
    postXOR(a, b, c);
  }
  
  // ======== Eb-last round: variable creation + DDT feasibility ========

  /**
   * Creates concrete IntVar variables for the last Eb round (X input and SB output)
   * and posts DDT feasibility constraints (no probability contribution).
   */
  public void initializeEbLastRound(IntVar[][] dXEbLast, IntVar[][] dSBEbLast, int[][] DXuppExtLast) {
    for (int i = 0; i < 4; i++)
      for (int j = 0; j < 4; j++) {
        if (DXuppExtLast[i][j] == 1) {
          dXEbLast[i][j]  = model.intVar("dXEbLast"  + i + j, 1, sboxTables.getSboxSize()-1, false);
          dSBEbLast[i][j] = model.intVar("dSBEbLast" + i + j, 1, sboxTables.getSboxSize()-1, false);
          model.table(new IntVar[]{dXEbLast[i][j], dSBEbLast[i][j]}, tableDDTfeasibility).post();
        }
      }
  }

  /**
   * Creates post-tweakey intermediate variables dY = dSB XOR dTK for rows 0 and 1
   * of the last Eb round, and posts XOR constraints.
   */
  public IntVar[][] initializeAndPostTweakeyEbLast(IntVar[][] dSBEbLast,
                                                    int[][] DXuppExtLast,
                                                    IntVar[][] dTKEbLast,
                                                    int[][] DSTKuppExtLast) {
    IntVar[][] dYEbLast = new IntVar[2][4];
    for (int i = 0; i < 2; i++)
      for (int j = 0; j < 4; j++) {
        boolean sbActive = (DXuppExtLast[i][j] == 1);
        boolean tkActive = (DSTKuppExtLast[i][j] == 1);
        IntVar tk = (tkActive && dTKEbLast != null) ? dTKEbLast[i][j] : null;
        if (tkActive && tk == null)
          throw new IllegalStateException("Step1 marks Eb-last TK cell [" + i + "][" + j + "] active, but Step2 key expansion produced no concrete key variable.");
        if (!sbActive && !tkActive) {
          dYEbLast[i][j] = null;
        } else if (sbActive && !tkActive) {
          dYEbLast[i][j] = dSBEbLast[i][j];
        } else if (!sbActive && tkActive) {
          dYEbLast[i][j] = tk;
        } else {
          dYEbLast[i][j]  = model.intVar("dYEbLast"  + i + j, 0, sboxTables.getSboxSize()-1, false);
          postXOR(dSBEbLast[i][j], tk, dYEbLast[i][j]);
        }
      }
    return dYEbLast;
  }

  /**
   * Posts SR+MC connection constraints from the last Eb round to the first Ed round (SK regime).
   */
  public void connectEbLastToEdFirstSK(IntVar[][] dSBEbLast,
                                        IntVar[][][] dXupper,
                                        int[][][] DXupper,
                                        int[][][] freeXupper,
                                        int[][] DXuppExtLast) {
    for (int j = 0; j < 4; j++) {
      // X_{r+1}[0,j] = SB_r[3,j+1] xor X_{r+1}[3,j]
      if (freeXupper[0][0][j] == 0 && freeXupper[0][3][j] == 0)
        postXOR(dSBEbLast[3][(j+1)%4], DXuppExtLast[3][(j+1)%4] == 0,
                dXupper[0][0][j],      DXupper[0][0][j] == 0,
                dXupper[0][3][j],      DXupper[0][3][j] == 0);

      // X_{r+1}[1,j] = SB_r[0,j]
      if (freeXupper[0][1][j] == 0)
        postEqual(dSBEbLast[0][j], DXuppExtLast[0][j] == 0,
                  dXupper[0][1][j], DXupper[0][1][j] == 0);

      // X_{r+1}[2,j] = SB_r[2,j+2] xor SB_r[1,j+3]
      if (freeXupper[0][2][j] == 0)
        postXOR(dXupper[0][2][j],      DXupper[0][2][j] == 0,
                dSBEbLast[2][(j+2)%4], DXuppExtLast[2][(j+2)%4] == 0,
                dSBEbLast[1][(j+3)%4], DXuppExtLast[1][(j+3)%4] == 0);

      // X_{r+1}[3,j] = SB_r[2,j+2] xor SB_r[0,j]
      if (freeXupper[0][3][j] == 0)
        postXOR(dSBEbLast[2][(j+2)%4], DXuppExtLast[2][(j+2)%4] == 0,
                dXupper[0][3][j],      DXupper[0][3][j] == 0,
                dSBEbLast[0][j],       DXuppExtLast[0][j] == 0);
    }
  }

  /**
   * Posts SR+MC connection constraints from the last Eb round to the first Ed round (TK regime).
   */
  public void connectEbLastToEdFirstTK(IntVar[][] dSBEbLast,
                                        IntVar[][] dYEbLast,
                                        IntVar[][][] dXupper,
                                        int[][][] DXupper,
                                        int[][][] freeXupper,
                                        int[][] DXuppExtLast,
                                        int[][] DSTKuppExtLast) {
    for (int j = 0; j < 4; j++) {
      boolean dY0_j_zero = (DXuppExtLast[0][j] == 0 && DSTKuppExtLast[0][j] == 0);
      boolean dY1_j3_zero = (DXuppExtLast[1][(j+3)%4] == 0 && DSTKuppExtLast[1][(j+3)%4] == 0);
      IntVar dY0_j = dYEbLast[0][j];
      IntVar dY1_j3 = dYEbLast[1][(j+3)%4];

      // X_{r+1}[0,j] = SB_r[3,j+1] xor X_{r+1}[3,j]
      if (freeXupper[0][0][j] == 0 && freeXupper[0][3][j] == 0)
        postXOR(dSBEbLast[3][(j+1)%4], DXuppExtLast[3][(j+1)%4] == 0,
                dXupper[0][0][j],      DXupper[0][0][j] == 0,
                dXupper[0][3][j],      DXupper[0][3][j] == 0);

      // X_{r+1}[1,j] = (SB_r[0,j] xor TK_r[0,j])
      if (freeXupper[0][1][j] == 0)
        postEqual(dY0_j, dY0_j_zero,
                  dXupper[0][1][j], DXupper[0][1][j] == 0);

      // X_{r+1}[2,j] = SB_r[2,j+2] xor (SB_r[1,j+3] xor TK_r[1,j+3])
      if (freeXupper[0][2][j] == 0)
        postXOR(dXupper[0][2][j],      DXupper[0][2][j] == 0,
                dSBEbLast[2][(j+2)%4], DXuppExtLast[2][(j+2)%4] == 0,
                dY1_j3,                dY1_j3_zero);

      // X_{r+1}[3,j] = SB_r[2,j+2] xor (SB_r[0,j] xor TK_r[0,j])
      if (freeXupper[0][3][j] == 0)
        postXOR(dSBEbLast[2][(j+2)%4], DXuppExtLast[2][(j+2)%4] == 0,
                dXupper[0][3][j],      DXupper[0][3][j] == 0,
                dY0_j,                 dY0_j_zero);
    }
  }

  // -------- Creation of Tables --------
  private static Tuples createRelationXor() {
    Tuples tuplesXor = new Tuples(true);
    for (int i = 0; i < 256; i++) {
      for (int j = 0; j < 256; j++) {
        tuplesXor.add(i, j, i ^ j);
      }
    }
    return tuplesXor;
  }
  
  private Tuples createRelationDDT(int factor) {
    Tuples tuplesSB = new Tuples(true);
    for (int gamma = 1; gamma < sboxTables.getSboxSize(); gamma++)
      for (int delta = 1; delta < sboxTables.getSboxSize(); delta++)
        if (sboxTables.ddt(gamma, delta) != 0)
          tuplesSB.add(gamma, delta, factor*probaExponent(sboxTables.ddt(gamma, delta)));
    return tuplesSB;
  }
  
  private Tuples createRelationBCT() {
    Tuples tuplesBCT = new Tuples(true);
    for (int gamma = 1; gamma < sboxTables.getSboxSize(); gamma++)
      for (int delta = 1; delta < sboxTables.getSboxSize(); delta++)
        if (sboxTables.bct(gamma, delta) != 0)
          tuplesBCT.add(gamma, delta, probaExponent(sboxTables.bct(gamma, delta)));
    return tuplesBCT;
  }
  
  private Tuples createRelationUBCT() {
    Tuples tuplesUBCT = new Tuples(true);
    sboxTables.getUBCTEntrySet().stream()
      .filter(entry -> (entry.getKey().getValue0() != 0 &&
                        entry.getKey().getValue1() != 0 &&
                        entry.getKey().getValue2() != 0))
      .forEach(entry -> addKeyValueInTable(tuplesUBCT, entry.getKey(), entry.getValue()));
    return tuplesUBCT;
  }

  private Tuples createRelationLBCT() {
    Tuples tuplesLBCT = new Tuples(true);
    sboxTables.getLBCTEntrySet().stream()
      .filter(entry -> (entry.getKey().getValue0() != 0 &&
                        entry.getKey().getValue1() != 0 &&
                        entry.getKey().getValue2() != 0))
      .forEach(entry -> addKeyValueInTable(tuplesLBCT, entry.getKey(), entry.getValue()));
    return tuplesLBCT;
  }
  
  private Tuples createRelationFBCT() {
    Tuples tuplesFBCT = new Tuples(true);
    sboxTables.getFBCTEntrySet().stream()
      .filter(entry -> (entry.getKey().getValue0() != 0 &&
                        entry.getKey().getValue1() != 0 &&
                        entry.getKey().getValue2() != 0 &&
                        entry.getKey().getValue3() != 0))
      .forEach(entry -> addKeyValueInTable(tuplesFBCT, entry.getKey(), entry.getValue()));
    return tuplesFBCT;
  }

  private void addKeyValueInTable(Tuples table, Triplet<Integer,Integer,Integer> key, int value) {
    table.add(key.getValue0(), key.getValue1(), key.getValue2(), probaExponent(value));
  }

  private void addKeyValueInTable(Tuples table, Quartet<Integer,Integer,Integer,Integer> key, int value) {
    table.add(key.getValue0(), key.getValue1(), key.getValue2(), key.getValue3(), probaExponent(value));
  }

  private int probaExponent(final int tableValue) {
    if (tableValue == 0 || tableValue > 256)
      throw new IllegalStateException("The value for this probability is not implemented, but it should not appear in the tables");
    return probaExponents[tableValue];
  }

  /**
   * 2-column DDT feasibility table: all valid (Δin, Δout) pairs regardless of probability.
   * Used for Eb extension SBox constraints where we need existence but not probability.
   */
  private Tuples createRelationDDTfeasibility() {
    Tuples tuplesSB = new Tuples(true);
    for (int gamma = 1; gamma < sboxTables.getSboxSize(); gamma++)
      for (int delta = 1; delta < sboxTables.getSboxSize(); delta++)
        if (sboxTables.ddt(gamma, delta) != 0)
          tuplesSB.add(gamma, delta);
    return tuplesSB;
  }
}
