package boomerangsearch;

import boomerangsearch.step1.Step1;
import boomerangsearch.step1.Step1Solution;

import gurobi.*;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.io.File;

@Command(
  name = "boomerangsearch",
  mixinStandardHelpOptions = true,
  version = "2.0-step1-only",
  description = "STEP1-only key-guessing / structural-sequence search for SKINNY"
)
public class BoomerangSearch implements Callable<Integer> {

  @Option(names = {"-verbose","-v"}, description = "Verbose mode. Default is false")
  private boolean verbose;

  @Option(names = {"-nbRounds","-r"}, defaultValue = "12", description = "Number of rounds of the fixed boomerang distinguisher. Default is ${DEFAULT-VALUE}")
  private int nbRounds;

  @Option(names = {"-nExtbRounds","-rb"}, defaultValue = "2", description = "Number of rounds extended before the distinguisher. Default is ${DEFAULT-VALUE}")
  private int nExtbRounds;

  @Option(names = {"-nExtfRounds","-rf"}, defaultValue = "4", description = "Number of rounds extended after the distinguisher. Default is ${DEFAULT-VALUE}")
  private int nExtfRounds;

  @Option(names = {"-regime","-tk"}, defaultValue = "2", description = "Regime: 0/1/2/3 for SK/TK1/TK2/TK3. Default is ${DEFAULT-VALUE}")
  private int regime;

  @Option(names = {"-blockSize","-bs"}, defaultValue = "8", description = "Cell size: 4 for SKINNY-64, 8 for SKINNY-128. Default is ${DEFAULT-VALUE}")
  private int blockSize;

  @Option(names = {"-distinguisherProbabilityExponent","-pr"}, defaultValue = "-1", description = "Positive exponent p for distinguisher probability 2^(-p). -1 uses the built-in value for the block size (55.34 for bs=4, 113.30 for bs=8).")
  private double distinguisherProbabilityExponent;

  @Option(names = {"-nbThreads","-t"}, defaultValue = "0", description = "Number of Gurobi threads. 0 lets Gurobi choose automatically. Default is ${DEFAULT-VALUE}")
  private int nbThreads;

  @Option(names = {"-timeLimit","-tl"}, defaultValue = "0", description = "Gurobi time limit in seconds (0 = no limit). Default is ${DEFAULT-VALUE}")
  private double timeLimit;

  @Option(names = {"-nonOptimalStep1Sols","-nonOpts1"}, description = "Keep additional STEP1 solutions from the Gurobi solution pool. Default is false")
  private boolean nonOptimalStep1Sols;

  @Option(names = {"-step1MinObjectiveValue","-s1obj"}, defaultValue = "-1", description = "Minimum allowed STEP1 objective. -1 means no lower bound. Default is ${DEFAULT-VALUE}")
  private int step1MinObjectiveValue;

  @Option(names = {"-step1MaxObjectiveValue","-s1maxobj"}, defaultValue = "-1", description = "Maximum allowed STEP1 objective. -1 means no upper bound. Default is ${DEFAULT-VALUE}")
  private int step1MaxObjectiveValue;

  @Option(names = {"-nbStep1Sols","-sols1"}, defaultValue = "1", description = "Maximum number of STEP1 solutions requested from the solution pool. Default is ${DEFAULT-VALUE}")
  private int nbStep1Sols;

  @Option(names = {"-step1output","-s1o"}, defaultValue = "output/step1.json", description = "STEP1 JSON output file. Default is ${DEFAULT-VALUE}")
  private File step1output;

  public static void main(final String... args) {
    System.exit(new CommandLine(new BoomerangSearch()).execute(args));
  }

  @Override
  public Integer call() {
    if (blockSize != 4 && blockSize != 8) {
      System.err.println("ERROR: -bs must be 4 or 8.");
      return 2;
    }
    if (regime < 0 || regime > 3) {
      System.err.println("ERROR: -tk must be one of 0, 1, 2, 3.");
      return 2;
    }
    if (nbRounds <= 0 || nExtbRounds < 0 || nExtfRounds < 0) {
      System.err.println("ERROR: -r must be > 0 and -rb/-rf must be >= 0.");
      return 2;
    }

    double pr = distinguisherProbabilityExponent;
    if (pr < 0) {
      pr = (blockSize == 4) ? 55.34 : 113.30;
    }
    if (pr <= 0) {
      System.err.println("ERROR: -pr must be positive, or -1 for the built-in default.");
      return 2;
    }

    File parent = step1output.getAbsoluteFile().getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      System.err.println("ERROR: cannot create output directory: " + parent);
      return 2;
    }

    System.out.printf(
      "STEP1 configuration: bs=%d, tk=%d, rb=%d, r=%d, rf=%d, pr=%.4f (probability = 2^(-pr))%n",
      blockSize, regime, nExtbRounds, nbRounds, nExtfRounds, pr);

    List<Step1Solution> step1Solutions = getStep1Solutions(pr);
    if (step1Solutions == null || step1Solutions.isEmpty()) {
      System.out.println("No STEP1 solution found. Exiting.");
      return 1;
    }

    Step1Solution.toFile(step1output, step1Solutions);

    Step1Solution best = step1Solutions.stream()
      .min(Comparator.comparingDouble(sol -> sol.objective))
      .orElse(step1Solutions.get(0));

    System.out.printf("BEST STEP1 TIME COMPLEXITY: 2^(%.4f)%n", best.objective);
    System.out.printf("  T1=%.4f, T2=%.4f, T3=%.4f, EPS=%d, epsilonp=%d, epsilonc=%d%n",
      best.T1, best.T2, best.T3, best.EPS, best.epsilonp, best.epsilonc);
    System.out.printf("  rb=%d, rb'=%d, rf=%d, rf'=%d, mb=%d, mb'=%d, mf=%d, mf'=%d%n",
      best.rb, best.rb_prime, best.rf, best.rf_prime, best.mb, best.mb_prime, best.mf, best.mf_prime);
    System.out.println("STEP1 solutions written to: " + step1output.getPath());

    return 0;
  }

  private List<Step1Solution> getStep1Solutions(final double pr) {
    try {
      GRBEnv env = new GRBEnv(true);
      env.set(GRB.IntParam.OutputFlag, verbose ? 1 : 0);
      env.start();

      Step1 step1 = new Step1(env, nbRounds, nExtbRounds, nExtfRounds, regime, blockSize, pr);
      if (verbose) {
        System.out.println("Starting STEP1 optimization");
      }

      List<Step1Solution> step1Solutions = step1.solve(
        nbStep1Sols,
        nonOptimalStep1Sols,
        step1MinObjectiveValue,
        step1MaxObjectiveValue,
        nbThreads,
        timeLimit);

      if (step1Solutions.isEmpty()) {
        System.out.println(verbose
          ? "No solution found (time limit reached or model is infeasible)."
          : "No solution found.");
      } else if (verbose) {
        double bestObjective = step1Solutions.stream().mapToDouble(sol -> sol.objective).min().orElse(Double.NaN);
        System.out.println("Best STEP1 objective exponent: " + bestObjective);
        System.out.println("Found " + step1Solutions.size() + " STEP1 solution(s)");
      }

      step1.dispose();
      env.dispose();
      return step1Solutions;
    } catch (GRBException e) {
      System.err.println("Gurobi error code " + e.getErrorCode() + ": " + e.getMessage());
      e.printStackTrace();
      return null;
    } catch (RuntimeException e) {
      System.err.println("STEP1 configuration/runtime error: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }
}
