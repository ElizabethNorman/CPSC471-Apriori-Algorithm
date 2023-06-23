# CPSC471 Apriori Algorithm

Fast implementation of the Apriori Algorithm for Data Mining. This is a classic data mining algorithm, improved slightly to produce fast results. I call my improvement "bucketing": as new itemsets are generated, the itemsets are sorted and then clustered together (put into buckets) by the first item in the itemset. Itemsets are clustered together via HashMap<Integer, ArrayList \<ItemSet>> with the lowest item being the key. With this technique, as we scan the transaction database (transactions are sorted), we can skip over huge amounts of itemsets at once if the transaction does not contain the key.

The datasets provided by the professor are included in this repository, varying in size and range of item IDs. Their "difficulty" is reflected in their runtimes. Minimum support threshold demonstrated in the table below are the same numbers that the professor had used to test our algorithms. 

Runtimes:

|filename|threshold|runtime (seconds)|
|--|--|--|
|data.txt|0.50|0.054|
|1k5L.txt|0.01|0.368|
|retail.txt|0.03|1.305|
|t25i10d10k.txt|0.02|14.068|
|connect.txt|0.98|7.113|

## Submission readMe
### This is the readMe I submitted alongside the assignment, note that class files are not provided in this repository.

There should be 2 .java and 2 .class files contained in this folder. They are named:

	- Apriori
	- ItemSet

In your command line set to the folder containing these 4 files and the datasets, run the program with:

java Apriori filenameofyourchoice.txt 0.xx

Where filenameofyourchoice is one of the test data files and 0.xx represents a percentage for your desired minimum support threshold. 

Always include .txt in the filename. The file should follow the same format as the files provided with the assignment handout.

Percentages must be between 0.00 and 1.00. 

For example, if you want to run retail at a 50% minimum support threshold you would enter in:

java Apriori retail.txt 0.5

There are minimal checks for correct input, this is because I am hoping that you will follow these instructions carefully.

## Final Notes

- Like other school projects on my github (notably my file simulator), there are issues with the code and it's not as clean as I'd like. I could edit code afterwards to make it as presentable as possible, but I feel this would be an innaccurate reflection of the work I produced as a student. Much of this code was built in a handful of days and some of it is brute force, but we all make do in time crunches.
- I got 100% on this :) 
