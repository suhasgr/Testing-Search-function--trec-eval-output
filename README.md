Testing-Search-function--trec-eval-output
=========================================

Trec - eval format for serching using TF-IDF approach

Test the search performance with the TREC standardized topic collections.

software must output up to top 1000 search results to a result file in a format that
enables the trec_eval program to produce evaluation reports. trec_eval expects its input to
be in the format described below.


QueryID Q0 DocID Rank Score RunID

For example:
10 Q0 DOC-NO1 1 0.23 run-1
10 Q0 DOC-NO2 2 0.53 run-1
10 Q0 DOC-NO3 3 0.15 run-1
: : : : : :
11 Q0 DOC-NOk 1 0.042 run
