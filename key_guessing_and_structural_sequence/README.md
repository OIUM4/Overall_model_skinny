# STEP1 Key-Guessing and Structural-Sequence Search on SKINNY

This folder contains a **STEP1-only** implementation for searching key-guessing and structural sequences for boomerang attacks on SKINNY. The optimization model uses the **Gurobi** solver and outputs the best STEP1 time-complexity exponent together with the corresponding solution structure.

The current example is configured for **SKINNY-64-192**.

## Compiling and running

The project requires Java, PowerShell, Gurobi, a valid Gurobi license, and the required Java libraries.

The required JAR files are expected in a directory named `newlibs` located one level above the project directory:

```text
../newlibs/
    gurobi.jar
    picocli-4.3.2.jar
    jackson-annotations-2.11.0.jar
    jackson-core-2.11.0.jar
    jackson-databind-2.11.0.jar
```

To compile the code, open PowerShell in the project directory and run:

```powershell
.\compile.ps1
```

The compilation generates:

```text
boomerangsearch.jar
```

After compilation, STEP1 can be run directly with:

```powershell
.\run-step1.ps1
```

If PowerShell reports that the script is not digitally signed, temporarily allow local script execution in the current PowerShell session:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

Then run:

```powershell
.\compile.ps1
.\run-step1.ps1
```

The equivalent Java command for the current experiment is:

```powershell
java -ea -jar boomerangsearch.jar -verbose -nonOpts1 "-bs=4" "-tk=3" "-rb=4" "-r=22" "-rf=5" "-pr=57.36" "-s1o=output/step1_(4,3).json" "-t=8" "-sols1=2"
```

The main parameters are:

- `-bs=4`: use 4-bit cells, corresponding to the 64-bit block version of SKINNY.
- `-tk=3`: use the TK3 setting. Together with `-bs=4`, this corresponds to **SKINNY-64-192**.
- `-rb=4`: extend 4 rounds before the fixed distinguisher.
- `-r=22`: use a 22-round fixed distinguisher.
- `-rf=5`: extend 5 rounds after the fixed distinguisher.
- `-pr=57.36`: set the distinguisher probability to approximately `2^(-57.36)`.
- `-t=8`: use up to 8 Gurobi threads.
- `-sols1=2`: request up to 2 STEP1 solutions.
- `-s1o=output/step1_(4,3).json`: write the STEP1 solutions to this JSON file.
- `-nonOpts1`: keep additional solutions from the Gurobi solution pool.
- `-verbose`: print detailed optimization information.

Thus, the current attack structure is:

```text
4 outer rounds
      +
22-round distinguisher
      +
5 outer rounds
```

After optimization, the program prints the best STEP1 time complexity in the form:

```text
BEST STEP1 TIME COMPLEXITY: 2^(X.XXXX)
```

## Distinguisher configuration

The main structural information of the fixed distinguisher is defined in:

```text
step1/Step1.java
```

inside:

```java
articleTrails()
```

`articleTrails()` contains the fixed information describing the distinguisher, including the upper and lower differential trails and the variables used to determine the propagation around the external boundaries of the distinguisher.

Typical trail variables include:

```java
DXupper[round][row][column]
DXlower[round][row][column]
```

When testing a different distinguisher, the main changes are therefore:

1. modify the fixed trail and boundary information in `articleTrails()`;
2. set `-r` to the number of rounds of the distinguisher;
3. set `-pr` to its probability exponent;
4. set `-rb` and `-rf` to the required outer-round extensions;
5. recompile with `.\compile.ps1` and run STEP1 again.

For example, the current configuration uses:

```text
-rb=4
-r=22
-rf=5
-pr=57.36
```

so `articleTrails()` should contain the structural information corresponding to the 22-round distinguisher used in this experiment.

## Explanation for files and directories

- `step1/` contains the STEP1 Gurobi model, solution classes, tweakey model, and the `articleTrails()` distinguisher configuration.
- `solutiontotikz/` contains utilities for converting STEP1 solutions to TikZ/HTML-style representations.
- `BoomerangSearch.java` is the main command-line entry point.
- `compile.ps1` compiles the Java source files and packages the runnable JAR files.
- `run-step1.ps1` runs the current STEP1 experiment with the default SKINNY-64-192 parameters.
- `boomerangsearch/` contains compiled Java classes generated during compilation.
- `boomerangsearch.jar` is the runnable JAR for the STEP1 search.
- `solutiontotikz.jar` is the runnable JAR for the solution-visualization utilities.
- `output/` contains generated STEP1 JSON solution files.
- `META-INF/` contains the manifest files used when packaging the JAR files.
- `model.lp` is an exported optimization model that can be inspected with Gurobi-compatible tools.
