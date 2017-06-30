# Text Analysis Operations for the Uncharted Spark Pipeline #

`sparkpipe-text-analytics` provides text analysis and transformation operations developed in support of Uncharted's work on the DARPA XDATA program.  They can be used stand-alone, but are designed to run as part of a [sparkpipe](https://github.com/unchartedsoftware/sparkpipe-core) pipeline.

## Operations ##

Currently supported operations:

Analytics:
* TF-IDF on full text strings and word bags
* LDA on full text strings and word bags
* Corpus dictionary generation

Transforms:
* Column text to word bags
* Column text to MLLib vector
* Column word bag to MLLib vector

## Building ##

Java 1.7+, Scala 2.11+ and Spark 2.0+ are required to build the library and run tests.  The Gradle wrapper is used so there is *no* requirement to have Gradle installed.

After checking out the source code, the library binary can be built from the project root and installed locally as follows:

`./gradlew build install docs`

The resulting JAR will be available in `project_root/build/libs`.  A full set of archives (binary, sources, test sources, docs) can be published to a local Maven repository via:

`./gradlew publish`

The above command requires that `MAVEN_REPO_URL`, `MAVEN_REPO_USERNAME` and `MAVEN_REPO_PASSWORD` be defined as environment variables.

## Example ##

The steps below use the Spark shell to load a CSV file with a document column into a `DataFrame` and runs the TF-IDF analytict to generate weights.

From the command prompt:
```
mkdir ~/text_example
cd ~/text_example
cp project_root/build/libs/sparkpipe-text-analytics-0.2.0.jar ~/text_example
wget https://s3.ca-central-1.amazonaws.com/tiling-examples/patent_abstracts.zip
${SPARK_HOME}/bin/spark-shell --packages software.uncharted.sparkpipe:sparkpipe-core:1.1.0 --jars ~/text-example/sparkpipe-text-analytics-0.2.0.jar
```

In the spark shell type `:paste` and paste the following code: 
```scala
import software.uncharted.sparkpipe.Pipe
import software.uncharted.sparkpipe.ops.core.{dataframe => dfo}
import software.uncharted.sparkpipe.ops.text.analytics

val results = Pipe(spark)
.to(dfo.io.read(
  "patent_abstracts.csv",
  "com.databricks.spark.csv",
  Map("header" -> "true", "inferSchema" -> "true", "delimiter" -> "\t")
))
.to(analytics.textTFIDF("abstract", "tfidf_scores", retainedTerms = Some(10)))
.run

results.show()
```

 The resulting `DataFrame` is equivalent to the original, with the addition of a new column to store the map of [term, TFIDF score] tuples.