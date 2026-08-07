package boomerangsearch.step1;

import gurobi.*;

import java.util.List;
import java.util.ArrayList;
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

  int EPS_MAX_KEYS_BETWEEN_FILTERS = Math.max(1,
    Integer.getInteger("eps.max.keys.between.filters",
      Integer.getInteger("eps.keys.per.layer", 4)));

  private GRBVar objective1Var;
  private GRBVar objective2Var;
  private GRBVar objective3Var;
  private GRBVar EPSVar;
  private GRBVar epsilonpVar;
  private GRBVar epsiloncVar;
  private EpsPart lowerEpsPart;
  private EpsPart upperEpsPart;

  /**
   * @param env the Gurobi environment
   * @param nbRounds the number of rounds of the boomerang
   * @param regime the regime of the analysis. 0, 1, 2 or 3 for SK, TK1, TK2 or TK3
   */
  public Step1(final GRBEnv env, final int nbRounds, final int nExtbRounds, final int nExtfRounds, final int regime, final int blocksize) throws GRBException {
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
	double weight_table = 0;
    double weight_ddt2 = 0;
    double n = 0;
    if (blocksize == 4) {
        weightmk = 4.0;
		weight_table = 1.0;
        weight_ddt2 = 1.0;
        n = 64;
    } 
    else if (blocksize == 8) {
        weightmk = 8.0;
        weight_table = 1.0;
        weight_ddt2 = 1.0;
		n = 128;
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

    obj[0] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "obj0");

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
	
    /*active<15*/
    GRBLinExpr sumActive = new GRBLinExpr();
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          sumActive.addTerm(1.0, DXuppExt[1][i][j]);
        }
    model.addConstr(sumActive, GRB.LESS_EQUAL, 15.0, "");
	
	GRBLinExpr cActive = new GRBLinExpr();
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          cActive.addTerm(1.0, DXlowExt[0][i][j]);
        }
    model.addConstr(cActive, GRB.GREATER_EQUAL, 1.0, "");
	
	
	/*upper part in  distinguisher*/
	int maxStart = (int)(nbRounds / 2) - (2 * regime - 2);
	if (maxStart >= 0){
		 GRBVar[] zeroStart = new GRBVar[maxStart + 1];

		GRBLinExpr atLeastOne = new GRBLinExpr();

		for (int r = 0; r <= maxStart; r++) {
			zeroStart[r] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "upper_zero_start_" + r);

			atLeastOne.addTerm(1.0, zeroStart[r]);

			GRBLinExpr windowSum = new GRBLinExpr();
			for (int rr = r; rr <= r + 2 * regime - 2; rr++) {
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 4; j++) {
						windowSum.addTerm(1.0, DXupper[rr][i][j]);
					}
				}
			}
			windowSum.addTerm((2 * regime - 1) * 16, zeroStart[r]);
			model.addConstr(windowSum, GRB.LESS_EQUAL, (2 * regime - 1) * 16, "upper_zero_window_" + r);
		}
		model.addConstr(atLeastOne, GRB.GREATER_EQUAL, 1.0, "upper_has_three_consecutive_zero_rounds");
	}
	
	/*lower part in distinguisher*/
	int maxStartlower = nbRounds - (2 * regime - 1);
	if (maxStartlower > (int)(nbRounds / 2)){
		 GRBVar[] zeroStartlower = new GRBVar[maxStartlower - (int)(nbRounds / 2) + 1];

		GRBLinExpr atLeastOnelower = new GRBLinExpr();

		for (int r = 0; r <= maxStartlower - (int)(nbRounds / 2); r++) {
			zeroStartlower[r] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "lower_zero_start_" + (r + (int)(nbRounds / 2)));

			atLeastOnelower.addTerm(1.0, zeroStartlower[r]);

			GRBLinExpr Sumlower = new GRBLinExpr();
			for (int rr = r + (int)(nbRounds / 2); rr <= r + (int)(nbRounds / 2) + 2 * regime - 2; rr++) {
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 4; j++) {
						Sumlower.addTerm(1.0, DXlower[rr][i][j]);
					}
				}
			}
			Sumlower.addTerm((2 * regime - 1) * 16, zeroStartlower[r]);
			model.addConstr(Sumlower, GRB.LESS_EQUAL, (2 * regime - 1) * 16, "lower_zero_" + (r + (int)(nbRounds / 2)));
		}

		model.addConstr(atLeastOnelower, GRB.GREATER_EQUAL, 1.0, "lower_has_consecutive_zero_rounds");
	}
	
	/*
	GRBLinExpr c = new GRBLinExpr();
	for (int round  = 1; round <= 2 * regime; round++)
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++) {
				c.addTerm(1.0, DXlower[nbRounds-round][i][j]);
			}
    model.addConstr(c, GRB.EQUAL, 0, "");*/
    
    
    /*bound of distinguisher*/
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
	
	GRBLinExpr D = new GRBLinExpr();
	for (int round = 0; round < nbRounds; round++)
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
			D.addTerm(weight_table, isTable[round][i][j]);
			D.addTerm(weight_ddt2, isDDT2[round][i][j]);
		}
	D.addConstant(0.5 * n);
	
	objective1Var = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "T1");
	GRBLinExpr obj1 = new GRBLinExpr();
	obj1.multAdd(1.0, D);
	obj1.addConstant(2.0);
	obj1.addTerm(1.0, mb_prime);
	obj1.addTerm(1.0, mf_prime);
	model.addConstr(objective1Var, GRB.EQUAL, obj1, "");
	

	objective2Var = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "T2");
	
	GRBVar filterp = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0.0, GRB.INTEGER, "");
	GRBLinExpr obj2_0 = new GRBLinExpr();
	obj2_0.addTerm(1.0, rb);
	obj2_0.addTerm(-1.0, rb_prime);
	model.addConstr(filterp, GRB.EQUAL, obj2_0, "");
	
	GRBVar filterc = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0.0, GRB.INTEGER, "");
	GRBLinExpr obj2_1 = new GRBLinExpr();
	obj2_1.addTerm(1.0, rf);
	obj2_1.addTerm(-1.0, rf_prime);
	obj2_1.multAdd(1.0, D);
	obj2_1.addConstant(-1.0 * n);;
	model.addConstr(filterc, GRB.EQUAL, obj2_1, "");
	
	GRBVar filter = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0.0, GRB.INTEGER, "");
	model.addGenConstrMin(filter, new GRBVar[]{filterp, filterc}, GRB.INFINITY, "");
	
	GRBLinExpr obj2 = new GRBLinExpr();
	obj2.multAdd(1.0, D);
	obj2.addTerm(1.0, mb_prime);
	obj2.addTerm(1.0, mf_prime);
	obj2.addTerm(1.0, filter);
	obj2.addConstant(1.0);
	model.addConstr(objective2Var, GRB.EQUAL, obj2, "");
	
	
	objective3Var = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "T3");
	EPSVar = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, "EPS");

	// EPS is one continuous process.  The lower extension is guessed first;
	// the upper extension starts from the exact score left by the lower part.
	// Each model layer is a filtering event, not an arbitrary key-guess step.
	GRBLinExpr zeroScore = new GRBLinExpr();
	lowerEpsPart = addContinuousEpsPart(
	  "low", buildLowerKeyCells(), buildLowerFilterCells(), zeroScore, weightmk);
	epsiloncVar = lowerEpsPart.peak;
	upperEpsPart = addContinuousEpsPart(
	  "upp", buildUpperKeyCells(), buildUpperFilterCells(), lowerEpsPart.finalScore, weightmk);
	epsilonpVar = upperEpsPart.peak;
	model.addGenConstrMax(EPSVar, new GRBVar[]{epsiloncVar, epsilonpVar}, 0.0, "EPS_continuous_max");
	
	GRBLinExpr obj3 = new GRBLinExpr();
	obj3.addTerm(1.0, mb_prime);
	obj3.addTerm(1.0, mf_prime);
	obj3.multAdd(2.0, D);
	obj3.addConstant(- 2.0 * n - (int)(Math.log(16 * (nExtbRounds + nbRounds + nExtfRounds)) / Math.log(2.0)));
	obj3.addTerm(2.0, rb);
	obj3.addTerm(-2.0, rb_prime);
	obj3.addTerm(2.0, rf);
	obj3.addTerm(-2.0, rf_prime);
	obj3.addTerm(1.0, EPSVar);
	model.addConstr(objective3Var, GRB.EQUAL, obj3, "");
	
    objective.addTerm(1.0, obj[0]);
    model.addConstr(objective, GRB.GREATER_EQUAL, objective1Var, "");
    model.addConstr(objective, GRB.GREATER_EQUAL, objective2Var, "");
    model.addConstr(objective, GRB.GREATER_EQUAL, objective3Var, "");
    
    model.setObjective(objective, GRB.MINIMIZE);
  }




  /** A compact integer triple used for key-cell dependencies: {round, row, col}. */
  private static int[] dep(final int r, final int i, final int j) {
    return new int[]{r, i, j};
  }

  private static final class KeyCell {
    final int round;
    final int row;
    final int column;
    final GRBVar known;
    final GRBVar initialGuess;

    KeyCell(final int round, final int row, final int column,
            final GRBVar known, final GRBVar initialGuess) {
      this.round = round;
      this.row = row;
      this.column = column;
      this.known = known;
      this.initialGuess = initialGuess;
    }
  }

  private static final class FilterCell {
    final String unit;
    final int round;
    final int row;
    final int column;
    final GRBVar filter;
    final GRBVar isFilter;
    final List<int[]> dependencies;

    FilterCell(final String unit, final int round, final int row, final int column,
               final GRBVar filter, final GRBVar isFilter,
               final List<int[]> dependencies) {
      this.unit = unit;
      this.round = round;
      this.row = row;
      this.column = column;
      this.filter = filter;
      this.isFilter = isFilter;
      this.dependencies = dependencies;
    }
  }

  private static final class EpsPart {
    final List<KeyCell> keys;
    final List<FilterCell> filters;
    final GRBVar[][] guessed;
    final GRBVar[][] activated;
    final GRBVar[] eventUsed;
    final GRBVar peak;
    final GRBLinExpr finalScore;

    EpsPart(final List<KeyCell> keys, final List<FilterCell> filters,
            final GRBVar[][] guessed, final GRBVar[][] activated,
            final GRBVar[] eventUsed, final GRBVar peak,
            final GRBLinExpr finalScore) {
      this.keys = keys;
      this.filters = filters;
      this.guessed = guessed;
      this.activated = activated;
      this.eventUsed = eventUsed;
      this.peak = peak;
      this.finalScore = finalScore;
    }
  }

  /** Dependencies of one upper-side late filter on extra NeedY key cells. */
  private List<int[]> upperNeedYDeps(final boolean xSide, final int fr, final int fi, final int fj) {
    final int rounds = nExtbRounds;
    final int keyRounds = Math.max(0, nExtbRounds - 1);
    final List<int[]> deps = new ArrayList<int[]>();
    if (rounds <= 0 || keyRounds <= 0) return deps;

    boolean[][][] xIs = new boolean[rounds][4][4];
    boolean[][][] yIs = new boolean[rounds][4][4];
    boolean[][][] dxGuess = new boolean[rounds][4][4];
    boolean[][][] dyGuess = new boolean[rounds][4][4];

    if (xSide) xIs[fr][fi][fj] = true;
    else       yIs[fr][fi][fj] = true;

    final int last = rounds - 1;
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        dxGuess[last][i][j] = yIs[last][i][j];
      }
    }

    for (int r = last - 1; r >= 0; r--) {
      for (int j = 0; j < 4; j++) {
        dyGuess[r][0][j] = dxGuess[r + 1][0][j] || dxGuess[r + 1][1][j] || dxGuess[r + 1][3][j];
        dyGuess[r][1][j] = dxGuess[r + 1][2][(j + 1) % 4];
        dyGuess[r][2][j] = dxGuess[r + 1][0][(j + 2) % 4] || dxGuess[r + 1][2][(j + 2) % 4] || dxGuess[r + 1][3][(j + 2) % 4];
        dyGuess[r][3][j] = dxGuess[r + 1][0][(j + 3) % 4];
      }
      for (int j = 0; j < 4; j++) {
        dxGuess[r][0][j] = xIs[r + 1][0][j] || xIs[r + 1][1][j] || xIs[r + 1][3][j]
          || dyGuess[r][0][j] || yIs[r][0][j];
        dxGuess[r][1][j] = xIs[r + 1][2][(j + 1) % 4]
          || dyGuess[r][1][j] || yIs[r][1][j];
        dxGuess[r][2][j] = xIs[r + 1][0][(j + 2) % 4] || xIs[r + 1][2][(j + 2) % 4] || xIs[r + 1][3][(j + 2) % 4]
          || dyGuess[r][2][j] || yIs[r][2][j];
        dxGuess[r][3][j] = xIs[r + 1][0][(j + 3) % 4]
          || dyGuess[r][3][j] || yIs[r][3][j];
      }
    }

    for (int r = 0; r < keyRounds; r++) {
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 4; j++) {
          if (dyGuess[r][i][j]) deps.add(dep(r, i, j));
        }
      }
    }
    return deps;
  }

  /** Dependencies of one lower-side late filter on extra NeedX key cells. */
  private List<int[]> lowerNeedXDeps(final boolean xSide, final int fr, final int fi, final int fj) {
    final int rounds = nExtfRounds;
    final List<int[]> deps = new ArrayList<int[]>();
    if (rounds <= 0) return deps;

    boolean[][][] xIs = new boolean[rounds][4][4];
    boolean[][][] wIs = new boolean[rounds][4][4];
    boolean[][][] dxGuess = new boolean[rounds][4][4];
    boolean[][][] dwGuess = new boolean[rounds][4][4];

    if (xSide) xIs[fr][fi][fj] = true;
    else       wIs[fr][fi][fj] = true;

    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        dxGuess[0][i][j] = xIs[0][i][j];
      }
    }

    final int[] shift = {0, 3, 2, 1};
    for (int r = 0; r < rounds; r++) {
      for (int j = 0; j < 4; j++) {
        for (int i = 0; i < 4; i++) {
          dwGuess[r][i][j] = wIs[r][i][j] || dxGuess[r][i][(j + shift[i]) % 4];
        }
      }
      if (r < rounds - 1) {
        for (int j = 0; j < 4; j++) {
          dxGuess[r + 1][0][j] = dwGuess[r][3][j] || xIs[r + 1][0][j];
          dxGuess[r + 1][1][j] = dwGuess[r][0][j] || dwGuess[r][1][j] || dwGuess[r][2][j] || xIs[r + 1][1][j];
          dxGuess[r + 1][2][j] = dwGuess[r][1][j] || xIs[r + 1][2][j];
          dxGuess[r + 1][3][j] = dwGuess[r][1][j] || dwGuess[r][2][j] || dwGuess[r][3][j] || xIs[r + 1][3][j];
        }
      }
    }

    for (int r = 0; r < rounds; r++) {
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 4; j++) {
          if (dxGuess[r][i][j]) deps.add(dep(r, i, j));
        }
      }
    }
    return deps;
  }

  private List<KeyCell> buildLowerKeyCells() {
    final List<KeyCell> keys = new ArrayList<KeyCell>();
    for (int r = 0; r < nExtfRounds; r++) {
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 4; j++) {
          keys.add(new KeyCell(r, i, j, DKnownDeclowExt[r][i][j], DXGuesslowExt[r][i][j]));
        }
      }
    }
    return keys;
  }

  private List<KeyCell> buildUpperKeyCells() {
    final List<KeyCell> keys = new ArrayList<KeyCell>();
    for (int r = 0; r < Math.max(0, nExtbRounds - 1); r++) {
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 4; j++) {
          keys.add(new KeyCell(r, i, j, DKnownEncuppExt[r][i][j], DYGuessuppExt[r][i][j]));
        }
      }
    }
    return keys;
  }

  private List<FilterCell> buildLowerFilterCells() {
    final List<FilterCell> filters = new ArrayList<FilterCell>();
    for (int r = 0; r < nExtfRounds; r++) {
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          filters.add(new FilterCell("LX", r, i, j,
            DXFilterlowExt[r][i][j], DXisFilterlowExt[r][i][j],
            lowerNeedXDeps(true, r, i, j)));
          filters.add(new FilterCell("LW", r, i, j,
            DWFilterlowExt[r][i][j], DWisFilterlowExt[r][i][j],
            lowerNeedXDeps(false, r, i, j)));
        }
      }
    }
    return filters;
  }

  private List<FilterCell> buildUpperFilterCells() {
    final List<FilterCell> filters = new ArrayList<FilterCell>();
    for (int r = 0; r < nExtbRounds; r++) {
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          filters.add(new FilterCell("UX", r, i, j,
            DXFilteruppExt[r][i][j], DXisFilteruppExt[r][i][j],
            upperNeedYDeps(true, r, i, j)));
          filters.add(new FilterCell("UY", r, i, j,
            DYFilteruppExt[r][i][j], DYisFilteruppExt[r][i][j],
            upperNeedYDeps(false, r, i, j)));
        }
      }
    }
    return filters;
  }

  private static int keyIndex(final List<KeyCell> keys, final int[] dependency) {
    for (int k = 0; k < keys.size(); k++) {
      final KeyCell key = keys.get(k);
      if (key.round == dependency[0] && key.row == dependency[1] && key.column == dependency[2]) {
        return k;
      }
    }
    throw new IllegalArgumentException("Unknown EPS key dependency: ("
      + dependency[0] + "," + dependency[1] + "," + dependency[2] + ")");
  }

  /*
   Build one continuous EPS phase.  A layer is a filtering event.  Therefore
   all keys added since the previous layer are exactly the keys guessed before
   this filtering event, and their count is bounded by
   EPS_MAX_KEYS_BETWEEN_FILTERS.  Every newly guessed key must be a dependency
   of at least one filter newly activated in the same event.
   */
  private EpsPart addContinuousEpsPart(
    final String prefix,
    final List<KeyCell> keys,
    final List<FilterCell> filters,
    final GRBLinExpr initialScore,
    final double weightmk
  ) throws GRBException {
    final int eventLayers = keys.size();
    final GRBVar peak = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, prefix + "_EPS_peak");
    final GRBVar[][] guessed = new GRBVar[eventLayers + 1][keys.size()];
    final GRBVar[][] activated = new GRBVar[eventLayers + 1][filters.size()];
    final GRBVar[] eventUsed = new GRBVar[eventLayers + 1];

    final int[][] dependencyIndexes = new int[filters.size()][];
    for (int f = 0; f < filters.size(); f++) {
      final List<int[]> deps = filters.get(f).dependencies;
      dependencyIndexes[f] = new int[deps.size()];
      for (int d = 0; d < deps.size(); d++) {
        dependencyIndexes[f][d] = keyIndex(keys, deps.get(d));
      }
    }

    for (int k = 0; k < keys.size(); k++) {
      final KeyCell key = keys.get(k);
      model.addConstr(key.initialGuess, GRB.LESS_EQUAL, key.known,
        prefix + "_initial_guess_known_" + key.round + "_" + key.row + "_" + key.column);
    }

    for (int t = 0; t <= eventLayers; t++) {
      for (int k = 0; k < keys.size(); k++) {
        final KeyCell key = keys.get(k);
        guessed[t][k] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
          prefix + "_G_" + t + "_" + key.round + "_" + key.row + "_" + key.column);

        if (t == 0) {
          model.addConstr(guessed[t][k], GRB.EQUAL, 0.0,
            prefix + "_G0_" + key.round + "_" + key.row + "_" + key.column);
        } else {
          model.addConstr(guessed[t - 1][k], GRB.LESS_EQUAL, guessed[t][k],
            prefix + "_G_mono_" + t + "_" + key.round + "_" + key.row + "_" + key.column);
        }

        GRBLinExpr onlyExtraKnown = new GRBLinExpr();
        onlyExtraKnown.addTerm(1.0, guessed[t][k]);
        onlyExtraKnown.addTerm(1.0, key.initialGuess);
        onlyExtraKnown.addTerm(-1.0, key.known);
        model.addConstr(onlyExtraKnown, GRB.LESS_EQUAL, 0.0,
          prefix + "_G_known_" + t + "_" + key.round + "_" + key.row + "_" + key.column);
      }

      for (int f = 0; f < filters.size(); f++) {
        final FilterCell filter = filters.get(f);
        activated[t][f] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
          prefix + "_A_" + t + "_" + filter.unit + "_"
            + filter.round + "_" + filter.row + "_" + filter.column);

        model.addConstr(activated[t][f], GRB.LESS_EQUAL, filter.filter,
          prefix + "_A_filter_" + t + "_" + f);

        GRBLinExpr lateOnly = new GRBLinExpr();
        lateOnly.addTerm(1.0, activated[t][f]);
        lateOnly.addTerm(1.0, filter.isFilter);
        model.addConstr(lateOnly, GRB.LESS_EQUAL, 1.0,
          prefix + "_A_late_" + t + "_" + f);

        if (t > 0) {
          model.addConstr(activated[t - 1][f], GRB.LESS_EQUAL, activated[t][f],
            prefix + "_A_mono_" + t + "_" + f);
        }

        GRBLinExpr immediate = new GRBLinExpr();
        immediate.addTerm(1.0, activated[t][f]);
        immediate.addTerm(-1.0, filter.filter);
        immediate.addTerm(1.0, filter.isFilter);

        for (final int k : dependencyIndexes[f]) {
          final KeyCell key = keys.get(k);

          GRBLinExpr covered = new GRBLinExpr();
          covered.addTerm(1.0, activated[t][f]);
          covered.addTerm(-1.0, guessed[t][k]);
          covered.addTerm(-1.0, key.initialGuess);
          model.addConstr(covered, GRB.LESS_EQUAL, 0.0,
            prefix + "_dep_covered_" + t + "_" + f + "_" + k);

          immediate.addTerm(-1.0, guessed[t][k]);
          immediate.addTerm(-1.0, key.initialGuess);
        }
        model.addConstr(immediate, GRB.GREATER_EQUAL, -dependencyIndexes[f].length,
          prefix + "_use_immediately_" + t + "_" + f);

        if (t == eventLayers) {
          GRBLinExpr useAll = new GRBLinExpr();
          useAll.addTerm(1.0, activated[t][f]);
          useAll.addTerm(-1.0, filter.filter);
          useAll.addTerm(1.0, filter.isFilter);
          model.addConstr(useAll, GRB.GREATER_EQUAL, 0.0,
            prefix + "_use_all_" + f);
        }
      }

      GRBLinExpr score = new GRBLinExpr(initialScore);
      for (int k = 0; k < keys.size(); k++) {
        score.addTerm(weightmk, guessed[t][k]);
      }
      for (int f = 0; f < filters.size(); f++) {
        score.addTerm(-2.0 * weightmk, activated[t][f]);
      }
      model.addConstr(peak, GRB.GREATER_EQUAL, score, prefix + "_peak_at_event_" + t);
    }

    for (int t = 1; t <= eventLayers; t++) {
      eventUsed[t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, prefix + "_event_used_" + t);

      GRBLinExpr newKeys = new GRBLinExpr();
      for (int k = 0; k < keys.size(); k++) {
        newKeys.addTerm(1.0, guessed[t][k]);
        newKeys.addTerm(-1.0, guessed[t - 1][k]);
      }

      GRBLinExpr newFilters = new GRBLinExpr();
      for (int f = 0; f < filters.size(); f++) {
        newFilters.addTerm(1.0, activated[t][f]);
        newFilters.addTerm(-1.0, activated[t - 1][f]);
      }

      GRBLinExpr filterEventUpper = new GRBLinExpr(newFilters);
      filterEventUpper.addTerm(-filters.size(), eventUsed[t]);
      model.addConstr(filterEventUpper, GRB.LESS_EQUAL, 0.0,
        prefix + "_event_filter_ub_" + t);

      GRBLinExpr filterEventLower = new GRBLinExpr();
      filterEventLower.addTerm(1.0, eventUsed[t]);
      filterEventLower.multAdd(-1.0, newFilters);
      model.addConstr(filterEventLower, GRB.LESS_EQUAL, 0.0,
        prefix + "_event_filter_lb_" + t);

      GRBLinExpr keyLimit = new GRBLinExpr(newKeys);
      keyLimit.addTerm(-EPS_MAX_KEYS_BETWEEN_FILTERS, eventUsed[t]);
      model.addConstr(keyLimit, GRB.LESS_EQUAL, 0.0,
        prefix + "_keys_between_filters_" + t);

      GRBLinExpr eventNeedsKey = new GRBLinExpr();
      eventNeedsKey.addTerm(1.0, eventUsed[t]);
      eventNeedsKey.multAdd(-1.0, newKeys);
      model.addConstr(eventNeedsKey, GRB.LESS_EQUAL, 0.0,
        prefix + "_event_has_key_" + t);

      if (t > 1) {
        model.addConstr(eventUsed[t], GRB.LESS_EQUAL, eventUsed[t - 1],
          prefix + "_pack_events_" + t);
      }

      // A key cannot be guessed early for a future filter.  It must occur in
      // the dependency set of a filtering unit activated in this exact event.
      for (int k = 0; k < keys.size(); k++) {
        GRBLinExpr servesCurrentFilter = new GRBLinExpr();
        servesCurrentFilter.addTerm(1.0, guessed[t][k]);
        servesCurrentFilter.addTerm(-1.0, guessed[t - 1][k]);
        for (int f = 0; f < filters.size(); f++) {
          boolean depends = false;
          for (final int dependencyIndex : dependencyIndexes[f]) {
            if (dependencyIndex == k) {
              depends = true;
              break;
            }
          }
          if (depends) {
            servesCurrentFilter.addTerm(-1.0, activated[t][f]);
            servesCurrentFilter.addTerm(1.0, activated[t - 1][f]);
          }
        }
        model.addConstr(servesCurrentFilter, GRB.LESS_EQUAL, 0.0,
          prefix + "_new_key_serves_event_" + t + "_" + k);
      }
    }

    // After all filtering events, any remaining involved keys that do not help
    // a late filter are guessed.  No filter can fire in this tail, so the score
    // is monotone there and only its final value can be the peak.
    GRBLinExpr finalScore = new GRBLinExpr(initialScore);
    for (final KeyCell key : keys) {
      finalScore.addTerm(weightmk, key.known);
      finalScore.addTerm(-weightmk, key.initialGuess);
    }
    for (final FilterCell filter : filters) {
      finalScore.addTerm(-2.0 * weightmk, filter.filter);
      finalScore.addTerm(2.0 * weightmk, filter.isFilter);
    }
    model.addConstr(peak, GRB.GREATER_EQUAL, finalScore, prefix + "_final_score");

    return new EpsPart(keys, filters, guessed, activated, eventUsed, peak, finalScore);
  }

  private void appendFilterActivationOrder(final List<String> order, final EpsPart part) throws GRBException {
    if (part == null) return;
    for (int t = 0; t < part.activated.length; t++) {
      for (int f = 0; f < part.filters.size(); f++) {
        final int current = (int) Math.round(part.activated[t][f].get(GRB.DoubleAttr.Xn));
        final int previous = (t == 0)
          ? 0
          : (int) Math.round(part.activated[t - 1][f].get(GRB.DoubleAttr.Xn));
        if (current > previous) {
          final FilterCell filter = part.filters.get(f);
          order.add(filter.unit + ":(" + filter.round + ", " + filter.row + ", " + filter.column + ")");
        }
      }
    }
  }

  private String getFilterActivationOrder() throws GRBException {
    final List<String> order = new ArrayList<String>();
    appendFilterActivationOrder(order, lowerEpsPart);
    appendFilterActivationOrder(order, upperEpsPart);
    return order.isEmpty() ? "(none)" : String.join(" --- ", order);
  }

  public List<Step1Solution> solve(final int nbSolutions, final boolean nonOptimalSolutions, final int minObjValue, final int maxObjValue, final int nbThreads, final double timeLimit) throws GRBException {
    //model.read("tune1.prm");
    model.write("model.lp");
    model.set(GRB.IntParam.Threads, nbThreads);
    if (timeLimit > 0)
      model.set(GRB.DoubleParam.TimeLimit, timeLimit);
    if (minObjValue != -1) {
      model.addConstr(objective, GRB.GREATER_EQUAL, minObjValue, "objectivemin");
    }

    if (maxObjValue != -1) {
      model.addConstr(objective, GRB.LESS_EQUAL, maxObjValue, "objectivemax");

      // Target-search mode for -s1maxobj:
      // We only need one Step1 solution at or below the requested upper bound.
      // This is much faster than collecting a solution pool when the goal is to
      // beat a known objective value, e.g. -s1maxobj=111 when 112 is known.
      model.set(GRB.DoubleParam.BestObjStop, maxObjValue + 0.5);
      model.set(GRB.DoubleParam.Cutoff, maxObjValue + 0.5);
      model.set(GRB.IntParam.SolutionLimit, 1);
      model.set(GRB.IntParam.MIPFocus, 1);
      model.set(GRB.DoubleParam.Heuristics, 0.5);

      model.set(GRB.IntParam.PoolSolutions, 1);
      model.set(GRB.IntParam.PoolSearchMode, 0);
    }
    else {
      model.set(GRB.DoubleParam.PoolGap, (nonOptimalSolutions) ? 1.0 : 0.005);
      model.set(GRB.IntParam.PoolSolutions, nbSolutions);
      model.set(GRB.IntParam.PoolSearchMode, 2);
    }

    model.set(GRB.IntParam.DualReductions, 0);
	
    model.optimize();
    //model.write("output.sol");
    //model.computeIIS();
    //model.write("model1.ilp");
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
	  int t1_val = (int) Math.round(objective1Var.get(GRB.DoubleAttr.Xn));
	  int t2_val = (int) Math.round(objective2Var.get(GRB.DoubleAttr.Xn));
      int t3_val = (int) Math.round(objective3Var.get(GRB.DoubleAttr.Xn));
      int pool_obj_val = (int) Math.round(model.get(GRB.DoubleAttr.PoolObjVal));

      System.out.println(String.format(
        "Step1 solution #%d: objective=%d, T1=%d, T2=%d, T3=%d, EPS=%d, mb=%d, mb_prime=%d, mf=%d, mf_prime=%d",
        solutionNumber, pool_obj_val, t1_val, t2_val, t3_val, eps_val, mb_val, mb_prime_val, mf_val, mf_prime_val));
      System.out.println("Filter activation order (lower -> upper): " + getFilterActivationOrder());

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
    // SKINNY-64-128
    if (blocksize==4 && regime == 2 && nbRounds == 18) {
      //maxlanes=3;
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
          }
    }
	// SKINNY-64-192
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
	// SKINNY-128-256
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
			
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++) {
				if (i == 1 && j == 1 ||
					i == 3 && j == 1)
					model.addConstr(DXlower[7][i][j], GRB.EQUAL, 0.0, "fix_DXlower_" + i + "_" + j + "_1");
				else
					model.addConstr(DXlower[7][i][j], GRB.EQUAL, 1.0, "fix_DXlower_" + i + "_" + j + "_0");
			}
		
    }  
    // SKINNY-128-384
    else if (blocksize==8 && regime == 3 && nbRounds == 23) {
      //maxlanes=1;
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
