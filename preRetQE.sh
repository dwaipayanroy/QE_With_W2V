#!/bin/bash
# Generate the properties file and consequently execute the preRetQE program

homepath=`eval echo ~$USER`
stopFilePath="$homepath/smart-stopwords"

docIdFieldName="docid"
fieldToSearch="content"
composeQuery=true
resPath="$homepath/"

if [ $# -ne 7 ]
then
    echo "Usage: " $0 " 5 arguments";
    echo "1. Index path: Absolute address of the Lucene index";
    echo "2. Query path: Absolute address of the query file in proper XML format";
    echo "3. Vector path: Absolute address of the .vec file, trained using word2vec";
    echo "4. Result path: ";
    echo "5. k: Number of pre-computed NNs to use as expansion terms";
    echo "6. queryMix: [0.0-1.0] e.g. 0.6";
    echo "7. Compose query: true/false";
    exit 1;
fi

prop_name="preRetQE.properties"
#echo $prop_name

indexPath=$1
queryPath=$2
vectorPath=$3
resPath=$4"/"
k=$5
queryMix=$6
composeQuery=$7

cd build/classes

# making the .properties file
cat > $prop_name << EOL

indexPath=$indexPath

queryPath=$queryPath

fieldToSearch=$fieldToSearch

docIdFieldName=$docIdFieldName

stopFilePath=$stopFilePath

resPath=$resPath

numHits= 1000

### Similarity functions:
#0 - DefaultSimilarity
#1 - BM25Similarity
#2 - LMJelinekMercerSimilarity
#3 - LMDirichletSimilarity
similarityFunction=2

param1=0.5
param2=0.0

vectorPath=$vectorPath

resPath=$resPath

k=$k

queryMix=$queryMix

composeQuery=$composeQuery

EOL
# .properties file made

date
java -cp $CLASSPATH:./lib/jsoup-1.7.3.jar:./lib/lucene-analyzers-common-5.3.1.jar:./lib/lucene-core-5.3.1.jar:./lib/lucene-queries-5.3.1.jar:./lib/lucene-queryparser-5.3.1.jar:./lib/lucene-backward-codecs-5.3.0.jar:./lib/sax2r2.jar -Xmx6g QEUsingW2V.PreRetrievalQE  $prop_name
date
