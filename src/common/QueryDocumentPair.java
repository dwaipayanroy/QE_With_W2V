
package common;

import static common.CommonVariables.FIELD_ID;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
public class QueryDocumentPair {
    private float lambda;

    String          propPath;
    Properties      prop;
    IndexReader     reader;
    IndexSearcher   searcher;
    String          indexPath;
    File            indexFile;
    String          stopFilePath;
    String          queryPath;
    File            queryFile;      // the query file
    int             queryFieldFlag; // 1. title; 2. +desc, 3. +narr
    String          []queryFields;  // to contain the fields of the query to be used for search
    Analyzer        analyzer;
    String          runName;
    int             numHits;
    boolean         boolIndexExists;
    String          resPath;        // path of the res file
    FileWriter      resFileWriter;  // the res file writer
    List<TRECQuery> queries;
    TRECQueryParser trecQueryparser;
    TRECQueryParser doidParser;
    int             simFuncChoice;
    float           param1, param2;
    CollectionStatistics cs;
    HashMap<String, TRECQuery> queryHashMap;

    public QueryDocumentPair(String propPath) throws IOException, Exception {

        this.propPath = propPath;
        prop = new Properties();
        try {
            prop.load(new FileReader(propPath));
        } catch (IOException ex) {
            System.err.println("Error: Properties file missing in "+propPath);
            System.exit(1);
        }
        //----- Properties file loaded

        // +++++ setting the analyzer with English Analyzer with Smart stopword list
        stopFilePath = prop.getProperty("stopFilePath");
        common.EnglishAnalyzerWithSmartStopword engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        //+++++ index path setting 
        indexPath = prop.getProperty("indexPath");
        indexFile = new File(indexPath);
        Directory indexDir = FSDirectory.open(indexFile);

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexPath);
            boolIndexExists = false;
            System.exit(1);
        }
        //----- index path set

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        queryFile = new File(queryPath);
        /* query path set */
        // TODO: queryFields unused

        /* constructing the query */
        trecQueryparser = new TRECQueryParser(queryPath, analyzer);
        doidParser = new TRECQueryParser(queryPath, null, FIELD_ID);
        queries = constructQueries();
        /* constructed the query */

        simFuncChoice = Integer.parseInt(prop.getProperty("similarityFunction"));
        if (null != prop.getProperty("param1"))
            param1 = Float.parseFloat(prop.getProperty("param1"));
        if (null != prop.getProperty("param2"))
            param2 = Float.parseFloat(prop.getProperty("param2"));

        /* setting reader and searcher */
        reader = DirectoryReader.open(FSDirectory.open(indexFile));

        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new DefaultSimilarity());

//        File fl = new File(resPath);
//        //if file exists, delete it
//        if(fl.exists())
//            System.out.println(fl.delete());
//
//        resFileWriter = new FileWriter(resPath, true);
//

        lambda = Float.parseFloat(prop.getProperty("LM_Lambda", "0.6"));
        cs = new CollectionStatistics(indexPath, "content");
        cs.buildCollectionStat();
        queryHashMap = new HashMap<>();
    }

    private List<TRECQuery> constructQueries() throws Exception {

        trecQueryparser.queryFileParse();
        return trecQueryparser.queries;
    }

    public void makeQueryHashMap() throws Exception {

        for (TRECQuery query : queries) {
            queryHashMap.put(query.qid, query);
//            System.out.println("Putting "+query.qid);
        }
    }

    public String[] getQueryTerms(String qid) throws Exception {
        TRECQuery q = queryHashMap.get(qid);
        if(null == q) {
            System.out.println("No query found with query-id: " + qid);
            return null;
        }
        else {
            Query luceneQuery = trecQueryparser.getAnalyzedQuery(q);
            return luceneQuery.toString("content").split(" ");
        }
    }

    public int getLuceneDocid(String docid) throws Exception {

        ScoreDoc[] hits = null;
        TopDocs topDocs = null;

        TopScoreDocCollector collector = TopScoreDocCollector.create(5, true);
        Query luceneQuery = doidParser.getAnalyzedQuery(docid);
        //System.out.println(luceneQuery.toString("content"));
        searcher.search(luceneQuery, collector);
        topDocs = collector.topDocs();
        hits = topDocs.scoreDocs;
        if(hits == null) {
            System.out.println("Nothing found");
            return -1;
        }
        else {
            return hits[0].doc;
        }
    }

    public static void main(String[] args) throws IOException, Exception {

        QueryDocumentPair collSearcher = null;

        /* // uncomment this if wants to run from inside Netbeans IDE
        args = new String[1];
        args[0] = "trec678-searcher.properties";
        //*/

        if(0 == args.length) {
            System.exit(1);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Using properties file: "+args[0]);
        collSearcher = new QueryDocumentPair(args[0]);

        collSearcher.makeQueryHashMap();

        String input;
        String qid;
        String docid;
        String[] qTerms;
        int luceneDocid;

        System.out.println("Score(t): Math.log(1+((1-LM_Lambda)*tf/docSize)/(lambda * cf/collSize))");
        do {
            System.out.println("Enter (qid docid) pair (space separated)\nType (e)xit to terminate: ");
            input = br.readLine();
            input = input.trim();
            if(input.charAt(0)=='e')
                break;
            qid = input.split(" ")[0];
            docid = input.split(" ")[1];
            qTerms = collSearcher.getQueryTerms(qid);
            System.out.println(Arrays.toString(qTerms));
            luceneDocid = collSearcher.getLuceneDocid(docid);
            //System.out.println("Lucene docid: "+luceneDocid);
            collSearcher.cs.getQueryDocumentVector(luceneDocid, qTerms, collSearcher.lambda);
        } while(true);
    }
}
