package boomerangsearch.step1;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.io.File;
import java.io.IOException;

public class Step1Solution {
  public int nbRounds;
  public int nExtbRounds;
  public int nExtfRounds;
  public int regime;
  public double objective;
  public int[][][] DXupper;
  public int[][][] freeXupper;
  public int[][][] freeSBupper;
  public Step1SolutionTweakey DTKupper;
  public int[][][] DXlower;
  public int[][][] freeXlower;
  public int[][][] freeSBlower;
  public Step1SolutionTweakey DTKlower;
  public int[][][] isTable;
  public int[][][] isDDT2;
  /*Ef*/
  public int[][][] DXlowExt;
  public int[][][] DSTKlowExt;
  public int[][][] DKnownDeclowExt;

  /*Eb*/
  public int[][][] DXuppExt;
  public int[][][] DSTKuppExt;
  public int[][][] DKnownEncuppExt;

  /*scalar bounds*/
  public int blocksize;
  public int rb;
  public int rb_prime;
  public int rf;
  public int rf_prime;
  public int mb;
  public int mb_prime;
  public int mf;
  public int mf_prime;

  /*objective diagnostics*/
  public double T1;
  public double T2;

  /*layered EPS diagnostics*/
  public int EPS;
  public int epsilonp;
  public int epsilonc;
  public double T3;

  /*fastfilter*/
  public int[][][] DXFixedlowExt;
  public int[][][] DWFixedlowExt;
  public int[][][] DXFilterlowExt;
  public int[][][] DWFilterlowExt;
  public int[][][] DXisFilterlowExt;
  public int[][][] DWisFilterlowExt;
  public int[][][] DXGuesslowExt;
  public int[][][] DWGuesslowExt;
  
  /*fastfilter*/
  public int[][][] DXFixeduppExt;
  public int[][][] DYFixeduppExt;
  public int[][][] DXFilteruppExt;
  public int[][][] DYFilteruppExt;
  public int[][][] DXisFilteruppExt;
  public int[][][] DYisFilteruppExt;
  public int[][][] DXGuessuppExt;
  public int[][][] DYGuessuppExt;


  public Step1Solution() {}

  public Step1Solution(int nbRounds, int nExtbRounds, int nExtfRounds, int regime, double objective, int[][][] DXupper, int[][][] freeXupper, int[][][] freeSBupper, Step1SolutionTweakey DTKupper, int[][][] DXlower, int[][][] freeXlower, int[][][] freeSBlower, Step1SolutionTweakey DTKlower, int[][][] isTable, int[][][] isDDT2, int[][][] DXlowExt, int[][][] DSTKlowExt, int[][][] DKnownDeclowExt, int[][][] DXuppExt, int[][][] DSTKuppExt, int[][][] DKnownEncuppExt, int[][][] DXFixedlowExt, int[][][] DWFixedlowExt, int[][][] DXFilterlowExt, int[][][] DWFilterlowExt, int[][][] DXisFilterlowExt, int[][][] DWisFilterlowExt, int[][][] DXGuesslowExt, int[][][] DWGuesslowExt, int[][][] DXFixeduppExt, int[][][] DYFixeduppExt, int[][][] DXFilteruppExt, int[][][] DYFilteruppExt, int[][][] DXisFilteruppExt, int[][][] DYisFilteruppExt, int[][][] DXGuessuppExt, int[][][] DYGuessuppExt, int blocksize, int rb, int rb_prime, int rf, int rf_prime, int mb, int mb_prime, int mf, int mf_prime, double T1, double T2, int EPS, int epsilonp, int epsilonc, double T3) {
    this.nbRounds = nbRounds;
    this.nExtbRounds = nExtbRounds;
    this.nExtfRounds = nExtfRounds;
    this.regime = regime;
    this.objective = objective;
    this.DXupper = DXupper;
    this.freeXupper = freeXupper;
    this.freeSBupper = freeSBupper;
    this.DTKupper = DTKupper;
    this.DXlower = DXlower;
    this.freeXlower = freeXlower;
    this.freeSBlower = freeSBlower;
    this.DTKlower = DTKlower;
    this.isTable = isTable;
    this.isDDT2 = isDDT2;
    this.DXlowExt = DXlowExt;
    this.DSTKlowExt = DSTKlowExt;
    this.DKnownDeclowExt = DKnownDeclowExt;
    this.DXuppExt = DXuppExt;
    this.DSTKuppExt = DSTKuppExt;
    this.DKnownEncuppExt = DKnownEncuppExt;
    this.DXFixedlowExt = DXFixedlowExt;
    this.DWFixedlowExt=DWFixedlowExt;
    this.DXFilterlowExt=DXFilterlowExt;
    this.DWFilterlowExt=DWFilterlowExt;
    this.DXisFilterlowExt=DXisFilterlowExt;
    this.DWisFilterlowExt=DWisFilterlowExt;
    this.DXGuesslowExt=DXGuesslowExt;
    this.DWGuesslowExt=DWGuesslowExt;
	this.DXFixeduppExt = DXFixeduppExt;
    this.DYFixeduppExt=DYFixeduppExt;
    this.DXFilteruppExt=DXFilteruppExt;
    this.DYFilteruppExt=DYFilteruppExt;
    this.DXisFilteruppExt=DXisFilteruppExt;
    this.DYisFilteruppExt=DYisFilteruppExt;
    this.DXGuessuppExt=DXGuessuppExt;
    this.DYGuessuppExt=DYGuessuppExt;
    this.blocksize = blocksize;
    this.rb = rb;
    this.rb_prime = rb_prime;
    this.rf = rf;
    this.rf_prime = rf_prime;
    this.mb = mb;
    this.mb_prime = mb_prime;
    this.mf = mf;
    this.mf_prime = mf_prime;
    this.T1 = T1;
    this.T2 = T2;
    this.EPS = EPS;
    this.epsilonp = epsilonp;
    this.epsilonc = epsilonc;
    this.T3 = T3;
  }

