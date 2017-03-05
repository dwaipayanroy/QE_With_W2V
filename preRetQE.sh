#!/bin/bash
# Generate the properties file and consequently execute the preRetQE program

docIdFieldName="docid"
fieldToSearch="content"
stopFilePath="/home/dwaipayan/smart-stopwords"
composeQuery=true

if [ $# -ne 6 ]
then
    echo "Usage: " $0 " 6-arguments";
    echo "1. collection-name: The prop file will be made having this name";
    echo "2. Index path: Absolute address of the Lucene index";
    echo "3. Query path: Absolute address of the query file in proper XML format";
    echo "4. Vector path: Absolute address of the .vec file, trained using word2vec";
	echo "5. k: Number of pre-computed NNs to use as expansion terms";
	echo "6. queryMix: [0.0-1.0] e.g. 0.6";
    exit 1;
fi

prop_name=$1"-preRetQE.properties"
#echo $prop_name

indexPath=$2
queryPath=$3
vectorPath=$4
k=$5
queryMix=$6

cd build/classes

# making the .properties file
cat > $prop_name << EOL

indexPath=$indexPath

queryPath=$queryPath

fieldToSearch=$fieldToSearch

docIdFieldName=$docIdFieldName

stopFilePath=$stopFilePath

numHits= 1000

### Similarity functions:
#0 - DefaultSimilarity
#1 - BM25Similarity
#2 - LMJelinekMercerSimilarity
#3 - LMDirichletSimilarity
similarityFunction=2

param1=0.6
param2=0.0

vectorPath=$vectorPath

k=$k

queryMix=$queryMix

composeQuery=$composeQuery

EOL
# .properties file made

date
java -cp $CLASSPATH:./lib/Common.jar:./lib/WordVectors.jar:./lib/jsoup-1.7.3.jar:./lib/lucene-analyzers-common-4.10.4-SNAPSHOT.jar:./lib/lucene-core-4.10.4-SNAPSHOT.jar:./lib/lucene-queries-4.10.4-SNAPSHOT.jar:./lib/lucene-queryparser-4.10.4-SNAPSHOT.jar:./lib/sax2r2.jar -Xmx6g QEUsingW2V.PreRetrievalQE  $prop_name
date
