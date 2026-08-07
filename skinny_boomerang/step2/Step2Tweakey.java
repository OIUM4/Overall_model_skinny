package boomerangsearch.step2;

import boomerangsearch.step1.Step1SolutionTweakey;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.List;
import java.util.ArrayList;

public class Step2Tweakey {
  private static final int[] invPermutationTKS = new int[]{2, 0, 4, 7, 6, 3, 5, 1};
  private final Tuples tableLFSRTK2;
  private final Tuples tableLFSRTK3;
  private final Model model;
  private final int nbRounds;
  private final int regime;
  private final int cellSize;
  private final IntVar[][][] dTK1;
  private final IntVar[][][] dTK2;
  private final IntVar[][][] dTK3;
  public  final IntVar[][][] dTK;
  private IntVar[][] dTK1BeforeFirst;
  private IntVar[][] dTK2BeforeFirst;
  private IntVar[][] dTK3BeforeFirst;
  private IntVar[][] dTKBeforeFirst;

  public Step2Tweakey(final Model model, final int nbRounds, final int regime, final int cellSize, final Step1SolutionTweakey step1TK) {
    if (regime == 0)
      throw new IllegalStateException("You should not define any tweakey in SK");
    this.model = model;
    this.nbRounds = nbRounds;
    this.regime = regime;
    this.cellSize = cellSize;
    tableLFSRTK2 = createRelationLFSRTK2();
    tableLFSRTK3 = createRelationLFSRTK3();
    dTK1 = new IntVar[nbRounds][2][4];
    dTK2 = new IntVar[nbRounds][2][4];
    dTK3 = new IntVar[nbRounds][2][4];
    dTK  = new IntVar[nbRounds][2][4];
    switch (regime) { // There is no break on purpose, because if TK3 then we have to initialize TK2 and TK1
    case 3: initializedTKLFSR(step1TK.lanes, dTK3, tableLFSRTK3, "3");
    case 2: initializedTKLFSR(step1TK.lanes, dTK2, tableLFSRTK2, "2");
    case 1: initializedTK1(step1TK.lanes);
    }
    switch (regime) {
    case 3: initializedTKfull123(step1TK.DTK);
      break;
    case 2: initializedTKfull12(step1TK.DTK);
      break;
    case 1: initializedTKfull1();
      break;
    }
  }



  /**
   * Return the concrete combined tweakey difference for the round immediately
   * before relative Ed round 0, i.e. the last Eb round. The value is derived
   * from the same key-schedule variables used for Ed instead of being a free
   * Step2 variable.
   */
  public IntVar[][] getBeforeFirstRoundTweakey(final int[][] DTKBeforeFirst) {
    if (dTKBeforeFirst == null)
      initializedBeforeFirstRound(DTKBeforeFirst);
    return dTKBeforeFirst;
  }

  private void initializedBeforeFirstRound(final int[][] DTKBeforeFirst) {
    if (nbRounds < 2)
      throw new IllegalStateException("At least two Ed rounds are needed to derive the previous tweakey round.");
    dTK1BeforeFirst = new IntVar[2][4];
    dTK2BeforeFirst = new IntVar[2][4];
    dTK3BeforeFirst = new IntVar[2][4];
    dTKBeforeFirst  = new IntVar[2][4];

    switch (regime) { // fall through intentionally, as in the forward key schedule
    case 3: initializedBeforeFirstTKLFSR(dTK3BeforeFirst, dTK3, tableLFSRTK3, "3");
    case 2: initializedBeforeFirstTKLFSR(dTK2BeforeFirst, dTK2, tableLFSRTK2, "2");
    case 1: initializedBeforeFirstTK1();
    }

    switch (regime) {
    case 3:
      initializedBeforeFirstTKfull123(DTKBeforeFirst);
      break;
    case 2:
      initializedBeforeFirstTKfull12(DTKBeforeFirst);
      break;
    case 1:
      initializedBeforeFirstTKfull1(DTKBeforeFirst);
      break;
    }
  }

  private void initializedBeforeFirstTKLFSR(final IntVar[][] dTKBefore,
                                            final IntVar[][][] dTK23,
                                            final Tuples tableLFSR,
                                            final String regimestr) {
    for (int cell = 0; cell < 8; cell++) {
      int nextCell = invPermutationTKS[cell];
      IntVar next = dTK23[1][nextCell/4][nextCell%4];
      if (next != null) {
        dTKBefore[cell/4][cell%4] = model.intVar("dTK" + regimestr + "-m1" + (cell/4) + (cell%4), 0, cellSize);
        model.table(new IntVar[]{dTKBefore[cell/4][cell%4], next}, tableLFSR).post();
      }
    }
  }

  private void initializedBeforeFirstTK1() {
    for (int cell = 0; cell < 8; cell++) {
      int nextCell = invPermutationTKS[cell];
      dTK1BeforeFirst[cell/4][cell%4] = dTK1[1][nextCell/4][nextCell%4];
    }
  }