  /**
   * Backward-compatible constructor for older code paths. EPS diagnostics are set to 0.
   */
  public Step1Solution(int nbRounds, int nExtbRounds, int nExtfRounds, int regime, double objective, int[][][] DXupper, int[][][] freeXupper, int[][][] freeSBupper, Step1SolutionTweakey DTKupper, int[][][] DXlower, int[][][] freeXlower, int[][][] freeSBlower, Step1SolutionTweakey DTKlower, int[][][] isTable, int[][][] isDDT2, int[][][] DXlowExt, int[][][] DSTKlowExt, int[][][] DKnownDeclowExt, int[][][] DXuppExt, int[][][] DSTKuppExt, int[][][] DKnownEncuppExt, int[][][] DXFixedlowExt, int[][][] DWFixedlowExt, int[][][] DXFilterlowExt, int[][][] DWFilterlowExt, int[][][] DXisFilterlowExt, int[][][] DWisFilterlowExt, int[][][] DXGuesslowExt, int[][][] DWGuesslowExt, int[][][] DXFixeduppExt, int[][][] DYFixeduppExt, int[][][] DXFilteruppExt, int[][][] DYFilteruppExt, int[][][] DXisFilteruppExt, int[][][] DYisFilteruppExt, int[][][] DXGuessuppExt, int[][][] DYGuessuppExt, int blocksize, int rb, int rb_prime, int rf, int rf_prime, int mb, int mb_prime, int mf, int mf_prime) {
    this(nbRounds, nExtbRounds, nExtfRounds, regime, objective, DXupper, freeXupper, freeSBupper, DTKupper, DXlower, freeXlower, freeSBlower, DTKlower, isTable, isDDT2, DXlowExt, DSTKlowExt, DKnownDeclowExt, DXuppExt, DSTKuppExt, DKnownEncuppExt, DXFixedlowExt, DWFixedlowExt, DXFilterlowExt, DWFilterlowExt, DXisFilterlowExt, DWisFilterlowExt, DXGuesslowExt, DWGuesslowExt, DXFixeduppExt, DYFixeduppExt, DXFilteruppExt, DYFilteruppExt, DXisFilteruppExt, DYisFilteruppExt, DXGuessuppExt, DYGuessuppExt, blocksize, rb, rb_prime, rf, rf_prime, mb, mb_prime, mf, mf_prime, 0, 0, 0, 0, 0, 0);
  }

  public void toFile(String fileName) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      mapper.writeValue(new File(fileName), this);
    }
    catch (JsonParseException e) { e.printStackTrace(); System.exit(1); }
    catch (JsonMappingException e) { e.printStackTrace(); System.exit(1); }
    catch (IOException e) { e.printStackTrace(); System.exit(1); }
  }

  public static void toFile(String fileName, List<Step1Solution> solutions) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      mapper.writeValue(new File(fileName), solutions);
    }
    catch (JsonParseException e) { e.printStackTrace(); System.exit(1); }
    catch (JsonMappingException e) { e.printStackTrace(); System.exit(1); }
    catch (IOException e) { e.printStackTrace(); System.exit(1); }
  }

  public static void toFile(File file, List<Step1Solution> solutions) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      mapper.writeValue(file, solutions);
    }
    catch (JsonParseException e) { e.printStackTrace(); System.exit(1); }
    catch (JsonMappingException e) { e.printStackTrace(); System.exit(1); }
    catch (IOException e) { e.printStackTrace(); System.exit(1); }
  }

  public static List<Step1Solution> fromFile(String fileName) {
    return fromFile(new File(fileName));
  }

  public static List<Step1Solution> fromFile(File file) {
    try {
      return new ObjectMapper().readValue(file, new TypeReference<List<Step1Solution>>(){});
    }
    catch (JsonParseException e) { e.printStackTrace(); System.exit(1); }
    catch (JsonMappingException e) { e.printStackTrace(); System.exit(1); }
    catch (IOException e) { e.printStackTrace(); System.exit(1); }
    return null; // Can't reach
  }
}
