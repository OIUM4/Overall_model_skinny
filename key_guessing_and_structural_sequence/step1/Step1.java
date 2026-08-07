package boomerangsearch.step1;

import gurobi.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.File;

public class Step1 {
  private final int nbRounds;
  private final int nExtbRounds;
  private final int nExtfRounds;
  private final int regime;
  private final int blocksize;
  private final GRBModel model;
  private final Step1Factory factory;
  private final GRBVar[][][] DXupper;
  private final GRBVar[][][] freeXupper;
  private final GRBVar[][][] freeSBupper;
  private final Step1Tweakey DTKupper;
  private final GRBVar[][][] DXlower;
  private final GRBVar[][][] freeXlower;
  private final GRBVar[][][] freeSBlower;
  private final Step1Tweakey DTKlower;
  private final GRBVar[][][] isDDT2;
  private final GRBVar[][][] isTable;
  private final GRBLinExpr   objective;
  
  /*Ef*/
  private static final int[] invPermutationTKS = new int[]{2, 0, 4, 7, 6, 3, 5, 1};
  private final GRBVar[][][] DXlowExt;
  private final GRBVar[][][] DSTKlowExt;
  private final GRBVar[][][] DKnownDeclowExt;

  /*Eb*/
  private final GRBVar[][][] DXuppExt;
  private final GRBVar[][][] DSTKuppExt;
  private final GRBVar[][][] DKnownEncuppExt;

  /*fastfilter*/
  private final GRBVar[][][] DXFixedlowExt;
  private final GRBVar[][][] DWFixedlowExt;
  private final GRBVar[][][] DXFilterlowExt;
  private final GRBVar[][][] DWFilterlowExt;
  private final GRBVar[][][] DXisFilterlowExt;
  private final GRBVar[][][] DWisFilterlowExt;
  private final GRBVar[][][] DXGuesslowExt;
  private final GRBVar[][][] DWGuesslowExt;
  
  /*fastfilter*/
  private final GRBVar[][][] DXFixeduppExt;
  private final GRBVar[][][] DYFixeduppExt;
  private final GRBVar[][][] DXFilteruppExt;
  private final GRBVar[][][] DYFilteruppExt;
  private final GRBVar[][][] DXisFilteruppExt;
  private final GRBVar[][][] DYisFilteruppExt;
  private final GRBVar[][][] DXGuessuppExt;
  private final GRBVar[][][] DYGuessuppExt;
  
  private final GRBVar[] obj;
  private final GRBVar[] advantage;
  private final GRBVar[] xmemory;

  // Unified upper/lower EPS schedule.
  private static final int EPS_UPPER = 0;
  private static final int EPS_LOWER = 1;
  private static final int EPS_MAX_KEYS_PER_FILTER_STEP = 3;

  // Values stored for each pool solution to make EPS validation easy.
  private GRBVar objective1Var;
  private GRBVar objective2Var;
  private GRBVar objective3Var;
  private GRBVar EPSVar;
  private GRBVar epsilonpVar;
  private GRBVar epsiloncVar;
  private GRBVar weightedCumulativeScoreVar;
  private MixedEpsSchedule mixedEpsSchedule;

  /**
   * @param env the Gurobi environment
   * @param nbRounds the number of rounds of the boomerang
   * @param regime the regime of the analysis. 0, 1, 2 or 3 for SK, TK1, TK2 or TK3
   */
  public Step1(final GRBEnv env, final int nbRounds, final int nExtbRounds, final int nExtfRounds, final int regime, final int blocksize, final double pr) throws GRBException {
    model = new GRBModel(env);
    this.nbRounds = nbRounds;
    this.nExtbRounds = nExtbRounds;
    this.nExtfRounds = nExtfRounds;
    this.regime = regime;
    this.blocksize = blocksize;

    factory = new Step1Factory(model, regime);
    DXupper = new GRBVar[nbRounds+1][4][4];
    freeXupper = new GRBVar[nbRounds][4][4];
    freeSBupper = new GRBVar[nbRounds][4][4];
    DTKupper = (regime == 0) ? null : new Step1Tweakey(model, nbRounds+1, regime, "upper_");
	
    DXlower = new GRBVar[nbRounds+1][4][4];
    freeXlower = new GRBVar[nbRounds][4][4];
    freeSBlower = new GRBVar[nbRounds][4][4];
    DTKlower = (regime == 0) ? null : new Step1Tweakey(model, nbRounds+1, regime, "lower_");
	
    isDDT2 = new GRBVar[nbRounds][4][4];
    isTable = new GRBVar[nbRounds][4][4];
    
    double weightmk = 0;
    double n = 0;
    double D = 0;
    if (blocksize == 4) {
        weightmk = 4.0;
        n = 64;
        D = 0.5 * (pr + n);
    }
    else if (blocksize == 8) {
        weightmk = 8.0;
        n = 128;
        D = 0.5 * (pr + n);
    }
    else {
        throw new IllegalArgumentException("blocksize must be 4 or 8");
    }
    if (pr <= 0) {
        throw new IllegalArgumentException("distinguisher probability exponent pr must be positive");
    }

    /*Ef*/
    DXlowExt = new GRBVar[nExtfRounds][4][4];
    DSTKlowExt = new GRBVar[nExtfRounds][2][4];
    DKnownDeclowExt = new GRBVar[nExtfRounds][4][4];
    /*Eb*/
    DXuppExt = new GRBVar[nExtbRounds+1][4][4];
    DSTKuppExt = new GRBVar[nExtbRounds][2][4];
    DKnownEncuppExt = new GRBVar[nExtbRounds][4][4];
    /*ADD fastfilter*/
    DXFixedlowExt = new GRBVar[nExtfRounds][4][4];
    DWFixedlowExt = new GRBVar[nExtfRounds][4][4];
    DXFilterlowExt = new GRBVar[nExtfRounds][4][4];
    DWFilterlowExt = new GRBVar[nExtfRounds][4][4];
    DXisFilterlowExt = new GRBVar[nExtfRounds][4][4];
    DWisFilterlowExt = new GRBVar[nExtfRounds][4][4];
    DXGuesslowExt = new GRBVar[nExtfRounds][4][4];
    DWGuesslowExt = new GRBVar[nExtfRounds][4][4];
	
	DXFixeduppExt = new GRBVar[nExtbRounds][4][4];
    DYFixeduppExt = new GRBVar[nExtbRounds][4][4];
    DXFilteruppExt = new GRBVar[nExtbRounds][4][4];
    DYFilteruppExt = new GRBVar[nExtbRounds][4][4];
    DXisFilteruppExt = new GRBVar[nExtbRounds][4][4];
    DYisFilteruppExt = new GRBVar[nExtbRounds][4][4];
    DXGuessuppExt = new GRBVar[nExtbRounds][4][4];
    DYGuessuppExt = new GRBVar[nExtbRounds][4][4];

    obj = new GRBVar[1];
    advantage = new GRBVar[1];
    xmemory = new GRBVar[1];

    // Initialization
    for (int round = 0; round < nbRounds; round++)
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          DXupper[round][i][j]     = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXupper"+round+i+j);
          freeXupper[round][i][j]  = model.addVar(0.0, 1.0, (round<=nbRounds/2)? 0.0 : 1.0, GRB.BINARY, "freeXupper"+round+i+j);
          freeSBupper[round][i][j] = model.addVar(0.0, 1.0, (round<=nbRounds/2)? 0.0 : 1.0, GRB.BINARY, "freeSBupper"+round+i+j);
		  
          DXlower[round][i][j]     = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXlower"+round+i+j);
          freeXlower[round][i][j]  = model.addVar(0.0, 1.0, (round>=nbRounds/2)? 0.0 : 1.0, GRB.BINARY, "freeXlower"+round+i+j);
          freeSBlower[round][i][j] = model.addVar(0.0, 1.0, (round>=nbRounds/2)? 0.0 : 1.0, GRB.BINARY, "freeSBlower"+round+i+j);
		  
          isDDT2[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "isDDT2"+round+i+j);
          isTable[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "isTable"+round+i+j);          
          }
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          DXupper[nbRounds][i][j]     = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXupper"+nbRounds+i+j);
          DXlower[nbRounds][i][j]     = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXlower"+nbRounds+i+j);        
          }

    obj[0] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "obj0");
    advantage[0] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "advantage0");
    xmemory[0] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "xmemory0");

    /*Ef*/
    for (int round = 0; round < nExtfRounds; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
        	  DXlowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXlowExt"+round+i+j);
                  DKnownDeclowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DKnownDeclowExt"+round+i+j);
                  /*fastfilter*/
                  DXFixedlowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXFixedlowExt"+round+i+j);
                  DWFixedlowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DWFixedlowExt"+round+i+j);
                  DXFilterlowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXFilterlowExt"+round+i+j);
                  DWFilterlowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DWFilterlowExt"+round+i+j);
                  DXisFilterlowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXisFilterlowExt"+round+i+j);
                  DWisFilterlowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DWisFilterlowExt"+round+i+j);
                  DXGuesslowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXGuesslowExt"+round+i+j);
                  DWGuesslowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DWGuesslowExt"+round+i+j);
        	  if (i<2)
        		  DSTKlowExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DSTKlowExt"+round+i+j);
          }
    
    /*Eb*/
    for (int round = 0; round < nExtbRounds+1; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
        	  DXuppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXuppExt"+round+i+j);
        	  if (round<nExtbRounds)
		  {
		        DKnownEncuppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DKnownEncuppExt"+round+i+j);
				
				/*fastfilter*/
                  DXFixeduppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXFixeduppExt"+round+i+j);
                  DYFixeduppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DYFixeduppExt"+round+i+j);
                  DXFilteruppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXFilteruppExt"+round+i+j);
                  DYFilteruppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DYFilteruppExt"+round+i+j);
                  DXisFilteruppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXisFilteruppExt"+round+i+j);
                  DYisFilteruppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DYisFilteruppExt"+round+i+j);
                  DXGuessuppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DXGuessuppExt"+round+i+j);
                  DYGuessuppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DYGuessuppExt"+round+i+j);
				
		      if(i<2)
        		  DSTKuppExt[round][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "DSTKuppExt"+round+i+j);
		  }
          }

    // Constraints
    if (regime == 0) {
      System.out.println("TODO, can replace the equality a=f in MC by a reference");
      factory.addLinearSK(DXupper);
      factory.addLinearSK(DXlower);
      factory.removeSymmetriesSK(DXupper[0]);
      model.addConstr(factory.sumState(DXupper[0]), GRB.GREATER_EQUAL, 1, "");
      model.addConstr(factory.sumState(DXlower[nbRounds-1]), GRB.GREATER_EQUAL, 1, "");
    }
    else {
      factory.addLinear(DXupper, DTKupper.DTK);
      factory.addLinear(DXlower, DTKlower.DTK);
    }

    factory.freePropagationUpper(freeXupper, freeSBupper, DXupper);
    factory.freePropagationLower(freeXlower, freeSBlower, DXlower);

    factory.objectiveConstraints(DXupper, freeXupper, freeSBupper, DXlower, freeXlower, freeSBlower, isTable, isDDT2);

    factory.addKnownDiffBounds(DXupper);
    factory.addKnownDiffBounds(DXlower);
    
    /*Ef*/
    factory.addlowExtLinear(DXlowExt,DSTKlowExt);
    factory.addlowExtDec(DXlowExt,DKnownDeclowExt);

    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
        	model.addConstr(DXlowExt[0][i][j], GRB.EQUAL, DXlower[nbRounds][i][j],"");
        }
    for (int i = 0; i < 8; i++) {
        int currentPos = i;
        for (int round = 0; round < nbRounds+nExtfRounds; round = round+2) {
          if (round >= nbRounds)
        	  model.addConstr(DSTKlowExt[round-nbRounds][currentPos/4][currentPos%4], GRB.EQUAL, DTKlower.lanes[i], "");
          currentPos = invPermutationTKS[currentPos];
        }
    }
    for (int i = 0; i < 8; i++) {
        int currentPos = i;
        for (int round = 1; round < nbRounds+nExtfRounds; round = round+2) {
        	if (round >= nbRounds)
        		model.addConstr(DSTKlowExt[round-nbRounds][currentPos/4][currentPos%4], GRB.EQUAL, DTKlower.lanes[i+8], "");
            currentPos = invPermutationTKS[currentPos];
        }
      }
     
	/*fastfilter*/
    factory.addDFixedlowExtLinear(DXFixedlowExt, DWFixedlowExt, DXlowExt);
    factory.addWfilterlowExt(DWFixedlowExt,DWFilterlowExt);
    factory.addXfilterlowExt(DXFixedlowExt,DXlowExt,DXFilterlowExt);
    factory.addguesslowExt(DXGuesslowExt,DWGuesslowExt,DXisFilterlowExt,DWisFilterlowExt);
    for (int round = 0; round < nExtfRounds; round++)
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
              model.addConstr(DXisFilterlowExt[round][i][j], GRB.LESS_EQUAL, DXFilterlowExt[round][i][j],"");
              model.addConstr(DWisFilterlowExt[round][i][j], GRB.LESS_EQUAL, DWFilterlowExt[round][i][j],"");
        }
	 

    /*Eb*/
    factory.adduppExtLinear(DXuppExt,DSTKuppExt);
	factory.adduppExtEnc(DXuppExt,DKnownEncuppExt);
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
        	model.addConstr(DXuppExt[nExtbRounds][i][j], GRB.EQUAL, DXupper[0][i][j],"");
        }
    for (int i = 0; i < 8; i++) {
        int currentPos = i;
        for (int round = nExtbRounds-1; round > -1; round = round-2) {
          currentPos = invPermutationTKS[currentPos];
          model.addConstr(DSTKuppExt[round][i/4][i%4], GRB.EQUAL, DTKupper.lanes[currentPos+8], "");        
        }
    }
    for (int i = 0; i < 8; i++) {
        int currentPos = i;
        for (int round = nExtbRounds-2; round > -1; round = round-2) {
          currentPos = invPermutationTKS[currentPos];
          model.addConstr(DSTKuppExt[round][i/4][i%4], GRB.EQUAL, DTKupper.lanes[currentPos], "");        
        }
    }
	
	/*fastfilter*/
    factory.addDFixeduppExtLinear(DXFixeduppExt, DYFixeduppExt, DXuppExt);
    factory.addYfilteruppExt(DYFixeduppExt, DXuppExt, DYFilteruppExt);
    factory.addXfilteruppExt(DXFixeduppExt, DXFilteruppExt);
    factory.addguessuppExt(DXGuessuppExt, DYGuessuppExt, DXisFilteruppExt, DYisFilteruppExt);
    for (int round = 0; round < nExtbRounds; round++)
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
              model.addConstr(DXisFilteruppExt[round][i][j], GRB.LESS_EQUAL, DXFilteruppExt[round][i][j],"");
              model.addConstr(DYisFilteruppExt[round][i][j], GRB.LESS_EQUAL, DYFilteruppExt[round][i][j],"");
        }
		
	/*articleTrails*/
    articleTrails(); 

    /*Upper bound of distinguisher*/
    factory.addBound(DXupper, DXlower, freeSBupper, freeXlower, isTable, isDDT2, blocksize, regime, nbRounds);
	
	
	/*bound of rb, r'b, rf, r'f*/	
	GRBVar rb = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "rb");
	GRBVar rf = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "rf");
	GRBVar rb_prime = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "rb_prime");
	GRBVar rf_prime = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "rf_prime");
	
	GRBLinExpr sum_rb = new GRBLinExpr();
    for (int i = 0; i < 4; i++){
        for (int j = 0; j < 4; j++) {
            sum_rb.addTerm(weightmk, DXuppExt[1][i][j]);
		}	
	}
	model.addConstr(rb, GRB.EQUAL, sum_rb, "");

	
	GRBLinExpr sum_rf = new GRBLinExpr();
	for (int i = 0; i < 4; i++){
        for (int j = 0; j < 4; j++) {
            sum_rf.addTerm(-1 * weightmk, DWFixedlowExt[nExtfRounds - 1][i][j]);
		}
	}
	sum_rf.addConstant(blocksize * 16);
	model.addConstr(rf, GRB.EQUAL, sum_rf, "");
	
	GRBLinExpr sum_rbp = new GRBLinExpr();
	for (int round = 0; round < nExtbRounds; round++){
		for (int i = 0; i < 4; i++){
			for (int j = 0; j < 4; j++){
				sum_rbp.addTerm(weightmk, DXisFilteruppExt[round][i][j]);
				sum_rbp.addTerm(weightmk, DYisFilteruppExt[round][i][j]);
			}
		}
	}
	model.addConstr(rb_prime, GRB.EQUAL, sum_rbp, "");
	
	GRBLinExpr sum_rfp = new GRBLinExpr();
	for (int round = 0; round < nExtfRounds; round++){
		for (int i = 0; i < 4; i++){
			for (int j = 0; j < 4; j++){
				sum_rfp.addTerm(weightmk, DXisFilterlowExt[round][i][j]);
				sum_rfp.addTerm(weightmk, DWisFilterlowExt[round][i][j]);
			}
		}
	}
	model.addConstr(rf_prime, GRB.EQUAL, sum_rfp, "");

    
    /*bound of mb, m'b, mf, m'f and hf*/
    GRBVar mb = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "mb");
	GRBVar mf = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "mf");
	GRBVar mb_prime = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "mb_prime");
	GRBVar mf_prime = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "mf_prime");
