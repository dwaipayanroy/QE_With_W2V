/**
 * COMPLETE.
*/
package QEUsingW2V;

/**
 *
 * @author dwaipayan
 */
    
import WordVectors.WordVec;
import WordVectors.WordVecs;
import common.CollectionStatistics;
import common.TRECQuery;
import common.TRECQueryParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */


public class PreRetrievalQE {

    Properties      prop;
    CollectionStatistics    collStat;
    String          indexPath;
    String          queryPath;      // path of the query file
    File            queryFile;      // the query file
    IndexReader     reader;
    IndexSearcher   searcher;
    String          stopFilePath;
    String          resPath;        // path of the res file
    FileWriter      resFileWriter;  // the res file writer
    int             numHits;      // number of document to retrieveWithExpansionTermsFromFile
    String          runName;        // name of the run
    List<TRECQuery> queries;
    File            indexFile;          // place where the index is stored
    Analyzer        analyzer;           // the analyzer
    boolean         boolIndexExists;    // boolean flag to indicate whether the index exists or not
    String          fieldToSearch;  // the field in the index to be searched (e.g. 'content')
    String          docIdFieldName;     // the field name of the unique-docid (e.g. 'docid')
    TRECQueryParser trecQueryParser;
    int             simFuncChoice;
    float           param1, param2;
    String          retFunction;        // retrieval function name

    String          nnDumpPath;     // path to the file containing the precomputed NNs
    int             k;              // k terms to be added in the query
    float           QMIX;
    WordVecs        wordVecs;
    boolean         toCompose;

