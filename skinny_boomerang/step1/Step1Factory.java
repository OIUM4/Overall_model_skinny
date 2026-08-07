package boomerangsearch.step1;

import gurobi.*;

public class Step1Factory {
  private GRBModel model;
  private int regime;

  /**
   * @param model the main gurobi model
   * @param regime whether we are on SK (TK0), TK1, TK2 or TK3
   */
  public Step1Factory(final GRBModel model, final int regime) {
    this.model = model;
    this.regime = regime;
  }

  /** Adds the constraints of the linear part of the trails : ART, SR and MC */
  public void addLinear(GRBVar[][][] DX, GRBVar[][][] DTK) throws GRBException {
    for (int round = 0; round < DX.length-1; round++)
      for (int j = 0; j < 4; j++) {
        addXor(DX[round+1][1][j], DX[round][0][j], DTK[round][0][j]);
        addXor(DX[round+1][2][j], DX[round][1][(j+3)%4], DTK[round][1][(j+3)%4], DX[round][2][(j+2)%4]);
        addXor(DX[round+1][3][j], DX[round+1][1][j], DX[round][2][(j+2)%4]);
        addXor(DX[round+1][0][j], DX[round+1][3][j], DX[round][3][(j+1)%4]);
      }
  }
  
  
  /**lowExt linear part: ART, SR and MC  (DX)*/
  public void addlowExtLinear(GRBVar[][][] DXlowExt, GRBVar[][][] DSTKlowExt) throws GRBException {
    for (int r = 0; r < DXlowExt.length-1; r++)
      for (int j = 0; j < 4; j++) {
		addOr(DXlowExt[r+1][0][j], DXlowExt[r][0][j], DSTKlowExt[r][0][j], DXlowExt[r][2][(j+2)%4], DXlowExt[r][3][(j+1)%4]);
		addOr(DXlowExt[r+1][1][j], DXlowExt[r][0][j], DSTKlowExt[r][0][j]);
        addOr(DXlowExt[r+1][2][j], DXlowExt[r][1][(j+3)%4], DSTKlowExt[r][1][(j+3)%4], DXlowExt[r][2][(j+2)%4]);
        addOr(DXlowExt[r+1][3][j], DXlowExt[r][0][j], DSTKlowExt[r][0][j], DXlowExt[r][2][(j+2)%4]);
      }
  }

  /* lowExt key: m_f  (DknownDec)*/
  public void addlowExtDec(GRBVar[][][] DXlowExt,GRBVar[][][] DknownDeclowExt) throws GRBException {
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
        	model.addConstr(DknownDeclowExt[0][i][j], GRB.EQUAL, DXlowExt[0][i][j], "");
        }
    