  private void ensureBeforeFirstCell(final IntVar var, final int i, final int j) {
    if (var == null)
      throw new IllegalStateException("Step1 marks Eb-last TK cell [" + i + "][" + j + "] active, but the key schedule has no corresponding lane.");
  }

  private void initializedBeforeFirstTKfull123(final int[][] DTKBeforeFirst) {
    for (int i = 0; i < 2; i++)
      for (int j = 0; j < 4; j++)
        if (DTKBeforeFirst[i][j] == 1) {
          ensureBeforeFirstCell(dTK1BeforeFirst[i][j], i, j);
          ensureBeforeFirstCell(dTK2BeforeFirst[i][j], i, j);
          ensureBeforeFirstCell(dTK3BeforeFirst[i][j], i, j);
          dTKBeforeFirst[i][j] = model.intVar("dTKEbLast" + i + j, 0, cellSize);
          Step2Factory.postXOR(model, dTK1BeforeFirst[i][j], dTK2BeforeFirst[i][j], dTK3BeforeFirst[i][j], dTKBeforeFirst[i][j], cellSize);
          model.arithm(dTKBeforeFirst[i][j], "!=", 0).post();
        }
        else if (dTK1BeforeFirst[i][j] != null)
          Step2Factory.postXOR(model, dTK1BeforeFirst[i][j], dTK2BeforeFirst[i][j], dTK3BeforeFirst[i][j]);
  }

  private void initializedBeforeFirstTKfull12(final int[][] DTKBeforeFirst) {
    for (int i = 0; i < 2; i++)
      for (int j = 0; j < 4; j++)
        if (DTKBeforeFirst[i][j] == 1) {
          ensureBeforeFirstCell(dTK1BeforeFirst[i][j], i, j);
          ensureBeforeFirstCell(dTK2BeforeFirst[i][j], i, j);
          dTKBeforeFirst[i][j] = model.intVar("dTKEbLast" + i + j, 0, cellSize);
          Step2Factory.postXOR(model, dTK1BeforeFirst[i][j], dTK2BeforeFirst[i][j], dTKBeforeFirst[i][j]);
          model.arithm(dTKBeforeFirst[i][j], "!=", 0).post();
        }
        else if (dTK1BeforeFirst[i][j] != null)
          dTK1BeforeFirst[i][j].eq(dTK2BeforeFirst[i][j]).post();
  }

  private void initializedBeforeFirstTKfull1(final int[][] DTKBeforeFirst) {
    for (int i = 0; i < 2; i++)
      for (int j = 0; j < 4; j++) {
        dTKBeforeFirst[i][j] = dTK1BeforeFirst[i][j];
        if (DTKBeforeFirst[i][j] == 1) {
          ensureBeforeFirstCell(dTKBeforeFirst[i][j], i, j);
          model.arithm(dTKBeforeFirst[i][j], "!=", 0).post();
        }
      }
  }

  // -------- Initialize the keys 1,2 and 3 --------
  private void initializeFirstTwoRounds(final int[] lanes, final IntVar[][][] dTK, final String name) {
    for (int i = 0; i < 2; i++)
      for (int j = 0; j < 4; j++) {
        if (lanes[i*4+j]   == 1)
          dTK[0][i][j] = model.intVar(name+"-"+0+i+j, 0, cellSize);
        if (lanes[8+i*4+j] == 1)
          dTK[1][i][j] = model.intVar(name+"-"+1+i+j, 0, cellSize);
      }
  }

  private void initializedTKLFSR(final int[] lanes, IntVar[][][] dTK23, Tuples tableLFSR, String regimestr) {
    initializeFirstTwoRounds(lanes, dTK23, "dTK"+regimestr);
    for (int round = 2; round < nbRounds; round++)
      for (int cell = 0; cell < 8; cell++)
        if (dTK23[round-2][cell/4][cell%4] != null) {
          int nextCell = invPermutationTKS[cell];
          dTK23[round][nextCell/4][nextCell%4] = model.intVar("dTK"+regimestr+"-"+round+(nextCell/4)+(nextCell%4), 0, cellSize);
          model.table(new IntVar[]{dTK23[round-2][cell/4][cell%4], dTK23[round][nextCell/4][nextCell%4]}, tableLFSR).post();
      }
  }

  private void initializedTK1(final int[] lanes) {
    initializeFirstTwoRounds(lanes, dTK1, "dTK1");
    for (int round = 2; round < nbRounds; round++)
      for (int cell = 0; cell < 8; cell++) {
        int nextCell = invPermutationTKS[cell];
        dTK1[round][nextCell/4][nextCell%4] = dTK1[round-2][cell/4][cell%4];
      }
  }