//	GRBLinExpr hf = new GRBLinExpr();
	
	
	GRBLinExpr sum_mb = new GRBLinExpr();
	GRBLinExpr sum_mbp = new GRBLinExpr();
    for (int round = 0; round < nExtbRounds - 1; round++){
        for (int i = 0; i < 2; i++){
            for (int j = 0; j < 4; j++) {
              sum_mb.addTerm(weightmk, DKnownEncuppExt[round][i][j]);
              sum_mbp.addTerm(weightmk, DYGuessuppExt[round][i][j]);
			}
		}
    }
	model.addConstr(mb, GRB.EQUAL, sum_mb, "");
	model.addConstr(mb_prime, GRB.EQUAL, sum_mbp, "");
	
	GRBLinExpr sum_mf = new GRBLinExpr();
	GRBLinExpr sum_mfp = new GRBLinExpr();
    for (int round = 0; round < nExtfRounds; round++){
        for (int i = 0; i < 2; i++){
            for (int j = 0; j < 4; j++) {
              sum_mf.addTerm(weightmk, DKnownDeclowExt[round][i][j]);
              sum_mfp.addTerm(weightmk, DXGuesslowExt[round][i][j]);
			}
		}
	}
    model.addConstr(mf, GRB.EQUAL, sum_mf, "");
	model.addConstr(mf_prime, GRB.EQUAL, sum_mfp, "");
      

    objective = new GRBLinExpr();	
	
	objective1Var = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "T1");
	GRBLinExpr obj1 = new GRBLinExpr();
	obj1.addConstant(2.0 + D);
	obj1.addTerm(1.0, mb_prime);
	obj1.addTerm(1.0, mf_prime);
	model.addConstr(objective1Var, GRB.EQUAL, obj1, "");
	

	objective2Var = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "T2");
	
	GRBVar filterp = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "");
	GRBLinExpr obj2_0 = new GRBLinExpr();
	obj2_0.addTerm(1.0, rb);
	obj2_0.addTerm(-1.0, rb_prime);
	model.addConstr(filterp, GRB.EQUAL, obj2_0, "");
	
	GRBVar filterc = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "");
	GRBLinExpr obj2_1 = new GRBLinExpr();
	obj2_1.addTerm(1.0, rf);
	obj2_1.addTerm(-1.0, rf_prime);
	obj2_1.addConstant(D - n);
	model.addConstr(filterc, GRB.EQUAL, obj2_1, "");
	
	GRBVar filter = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "");
	model.addGenConstrMin(filter, new GRBVar[]{filterp, filterc}, GRB.INFINITY, "");
	
	GRBLinExpr obj2 = new GRBLinExpr();
	obj2.addConstant(D);
	obj2.addTerm(1.0, mb_prime);
	obj2.addTerm(1.0, mf_prime);
	obj2.addTerm(1.0, filter);
	obj2.addConstant(1.0);
	model.addConstr(objective2Var, GRB.EQUAL, obj2, "");
	
	
	objective3Var = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "T3");
    CombinedEpsResult epsResult = addCombinedClosureTriggeredEps(weightmk);
    EPSVar = epsResult.totalPeak;
    epsilonpVar = epsResult.upperPeak;
    epsiloncVar = epsResult.lowerPeak;
    weightedCumulativeScoreVar = epsResult.weightedCumulativeScore;

	
	GRBLinExpr obj3 = new GRBLinExpr();
	obj3.addTerm(1.0, mb_prime);
	obj3.addTerm(1.0, mf_prime);
	obj3.addConstant(2.0 * D - 2.0 * n - (Math.log(16 * (nExtbRounds + nbRounds + nExtfRounds)) / Math.log(2.0)));
	obj3.addTerm(2.0, rb);
	obj3.addTerm(-2.0, rb_prime);
	obj3.addTerm(2.0, rf);
	obj3.addTerm(-2.0, rf_prime);
	obj3.addTerm(1.0, EPSVar);
	model.addConstr(objective3Var, GRB.EQUAL, obj3, "");
	
	GRBLinExpr gap = new GRBLinExpr();
	gap.multAdd(1.0, obj3);
	gap.multAdd(-1.0, obj2);
	model.addConstr(gap, GRB.LESS_EQUAL, 10.0, "");
	
    objective.addTerm(1.0, obj[0]);
    model.addConstr(objective, GRB.GREATER_EQUAL, objective1Var, "");
    model.addConstr(objective, GRB.GREATER_EQUAL, objective2Var, "");
    model.addConstr(objective, GRB.GREATER_EQUAL, objective3Var, "");
    
    GRBLinExpr primaryObjective = new GRBLinExpr(objective);
    GRBLinExpr weightedScoreObjective = new GRBLinExpr();
    weightedScoreObjective.addTerm(1.0, weightedCumulativeScoreVar);
    model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
    model.setObjectiveN(primaryObjective, 0, 2, 1.0, 0.0, 0.0,
      "primary_complexity");
    model.setObjectiveN(weightedScoreObjective, 1, 1, 1.0, 0.0, 0.0,
      "secondary_descending_weighted_cumulative_eps_score");
  }


  /** A compact integer triple used for local filter dependencies. */
  private static int[] dep(final int r, final int i, final int j) {
    return new int[]{r, i, j};
  }

  /** Mirrors Step1Factory.addguessuppExt() for one selected upper filter. */
  private List<int[]> upperNeedYDeps(final boolean xSide, final int fr,
      final int fi, final int fj) {
    final int rounds = nExtbRounds;
    final int keyRounds = Math.max(0, nExtbRounds - 1);
    final List<int[]> deps = new ArrayList<int[]>();
    if (rounds <= 0 || keyRounds <= 0) {
      return deps;
    }

    boolean[][][] xIs = new boolean[rounds][4][4];
    boolean[][][] yIs = new boolean[rounds][4][4];
    boolean[][][] dxGuess = new boolean[rounds][4][4];
    boolean[][][] dyGuess = new boolean[rounds][4][4];
    if (xSide) {
      xIs[fr][fi][fj] = true;
    }
    else {
      yIs[fr][fi][fj] = true;
    }

    final int last = rounds - 1;
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        dxGuess[last][i][j] = yIs[last][i][j];
      }
    }

    for (int r = last - 1; r >= 0; r--) {
      for (int j = 0; j < 4; j++) {
        dyGuess[r][0][j] = dxGuess[r + 1][0][j]
          || dxGuess[r + 1][1][j] || dxGuess[r + 1][3][j];
        dyGuess[r][1][j] = dxGuess[r + 1][2][(j + 1) % 4];
        dyGuess[r][2][j] = dxGuess[r + 1][0][(j + 2) % 4]
          || dxGuess[r + 1][2][(j + 2) % 4]
          || dxGuess[r + 1][3][(j + 2) % 4];
        dyGuess[r][3][j] = dxGuess[r + 1][0][(j + 3) % 4];
      }
      for (int j = 0; j < 4; j++) {
        dxGuess[r][0][j] = xIs[r + 1][0][j] || xIs[r + 1][1][j]
          || xIs[r + 1][3][j] || dyGuess[r][0][j] || yIs[r][0][j];
        dxGuess[r][1][j] = xIs[r + 1][2][(j + 1) % 4]
          || dyGuess[r][1][j] || yIs[r][1][j];
        dxGuess[r][2][j] = xIs[r + 1][0][(j + 2) % 4]
          || xIs[r + 1][2][(j + 2) % 4]
          || xIs[r + 1][3][(j + 2) % 4]
          || dyGuess[r][2][j] || yIs[r][2][j];
        dxGuess[r][3][j] = xIs[r + 1][0][(j + 3) % 4]
          || dyGuess[r][3][j] || yIs[r][3][j];
      }
    }

    for (int r = 0; r < keyRounds; r++) {
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 4; j++) {
          if (dyGuess[r][i][j]) {
            deps.add(dep(r, i, j));
          }
        }
      }
    }
    return deps;
  }

  /** Mirrors Step1Factory.addguesslowExt() for one selected lower filter. */
  private List<int[]> lowerNeedXDeps(final boolean xSide, final int fr,
      final int fi, final int fj) {
    final int rounds = nExtfRounds;
    final int keyRounds = Math.max(0, nExtfRounds);
    final List<int[]> deps = new ArrayList<int[]>();
    if (rounds <= 0 || keyRounds <= 0) {
      return deps;
    }

    boolean[][][] xIs = new boolean[rounds][4][4];
    boolean[][][] wIs = new boolean[rounds][4][4];
    boolean[][][] dxGuess = new boolean[rounds][4][4];
    boolean[][][] dwGuess = new boolean[rounds][4][4];
    if (xSide) {
      xIs[fr][fi][fj] = true;
    }
    else {
      wIs[fr][fi][fj] = true;
    }

    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        dxGuess[0][i][j] = xIs[0][i][j];
      }
    }

    final int[] shift = {0, 3, 2, 1};
    for (int r = 0; r < rounds; r++) {
      for (int j = 0; j < 4; j++) {
        for (int i = 0; i < 4; i++) {
          dwGuess[r][i][j] = wIs[r][i][j]
            || dxGuess[r][i][(j + shift[i]) % 4];
        }
      }
      if (r < rounds - 1) {
        for (int j = 0; j < 4; j++) {
          dxGuess[r + 1][0][j] = dwGuess[r][3][j] || xIs[r + 1][0][j];
          dxGuess[r + 1][1][j] = dwGuess[r][0][j] || dwGuess[r][1][j]
            || dwGuess[r][2][j] || xIs[r + 1][1][j];
          dxGuess[r + 1][2][j] = dwGuess[r][1][j] || xIs[r + 1][2][j];
          dxGuess[r + 1][3][j] = dwGuess[r][1][j] || dwGuess[r][2][j]
            || dwGuess[r][3][j] || xIs[r + 1][3][j];
        }
      }
    }

    for (int r = 0; r < keyRounds; r++) {
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 4; j++) {
          if (dxGuess[r][i][j]) {
            deps.add(dep(r, i, j));
          }
        }
      }
    }
    return deps;
  }

  private static int localKeyIndex(final int r, final int i, final int j) {
    return 8 * r + 4 * i + j;
  }

  /** Converts the original static dependency list into unconditional EPS dependencies. */
  private static List<EpsDependency> unconditionalDeps(
      final List<int[]> localDeps, final int offset) {
    LinkedHashSet<Integer> unique = new LinkedHashSet<Integer>();
    for (int[] key : localDeps) {
      unique.add(offset + localKeyIndex(key[0], key[1], key[2]));
    }

    List<EpsDependency> result = new ArrayList<EpsDependency>();
    for (int keyIndex : unique) {
      result.add(new EpsDependency(keyIndex, null));
    }
    return result;
  }

  /**
   * First inverse-MixColumns propagation for an upper X-filter.
   *
   * These are Y-state cells in round fr-1 whose exact values are needed only
   * when their corresponding differences are active.  For an inactive
   * difference, the S-box input/output difference is zero and no fixed state
   * value has to be recovered.
   */
  private static List<int[]> upperMixFirstYCells(final int fr,
      final int fi, final int fj) {
    List<int[]> cells = new ArrayList<int[]>();
    final int r = fr - 1;
    if (r < 0) {
      return cells;
    }

    switch (fi) {
      case 0:
        cells.add(dep(r, 0, fj));
        cells.add(dep(r, 2, (fj + 2) % 4));
        cells.add(dep(r, 3, (fj + 1) % 4));
        break;
      case 1:
        cells.add(dep(r, 0, fj));
        break;
      case 2:
        cells.add(dep(r, 1, (fj + 3) % 4));
        cells.add(dep(r, 2, (fj + 2) % 4));
        break;
      case 3:
        cells.add(dep(r, 0, fj));
        cells.add(dep(r, 2, (fj + 2) % 4));
        break;
      default:
        throw new IllegalArgumentException("Invalid row index: " + fi);
    }
    return cells;
  }

  /**
   * First MixColumns propagation for a lower W-filter.
   *
   * For example, W(fr,1,j) reaches X(fr+1,1,j), X(fr+1,2,j) and
   * X(fr+1,3,j), exactly as in Fig. 11.  Each resulting branch is retained
   * only when the corresponding DXlowExt difference is active.
   */
  private static List<int[]> lowerMixFirstXCells(final int fr,
      final int fi, final int fj) {
    List<int[]> cells = new ArrayList<int[]>();
    final int r = fr + 1;
    switch (fi) {
      case 0:
        cells.add(dep(r, 1, fj));
        break;
      case 1:
        cells.add(dep(r, 1, fj));
        cells.add(dep(r, 2, fj));
        cells.add(dep(r, 3, fj));
        break;
      case 2:
        cells.add(dep(r, 1, fj));
        cells.add(dep(r, 3, fj));
        break;
      case 3:
        cells.add(dep(r, 0, fj));
        cells.add(dep(r, 3, fj));
        break;
      default:
        throw new IllegalArgumentException("Invalid row index: " + fi);
    }
    return cells;
  }

  /** Creates a binary variable equal to the OR of the supplied conditions. */
  private GRBVar addRequirementOr(final String name,
      final List<GRBVar> conditions) throws GRBException {
    if (conditions.size() == 1) {
      return conditions.get(0);
    }

    GRBVar required = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name);
    GRBLinExpr upper = new GRBLinExpr();
    upper.addTerm(1.0, required);
    for (int c = 0; c < conditions.size(); c++) {
      GRBVar condition = conditions.get(c);
      model.addConstr(required, GRB.GREATER_EQUAL, condition,
        name + "_ge_" + c);
      upper.addTerm(-1.0, condition);
    }
    model.addConstr(upper, GRB.LESS_EQUAL, 0.0, name + "_le_sum");
    return required;
  }

  /**
   * Builds differential-gated dependencies for an upper MixColumns X-filter.
   *
   * The first inverse-MC layer is a difference propagation.  Only an active
   * resulting DXuppExt cell creates an exact-value requirement.  From that
   * point onward, the original Y-filter value propagation is reused.
   */
  private List<EpsDependency> upperMixDependencies(final int fr,
      final int fi, final int fj, final int offset) throws GRBException {
    Map<Integer, List<GRBVar>> conditionsByKey =
      new LinkedHashMap<Integer, List<GRBVar>>();

    for (int[] source : upperMixFirstYCells(fr, fi, fj)) {
      GRBVar activeDifference = DXuppExt[source[0]][source[1]][source[2]];
      for (int[] localKey : upperNeedYDeps(false,
          source[0], source[1], source[2])) {
        int keyIndex = offset
          + localKeyIndex(localKey[0], localKey[1], localKey[2]);
        List<GRBVar> conditions = conditionsByKey.get(keyIndex);
        if (conditions == null) {
          conditions = new ArrayList<GRBVar>();
          conditionsByKey.put(keyIndex, conditions);
        }
        if (!conditions.contains(activeDifference)) {
          conditions.add(activeDifference);
        }
      }
    }

    List<EpsDependency> result = new ArrayList<EpsDependency>();
    for (Map.Entry<Integer, List<GRBVar>> entry
        : conditionsByKey.entrySet()) {
      GRBVar required = addRequirementOr(
        "need_UX_" + fr + "_" + fi + "_" + fj + "_k" + entry.getKey(),
        entry.getValue());
      result.add(new EpsDependency(entry.getKey(), required));
    }
    return result;
  }

  /**
   * Builds differential-gated dependencies for a lower MixColumns W-filter.
   *
   * The first MC layer determines which X differences are involved.  An
   * inactive DXlowExt branch contributes no fixed-value/key requirement;
   * active branches continue with the unchanged X-filter propagation.
   */
  private List<EpsDependency> lowerMixDependencies(final int fr,
      final int fi, final int fj, final int offset) throws GRBException {
    Map<Integer, List<GRBVar>> conditionsByKey =
      new LinkedHashMap<Integer, List<GRBVar>>();

    for (int[] source : lowerMixFirstXCells(fr, fi, fj)) {
      if (source[0] < 0 || source[0] >= nExtfRounds) {
        continue;
      }
      GRBVar activeDifference = DXlowExt[source[0]][source[1]][source[2]];
      for (int[] localKey : lowerNeedXDeps(true,
          source[0], source[1], source[2])) {
        int keyIndex = offset
          + localKeyIndex(localKey[0], localKey[1], localKey[2]);
        List<GRBVar> conditions = conditionsByKey.get(keyIndex);
        if (conditions == null) {
          conditions = new ArrayList<GRBVar>();
          conditionsByKey.put(keyIndex, conditions);
        }
        if (!conditions.contains(activeDifference)) {
          conditions.add(activeDifference);
        }
      }
    }

    List<EpsDependency> result = new ArrayList<EpsDependency>();
    for (Map.Entry<Integer, List<GRBVar>> entry
        : conditionsByKey.entrySet()) {
      GRBVar required = addRequirementOr(
        "need_LW_" + fr + "_" + fi + "_" + fj + "_k" + entry.getKey(),
        entry.getValue());
      result.add(new EpsDependency(entry.getKey(), required));
    }
    return result;
  }

  /**
   * One key dependency of a filter.
   *
   * required == null means an unconditional dependency.  Otherwise required
   * is a binary expression/variable saying whether this key is actually
   * needed for the selected differential trail.
   */
  private static final class EpsDependency {
    final int keyIndex;
    final GRBVar required;

    EpsDependency(final int keyIndex, final GRBVar required) {
      this.keyIndex = keyIndex;
      this.required = required;
    }
  }

  private static final class EpsKey {
    final int side;
    final int r;
    final int i;
    final int j;
    final GRBVar known;
    final GRBVar pairGuessed;

    EpsKey(final int side, final int r, final int i, final int j,
        final GRBVar known, final GRBVar pairGuessed) {
      this.side = side;
      this.r = r;
      this.i = i;
      this.j = j;
      this.known = known;
      this.pairGuessed = pairGuessed;
    }
  }

  private static final class EpsFilter {
    final int side;
    final String kind;
    final int r;
    final int i;
    final int j;
    final GRBVar filter;
    final GRBVar isFilter;
    final List<EpsDependency> deps;

    EpsFilter(final int side, final String kind, final int r, final int i,
        final int j, final GRBVar filter, final GRBVar isFilter,
        final List<EpsDependency> deps) {
      this.side = side;
      this.kind = kind;
      this.r = r;
      this.i = i;
      this.j = j;
      this.filter = filter;
      this.isFilter = isFilter;
      this.deps = deps;
    }

    String label() {
      return (side == EPS_UPPER ? "U:" : "L:") + kind + ":(" + r
        + ", " + i + ", " + j + ")";
    }
  }

  private static final class MixedEpsSchedule {
    final List<EpsKey> keys;
    final List<EpsFilter> filters;
    final GRBVar[][] guessed;
    final GRBVar[][] applied;
    final GRBVar[] active;
    final double weightmk;

    MixedEpsSchedule(final List<EpsKey> keys, final List<EpsFilter> filters,
        final GRBVar[][] guessed, final GRBVar[][] applied,
        final GRBVar[] active, final double weightmk) {
      this.keys = keys;
      this.filters = filters;
      this.guessed = guessed;
      this.applied = applied;
      this.active = active;
      this.weightmk = weightmk;
    }
  }

  private static final class CombinedEpsResult {
    final GRBVar totalPeak;
    final GRBVar upperPeak;
    final GRBVar lowerPeak;
    final GRBVar weightedCumulativeScore;

    CombinedEpsResult(final GRBVar totalPeak, final GRBVar upperPeak,
        final GRBVar lowerPeak, final GRBVar weightedCumulativeScore) {
      this.totalPeak = totalPeak;
      this.upperPeak = upperPeak;
      this.lowerPeak = lowerPeak;
      this.weightedCumulativeScore = weightedCumulativeScore;
    }
  }

  /** Encodes late = filter AND NOT isFilter. */
  private GRBVar addLateFilter(final String name, final GRBVar filter,
      final GRBVar isFilter) throws GRBException {
    GRBVar late = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name);
    model.addConstr(late, GRB.LESS_EQUAL, filter, name + "_le_filter");

    GRBLinExpr notFirst = new GRBLinExpr();
    notFirst.addTerm(1.0, late);
    notFirst.addTerm(1.0, isFilter);
    model.addConstr(notFirst, GRB.LESS_EQUAL, 1.0, name + "_not_first");

    GRBLinExpr exact = new GRBLinExpr();
    exact.addTerm(1.0, late);
    exact.addTerm(-1.0, filter);
    exact.addTerm(1.0, isFilter);
    model.addConstr(exact, GRB.GREATER_EQUAL, 0.0,
      name + "_ge_filter_minus_first");
    return late;
  }

  private static EpsDependency dependencyFor(final EpsFilter filter,
      final int keyIndex) {
    for (EpsDependency dependency : filter.deps) {
      if (dependency.keyIndex == keyIndex) {
        return dependency;
      }
    }
    return null;
  }

  /** active=1 selects the cumulative score; inactive steps have cost zero. */
  private GRBVar addConditionalScore(final String name,
      final GRBLinExpr score, final GRBVar active, final double bound)
      throws GRBException {
    GRBVar eventCost = model.addVar(-bound, bound, 0.0, GRB.CONTINUOUS, name);

    GRBLinExpr leScore = new GRBLinExpr();
    leScore.addTerm(1.0, eventCost);
    leScore.multAdd(-1.0, score);
    leScore.addTerm(bound, active);
    model.addConstr(leScore, GRB.LESS_EQUAL, bound, name + "_le_score");

    GRBLinExpr geScore = new GRBLinExpr();
    geScore.addTerm(1.0, eventCost);
    geScore.multAdd(-1.0, score);
    geScore.addTerm(-bound, active);
    model.addConstr(geScore, GRB.GREATER_EQUAL, -bound, name + "_ge_score");

    GRBLinExpr inactiveUpper = new GRBLinExpr();
    inactiveUpper.addTerm(1.0, eventCost);
    inactiveUpper.addTerm(-bound, active);
    model.addConstr(inactiveUpper, GRB.LESS_EQUAL, 0.0,
      name + "_inactive_upper");

    GRBLinExpr inactiveLower = new GRBLinExpr();
    inactiveLower.addTerm(1.0, eventCost);
    inactiveLower.addTerm(bound, active);
    model.addConstr(inactiveLower, GRB.GREATER_EQUAL, 0.0,
      name + "_inactive_lower");
    return eventCost;
  }

  /**
   * Builds one genuine upper/lower mixed EPS schedule.
   *
   * Every active step guesses 1--3 new key cells and activates at least one
   * new late filter.  Each newly guessed key must be a dependency of a filter
   * activated in that same step, so key material cannot be stockpiled.  The
   * score is cumulative over the shared U/L timeline.
   */
  private CombinedEpsResult addCombinedClosureTriggeredEps(
      final double weightmk) throws GRBException {
    final int upperKeyRounds = Math.max(0, nExtbRounds - 1);
    final int lowerKeyRounds = Math.max(0, nExtfRounds);
    final int upperKeyCount = 8 * upperKeyRounds;

    List<EpsKey> keys = new ArrayList<EpsKey>();
    for (int r = 0; r < upperKeyRounds; r++) {
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 4; j++) {
          keys.add(new EpsKey(EPS_UPPER, r, i, j,
            DKnownEncuppExt[r][i][j], DYGuessuppExt[r][i][j]));
        }
      }
    }
    for (int r = 0; r < lowerKeyRounds; r++) {
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 4; j++) {
          keys.add(new EpsKey(EPS_LOWER, r, i, j,
            DKnownDeclowExt[r][i][j], DXGuesslowExt[r][i][j]));
        }
      }
    }

    List<EpsFilter> filters = new ArrayList<EpsFilter>();
    for (int r = 0; r < nExtbRounds; r++) {
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          filters.add(new EpsFilter(EPS_UPPER, "X", r, i, j,
            DXFilteruppExt[r][i][j], DXisFilteruppExt[r][i][j],
            upperMixDependencies(r, i, j, 0)));
          filters.add(new EpsFilter(EPS_UPPER, "Y", r, i, j,
            DYFilteruppExt[r][i][j], DYisFilteruppExt[r][i][j],
            unconditionalDeps(upperNeedYDeps(false, r, i, j), 0)));
        }
      }
    }
    for (int r = 0; r < nExtfRounds; r++) {
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          filters.add(new EpsFilter(EPS_LOWER, "X", r, i, j,
            DXFilterlowExt[r][i][j], DXisFilterlowExt[r][i][j],
            unconditionalDeps(lowerNeedXDeps(true, r, i, j),
              upperKeyCount)));
          filters.add(new EpsFilter(EPS_LOWER, "W", r, i, j,
            DWFilterlowExt[r][i][j], DWisFilterlowExt[r][i][j],
            lowerMixDependencies(r, i, j, upperKeyCount)));
        }
      }
    }

    final int keyCount = keys.size();
    final int filterCount = filters.size();
    GRBVar totalPeak = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER,
      "EPS_mixed");
    GRBVar upperPeak = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER,
      "epsilonp_mixed");
    GRBVar lowerPeak = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER,
      "epsilonc_mixed");
    GRBVar weightedCumulativeScore = model.addVar(-GRB.INFINITY,
      GRB.INFINITY, 0.0, GRB.CONTINUOUS,
      "EPS_descending_weighted_cumulative_score");
    GRBLinExpr weightedCumulativeScoreSum = new GRBLinExpr();

    if (keyCount == 0 || filterCount == 0) {
      model.addConstr(totalPeak, GRB.EQUAL, 0.0, "EPS_mixed_zero");
      model.addConstr(upperPeak, GRB.EQUAL, 0.0, "epsilonp_mixed_zero");
      model.addConstr(lowerPeak, GRB.EQUAL, 0.0, "epsilonc_mixed_zero");
      model.addConstr(weightedCumulativeScore, GRB.EQUAL, 0.0,
        "EPS_descending_weighted_cumulative_score_zero");
      mixedEpsSchedule = new MixedEpsSchedule(keys, filters,
        new GRBVar[1][keyCount], new GRBVar[1][filterCount],
        new GRBVar[0], weightmk);
      return new CombinedEpsResult(totalPeak, upperPeak, lowerPeak,
        weightedCumulativeScore);
    }

    // Every active step consumes at least one key and at least one filter.
    final int stages = Math.min(keyCount, filterCount);
    GRBVar[] late = new GRBVar[filterCount];
    for (int f = 0; f < filterCount; f++) {
      EpsFilter cell = filters.get(f);
      late[f] = addLateFilter("late_mixed_" + f + "_" + cell.side + "_"
        + cell.kind + "_" + cell.r + "_" + cell.i + "_" + cell.j,
        cell.filter, cell.isFilter);
    }

    GRBVar[][] guessed = new GRBVar[stages + 1][keyCount];
    for (int t = 0; t <= stages; t++) {
      for (int k = 0; k < keyCount; k++) {
        guessed[t][k] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
          "G_mixed_" + t + "_" + k);
      }
    }

    GRBVar[][] applied = new GRBVar[stages + 1][filterCount];
    for (int t = 0; t <= stages; t++) {
      for (int f = 0; f < filterCount; f++) {
        applied[t][f] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
          "H_mixed_" + t + "_" + f);
      }
    }

    GRBVar[] active = new GRBVar[stages];
    for (int t = 0; t < stages; t++) {
      active[t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
        "filter_step_active_mixed_" + (t + 1));
    }
    mixedEpsSchedule = new MixedEpsSchedule(keys, filters, guessed,
      applied, active, weightmk);

    for (int k = 0; k < keyCount; k++) {
      EpsKey key = keys.get(k);
      model.addConstr(key.pairGuessed, GRB.LESS_EQUAL, key.known,
        "pair_guess_known_mixed_" + k);
      model.addConstr(guessed[0][k], GRB.EQUAL, 0.0,
        "G0_mixed_" + k);
      for (int t = 0; t <= stages; t++) {
        GRBLinExpr validGuess = new GRBLinExpr();
        validGuess.addTerm(1.0, guessed[t][k]);
        validGuess.addTerm(1.0, key.pairGuessed);
        model.addConstr(validGuess, GRB.LESS_EQUAL, key.known,
          "G_known_mixed_" + t + "_" + k);
        if (t > 0) {
          model.addConstr(guessed[t - 1][k], GRB.LESS_EQUAL,
            guessed[t][k], "G_mono_mixed_" + t + "_" + k);
        }
      }
    }

    for (int f = 0; f < filterCount; f++) {
      EpsFilter cell = filters.get(f);
      model.addConstr(applied[stages][f], GRB.EQUAL, late[f],
        "Hfinal_mixed_" + f);
      for (int t = 0; t <= stages; t++) {
        model.addConstr(applied[t][f], GRB.LESS_EQUAL, late[f],
          "H_late_mixed_" + t + "_" + f);
        if (t > 0) {
          model.addConstr(applied[t - 1][f], GRB.LESS_EQUAL,
            applied[t][f], "H_mono_mixed_" + t + "_" + f);
        }

        // A conditional MixColumns dependency is considered ready when
        // either its difference branch is inactive or its key is available.
        // Unconditional S-box dependencies keep the original formulation.
        GRBLinExpr closure = new GRBLinExpr();
        closure.addTerm(1.0, applied[t][f]);
        closure.addTerm(-1.0, late[f]);
        for (int d = 0; d < cell.deps.size(); d++) {
          EpsDependency dependency = cell.deps.get(d);
          int keyIndex = dependency.keyIndex;
          EpsKey key = keys.get(keyIndex);

          if (dependency.required == null) {
            GRBLinExpr available = new GRBLinExpr();
            available.addTerm(1.0, applied[t][f]);
            available.addTerm(-1.0, key.pairGuessed);
            available.addTerm(-1.0, guessed[t][keyIndex]);
            model.addConstr(available, GRB.LESS_EQUAL, 0.0,
              "H_dep_mixed_" + t + "_" + f + "_" + keyIndex);
            closure.addTerm(-1.0, key.pairGuessed);
            closure.addTerm(-1.0, guessed[t][keyIndex]);
          }
          else {
            GRBVar ready = model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
              "ready_mixed_" + t + "_" + f + "_" + keyIndex);

            // ready = (NOT required) OR pairGuessed OR guessed[t].
            GRBLinExpr inactiveBranch = new GRBLinExpr();
            inactiveBranch.addTerm(1.0, ready);
            inactiveBranch.addTerm(1.0, dependency.required);
            model.addConstr(inactiveBranch, GRB.GREATER_EQUAL, 1.0,
              "ready_inactive_mixed_" + t + "_" + f + "_" + keyIndex);
            model.addConstr(ready, GRB.GREATER_EQUAL, key.pairGuessed,
              "ready_pair_mixed_" + t + "_" + f + "_" + keyIndex);
            model.addConstr(ready, GRB.GREATER_EQUAL, guessed[t][keyIndex],
              "ready_eps_mixed_" + t + "_" + f + "_" + keyIndex);

            GRBLinExpr readyUpper = new GRBLinExpr();
            readyUpper.addTerm(1.0, ready);
            readyUpper.addTerm(1.0, dependency.required);
            readyUpper.addTerm(-1.0, key.pairGuessed);
            readyUpper.addTerm(-1.0, guessed[t][keyIndex]);
            model.addConstr(readyUpper, GRB.LESS_EQUAL, 1.0,
              "ready_upper_mixed_" + t + "_" + f + "_" + keyIndex);

            model.addConstr(applied[t][f], GRB.LESS_EQUAL, ready,
              "H_cond_dep_mixed_" + t + "_" + f + "_" + keyIndex);
            closure.addTerm(-1.0, ready);
          }
        }

        // Full closure: a late filter is activated immediately once every
        // unconditional dependency and every active conditional dependency
        // is ready.
        model.addConstr(closure, GRB.GREATER_EQUAL, -cell.deps.size(),
          "H_closure_mixed_" + t + "_" + f);
      }
    }

    for (int t = 1; t <= stages; t++) {
      GRBLinExpr newKeyCount = new GRBLinExpr();
      for (int k = 0; k < keyCount; k++) {
        newKeyCount.addTerm(1.0, guessed[t][k]);
        newKeyCount.addTerm(-1.0, guessed[t - 1][k]);
      }

      GRBLinExpr newFilterCount = new GRBLinExpr();
      for (int f = 0; f < filterCount; f++) {
        newFilterCount.addTerm(1.0, applied[t][f]);
        newFilterCount.addTerm(-1.0, applied[t - 1][f]);
      }

      // active iff at least one new filter is activated.
      GRBLinExpr activeNeedsFilter = new GRBLinExpr(newFilterCount);
      activeNeedsFilter.addTerm(-1.0, active[t - 1]);
      model.addConstr(activeNeedsFilter, GRB.GREATER_EQUAL, 0.0,
        "mixed_active_needs_filter_" + t);
      GRBLinExpr filterNeedsActive = new GRBLinExpr(newFilterCount);
      filterNeedsActive.addTerm(-filterCount, active[t - 1]);
      model.addConstr(filterNeedsActive, GRB.LESS_EQUAL, 0.0,
        "mixed_filter_needs_active_" + t);

      // Every filtering step guesses between one and three new key cells;
      // inactive steps guess none.
      GRBLinExpr atLeastOneKey = new GRBLinExpr(newKeyCount);
      atLeastOneKey.addTerm(-1.0, active[t - 1]);
      model.addConstr(atLeastOneKey, GRB.GREATER_EQUAL, 0.0,
        "mixed_step_min_keys_" + t);
      GRBLinExpr atMostThreeKeys = new GRBLinExpr(newKeyCount);
      atMostThreeKeys.addTerm(-EPS_MAX_KEYS_PER_FILTER_STEP, active[t - 1]);
      model.addConstr(atMostThreeKeys, GRB.LESS_EQUAL, 0.0,
        "mixed_step_max_keys_" + t);

      // No empty gaps between active filtering steps.
      if (t < stages) {
        model.addConstr(active[t], GRB.LESS_EQUAL, active[t - 1],
          "mixed_active_prefix_" + t);
      }

      // No stockpiling: every key introduced now must participate in at least
      // one newly activated filter at this step.  For a MixColumns filter,
      // the differential branch that requires the key must also be active.
      for (int k = 0; k < keyCount; k++) {
        GRBLinExpr usedNow = new GRBLinExpr();
        usedNow.addTerm(1.0, guessed[t][k]);
        usedNow.addTerm(-1.0, guessed[t - 1][k]);

        for (int f = 0; f < filterCount; f++) {
          EpsDependency dependency = dependencyFor(filters.get(f), k);
          if (dependency == null) {
            continue;
          }

          if (dependency.required == null) {
            usedNow.addTerm(-1.0, applied[t][f]);
            usedNow.addTerm(1.0, applied[t - 1][f]);
          }
          else {
            GRBVar usedByNewFilter = model.addVar(
              0.0, 1.0, 0.0, GRB.BINARY,
              "used_cond_mixed_" + t + "_" + f + "_" + k);

            GRBLinExpr leRequired = new GRBLinExpr();
            leRequired.addTerm(1.0, usedByNewFilter);
            leRequired.addTerm(-1.0, dependency.required);
            model.addConstr(leRequired, GRB.LESS_EQUAL, 0.0,
              "used_cond_required_mixed_" + t + "_" + f + "_" + k);

            GRBLinExpr leNewFilter = new GRBLinExpr();
            leNewFilter.addTerm(1.0, usedByNewFilter);
            leNewFilter.addTerm(-1.0, applied[t][f]);
            leNewFilter.addTerm(1.0, applied[t - 1][f]);
            model.addConstr(leNewFilter, GRB.LESS_EQUAL, 0.0,
              "used_cond_newfilter_mixed_" + t + "_" + f + "_" + k);

            GRBLinExpr geAnd = new GRBLinExpr();
            geAnd.addTerm(1.0, usedByNewFilter);
            geAnd.addTerm(-1.0, dependency.required);
            geAnd.addTerm(-1.0, applied[t][f]);
            geAnd.addTerm(1.0, applied[t - 1][f]);
            model.addConstr(geAnd, GRB.GREATER_EQUAL, -1.0,
              "used_cond_and_mixed_" + t + "_" + f + "_" + k);

            usedNow.addTerm(-1.0, usedByNewFilter);
          }
        }
        model.addConstr(usedNow, GRB.LESS_EQUAL, 0.0,
          "mixed_key_used_now_" + t + "_" + k);
      }
    }

    final double scoreBound = weightmk * (keyCount + 2.0 * filterCount);
    GRBVar[] totalEventCosts = new GRBVar[stages];
    GRBVar[] upperEventCosts = new GRBVar[stages];
    GRBVar[] lowerEventCosts = new GRBVar[stages];

    for (int t = 1; t <= stages; t++) {
      GRBLinExpr totalScore = new GRBLinExpr();
      GRBLinExpr upperScore = new GRBLinExpr();
      GRBLinExpr lowerScore = new GRBLinExpr();
      for (int k = 0; k < keyCount; k++) {
        EpsKey key = keys.get(k);
        totalScore.addTerm(weightmk, guessed[t][k]);
        if (key.side == EPS_UPPER) {
          upperScore.addTerm(weightmk, guessed[t][k]);
        }
        else {
          lowerScore.addTerm(weightmk, guessed[t][k]);
        }
      }
      for (int f = 0; f < filterCount; f++) {
        EpsFilter filter = filters.get(f);
        totalScore.addTerm(-2.0 * weightmk, applied[t][f]);
        if (filter.side == EPS_UPPER) {
          upperScore.addTerm(-2.0 * weightmk, applied[t][f]);
        }
        else {
          lowerScore.addTerm(-2.0 * weightmk, applied[t][f]);
        }
      }

      totalEventCosts[t - 1] = addConditionalScore(
        "mixed_total_cost_" + t, totalScore, active[t - 1], scoreBound);
      upperEventCosts[t - 1] = addConditionalScore(
        "mixed_upper_cost_" + t, upperScore, active[t - 1], scoreBound);
      lowerEventCosts[t - 1] = addConditionalScore(
        "mixed_lower_cost_" + t, lowerScore, active[t - 1], scoreBound);

      // Let s_t be the cumulative EPS score after closure at active step t.
      // For N active steps,
      //   N*s_1 + (N-1)*s_2 + ... + s_N
      // equals the sum of active score prefixes:
      //   s_1 + (s_1+s_2) + ... + (s_1+...+s_N).
      // Because active steps are contiguous, conditioning each prefix on
      // active[t-1] gives this expression without any bilinear term or
      // dependence on the maximum number of padding stages.
      GRBLinExpr scorePrefix = new GRBLinExpr();
      for (int q = 0; q < t; q++) {
        scorePrefix.addTerm(1.0, totalEventCosts[q]);
      }
      GRBVar activeScorePrefix = addConditionalScore(
        "mixed_active_score_prefix_" + t, scorePrefix, active[t - 1],
        t * scoreBound);
      weightedCumulativeScoreSum.addTerm(1.0, activeScorePrefix);
    }

    model.addConstr(weightedCumulativeScore, GRB.EQUAL,
      weightedCumulativeScoreSum,
      "EPS_descending_weighted_cumulative_score_def");

    model.addGenConstrMax(totalPeak, totalEventCosts, 0.0,
      "EPS_mixed_continuous_peak");
    model.addGenConstrMax(upperPeak, upperEventCosts, 0.0,
      "epsilonp_mixed_continuous_peak");
    model.addGenConstrMax(lowerPeak, lowerEventCosts, 0.0,
      "epsilonc_mixed_continuous_peak");

    return new CombinedEpsResult(totalPeak, upperPeak, lowerPeak,
      weightedCumulativeScore);
  }

  private static boolean isOne(final GRBVar variable) throws GRBException {
    return variable != null
      && Math.round(variable.get(GRB.DoubleAttr.Xn)) == 1L;
  }

  private static String formatStepScore(final double score) {
    long rounded = Math.round(score);
    if (Math.abs(score - rounded) < 1e-7) {
      return Long.toString(rounded);
    }
    return String.format(java.util.Locale.ROOT, "%.2f", score);
  }

  /** Prints the actual modelled mixed U/L filter activation sequence. */
  private void printFilterActivationOrder(final int solutionNumber)
      throws GRBException {
    List<String> order = new ArrayList<String>();
    List<String> cumulativeScores = new ArrayList<String>();
    List<Double> cumulativeScoreValues = new ArrayList<Double>();
    List<String> heldKeyCounts = new ArrayList<String>();
    double cumulativeScoreSum = 0.0;
    if (mixedEpsSchedule != null) {
      for (int t = 1; t < mixedEpsSchedule.guessed.length; t++) {
        if (!isOne(mixedEpsSchedule.active[t - 1])) {
          continue;
        }
        for (int f = 0; f < mixedEpsSchedule.filters.size(); f++) {
          if (isOne(mixedEpsSchedule.applied[t][f])
              && !isOne(mixedEpsSchedule.applied[t - 1][f])) {
            order.add(mixedEpsSchedule.filters.get(f).label());
          }
        }

        int guessedCount = 0;
        int appliedCount = 0;
        for (int k = 0; k < mixedEpsSchedule.keys.size(); k++) {
          if (isOne(mixedEpsSchedule.guessed[t][k])) {
            guessedCount++;
          }
        }
        for (int f = 0; f < mixedEpsSchedule.filters.size(); f++) {
          if (isOne(mixedEpsSchedule.applied[t][f])) {
            appliedCount++;
          }
        }
        double cumulativeScore =
          mixedEpsSchedule.weightmk * guessedCount
          - 2.0 * mixedEpsSchedule.weightmk * appliedCount;
        cumulativeScores.add(formatStepScore(cumulativeScore));
        cumulativeScoreValues.add(cumulativeScore);
        heldKeyCounts.add(Integer.toString(guessedCount));
        cumulativeScoreSum += cumulativeScore;
      }
    }

    System.out.println("EPS filter activation order for solution #"
      + solutionNumber + ": "
      + (order.isEmpty() ? "(none)" : String.join(" --- ", order)));
    System.out.println("EPS cumulative score by step for solution #"
      + solutionNumber + ": "
      + (cumulativeScores.isEmpty()
        ? "(none)" : String.join(" .... ", cumulativeScores)));
  }

  public List<Step1Solution> solve(
    final int nbSolutions, 
	final boolean nonOptimalSolutions, 
	final int minObjValue, 
	final int maxObjValue,
	final int nbThreads, 
	final double timeLimit
) throws GRBException {
    File tuneFile = new File("tune1.prm");
    if (tuneFile.isFile()) {
      model.read(tuneFile.getPath());
    }
    model.write("model.lp");
    model.set(GRB.IntParam.Threads, nbThreads);
    if (timeLimit > 0)
      model.set(GRB.DoubleParam.TimeLimit, timeLimit);
    if (minObjValue != -1)
      model.addConstr(objective, GRB.GREATER_EQUAL, minObjValue, "objectivemin");
    if (maxObjValue != -1)
	  model.addConstr(objective, GRB.LESS_EQUAL, maxObjValue, "objectivemax");
    //GRBConstr c0 = model.addConstr(freeXlower[nbRounds/2][0][0], GRB.EQUAL, 1.0, "");
    //GRBConstr c1 = model.addConstr(freeSBupper[nbRounds/2][0][0], GRB.EQUAL, 1.0, "");
    //model.optimize();
    //model.remove(c0);
    //model.remove(c1);
    model.set(GRB.DoubleParam.PoolGap, (nonOptimalSolutions) ? 1.0 : 0.005);
    model.set(GRB.IntParam.PoolSolutions, nbSolutions);
    // Gurobi's systematic PoolSearchMode=2 is not compatible with a native
    // hierarchical multi-objective solve. Mode 0 keeps any solutions found
    // during the lexicographic passes, but does not guarantee nbSolutions.
    model.set(GRB.IntParam.PoolSearchMode, 0);
    if (nbSolutions > 1) {
      System.out.println("EPS multi-objective mode: PoolSearchMode=0; "
        + "additional pool solutions are incidental, not guaranteed.");
    }
    model.set(GRB.IntParam.DualReductions, 0);
	
    model.optimize();
    //model.write("output.sol");
    //model.computeIIS();
    //model.write("model1.ilp");
	
	if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
    System.out.println("Model is infeasible, computing IIS...");
    model.computeIIS();
    model.write("debug.ilp");
    model.write("debug.lp");
    System.exit(1);
	}
	
    return getAllFoundSolutions();
  }

  public void dispose() throws GRBException {
    model.dispose();
  }

  public List<Step1Solution> getAllFoundSolutions() throws GRBException {
    return IntStream.range(0, model.get(GRB.IntAttr.SolCount)).boxed()
      .map(solNb -> getSolution(solNb))
      .collect(Collectors.toList());
  }

  private Step1Solution getSolution(final int solutionNumber) {
    try {
      model.set(GRB.IntParam.SolutionNumber, solutionNumber);
      int[][][] DXupperValue     = new int[nbRounds+1][4][4];
      int[][][] freeXupperValue  = new int[nbRounds][4][4];
      int[][][] freeSBupperValue = new int[nbRounds][4][4];
      int[][][] DXlowerValue     = new int[nbRounds+1][4][4];
      int[][][] freeXlowerValue  = new int[nbRounds][4][4];
      int[][][] freeSBlowerValue = new int[nbRounds][4][4];
      int[][][] isTableValue     = new int[nbRounds][4][4];
      int[][][] isDDT2Value      = new int[nbRounds][4][4];
	  
      /*Ef*/
      int[][][] DXlowExtValue    = new int[nExtfRounds][4][4];
      int[][][] DSTKlowExtValue    = new int[nExtfRounds][2][4];
      int[][][] DKnownDeclowExtValue    = new int[nExtfRounds][4][4];
	  
	  /*fastfilter*/
      int[][][] DXFixedlowExtValue    = new int[nExtfRounds][4][4];
      int[][][] DWFixedlowExtValue    = new int[nExtfRounds][4][4];
      int[][][] DXFilterlowExtValue   = new int[nExtfRounds][4][4];
      int[][][] DWFilterlowExtValue   = new int[nExtfRounds][4][4];
      int[][][] DXisFilterlowExtValue = new int[nExtfRounds][4][4];
      int[][][] DWisFilterlowExtValue = new int[nExtfRounds][4][4];
      int[][][] DXGuesslowExtValue    = new int[nExtfRounds][4][4];
      int[][][] DWGuesslowExtValue    = new int[nExtfRounds][4][4];
	  
      /*Eb*/
      int[][][] DXuppExtValue    = new int[nExtbRounds+1][4][4];
      int[][][] DSTKuppExtValue    = new int[nExtbRounds][2][4];
      int[][][] DKnownEncuppExtValue    = new int[nExtbRounds][4][4];

      /*fastfilter*/
      int[][][] DXFixeduppExtValue    = new int[nExtbRounds][4][4];
      int[][][] DYFixeduppExtValue    = new int[nExtbRounds][4][4];
      int[][][] DXFilteruppExtValue   = new int[nExtbRounds][4][4];
      int[][][] DYFilteruppExtValue   = new int[nExtbRounds][4][4];
      int[][][] DXisFilteruppExtValue = new int[nExtbRounds][4][4];
      int[][][] DYisFilteruppExtValue = new int[nExtbRounds][4][4];
      int[][][] DXGuessuppExtValue    = new int[nExtbRounds][4][4];
      int[][][] DYGuessuppExtValue    = new int[nExtbRounds][4][4];

   

      /*Ef*/
      for (int round = 0; round < nExtfRounds; round++)
          for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
            	DXlowExtValue[round][i][j] = (int) Math.round(DXlowExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DKnownDeclowExtValue[round][i][j] = (int) Math.round(DKnownDeclowExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DXFixedlowExtValue[round][i][j] = (int) Math.round(DXFixedlowExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DWFixedlowExtValue[round][i][j] = (int) Math.round(DWFixedlowExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DXFilterlowExtValue[round][i][j] = (int) Math.round(DXFilterlowExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DWFilterlowExtValue[round][i][j] = (int) Math.round(DWFilterlowExt[round][i][j].get(GRB.DoubleAttr.Xn));  
                DXisFilterlowExtValue[round][i][j] = (int) Math.round(DXisFilterlowExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DWisFilterlowExtValue[round][i][j] = (int) Math.round(DWisFilterlowExt[round][i][j].get(GRB.DoubleAttr.Xn));  
                DXGuesslowExtValue[round][i][j] = (int) Math.round(DXGuesslowExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DWGuesslowExtValue[round][i][j] = (int) Math.round(DWGuesslowExt[round][i][j].get(GRB.DoubleAttr.Xn)); 
            	if (i<2)
            		DSTKlowExtValue[round][i][j] = (int) Math.round(DSTKlowExt[round][i][j].get(GRB.DoubleAttr.Xn));
            }
   
      /*Eb*/
      for (int round = 0; round < nExtbRounds; round++)
          for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
            	DXuppExtValue[round][i][j] = (int) Math.round(DXuppExt[round][i][j].get(GRB.DoubleAttr.Xn));
				DKnownEncuppExtValue[round][i][j] = (int) Math.round(DKnownEncuppExt[round][i][j].get(GRB.DoubleAttr.Xn));
				DXFixeduppExtValue[round][i][j] = (int) Math.round(DXFixeduppExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DYFixeduppExtValue[round][i][j] = (int) Math.round(DYFixeduppExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DXFilteruppExtValue[round][i][j] = (int) Math.round(DXFilteruppExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DYFilteruppExtValue[round][i][j] = (int) Math.round(DYFilteruppExt[round][i][j].get(GRB.DoubleAttr.Xn));  
                DXisFilteruppExtValue[round][i][j] = (int) Math.round(DXisFilteruppExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DYisFilteruppExtValue[round][i][j] = (int) Math.round(DYisFilteruppExt[round][i][j].get(GRB.DoubleAttr.Xn));  
                DXGuessuppExtValue[round][i][j] = (int) Math.round(DXGuessuppExt[round][i][j].get(GRB.DoubleAttr.Xn));
                DYGuessuppExtValue[round][i][j] = (int) Math.round(DYGuessuppExt[round][i][j].get(GRB.DoubleAttr.Xn));
            	if (i<2)
            		DSTKuppExtValue[round][i][j] = (int) Math.round(DSTKuppExt[round][i][j].get(GRB.DoubleAttr.Xn));
            }
      for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                DXuppExtValue[nExtbRounds][i][j] = (int) Math.round(DXuppExt[nExtbRounds][i][j].get(GRB.DoubleAttr.Xn));
      }
      for (int round = 0; round < nbRounds; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            DXupperValue[round][i][j] = (int) Math.round(DXupper[round][i][j].get(GRB.DoubleAttr.Xn));
            freeXupperValue[round][i][j] = (int) Math.round(freeXupper[round][i][j].get(GRB.DoubleAttr.Xn));
            freeSBupperValue[round][i][j] = (int) Math.round(freeSBupper[round][i][j].get(GRB.DoubleAttr.Xn));
            DXlowerValue[round][i][j] = (int) Math.round(DXlower[round][i][j].get(GRB.DoubleAttr.Xn));
            freeXlowerValue[round][i][j] = (int) Math.round(freeXlower[round][i][j].get(GRB.DoubleAttr.Xn));
            freeSBlowerValue[round][i][j] = (int) Math.round(freeSBlower[round][i][j].get(GRB.DoubleAttr.Xn));
            isTableValue[round][i][j] = (int) Math.round(isTable[round][i][j].get(GRB.DoubleAttr.Xn));
            isDDT2Value[round][i][j] = (int) Math.round(isDDT2[round][i][j].get(GRB.DoubleAttr.Xn));
          }
      for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            DXupperValue[nbRounds][i][j] = (int) Math.round(DXupper[nbRounds][i][j].get(GRB.DoubleAttr.Xn));       
            DXlowerValue[nbRounds][i][j] = (int) Math.round(DXlower[nbRounds][i][j].get(GRB.DoubleAttr.Xn));            
          }
      // Compute scalar bounds from solution values
      double weightmk = (blocksize == 4) ? 4.0 : 8.0;
      int rb_val = 0, rf_val = 0, rb_prime_val = 0, rf_prime_val = 0;
      int mb_val = 0, mb_prime_val = 0, mf_val = 0, mf_prime_val = 0;
      for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
        if (DXuppExtValue.length > 1) rb_val += DXuppExtValue[1][i][j];
      }
      rb_val = (int)(weightmk * rb_val);
      // rf = blocksize*16 - weightmk * sum(DWFixedlowExt[nExtfRounds-1])
      int rf_sum = 0;
      if (nExtfRounds - 1 >= 0 && nExtfRounds - 1 < DWFixedlowExtValue.length) {
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) rf_sum += DWFixedlowExtValue[nExtfRounds - 1][i][j];
      }
      rf_val = blocksize * 16 - (int)(weightmk * rf_sum);
      int ebLen = Math.min(nExtbRounds, DXisFilteruppExtValue.length);
      for (int round = 0; round < ebLen; round++)
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
          rb_prime_val += DXisFilteruppExtValue[round][i][j] + DYisFilteruppExtValue[round][i][j];
        }
      rb_prime_val = (int)(weightmk * rb_prime_val);
      int efLen = Math.min(nExtfRounds, DXisFilterlowExtValue.length);
      for (int round = 0; round < efLen; round++)
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
          rf_prime_val += DXisFilterlowExtValue[round][i][j] + DWisFilterlowExtValue[round][i][j];
        }
      rf_prime_val = (int)(weightmk * rf_prime_val);
      int mbEbLen = Math.min(nExtbRounds - 1, DKnownEncuppExtValue.length);
      for (int round = 0; round < mbEbLen; round++)
        for (int i = 0; i < 2; i++) for (int j = 0; j < 4; j++) {
          mb_val += DKnownEncuppExtValue[round][i][j];
          mb_prime_val += DYGuessuppExtValue[round][i][j];
        }
      mb_val = (int)(weightmk * mb_val);
      mb_prime_val = (int)(weightmk * mb_prime_val);
      for (int round = 0; round < DKnownDeclowExtValue.length; round++)
        for (int i = 0; i < 2; i++) for (int j = 0; j < 4; j++) {
          mf_val += DKnownDeclowExtValue[round][i][j];
          mf_prime_val += DXGuesslowExtValue[round][i][j];
        }
      mf_val = (int)(weightmk * mf_val);
      mf_prime_val = (int)(weightmk * mf_prime_val);

      int epsilonp_val = (int) Math.round(epsilonpVar.get(GRB.DoubleAttr.Xn));
	  int epsilonc_val = (int) Math.round(epsiloncVar.get(GRB.DoubleAttr.Xn));
	  int eps_val = (int) Math.round(EPSVar.get(GRB.DoubleAttr.Xn));
	  double t1_val = objective1Var.get(GRB.DoubleAttr.Xn);
	  double t2_val = objective2Var.get(GRB.DoubleAttr.Xn);
	  double t3_val = objective3Var.get(GRB.DoubleAttr.Xn);
	  double pool_obj_val = obj[0].get(GRB.DoubleAttr.Xn);

      System.out.println(String.format(
		"Step1 solution #%d: objective=%.2f, T1=%.2f, T2=%.2f, T3=%.2f, EPS=%d, mb=%d, mb_prime=%d, mf=%d, mf_prime=%d",
		solutionNumber, pool_obj_val, t1_val, t2_val, t3_val, eps_val, mb_val, mb_prime_val, mf_val, mf_prime_val));
      printFilterActivationOrder(solutionNumber);

      return new Step1Solution(nbRounds, nExtbRounds, nExtfRounds, regime, pool_obj_val, DXupperValue, freeXupperValue, freeSBupperValue, (regime == 0) ? null : DTKupper.getValue(), DXlowerValue, freeXlowerValue, freeSBlowerValue, (regime == 0) ? null : DTKlower.getValue(), isTableValue, isDDT2Value, DXlowExtValue, DSTKlowExtValue, DKnownDeclowExtValue, DXuppExtValue, DSTKuppExtValue, DKnownEncuppExtValue, DXFixedlowExtValue, DWFixedlowExtValue, DXFilterlowExtValue, DWFilterlowExtValue, DXisFilterlowExtValue, DWisFilterlowExtValue, DXGuesslowExtValue, DWGuesslowExtValue, DXFixeduppExtValue, DYFixeduppExtValue, DXFilteruppExtValue, DYFilteruppExtValue, DXisFilteruppExtValue, DYisFilteruppExtValue, DXGuessuppExtValue, DYGuessuppExtValue, blocksize, rb_val, rb_prime_val, rf_val, rf_prime_val, mb_val, mb_prime_val, mf_val, mf_prime_val, t1_val, t2_val, eps_val, epsilonp_val, epsilonc_val, t3_val);
    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
      return null; // Can't access
    }
  }


private void articleTrails() throws GRBException {
    // -------- Article trails --------
    int maxlanes = 16;
	
    if (blocksize==4 && regime == 2 && nbRounds == 18) {
      //maxlanes=3;
	  /* old distinguisher 55.34*/
	  for (int round = 0; round < 9; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            if (round == 0 && i == 3 && j == 3 ||
			
				round == 1 && i == 1 && j == 2 ||
				round == 1 && i == 3 && j == 2 ||
				
				round == 2 && i == 0 && j == 1 ||
				round == 2 && i == 2 && j == 3 ||
				
				round == 3 && i == 1 && j == 1 ||
				
				round == 7 && i == 2 && j == 2 ||
           
				round == 8 && i == 0 && j == 0 ||
				round == 8 && i == 3 && j == 0)
              model.addConstr(DXupper[round][i][j], GRB.EQUAL, 1.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_1");
            else
              model.addConstr(DXupper[round][i][j], GRB.EQUAL, 0.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_0");
          }
      for (int round = 12; round < 18; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            if (round == 12 && i == 3 && j == 2 ||
				round == 13 && i == 0 && j == 1)
              model.addConstr(DXlower[round][i][j], GRB.EQUAL, 1.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_1");
            else
              model.addConstr(DXlower[round][i][j], GRB.EQUAL, 0.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_0");
          }
	  
	  /* new distinguisher 53.72*/
	  /*
      for (int round = 0; round < 9; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            if (round == 0 && i == 3 && j == 3 ||
			
				round == 1 && i == 1 && j == 2 ||
				round == 1 && i == 3 && j == 2 ||
				
				round == 2 && i == 0 && j == 1 ||
				round == 2 && i == 2 && j == 3 ||
				
				round == 3 && i == 1 && j == 1 ||
				
				round == 7 && i == 2 && j == 2 ||
           
				round == 8 && i == 0 && j == 0 ||
				round == 8 && i == 3 && j == 0)
              model.addConstr(DXupper[round][i][j], GRB.EQUAL, 1.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_1");
            else
              model.addConstr(DXupper[round][i][j], GRB.EQUAL, 0.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_0");
          }
      for (int round = 12; round < 18; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            if (round == 12 && i == 3 && j == 0 ||
				round == 13 && i == 0 && j == 3)
              model.addConstr(DXlower[round][i][j], GRB.EQUAL, 1.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_1");
            else
              model.addConstr(DXlower[round][i][j], GRB.EQUAL, 0.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_0");
          }*/
    }
	// SKINNY-64-192  57.36
    else if (blocksize==4 && regime == 3 && nbRounds == 22) {
      //maxlanes=1;
      for (int round = 0; round < 11; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            if (round == 0 && i == 1 && j == 1 ||
                round == 0 && i == 1 && j == 3 ||
				round == 0 && i == 2 && j == 0 ||
                round == 0 && i == 3 && j == 3 ||
				
                round == 1 && i == 3 && j == 2 ||
				
				round == 2 && i == 0 && j == 1 ||
				
                round == 9 && i == 2 && j == 1 ||

				round == 10 && i == 0 && j == 3 ||
				round == 10 && i == 2 && j == 3 ||
				round == 10 && i == 3 && j == 3)
              model.addConstr(DXupper[round][i][j], GRB.EQUAL, 1.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_1");
            else
              model.addConstr(DXupper[round][i][j], GRB.EQUAL, 0.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_0");
          }
      for (int round = 14; round < 22; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            if (round == 14 && i == 3 && j == 2 ||
				round == 15 && i == 0 && j == 1)
              model.addConstr(DXlower[round][i][j], GRB.EQUAL, 1.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_1");
            else
              model.addConstr(DXlower[round][i][j], GRB.EQUAL, 0.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_0");
          }
      
      model.addConstr(freeSBlower[9][2][1], GRB.EQUAL, 0.0, "fix_freeSBlower_9_2_1_0");
      model.addConstr(freeXupper[14][3][2], GRB.EQUAL, 1.0, "fix_freeXupper_14_3_2_1");
    }
	// SKINNY-128-256  99.46
    else if (blocksize==8 && regime == 2 && nbRounds == 19) {
		//maxlanes=1;
		for (int round = 0; round < 8; round++)
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < 4; j++) {
					if (round == 0 && i == 3 && j == 3 ||
						round == 1 && i == 0 && j == 2 ||
						round == 6 && i == 2 && j == 3 ||
						round == 7 && i == 0 && j == 1 ||
						round == 7 && i == 2 && j == 1 ||
                		round == 7 && i == 3 && j == 1)
							model.addConstr(DXupper[round][i][j], GRB.EQUAL, 1.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_1");
					else
							model.addConstr(DXupper[round][i][j], GRB.EQUAL, 0.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_0");
				}
		
		for (int round = 13; round < 19; round++)
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < 4; j++) {
					if (round == 13 && i == 3 && j == 0 ||
						round == 14 && i == 0 && j == 3)
						model.addConstr(DXlower[round][i][j], GRB.EQUAL, 1.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_1");
					else
						model.addConstr(DXlower[round][i][j], GRB.EQUAL, 0.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_0");
				}
		
		model.addConstr(freeSBlower[6][2][3], GRB.EQUAL, 0.0, "fix_freeSBlower_6_2_3_0");
		model.addConstr(freeXupper[10][2][3], GRB.EQUAL, 0.0, "fix_freeXupper_10_2_3_0");
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++) {
					model.addConstr(freeXupper[11][i][j], GRB.EQUAL, 1.0, "fix_freeXupper_11_" + i + "_" + j + "_1");
			}
		
    }  
    // SKINNY-128-384  105.70
    else if (blocksize==8 && regime == 3 && nbRounds == 23) {
      maxlanes=1;
      for (int round = 0; round < 10; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            if (round == 0 && i == 3 && j == 3 ||
                round == 1 && i == 0 && j == 2 ||
                round == 8 && i == 2 && j == 2 ||
                round == 9 && i == 0 && j == 0 ||
                round == 9 && i == 2 && j == 0 ||
                round == 9 && i == 3 && j == 0)

              model.addConstr(DXupper[round][i][j], GRB.EQUAL, 1.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_1");
            else
              model.addConstr(DXupper[round][i][j], GRB.EQUAL, 0.0, "fix_DXupper_" + round + "_" + i + "_" + j + "_0");
          }
		  
      for (int round = 15; round < 23; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
			if (round == 15 && i == 3 && j == 2 ||
			    round == 16 && i == 0 && j == 1)
				
              model.addConstr(DXlower[round][i][j], GRB.EQUAL, 1.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_1");
            else
              model.addConstr(DXlower[round][i][j], GRB.EQUAL, 0.0, "fix_DXlower_" + round + "_" + i + "_" + j + "_0");
          }
	  
	  model.addConstr(freeSBlower[9][0][0], GRB.EQUAL, 1.0, "fix_freeSBlower_9_0_0_1");
	  model.addConstr(freeSBlower[9][2][0], GRB.EQUAL, 1.0, "fix_freeSBlower_9_2_0_1");
	  model.addConstr(freeSBlower[9][3][0], GRB.EQUAL, 1.0, "fix_freeSBlower_9_3_0_1");
	  for (int i = 0; i < 4; i++)
		for (int j = 0; j < 4; j++) {
			model.addConstr(freeXupper[14][i][j], GRB.EQUAL, 1.0, "fix_freeXupper_14_" + i + "_" + j + "_1");
		}
    }
    GRBLinExpr sumLanesupp = new GRBLinExpr();
    GRBLinExpr sumLaneslow = new GRBLinExpr();
    for (int i = 0; i < 16; i++) {
        sumLanesupp.addTerm(1.0, DTKupper.lanes[i]);
        sumLaneslow.addTerm(1.0, DTKlower.lanes[i]);
    }
    model.addConstr(sumLanesupp, GRB.LESS_EQUAL, maxlanes, "");
    model.addConstr(sumLaneslow, GRB.LESS_EQUAL, maxlanes, "");
  }
  


}