    for (int r = 0; r < DXlowExt.length-1; r++) {
      for (int j = 0; j < 4; j++) {
        addOr(DknownDeclowExt[r+1][0][j], DknownDeclowExt[r][3][(j+1)%4], DXlowExt[r+1][0][j]);
        addOr(DknownDeclowExt[r+1][1][j], DknownDeclowExt[r][0][j], DknownDeclowExt[r][1][(j+3)%4], DknownDeclowExt[r][2][(j+2)%4], DXlowExt[r+1][1][j]);
        addOr(DknownDeclowExt[r+1][2][j], DknownDeclowExt[r][1][(j+3)%4], DXlowExt[r+1][2][j]);
        addOr(DknownDeclowExt[r+1][3][j], DknownDeclowExt[r][1][(j+3)%4], DknownDeclowExt[r][2][(j+2)%4], DknownDeclowExt[r][3][(j+1)%4], DXlowExt[r+1][3][j]);
     }
    }       
  }
  
    /** lowExt fastfilter:  (DXFixed, DWFixed)*/
  public void addDFixedlowExtLinear(GRBVar[][][] DXFixedlowExt,GRBVar[][][] DWFixedlowExt,GRBVar[][][] DXlowExt) throws GRBException {
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
        	model.addConstr(DXFixedlowExt[0][i][j], GRB.EQUAL, 1, "");
        }
		
	for (int r = 0; r < DWFixedlowExt.length; r++)
		for (int j = 0; j < 4; j++) {
			model.addConstr(linExprOf(DWFixedlowExt[r][0][j], DXlowExt[r][0][j]), GRB.EQUAL, 1, "");
			model.addConstr(linExprOf(DWFixedlowExt[r][1][j], DXlowExt[r][1][(j + 3) % 4]), GRB.EQUAL, 1, "");
			model.addConstr(linExprOf(DWFixedlowExt[r][2][j], DXlowExt[r][2][(j + 2) % 4]), GRB.EQUAL, 1, "");
			model.addConstr(linExprOf(DWFixedlowExt[r][3][j], DXlowExt[r][3][(j + 1) % 4]), GRB.EQUAL, 1, "");
		}
	
    for (int r = 0; r < DXFixedlowExt.length-1; r++) {
      for (int j = 0; j < 4; j++) {
        addAnd(DXFixedlowExt[r+1][0][j], DWFixedlowExt[r][0][j], DWFixedlowExt[r][2][j], DWFixedlowExt[r][3][j]);
        model.addConstr(DXFixedlowExt[r+1][1][j], GRB.EQUAL, DWFixedlowExt[r][0][j], "");
        addAnd(DXFixedlowExt[r+1][2][j], DWFixedlowExt[r][1][j], DWFixedlowExt[r][2][j]);
        addAnd(DXFixedlowExt[r+1][3][j], DWFixedlowExt[r][0][j], DWFixedlowExt[r][2][j]);
     }
    }
  }
  
    /** lowExt cells that could be used to filter quartets:  (DXFilter)*/
    public void addXfilterlowExt(GRBVar[][][] DXFixedlowExt,GRBVar[][][] DXlowExt,GRBVar[][][] DXFilterlowExt) throws GRBException {
    for (int r = 0; r < DXFixedlowExt.length; r++) {
      for (int j = 0; j < 4; j++) {
        for (int i = 0; i < 4; i++) {
          addAnd(DXFilterlowExt[r][i][j], DXFixedlowExt[r][i][j], DXlowExt[r][i][j]);
        }
      }
    }
  }

  
   /** lowExt cells that could be used to filter quartets:  (DWFilter)*/
   public void addWfilterlowExt(GRBVar[][][] DWFixedlowExt,GRBVar[][][] DWFilterlowExt) throws GRBException {
    double[][] t = {
		{0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0},
		{0.0, 1.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0},
		{0.0, 0.0, -1.0, 0.0, 0.0, -1.0, 0.0, 0.0},
		{0.0, -1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0},
		{0.0, 0.0, 1.0, 0.0, 0.0, 0.0, -1.0, 0.0},
		{-1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0},
		{1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0},
		{0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, -1.0},
		{-1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -1.0},
		{1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 1.0},
		{0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 1.0}
	};
	
    double[] con = {0.0, 0.0, -1.0, 0.0, 0.0, -1.0, 0.0, 0.0, -2.0, 0.0, 0.0};
    for (int r = 0; r < DWFixedlowExt.length; r++) {
      for (int j = 0; j < 4; j++) {
		  
        if ( r == DWFixedlowExt.length - 1) {        
            for (int i = 0; i < 4; i++) {
              model.addConstr(DWFilterlowExt[r][i][j], GRB.EQUAL, 0, "");
            }     
        }
        else {
         for (int k = 0; k < 11; k++) {
            model.addConstr(linExprOf(t[k], DWFixedlowExt[r][0][j], DWFixedlowExt[r][1][j], DWFixedlowExt[r][2][j], DWFixedlowExt[r][3][j], DWFilterlowExt[r][0][j], DWFilterlowExt[r][1][j], DWFilterlowExt[r][2][j], DWFilterlowExt[r][3][j]), GRB.GREATER_EQUAL, con[k], "");
          }
        }
      }
    }
    
  }
  
   /** lowExt guessed subtweakey cells:  (DXGuess, DWGuess, DXisFilter, DWisFilter)*/
  public void addguesslowExt(GRBVar[][][] DXGuesslowExt,GRBVar[][][] DWGuesslowExt,GRBVar[][][] DXisFilterlowExt,GRBVar[][][] DWisFilterlowExt) throws GRBException {
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
        	model.addConstr(DXGuesslowExt[0][i][j], GRB.EQUAL, DXisFilterlowExt[0][i][j], "");
        }
    
    int[] shift = {0, 3, 2, 1};
    for (int r = 0; r < DWisFilterlowExt.length; r++) {
      for (int j = 0; j < 4; j++) {
        for (int i = 0; i < 4; i++) {
          addOr(DWGuesslowExt[r][i][j], DWisFilterlowExt[r][i][j], DXGuesslowExt[r][i][(j+shift[i])%4]);
        }
      }
    }

    for (int r = 0; r < DXisFilterlowExt.length-1; r++) {
      for (int j = 0; j < 4; j++) {
        addOr(DXGuesslowExt[r+1][0][j], DWGuesslowExt[r][3][j], DXisFilterlowExt[r+1][0][j]);
		addOr(DXGuesslowExt[r+1][1][j], DWGuesslowExt[r][0][j], DWGuesslowExt[r][1][j], DWGuesslowExt[r][2][j], DXisFilterlowExt[r+1][1][j]);
        addOr(DXGuesslowExt[r+1][2][j], DWGuesslowExt[r][1][j], DXisFilterlowExt[r+1][2][j]);
        addOr(DXGuesslowExt[r+1][3][j], DWGuesslowExt[r][1][j], DWGuesslowExt[r][2][j], DWGuesslowExt[r][3][j], DXisFilterlowExt[r+1][3][j]);
     }
    }       
  }
  
  
 
  /**UppExt linear part: ART, SR and MC  (DX)**/
  public void adduppExtLinear(GRBVar[][][] DXuppExt, GRBVar[][][] DSTKuppExt) throws GRBException {
    for (int r = DXuppExt.length - 2; r > -1; r = r - 1){
		for (int j = 0; j < 4; j++){
			if (r == DXuppExt.length - 2){
				addXor(DXuppExt[r][0][j], DSTKuppExt[r][0][j], DXuppExt[r+1][1][j]);
				addXor(DXuppExt[r][1][j], DSTKuppExt[r][1][j], DXuppExt[r+1][1][(j+1)%4], DXuppExt[r+1][2][(j+1)%4], DXuppExt[r+1][3][(j+1)%4]);
				addXor(DXuppExt[r][2][j], DXuppExt[r+1][1][(j+2)%4], DXuppExt[r+1][3][(j+2)%4]);
				addXor(DXuppExt[r][3][j], DXuppExt[r+1][0][(j+3)%4], DXuppExt[r+1][3][(j+3)%4]);

			}
			else{
				addOr(DXuppExt[r][0][j], DSTKuppExt[r][0][j], DXuppExt[r+1][1][j]);
				addOr(DXuppExt[r][1][j], DSTKuppExt[r][1][j], DXuppExt[r+1][1][(j+1)%4], DXuppExt[r+1][2][(j+1)%4], DXuppExt[r+1][3][(j+1)%4]);
				addOr(DXuppExt[r][2][j], DXuppExt[r+1][1][(j+2)%4], DXuppExt[r+1][3][(j+2)%4]);
				addOr(DXuppExt[r][3][j], DXuppExt[r+1][0][(j+3)%4], DXuppExt[r+1][3][(j+3)%4]);
			}
		}
	}
  }

  /* UppExt key: m_b  (DknownDec)*/
  public void adduppExtEnc(GRBVar[][][] DXuppExt, GRBVar[][][] DknownEncuppExt) throws GRBException {
	for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
        	model.addConstr(DknownEncuppExt[DknownEncuppExt.length-1][i][j], GRB.EQUAL, 0, "");
        }
	for (int r = DknownEncuppExt.length-2; r > -1; r = r - 1) {
		for (int j = 0; j < 4; j++) {
			addOr(DknownEncuppExt[r][0][j], DknownEncuppExt[r+1][0][j], DknownEncuppExt[r+1][1][j], DknownEncuppExt[r+1][3][j], DXuppExt[r+1][0][j], DXuppExt[r+1][1][j], DXuppExt[r+1][3][j]);
			addOr(DknownEncuppExt[r][1][j], DknownEncuppExt[r+1][2][(j+1)%4], DXuppExt[r+1][2][(j+1)%4]);
			addOr(DknownEncuppExt[r][2][j], DknownEncuppExt[r+1][0][(j+2)%4], DknownEncuppExt[r+1][2][(j+2)%4], DknownEncuppExt[r+1][3][(j+2)%4], DXuppExt[r+1][0][(j+2)%4], DXuppExt[r+1][2][(j+2)%4], DXuppExt[r+1][3][(j+2)%4]);
			addOr(DknownEncuppExt[r][3][j], DknownEncuppExt[r+1][0][(j+3)%4], DXuppExt[r+1][0][(j+3)%4]);
				
		}
	}
  }
  

  /** uppExt fastfilter:  (DXFixed, DYFixed)*/
   public void addDFixeduppExtLinear(GRBVar[][][] DXFixeduppExt,GRBVar[][][] DYFixeduppExt, GRBVar[][][] DXuppExt) throws GRBException {
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
        	model.addConstr(DYFixeduppExt[DYFixeduppExt.length - 1][i][j], GRB.EQUAL, 1, "");
        }
	
	for (int r = DXFixeduppExt.length - 1; r > -1; r = r - 1)
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++) {
				model.addConstr(linExprOf(DXFixeduppExt[r][i][j], DXuppExt[r][i][j]), GRB.EQUAL, 1, "");
			}
	
    for (int r = DYFixeduppExt.length - 2; r > -1; r = r - 1) {
      for (int j = 0; j < 4; j++) {
		model.addConstr(DYFixeduppExt[r][0][j], GRB.EQUAL, DXFixeduppExt[r+1][1][j], "");  
        addAnd(DYFixeduppExt[r][1][j], DXFixeduppExt[r+1][1][(j+1)%4], DXFixeduppExt[r+1][2][(j+1)%4], DXFixeduppExt[r+1][3][(j+1)%4]);
        addAnd(DYFixeduppExt[r][2][j], DXFixeduppExt[r+1][1][(j+2)%4], DXFixeduppExt[r+1][3][(j+2)%4]);
        addAnd(DYFixeduppExt[r][3][j], DXFixeduppExt[r+1][0][(j+3)%4], DXFixeduppExt[r+1][3][(j+3)%4]);
     }
    }
  }
  
  
    /** uppExt cells that could be used to filter quartets:  (DYFilter)*/
   public void addYfilteruppExt(GRBVar[][][] DYFixeduppExt,GRBVar[][][] DXuppExt,GRBVar[][][] DYFilteruppExt) throws GRBException {
    for (int r = 0; r < DYFixeduppExt.length; r++) {
		for (int j = 0; j < 4; j++) {
			if (r == 0){
				for (int i = 0; i < 4; i++) {
					model.addConstr(DYFilteruppExt[r][i][j], GRB.EQUAL, 0, "");  
				}
			}
			else{
				for (int i = 0; i < 4; i++) {
					addAnd(DYFilteruppExt[r][i][j], DYFixeduppExt[r][i][j], DXuppExt[r][i][j]);
				}
			}
		}
	}
  }


  
    /** uppExt cells that could be used to filter quartets:  (DXFilter)*/ 
  public void addXfilteruppExt(GRBVar[][][] DXFixeduppExt,GRBVar[][][] DXFilteruppExt) throws GRBException {
	double[][] t = {
		{1.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0},
		{0.0, 0.0, 0.0, -1.0, -1.0, 0.0, 0.0, 0.0},
		{-1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0},
		{0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0},
		{0.0, 0.0, 1.0, 0.0, 0.0, 0.0, -1.0, 0.0},
		{0.0, -1.0, 0.0, -1.0, 0.0, 0.0, -1.0, 0.0},
		{0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0},
		{0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 1.0, 0.0},
		{0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, -1.0},
		{0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0},
		{0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 1.0}
	};	
	double[] con = {0.0, -1.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, -1.0, 0.0};
	
	for (int r = 0; r < DXFixeduppExt.length; r++) {
		for (int j = 0; j < 4; j++) {
			if ( r <= 1 ) {        
				for (int i = 0; i < 4; i++) {
					model.addConstr(DXFilteruppExt[r][i][j], GRB.EQUAL, 0, "");
				}     
			}
			else {
				for (int k = 0; k < 11; k++) {
					model.addConstr(linExprOf(t[k], DXFixeduppExt[r][0][j], DXFixeduppExt[r][1][j], DXFixeduppExt[r][2][j], DXFixeduppExt[r][3][j], DXFilteruppExt[r][0][j], DXFilteruppExt[r][1][j], DXFilteruppExt[r][2][j], DXFilteruppExt[r][3][j]), GRB.GREATER_EQUAL, con[k], "");
				}	
			}
		}
	}
  }
  
  
     /** uppExt guessed subtweakey cells:  (DXGuess, DYGuess, DXisFilter, DYisFilter)*/
  public void addguessuppExt(GRBVar[][][] DXGuessuppExt,GRBVar[][][] DYGuessuppExt,GRBVar[][][] DXisFilteruppExt,GRBVar[][][] DYisFilteruppExt) throws GRBException {
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
        	model.addConstr(DXGuessuppExt[DXisFilteruppExt.length-1][i][j], GRB.EQUAL, DYisFilteruppExt[DYisFilteruppExt.length-1][i][j], "");
			model.addConstr(DYGuessuppExt[DXisFilteruppExt.length-1][i][j], GRB.EQUAL, 0, "");
        }
    
	for (int r = DYisFilteruppExt.length-2; r > -1; r = r - 1) {
		for (int j = 0; j < 4; j++) {
			addOr(DYGuessuppExt[r][0][j], DXGuessuppExt[r+1][0][j], DXGuessuppExt[r+1][1][j], DXGuessuppExt[r+1][3][j]);
			addOr(DYGuessuppExt[r][1][j], DXGuessuppExt[r+1][2][(j+1)%4]);
			addOr(DYGuessuppExt[r][2][j], DXGuessuppExt[r+1][0][(j+2)%4], DXGuessuppExt[r+1][2][(j+2)%4], DXGuessuppExt[r+1][3][(j+2)%4]);
			addOr(DYGuessuppExt[r][3][j], DXGuessuppExt[r+1][0][(j+3)%4]);
		}
    }
	
    for (int r = DYisFilteruppExt.length-2; r > -1; r = r -1) {
      for (int j = 0; j < 4; j++) {
			addOr(DXGuessuppExt[r][0][j], DXisFilteruppExt[r+1][0][j], DXisFilteruppExt[r+1][1][j], DXisFilteruppExt[r+1][3][j], DYGuessuppExt[r][0][j], DYisFilteruppExt[r][0][j]);
			addOr(DXGuessuppExt[r][1][j], DXisFilteruppExt[r+1][2][(j+1)%4], DYGuessuppExt[r][1][j], DYisFilteruppExt[r][1][j]);
			addOr(DXGuessuppExt[r][2][j], DXisFilteruppExt[r+1][0][(j+2)%4], DXisFilteruppExt[r+1][2][(j+2)%4], DXisFilteruppExt[r+1][3][(j+2)%4], DYGuessuppExt[r][2][j], DYisFilteruppExt[r][2][j]);
			addOr(DXGuessuppExt[r][3][j], DXisFilteruppExt[r+1][0][(j+3)%4], DYGuessuppExt[r][3][j], DYisFilteruppExt[r][3][j]);
        }
    }
       
  }
  
  
  public void addBound(
    GRBVar[][][] DXupper, 
	GRBVar[][][] DXlower, 
	GRBVar[][][] freeSBupper, 
	GRBVar[][][] freeXlower, 
	GRBVar[][][] isTable,
	GRBVar[][][] isDDT2, 
	int blocksize, 
	int regime, 
	int nbRounds
) throws GRBException {
    
	int maxprob = 60;
    if (blocksize == 4) 
        maxprob = 38;
    else if (blocksize == 8) 
        maxprob = 60;

    GRBLinExpr sumother = new GRBLinExpr();
	GRBLinExpr sumDDT2 = new GRBLinExpr();
    for (int round = 0; round < isTable.length; round++)
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          sumother.addTerm(1.0, isTable[round][i][j]);
          sumother.addTerm(-1.0, isDDT2[round][i][j]);
		  sumDDT2.addTerm(1.0, isDDT2[round][i][j]);
        }
	GRBLinExpr Bounds = new GRBLinExpr();
	Bounds.multAdd(1.0, sumother);
	Bounds.multAdd(2.0, sumDDT2);
    model.addConstr(Bounds, GRB.LESS_EQUAL, maxprob, "");
	
	/*GRBLinExpr GAP = new GRBLinExpr();
	GAP.multAdd(1.0, sumDDT2);
	GAP.multAdd(-1.0, sumother);
	model.addConstr(GAP, GRB.GREATER_EQUAL, 0, "");*/
  }

  /** Adds the constraints of the linear part of the trails for SK : ART, SR and MC */
  public void addLinearSK(GRBVar[][][] DX) throws GRBException {
    for (int round = 0; round < DX.length-1; round++)
      for (int j = 0; j < 4; j++) {
        model.addConstr(DX[round+1][1][j], GRB.EQUAL, DX[round][0][j], "");
        addXor(DX[round+1][2][j], DX[round][1][(j+3)%4], DX[round][2][(j+2)%4]);
        addXor(DX[round+1][3][j], DX[round+1][1][j], DX[round][2][(j+2)%4]);
        addXor(DX[round+1][0][j], DX[round+1][3][j], DX[round][3][(j+1)%4]);
      }
  }

  /** Adds constraints on the free variables between upper and lower trail */
  public void freePropagation(GRBVar[][][] DXup, GRBVar[][][] freeSBup, GRBVar[][][] DXlo, GRBVar[][][] freeXlo) throws GRBException {
    double[] t1 = {1.0, -1.0, -1.0, 1.0};
    double[] t2 = {2.0, -2.0, -1.0, 1.0, -1.0, 1.0};
    double[] t3 = {3.0, -3.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0};
    for (int r = 0; r < freeXlo.length-1; r++) {
      for (int j = 0; j < 4; j++) {
        model.addConstr(linExprOf(t3, freeSBup[r][0][j], DXup[r][0][j], freeSBup[r+1][0][j], freeXlo[r+1][0][j], freeSBup[r+1][1][j], freeXlo[r+1][1][j], freeSBup[r+1][3][j], freeXlo[r+1][3][j]), GRB.GREATER_EQUAL, -5.0, "");
        model.addConstr(linExprOf(t1, freeSBup[r][1][j], DXup[r][1][j], freeSBup[r+1][2][(j+1)%4], freeXlo[r+1][2][(j+1)%4]), GRB.GREATER_EQUAL, -1.0, "");
        model.addConstr(linExprOf(t3, freeSBup[r][2][j], DXup[r][2][j], freeSBup[r+1][2][(j+2)%4], freeXlo[r+1][2][(j+2)%4], freeSBup[r+1][2][(j+2)%4], freeXlo[r+1][2][(j+2)%4], freeSBup[r+1][3][(j+2)%4], freeXlo[r+1][3][(j+2)%4]), GRB.GREATER_EQUAL, -5.0, "");
        model.addConstr(linExprOf(t1, freeSBup[r][3][j], DXup[r][3][j], freeSBup[r+1][0][(j+3)%4], freeXlo[r+1][0][(j+3)%4]), GRB.GREATER_EQUAL, -1.0, "");

        model.addConstr(linExprOf(t1, freeXlo[r+1][0][j], DXlo[r+1][0][j], freeXlo[r][3][(j+1)%4], freeSBup[r][3][(j+1)%4]), GRB.GREATER_EQUAL, -1.0, "");
        model.addConstr(linExprOf(t3, freeXlo[r+1][1][j], DXlo[r+1][1][j], freeXlo[r][0][j], freeSBup[r][0][j], freeXlo[r][1][(j+3)%4], freeSBup[r][1][(j+3)%4], freeXlo[r][2][(j+2)%4], freeSBup[r][2][(j+2)%4]), GRB.GREATER_EQUAL, -5.0, "");
        model.addConstr(linExprOf(t1, freeXlo[r+1][2][j], DXlo[r+1][2][j], freeXlo[r][1][(j+3)%4], freeSBup[r][1][(j+3)%4]), GRB.GREATER_EQUAL, -1.0, "");
        model.addConstr(linExprOf(t3, freeXlo[r+1][3][j], DXlo[r+1][3][j], freeXlo[r][1][(j+3)%4], freeSBup[r][1][(j+3)%4], freeXlo[r][2][(j+2)%4], freeSBup[r][2][(j+2)%4], freeXlo[r][3][(j+1)%4], freeSBup[r][3][(j+1)%4]), GRB.GREATER_EQUAL, -5.0, "");
      }
    }
  }

  /** Adds constraints on the free variables in the upper trail */
  public void freePropagationUpper(GRBVar[][][] freeX, GRBVar[][][] freeSB, GRBVar[][][] DX) throws GRBException {
    for (int r = 0; r < freeX.length-1; r++)
      for (int j = 0; j < 4; j++) {
        addOr(freeX[r+1][0][j], freeSB[r][0][j], freeSB[r][2][(j+2)%4], freeSB[r][3][(j+1)%4]);
        model.addConstr(freeX[r+1][1][j], GRB.EQUAL, freeSB[r][0][j], "");
        addOr(freeX[r+1][2][j], freeSB[r][1][(j+3)%4], freeSB[r][2][(j+2)%4]);
        addOr(freeX[r+1][3][j], freeSB[r][0][j], freeSB[r][2][(j+2)%4]);
      }
  }

  /** Adds constraints on the free variables in the upper trail */
  public void freePropagationLower(GRBVar[][][] freeX, GRBVar[][][] freeSB, GRBVar[][][] DX) throws GRBException {
    for (int r = 0; r < freeX.length-1; r++)
      for (int j = 0; j < 4; j++) {
        model.addConstr(freeSB[r][0][j], GRB.EQUAL, freeX[r+1][1][j], "");
        addOr(freeSB[r][1][j], freeX[r+1][1][(j+1)%4],freeX[r+1][2][(j+1)%4],freeX[r+1][3][(j+1)%4]);
        addOr(freeSB[r][2][j], freeX[r+1][1][(j+2)%4], freeX[r+1][3][(j+2)%4]);
        addOr(freeSB[r][3][j], freeX[r+1][0][(j+3)%4], freeX[r+1][3][(j+3)%4]);
      }
  }

  public void objectiveConstraints(GRBVar[][][] DXupper, GRBVar[][][] freeXupper, GRBVar[][][] freeSBupper, GRBVar[][][] DXlower, GRBVar[][][] freeXlower, GRBVar[][][] freeSBlower, GRBVar[][][] isTable, GRBVar[][][] isDDT2) throws GRBException {
    for (int round = 0; round < freeXupper.length; round++)
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          oneObjectiveConstraint(DXupper[round][i][j], freeXupper[round][i][j], freeSBupper[round][i][j], DXlower[round][i][j], freeXlower[round][i][j], freeSBlower[round][i][j], isTable[round][i][j], isDDT2[round][i][j]);
        }
  }

  public void oneObjectiveConstraint(GRBVar DXupper, GRBVar freeXupper, GRBVar freeSBupper, GRBVar DXlower, GRBVar freeXlower, GRBVar freeSBlower, GRBVar isTable, GRBVar isDDT2) throws GRBException {
    model.addConstr(DXupper, GRB.GREATER_EQUAL, freeSBupper, ""); // if free then =1 -> cy' exy
    model.addConstr(DXlower, GRB.GREATER_EQUAL, freeXlower,  ""); // Symmetry        -> cz' ezt
    model.addConstr(freeSBupper, GRB.GREATER_EQUAL, freeXupper, ""); // free propagates -> cy cx'
    model.addConstr(freeXlower, GRB.GREATER_EQUAL, freeSBlower, ""); // Symmetry        -> cz ct'
    model.addConstr(isTable, GRB.GREATER_EQUAL, isDDT2, ""); // DDT2 -> isTable -> d1 d2'
    model.addConstr(2.0, GRB.GREATER_EQUAL, linExprOf(freeSBupper, freeXlower, isTable), ""); // BCT and others -> cy' cz' d1'
    model.addConstr(linExprOf(freeSBupper, isTable), GRB.GREATER_EQUAL, DXupper, ""); //          -> cy exy' d1
    model.addConstr(linExprOf(freeXlower,  isTable), GRB.GREATER_EQUAL, DXlower, ""); // Symmetry -> cz ezt' d1
    model.addConstr(DXupper, GRB.GREATER_EQUAL, linExprOf(-1.0, freeXlower,  isTable), ""); //          -> cz' exy d1'
    model.addConstr(DXlower, GRB.GREATER_EQUAL, linExprOf(-1.0, freeSBupper, isTable), ""); // Symmetry -> cy' ezt d1'
    model.addConstr(linExprOf(DXupper, DXlower), GRB.GREATER_EQUAL, isTable, ""); // -> exy ezt d1'
    model.addConstr(isDDT2, GRB.GREATER_EQUAL, linExprOf(-1.0, freeXupper,  DXlower), ""); //          -> cx' ezt' d2
    model.addConstr(isDDT2, GRB.GREATER_EQUAL, linExprOf(-1.0, freeSBlower, DXupper), ""); // Symmetry -> ct' exy' d2
    model.addConstr(linExprOf(freeXupper, freeSBlower), GRB.GREATER_EQUAL, isDDT2, ""); // cx ct d2'
	
//	model.addConstr(3.0, GRB.GREATER_EQUAL, linExprOf(freeXupper, freeSBupper, freeXlower, freeSBlower, isTable), "");
	//model.addConstr(linExprOf(freeXupper, freeSBlower, isTable), GRB.GREATER_EQUAL, linExprOf(-1.0, freeSBupper, freeXlower), "");
  }

  public void objectiveConstraintsDouble(GRBVar[][][] DXupper, GRBVar[][][] freeXupper, GRBVar[][][] freeSBupper, GRBVar[][][] DXlower, GRBVar[][][] freeXlower, GRBVar[][][] freeSBlower, GRBVar[][][] isTable, GRBVar[][][] isDDT2) throws GRBException {
    for (int round = 0; round < freeXupper.length; round++)
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++) {
          oneObjectiveConstraintDouble(DXupper[round][i][j], freeXupper[round][i][j], freeSBupper[round][i][j], DXlower[round][i][j], freeXlower[round][i][j], freeSBlower[round][i][j], isTable[round][i][j], isDDT2[round][i][j]);
        }
  }

  public void oneObjectiveConstraintDouble(GRBVar DXupper, GRBVar freeXupper, GRBVar freeSBupper, GRBVar DXlower, GRBVar freeXlower, GRBVar freeSBlower, GRBVar isTable, GRBVar isDDT2) throws GRBException {
    boolean optimized = false;
    model.addConstr(DXupper, GRB.GREATER_EQUAL, freeSBupper, ""); // if free then =1  -> cy' exy
    model.addConstr(DXlower, GRB.GREATER_EQUAL, freeXlower,  ""); // Symmetry         -> cz' ezt
    if (!optimized) {
      model.addConstr(DXupper, GRB.GREATER_EQUAL, isDDT2, ""); // need value for ddt2 -> exy d2'
      model.addConstr(DXlower, GRB.GREATER_EQUAL, isDDT2, ""); // Symmetry            -> ezt d2'
    }
    else {
      model.addConstr(linExprOf(new double[]{1.0,1.0,-2.0}, DXupper, DXlower, isDDT2), GRB.GREATER_EQUAL, 0, "");
    }
    model.addConstr(freeSBupper, GRB.GREATER_EQUAL, freeXupper, ""); // free propagates  -> cy cx'
    model.addConstr(freeXlower, GRB.GREATER_EQUAL, freeSBlower, ""); // Symmetry         -> cz ct'
    model.addConstr(1.0, GRB.GREATER_EQUAL, linExprOf(isTable, isDDT2), ""); // not twice at the same time -> d1' d2'
    if (!optimized) {
      model.addConstr(2.0, GRB.GREATER_EQUAL, linExprOf(freeSBupper, freeXlower, isTable), ""); // BCT and others -> cy' cz' d1'
      model.addConstr(2.0, GRB.GREATER_EQUAL, linExprOf(freeSBupper, freeXlower, isDDT2),  ""); // Same for DDT2  -> cy' cz' d2'
    }
    else {
      model.addConstr(4, GRB.GREATER_EQUAL, linExprOf(new double[]{2.0,2.0,1.0,1.0}, freeSBupper, freeXlower, isTable, isDDT2), "");
    }
    model.addConstr(linExprOf(freeSBupper, freeSBlower, isTable), GRB.GREATER_EQUAL, DXupper, ""); //          -> cy ct exy' d1
    model.addConstr(linExprOf(freeXlower,  freeXupper,  isTable), GRB.GREATER_EQUAL, DXlower, ""); // Symmetry -> cx cz ezt' d1
    model.addConstr(DXupper, GRB.GREATER_EQUAL, linExprOf(-1.0, freeXlower,  isTable), ""); //          -> cz' exy d1'
    model.addConstr(DXlower, GRB.GREATER_EQUAL, linExprOf(-1.0, freeSBupper, isTable), ""); // Symmetry -> cy' ezt d1'
    model.addConstr(linExprOf(DXupper, DXlower), GRB.GREATER_EQUAL, isTable, ""); // need one non zero for table -> exy ezt d1'
    model.addConstr(isDDT2, GRB.GREATER_EQUAL, linExprOf(-1.0, freeXupper,  DXlower), ""); //          -> cx' ezt' d2
    model.addConstr(isDDT2, GRB.GREATER_EQUAL, linExprOf(-1.0, freeSBlower, DXupper), ""); // Symmetry -> ct' exy' d2
  }

  public void addXor(GRBVar ... vars) throws GRBException {
    for (int i = 0; i < vars.length; i++) {
      GRBLinExpr sumOthers = new GRBLinExpr();
      for (int j = 0; j < vars.length; j++)
        if (j != i)
          sumOthers.addTerm(1.0, vars[j]);
      model.addConstr(vars[i], GRB.LESS_EQUAL, sumOthers, "");
    }
  }

  public void addAnd(GRBVar mainVar, GRBVar ... vars) throws GRBException {
    GRBLinExpr sumOthers = new GRBLinExpr();
    for (int i = 0; i < vars.length; i++) {
        sumOthers.addTerm(1.0, vars[i]);
        model.addConstr(mainVar, GRB.LESS_EQUAL, vars[i], "");
    }
    sumOthers.addTerm(-1.0, mainVar);
    model.addConstr(sumOthers, GRB.LESS_EQUAL, vars.length-1, "");
  }
  
  public void addOr(GRBVar mainVar, GRBVar ... vars) throws GRBException {
    if (true)
      model.addGenConstrOr(mainVar, vars, "");
    else {
      GRBLinExpr sumOthers = new GRBLinExpr();
      for (int i = 0; i < vars.length; i++) {
          sumOthers.addTerm(1.0, vars[i]);
          model.addConstr(vars[i], GRB.LESS_EQUAL, mainVar, "");
      }
      model.addConstr(mainVar, GRB.LESS_EQUAL, sumOthers, "");
    }
  }

  /** Ensures that there is no X round of DX and DTK to zero, where X depends on the regime */
  public void ensureNonZeroDifference(GRBVar[][][] DX, GRBVar[][][] DTK) throws GRBException {
    int nbNonZeroRounds = 0;
    switch (regime) {
    case 3: nbNonZeroRounds = 6;
      break;
    case 2: nbNonZeroRounds = 4;
      break;
    case 1: nbNonZeroRounds = 2;
      break;
    case 0: nbNonZeroRounds = 0;
      break;
    }
    if (DX.length >= nbNonZeroRounds+1) {
      GRBLinExpr nonZeroSum = new GRBLinExpr();
      for (int round = 0; round < nbNonZeroRounds; round++)
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            nonZeroSum.addTerm(1.0, DX[round][i][j]);
            if (i < 2)
              nonZeroSum.addTerm(1.0, DTK[round][i][j]);
          }
      model.addConstr(nonZeroSum, GRB.GREATER_EQUAL, 1, "nonZeroDiff");
    }
  }

  /** Remove symmetries in SK */
  public void removeSymmetriesSK(GRBVar[][] DX0) throws GRBException {
    // First row
    GRBLinExpr previousLines = new GRBLinExpr();
    for (int i = 0; i < 4; i++) {
      GRBLinExpr firstZero = new GRBLinExpr();
      firstZero.addTerm(3.0, DX0[i][0]);
      firstZero.multAdd(3.0, previousLines);
      model.addConstr(firstZero, GRB.GREATER_EQUAL, linExprOf(DX0[i][1],DX0[i][2],DX0[i][3]), "");
      model.addConstr(previousLines, GRB.GREATER_EQUAL, linExprOf(new double[]{-1.0,-1.0,1.0}, DX0[i][1], DX0[i][2], DX0[i][3]), "");
      previousLines.add(linExprOf(DX0[i]));
    }
  }

  public GRBLinExpr linExprOf(double[] coeffs, GRBVar ... vars) throws GRBException {
    GRBLinExpr ofVars = new GRBLinExpr();
    ofVars.addTerms(coeffs, vars);
    return ofVars;
  }

  public GRBLinExpr linExprOf(double constant, GRBVar ... vars) {
    GRBLinExpr ofVars = linExprOf(vars);
    ofVars.addConstant(constant);
    return ofVars;
  }

  public GRBLinExpr linExprOf(GRBVar ... vars) {
    GRBLinExpr expr = new GRBLinExpr();
    for (GRBVar var : vars)
      expr.addTerm(1.0, var);
    return expr;
  }

  public GRBLinExpr sumState(GRBVar[][] state) throws GRBException {
    GRBLinExpr sum = new GRBLinExpr();
    for (int i = 0; i < 4; i++)
      for (int j = 0; j < 4; j++)
        sum.addTerm(1.0, state[i][j]);
    return sum;
  }

  public void addKnownDiffBounds(GRBVar[][][] DX) throws GRBException {
    int[] diffBounds = new int[0];
    switch (regime) {
    case 0:
      diffBounds = new int[]{0,1,2,5,8,12,16,26,36,41,46,51,55,58,61,66,75,82,88,92,96,102,108,114,116,124,132,138,136,148,158};
      break;
    case 1:
      diffBounds = new int[]{0,0,0,1,2,3,6,10,13,16,23,32,38,41,45,49,54,59,62,66,70,75,79,83,85,88,95,102,108,112,120};
      break;
    case 2:
      diffBounds = new int[]{0,0,0,0,0,1,2,3,6,9,12,16,21,25,31,35,40,43,46,52,57,59,64,67,72,75,82,85,88,92,96};
      break;
    case 3:
      diffBounds = new int[]{0,0,0,0,0,0,0,1,2,3,6,10,13,16,19,24,27,31,35,43,45,48,51,55,58,60,65,72,77,81,85};
      break;
    }
    for (int start = 0; start < DX.length-1; start++)
      for (int end = start + 4; end < DX.length-1; end++) {
        GRBLinExpr sumRounds = new GRBLinExpr();
        for (int round = start; round <= end; round++)
          sumRounds.add(sumState(DX[round]));
        model.addConstr(sumRounds, GRB.GREATER_EQUAL, diffBounds[end-start+1], "");
      }
  }

}
