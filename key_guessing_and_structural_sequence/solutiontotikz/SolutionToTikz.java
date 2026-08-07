package boomerangsearch.solutiontotikz;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
  name = "solutiontotikz",
  mixinStandardHelpOptions = true,
  version = "2.0-step1-only",
  description = "Convert a STEP1 JSON solution to TikZ"
)
public class SolutionToTikz implements Callable<Integer> {

  @Option(names = {"-solNumber","-sol"}, defaultValue = "0", description = "STEP1 solution index. Default is ${DEFAULT-VALUE}")
  private int solNumber;

  @Option(names = {"-step1input","-s1i"}, required = true, description = "STEP1 JSON file to convert to TikZ.")
  private String step1input;

  public static void main(final String... args) {
    System.exit(new CommandLine(new SolutionToTikz()).execute(args));
  }

  @Override
  public Integer call() {
    System.out.println(new Step1SolutionToTikz(step1input, solNumber).generate());
    return 0;
  }
}
