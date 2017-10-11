## wGMDH
Group Method of Data Handling in Java, wrapped as a Weka Classifier. The project started off as http://wgmdh.irb.hr/en/project/

![wGMDH shapshot](/example-image.png)

# Features
Multilayered Selectional Combinatorial GMDH with several performance measures:
- SSE
- RRSE
- Compound Error
  - a formulation of model performance which, besides error measure, includes model complexity in terms of execution time of the model on low-power embedded devices [1] [].

Used to detect and prevent overfitting, performance measure of models can be evaluated:
- by N-fold crossvalidation
- using train-test split.

Because traversing the entire set of possible solutions is combinatorially prohibitive, the package utilizes beam-search by limiting the number of models (aka neurons) which can be used as building blocks for more complex models. This can be done in one of the following two ways
- by keeping N best models (according to the selected performance measure) from the previous layer as candidates
- using Correlation-based feature selection
  - the rationale behind it is to preserve a set of candidates which perform well, but are additionally mutually different.
  - this is an innovation and unpublished, so drop me a line in case it works nicely for you.

# Dependencies
The .jar files in /Dependencies need to be available at compile-time.

# Considerations
wGMDH has not been maintained for a while and the last weka version version wGMDH has been tested with was weka 3.5.8. However, it can be easily invoked from code. Please check wGmdh.jGmdh.playground.Toy as an example of how to do it.

# Please cite
If you have used this code in your publication, please include references to [1], [2] and [3].

# References
[1] Marić, Ivan; Ivek, Ivan. Self-Organizing Polynomial Networks for Time-Constrained Applications. // IEEE transactions on industrial electronics. 58 (2011) , 5; 2019-2029: 10.1109/TIE.2010.2051934
[2] Marić, Ivan; Ivek, Ivan.
Compensation for Joule–Thomson effect in flowrate measurements by GMDH polynomial. // Flow measurement and instrumentation. 21 (2010) , 2; 134-142
[3] I.Ivek, "wGMDH." [Online]. Available: https://github.com/iivek/weka-gmdh