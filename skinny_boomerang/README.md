# Boomerang Search on SKINNY

This folder contains the implementation of the GCRA boomerang-search framework for SKINNY.

The program uses **Gurobi** for the Step 1 truncated-boomerang search and **Choco Solver** for the Step 2 concrete-boomerang search and probability evaluation.

## Compiling and running

The project is compiled with PowerShell.

The required Java libraries are expected to be stored in a `newlibs/` directory located one level above this project directory. In particular, the project uses Gurobi, Choco Solver, Jackson, Picocli, JavaTuples, and `sandwichproba.jar`.

To compile the project, open PowerShell in the project directory and run:

```powershell
.\compile.ps1
```

The script compiles the Java source files and generates:

```text
boomerangsearch.jar
solutiontotikz.jar
```

It also creates the `output/` directory automatically.

If PowerShell blocks the script because it is not digitally signed, the execution policy can be changed for the current PowerShell session only:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

Then run:

```powershell
.\compile.ps1
```

### Step 1

Step 1 uses Gurobi to search for truncated boomerang structures.

For example:

```powershell
java -ea -jar boomerangsearch.jar -verbose -noStep2 -nonOpts1 "-bs=4" "-tk=2" "-rb=3" "-r=18" "-rf=4" "-s1o=output/(4,2)_step1.json" "-t=8" "-sols1=1"
```

The main parameters are:

```text
-bs       cell size: 4 for SKINNY-64, 8 for SKINNY-128
-tk       tweakey regime: 0 = SK, 1 = TK1, 2 = TK2, 3 = TK3
-rb       number of rounds extended before the distinguisher
-r        number of rounds in the boomerang distinguisher
-rf       number of rounds extended after the distinguisher
-t        number of Gurobi threads
-sols1    maximum number of Step 1 solutions
-s1o      Step 1 output JSON file
-noStep2  stop after Step 1
```

The complete list of command-line options can be displayed with:

```powershell
java -jar boomerangsearch.jar -help
```

### Step 2

Step 2 reads a Step 1 solution and uses Choco Solver to search for concrete boomerang differentials.

For example:

```powershell
java -jar boomerangsearch.jar -verbose "-bs=4" "-tk=2" "-s1i=output/(4,2)_step1.json" "-s2o=output/(4,2)_step2.json" "-cg=1"
```

The main Step 2 parameters are:

```text
-s1i      Step 1 input JSON file
-s2o      Step 2 output JSON file
-cg       cluster gap used in the probability computation
-sols2    maximum number of Step 2 solutions for each Step 1 solution
```

### Step 1 target-search mode

The option:

```text
-tl=<seconds>
```

sets a Gurobi time limit.

## Generation of visualizations

### HTML visualization

A Step 1 JSON solution can be converted into an HTML visualization with:

```powershell
.\run-visualize.ps1
```

The script uses the following default files:

```text
input : output/(8,2)_step1.json
output: output/(8,2)_step1_visual.html
```

A different input file, output file, and solution number can be specified as:

```powershell
.\run-visualize.ps1 "output/(4,2)_step1.json" "output/(4,2)_step1_visual.html" 0
```

The solution number starts from `0`.

### TikZ generation

TikZ output combining Step 1 and Step 2 can be generated with:

```powershell
.\run-tikz.ps1
```

The general form is:

```powershell
.\run-tikz.ps1 <Step1 JSON> <Step2 JSON> <output TEX> <Step1 solution number>
```

For example:

```powershell
.\run-tikz.ps1 "output/(4,2)_step1.json" "output/(4,2)_step2.json" "output/(4,2)_figure.tex" 0
```

The generated `.tex` file can then be compiled with LaTeX:

```powershell
pdflatex "output/(4,2)_figure.tex"
```

The underlying command-line tool is `solutiontotikz.jar`. Step 1 provides the grid and coloring, while Step 2 provides the concrete differential values that are overlaid on the Step 1 representation.

## Explanation for files and directories

- `step1/` contains the Gurobi model and Step 1 solution classes.
- `step2/` contains the Choco Solver model, Step 2 solution classes, and strategy construction.
- `solutiontotikz/` contains the TikZ and HTML visualization code.
- `BoomerangSearch.java` is the main command-line entry point.
- `compile.ps1` compiles the project and packages the runnable JAR files.
- `run-visualize.ps1` generates an HTML representation of a Step 1 solution.
- `run-tikz.ps1` combines Step 1 and Step 2 outputs into a TikZ/LaTeX figure.
- `boomerangsearch/` contains compiled `.class` files.
- `boomerangsearch.jar` is the runnable JAR for Step 1 and Step 2.
- `solutiontotikz.jar` is the runnable JAR for TikZ generation.
- `output/` contains JSON solutions and generated visualization files.
- `META-INF/` contains the JAR manifest files and dependency declarations.

## Continuous EPS model

The Step 1 EPS search treats filtering as one continuous process.

The lower extension is processed first, and the upper extension continues from the final lower-extension score. A model layer corresponds to a filtering event rather than to a raw key-guessing step.