  // -------- Initialize the xor of the keys --------
  private void initializedTKfull123(final int[][][] DTK) {
    for (int round = 0; round < nbRounds; round++)
      for (int i = 0; i < 2; i++)
        for (int j = 0; j < 4; j++)
          if (DTK[round][i][j] == 1) {
            dTK[round][i][j] = model.intVar("dTK"+round+i+j, 0, cellSize);
            Step2Factory.postXOR(model, dTK1[round][i][j], dTK2[round][i][j], dTK3[round][i][j], dTK[round][i][j], cellSize);
          }
          else
            if (dTK1[round][i][j] != null) // lane is not zero
              Step2Factory.postXOR(model, dTK1[round][i][j], dTK2[round][i][j], dTK3[round][i][j]);

  }

  private void initializedTKfull12(final int[][][] DTK) {
    for (int round = 0; round < nbRounds; round++)
      for (int i = 0; i < 2; i++)
        for (int j = 0; j < 4; j++)
          if (DTK[round][i][j] == 1) {
            dTK[round][i][j] = model.intVar("dTK"+round+i+j, 0, cellSize);
            Step2Factory.postXOR(model, dTK1[round][i][j], dTK2[round][i][j], dTK[round][i][j]);
          }
          else
            if (dTK1[round][i][j] != null) {// lane is not zero
              dTK1[round][i][j].eq(dTK2[round][i][j]).post();
            } 
   // if (dTK1[0][1][1] != null) // lane is not zero
    //	dTK1[0][1][1].eq(137).post();
  }

  private void initializedTKfull1() {
    for (int round = 0; round < nbRounds; round++)
      for (int i = 0; i < 2; i++)
        for (int j = 0; j < 4; j++)
          dTK[round][i][j] = dTK1[round][i][j];
  }

  public void fixSolution(Step2SolutionTweakey step2TK) {
    for (int i = 0; i < 2; i++)
      for (int j = 0; j < 4; j++)
        for (int round = 0; round < nbRounds-1; round++) {
          if (dTK1[round][i][j] != null)
            dTK1[round][i][j].eq(step2TK.dTK1[round][i][j]).post();
          if (dTK2[round][i][j] != null)
            dTK2[round][i][j].eq(step2TK.dTK2[round][i][j]).post();
          if (dTK3[round][i][j] != null)
            dTK3[round][i][j].eq(step2TK.dTK3[round][i][j]).post();
          if (dTK[round][i][j] != null)
            dTK[round][i][j].eq(step2TK.dTK[round][i][j]).post();
        }
  }

  public IntVar[] getVariables() {
    List<IntVar> keyVariables = new ArrayList<IntVar>();
    for (int round = 0; round < 2; round++)
      for (int i = 0; i < 2; i++)
        for (int j = 0; j < 4; j++)
          switch (regime) {
          case 3: if (dTK3[round][i][j] != null) keyVariables.add(dTK3[round][i][j]);
          case 2: if (dTK2[round][i][j] != null) keyVariables.add(dTK2[round][i][j]);
          case 1: if (dTK1[round][i][j] != null) keyVariables.add(dTK1[round][i][j]);
          }
    return ArrayUtils.toArray(keyVariables);
  }

  public Step2SolutionTweakey getValue() {
    int[][][] dTK1Value = new int[nbRounds][2][4];
    int[][][] dTK2Value = new int[nbRounds][2][4];
    int[][][] dTK3Value = new int[nbRounds][2][4];
    int[][][] dTKValue  = new int[nbRounds][2][4];
    for (int i = 0; i < 2; i++)
      for (int j = 0; j < 4; j++)
        for (int round = 0; round < nbRounds; round++) {
          if (dTK1[round][i][j] != null)
            dTK1Value[round][i][j] = dTK1[round][i][j].getValue();
          if (dTK2[round][i][j] != null)
            dTK2Value[round][i][j] = dTK2[round][i][j].getValue();
          if (dTK3[round][i][j] != null)
            dTK3Value[round][i][j] = dTK3[round][i][j].getValue();
          if (dTK[round][i][j] != null)
            dTKValue[round][i][j]  = dTK[round][i][j].getValue();
        }
    return new Step2SolutionTweakey(dTK1Value, dTK2Value, dTK3Value, dTKValue);
  }

  // -------- Creation of LFSR tables --------
  private Tuples createRelationLFSRTK2() {
    Tuples tuples = new Tuples(true);
    if (cellSize == 16)
      for (int i = 0; i < 16; i++)
        tuples.add(i, ((i << 1) % 16) + new int[]{0,1,1,0}[i/4]);
    else
      for (int i = 0; i < 256; i++)
        tuples.add(i, (((i << 1) % 256) + ((i >> 5) + (i >> 7)) % 2));
    return tuples;
  }
  
  private Tuples createRelationLFSRTK3() {
    Tuples tuples = new Tuples(true);
    if (cellSize == 16)
      for (int i = 0; i < 16; i++)
        tuples.add(i, ((i/8)^(i%2))*8 + i/2);
    else
      for (int i = 0; i < 256; i++)
        tuples.add(i, (((i/64)^i)%2)*128 + i/2);
    return tuples;
  }
}
