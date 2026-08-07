# Source Code

This repository contains the source code and experimental results for the automated analysis presented in our work on rectangle attacks against SKINNY.

The implementation is developed by modifying and extending existing automated cryptanalysis models. In particular, our code builds on the automated boomerang-search framework of **Delaune, Derbez, and Vavrille**, *Catching the Fastest Boomerangs: Application to SKINNY*, and the automated key-recovery model of **Dong, Qin, Sun, and Wang**, *Key Guessing Strategies for Linear Key-Schedule Algorithms in Rectangle Attacks*. We sincerely thank the authors for making their models and source code publicly available.

The different folders are:

- `skinny_boomerang`: the modified automated boomerang-search framework for SKINNY, including the search of truncated and concrete boomerang distinguishers.
- `key_guessing_and_structural_sequence`: the extended framework for optimizing the initial key-guessing strategy and structural precomputation-table sequence for a given distinguisher and its outer-round extensions.
- `distinguisher`: the distinguishers and attack results used in our work, together with the code for verifying their probabilities and the generated visualizations.
- `newlibs`: the Java libraries required by the different tools. Some libraries, especially `gurobi.jar`, may need to be replaced according to the local Gurobi installation.

More details about compilation, execution, and parameters are provided in the `README` file inside each corresponding folder.