    public PreRetrievalQE(Properties prop) throws IOException, Exception {

        this.prop = prop;
        /* property file loaded */

        // +++++ setting the analyzer with English Analyzer with Smart stopword list
        stopFilePath = prop.getProperty("stopFilePath");
        common.EnglishAnalyzerWithSmartStopword engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        /* index path setting */
        indexPath = prop.getProperty("indexPath");
        System.out.println("Using index at: " + indexPath);
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile.toPath());

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexFile.getAbsolutePath());
            System.out.println("Terminating");
            boolIndexExists = false;
            System.exit(1);
        }
        fieldToSearch = prop.getProperty("fieldToSearch","content");
        System.out.println("Field: "+fieldToSearch+ " of index will be searched.");
        /* index path set */
        docIdFieldName = prop.getProperty("docIdFieldName","docid");

        simFuncChoice = Integer.parseInt(prop.getProperty("similarityFunction"));
        param1 = Float.parseFloat(prop.getProperty("param1"));
        param2 = Float.parseFloat(prop.getProperty("param2"));

        /* setting reader and searcher */
        reader = DirectoryReader.open(FSDirectory.open(indexFile.toPath()));
        searcher = new IndexSearcher(reader);
        setSimilarityFunction(simFuncChoice, param1, param2);
        /* reader and searher set */

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        queryFile = new File(queryPath);
        /* query path set */

        /* constructing the query */
        queries = constructQueries();
        /* constructed the query */
        trecQueryParser = new TRECQueryParser(queryPath, analyzer, fieldToSearch);

        numHits = Integer.parseInt(prop.getProperty("numHits", "1000"));

        /* All word vectors are loaded in wordVecs.wordvecmap */
        wordVecs = new WordVecs(prop);
        k = Integer.parseInt(prop.getProperty("k"));
        QMIX = Float.parseFloat(prop.getProperty("queryMix"));
        toCompose = Boolean.parseBoolean(prop.getProperty("composeQuery"));

        /* setting res path */
        setRunName_ResFileName();
        resFileWriter = new FileWriter(resPath);
        System.out.println("Result will be stored in: "+resPath);
        /* res path set */
    }

    private void setSimilarityFunction(int choice, float param1, float param2) {

        switch(choice) {
            case 0:
                searcher.setSimilarity(new DefaultSimilarity());
                retFunction = "Default";
                break;
            case 1:
                searcher.setSimilarity(new BM25Similarity(param1, param2));
                retFunction = "BM25-"+param1+"-"+param2;
                break;
            case 2:
                searcher.setSimilarity(new LMJelinekMercerSimilarity(param1));
                retFunction = "LMJM-"+param1;
                break;
            case 3:
                searcher.setSimilarity(new LMDirichletSimilarity(param1));
                retFunction = "LMDir-"+(int)param1;
                break;
        }
    }

    private void setRunName_ResFileName() {

        String vectorPath = new File(prop.getProperty("vectorPath")).getName();

        runName = queryFile.getName()+"-"+retFunction+"-preRetQE"+vectorPath;
        if(toCompose)
            runName = runName + "-composed";
        runName = runName.replace(" ", "").replace("(", "").replace(")", "").replace("00000", "");
        runName = runName.concat("-" + k + "-" + QMIX);
        if(null == prop.getProperty("resPath"))
            resPath = "/home/dwaipayan/";
        else
            resPath = prop.getProperty("resPath");
        resPath = resPath+runName + ".res";
    }

    /**
     * Parses the query from the file and makes a List<TRECQuery> 
     *  containing all the queries
     * @return
     * @throws Exception 
     */
    private List<TRECQuery> constructQueries() throws Exception {

        System.out.println("Reading queries from: "+queryPath);
        TRECQueryParser parser = new TRECQueryParser(queryPath, analyzer);
        parser.queryFileParse();
        return parser.queries;
    }

    /**
     * Makes Q' = vec(Q) U Qc
     * @throws Exception 
     * @return
     */
    public List<WordVec> makeQueryVectorForms(String qTerms[]) throws Exception {
        
        WordVec singleWV = null;

        List<WordVec> vec_Q = new ArrayList<>();
        List<WordVec> q_c = new ArrayList<>();
        List<WordVec> q_prime = new ArrayList<>();

        // vec(Q)
        for (String qTerm : qTerms) {
            singleWV = wordVecs.wordvecmap.get(qTerm);
            if(null != singleWV) {   // query t has a vector associated with it
                singleWV.norm = singleWV.getNorm();
                singleWV.word = qTerm;
                vec_Q.add(singleWV);
            }
        }
        q_prime.addAll(vec_Q);
        // --- original query-term vectors are added

        if(true == toCompose) {
        // Qc
            System.out.println("Composing ");
            for (int i = 0; i < vec_Q.size()-1; i++) {
                singleWV = vec_Q.get(i).add(vec_Q.get(i), vec_Q.get(i+1));
                singleWV.norm = singleWV.getNorm();
                singleWV.word = vec_Q.get(i).word+"+"+vec_Q.get(i+1).word;
                q_c.add(singleWV);
            }
            q_prime.addAll(q_c);
        }
        // --- composed query terms are added

        return q_prime;
    }

    /**
     * Returns a hashmap of similar terms of q_prime, computed over the entire vocabulary
     * @param q_prime List of the vectors of the query terms as well as the pairwise composed forms
     * @return Hashmap of terms from across the collection which are similar to q_prime
     * @throws IOException 
     */
    public HashMap<String, WordProbability> computeAndSortExpansionTerms (List<WordVec> q_prime) throws IOException {

        List<WordVec> sortedExpansionTerms = new ArrayList<>();
        for (WordVec wv : q_prime) {
            //System.out.println(wv.word);
            sortedExpansionTerms.addAll(wordVecs.computeNNs(wv));
        }
        // sortedExpansionTerms now contains similar terms of query terms (unsorted)

        Collections.sort(sortedExpansionTerms, new Comparator<WordVec>(){
            @Override
            public int compare(WordVec t1, WordVec t2) {
                return t1.querySim<t2.querySim?1:t1.querySim==t2.querySim?0:-1;
            }});
        // sortedExpansionTerms now sorted

        int expansionTermCount = 0;
        double norm = 0;
        HashMap<String, WordProbability> hashmap_et = new LinkedHashMap<>();  // to contain M terms with top P(w|R) among each w in R
        for (WordVec singleTerm : sortedExpansionTerms) {
            if (null == hashmap_et.get(singleTerm.word)) {
                hashmap_et.put(singleTerm.word, new WordProbability(singleTerm.word, (float)singleTerm.querySim));
                expansionTermCount++;
                norm += singleTerm.querySim;
                if(expansionTermCount>=k)
                    break;
            }
            //* else: The t is already entered in the hash-map 
        }

        // +++ normalization
        for (Map.Entry<String, WordProbability> entrySet : hashmap_et.entrySet()) {
            String key = entrySet.getKey();
            WordProbability value = entrySet.getValue();
            value.p_w_given_R /= norm;
        }
        // --- normalization

        return hashmap_et;
    }


    /**
     * Returns MLE of a query term q in Q;<p>
     * P(w|Q) = tf(w,Q)/|Q|
     * @param qTerms all query terms
     * @param qTerm query term under consideration
     * @return 
     */
    public float returnMLE_of_q_in_Q(String[] qTerms, String qTerm) {

        int count=0;
        for (String queryTerm : qTerms)
            if (qTerm.equals(queryTerm))
                count++;
        return ( (float)count / (float)qTerms.length );
    } // ends returnMLE_of_w_in_Q()

    /**
     * Returns the new formed, expanded query
     * @param qTerms[] The initial query terms
     * @return BooleanQuery - The expanded query in boolean format
     * @throws Exception 
     */
    public BooleanQuery makeNewQuery(String qTerms[]) throws Exception {

        List<WordVec> q_prime = makeQueryVectorForms(qTerms);

//        List<WordVec> expansionTerms = returnPreRetrievalExpansionTerms(qTerms);
//        HashMap<String, WordProbability> hashmap_et = sortExpansionTerms(q_prime, expansionTerms);
        HashMap<String, WordProbability> hashmap_et = computeAndSortExpansionTerms(q_prime);
        // Now hashmap_et contains all the expansion terms (normalized weights). No query specific information.

        /*
        for (WordVec wv : expansionTerms) 
            System.out.print(wv.word+" ");
        System.out.println();
        */

        BooleanQuery booleanQuery = new BooleanQuery();
        Term thisTerm;


        float normFactor;

        normFactor = 0;
        //* Each w of hashmap_et: existing-weight to be QMIX*existing-weight 
        for (Map.Entry<String, WordProbability> entrySet : hashmap_et.entrySet()) {
            String key = entrySet.getKey();
            WordProbability value = entrySet.getValue();
            value.p_w_given_R = value.p_w_given_R * QMIX;
            normFactor += value.p_w_given_R;
        }

        // Now weight(w) = QMIX*existing-weight 
        //* Each w which are also query terms: weight(w) += (1-QMIX)*P(w|Q)
        //      P(w|Q) = tf(w,Q)/|Q|
        for (String qTerm : qTerms) {
            WordProbability existingTerm = hashmap_et.get(qTerm);
            float newWeight = (1.0f-QMIX) * returnMLE_of_q_in_Q(qTerms, qTerm);
            if (null != existingTerm) { // qTerm is already in hashmap_et
                existingTerm.p_w_given_R += newWeight;
            }
            else  // the qTerm is not in R
                hashmap_et.put(qTerm, new WordProbability(qTerm, newWeight));
            normFactor += newWeight;
        }

        for (Map.Entry<String, WordProbability> entrySet : hashmap_et.entrySet()) {
            thisTerm = new Term(fieldToSearch, entrySet.getKey());
            Query tq = new TermQuery(thisTerm);
            tq.setBoost((float)entrySet.getValue().p_w_given_R/normFactor);
            BooleanQuery.setMaxClauseCount(4096);
            booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
            //System.out.println(singleTerm.word+"^"+singleTerm.querySim);
        }

        return booleanQuery;
    }

    public void retrieveAll() throws Exception {

        ScoreDoc[] hits;
        TopDocs topDocs;
        TopScoreDocCollector collector;
        List<WordVec> preRetrievalExpTerms;
//        FileWriter baselineRes = new FileWriter(resPath+".baseline");

        for (TRECQuery query : queries) {
            collector = TopScoreDocCollector.create(numHits);
            Query luceneQuery = trecQueryParser.getAnalyzedQuery(query);

            System.out.println(query.qid+": Initial query: " + luceneQuery.toString(fieldToSearch));

            BooleanQuery bq = makeNewQuery(luceneQuery.toString(fieldToSearch).split(" "));

            System.out.println(bq.toString(fieldToSearch));
            searcher.search(bq, collector);
            topDocs = collector.topDocs();
            hits = topDocs.scoreDocs;
            int hits_length = hits.length;
            System.out.println(hits_length + " results retrieved  for query: " + query.qid);

            // +++ Writing the result file 
            resFileWriter = new FileWriter(resPath, true);
            StringBuffer resBuffer = new StringBuffer();
            for (int i = 0; i < hits_length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                resBuffer.append(query.qid).append("\tQ0\t").
                    append(d.get(docIdFieldName)).append("\t").
                    append((i)).append("\t").
                    append(hits[i].score).append("\t").
                    append(runName).append("\n");
            }
            resFileWriter.write(resBuffer.toString());
            resFileWriter.close();
            // --- result file written
        } // ends for each query
    } // ends retrieveAll

    public static void main(String[] args) throws IOException, Exception {

        Properties prop = new Properties();
        if(1 != args.length) {
            System.out.println("Usage: java PreRetrievalQE <.properties>");
            System.exit(1);
        }

        prop.load(new FileReader(args[0]));
        PreRetrievalQE preRetqe = new PreRetrievalQE(prop);

        preRetqe.retrieveAll();
    }
}