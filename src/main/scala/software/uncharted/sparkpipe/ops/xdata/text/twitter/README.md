# Twitter Topic Modelling
This operation is an implementation of the xdata [topic modelling](https://stash.uncharted.software/projects/XDATA/repos/topic-modelling/browse) project. Below are the notes associated with that project:

## Original Research Notes:

This project is an extension of Biterm Topic Models (Yan et al. 2013)
Variations implemented here include:
  - A Dirichlet Process for nonparametric Bayesian inference so the model can grow in complexity
  - Option of using weights (TFIDF scores) on words rather than +/- 1 for sample recorder counts
  - The word dictionary can be inferred for each day separately from the input text.

 General inputs:
    rdd                             - key-value RDD partitoned by date (date, (tweet_id, tweet_text))

BDPParallel input:
    iterator: Iterator[(String, (String, String))]      - date-partitioned records (date, (tweet_id, tweet_text))
    stpbroad: Set[String]]          - set of stopwords
    iterN: Int                      - number of iterations (150)
    k:Int                           - number of k topics to start with (2)
    alpha: Double                   - Dirichelet clumpiness hyperparameter (1/e)
    eta: Double                     - Dirichelet     hyperparameter (0.01)

Optional inputs:
    word_dict/ words                - dictionary

BDP input:
    biterms: Array[Biterm]
    words: Array[String]
    iterN: Int, k: Int, alpha: Double, eta: Double


#### PARAMETERS (SEMI-FIXED)
var k = 2
For nonparametric models k represents the number of topics to *start* with


#### HYPERPARAMETERS  (SEMI-FIXED)
n.b. In Bayesian statistics a hyperparameter is a parameter over a prior distribution
i.e. a parameter over parameters. The term is used to distinguish them from the
parameters of of underlying model

val alpha = 1 / Math.E
  α is the Bayesian prior on the document-topic probabilities. It is also referred to
  as a concentration parameter or a scaling parameter. It can be thought of as
  representing our prior belief about about the data distribution; how 'clumpy' we
  believe the topics to be. Smaller values of alpha imply fewer topics, larger values, more topics.

  alpha specifies how strong the discretization of base distribution is. In the limit
  of α -> 0 the realizations are concentrated at a single value, while in the limit of
  α -> ∞ the realizations become continuous.

  The Gamma prior of α ∝ Γ(1,1) (i.e. α = 1/ℇ) used here has been commonly used as a
  good starting point in the literature and was empirically found to produce
  satisfactory topic clustering


val eta = 0.01
	η (also Gₒ)	is a parameter which represents the base measure (or base distribution)
	of the Dirichlet Process. The Dirichlet Process draws distributions around the
	base distribution analogous to how a normal distribution draws numbers around its mean.
  eta is a measure of the word/topic concentration; the 'smoothness' of the underlying
  Dirichlet distribution.

  The Gamma prior of η ∝ Γ(1,0.1)  (i.e. 0.01) was used following best-practices in the literature.


#### INPUT PARAMETERS
iterN:	the number of iterations of MCMC sampling to run
	- the given/default (150) is fine (empirically derived)

An array of dates
	- used to partition data by date & run each on a separate core
	- alternately list of dates could be inferred from the data

Path to Twitter data on HDFS
	- data must be preprocessed in two steps:
		(1) map to schema (date, id, text)
		(2) clean & normalize text


#### OTHER PARAMETERS
val lang = "en"
	- this actually isn't used in the example script
	- this allows you to possibly filter the input data by language

Stopwords
	- these are pretty much hard-coded. (stopwords don't change)
	- I have included stopword in resources. I will add some more to the repo.
	- if run globally (multilingual) concatenating all stopword files is fine

TFIDF scores
	- TFIDF score can optionally used as input to the sample recorders rather than +/-1.
	 This has the effect of discounting terms that are frequent over the whole corpus
	 while increasing the influence of terms that occur frequently in a short time span.
	- A separate stage computes TFIDF scores for a given corpus


#### OUTPUTTING RESULTS
topic_dist
	For annotating data you need to output the top N terms from topic_dist for
	each topic cluster.

(I write the results to a _local_ file for my own convenience.)


#### N.B.
theta:	global topic distribution
phi:	topic-word distribution
	- these are the parameters learned by MCMC inference.
	- It can be a good idea to keep them around (but not needed for annotation)
Note that once you have learned theta/phi you can subsequently use them to quickly
re-compute topic_dist, and/or quickly compute topic labels for a document
in the original set.

nzMap:	the number of documents in each cluster
	- probably not useful for annotation
