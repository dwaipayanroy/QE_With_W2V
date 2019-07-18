
package WordVectors;

import common.EnglishAnalyzerWithSmartStopword;
import common.TRECQuery;
import common.TRECQueryParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Character.isLetter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.lucene.analysis.Analyzer;
import org.xml.sax.SAXException;

/**
 *
 * @author dwaipayan
 * 
 * Calculates the similar terms of each of the word of the vocabulary dump
 * 
 */
public class WordVecs {

    static Properties  prop;       // the properties file
    public int         k;          // k in kNN; Number of NNs to precompute and store
    public HashMap<String, WordVec> wordvecmap;    // each word and its vector
    public HashMap<String, List<WordVec>> nearestWordVecsMap; // Store the pre-computed NNs after read from file
    static WordVecs singleTon;

    
    public WordVecs(String propPath) throws Exception {

        System.out.println(propPath);
        prop = new Properties();
        prop.load(new FileReader(propPath));
        
        if(prop.getProperty("vectorPath")!=null) {
            String wordvecFile = prop.getProperty("vectorPath");

            k = Integer.parseInt(prop.getProperty("k", "15"));
            wordvecmap = new HashMap();
            System.out.println("Loading word vectors");
            try (FileReader fr = new FileReader(wordvecFile);
                BufferedReader br = new BufferedReader(fr)) {
                String line;

                while ((line = br.readLine()) != null) {
                    WordVec wv = new WordVec(line);
                    if(isLegalToken(wv.word))
                        wordvecmap.put(wv.word, wv);
                }
            }
            System.out.println("Word vectors loaded");
        }
        else {
            System.err.println("vectorPath not set in properties");
            System.exit(1);
        }

    }


    public WordVecs(Properties prop) throws Exception { 

        this.prop = prop;

        if(prop.containsKey("vectorPath")) {
            String wordvecFile = prop.getProperty("vectorPath");

            k = Integer.parseInt(prop.getProperty("k", "15"));
            wordvecmap = new HashMap();
            System.out.println("Started loading all vectors into hashmap...");
            try (FileReader fr = new FileReader(wordvecFile);
                BufferedReader br = new BufferedReader(fr)) {
                String line;

                while ((line = br.readLine()) != null) {
                    WordVec wv = new WordVec(line);
                    if(isLegalToken(wv.word))
                        wordvecmap.put(wv.word, wv);
                }
            }
        }
        else {
            System.err.println("vectorPath not set in properties");
            System.exit(1);
        }
    }

    // for cross-validation
    public void setK(int k) {
        this.k = k;
    }

    public void printAllNNs() {
        for (Map.Entry<String, List<WordVec>> entry : nearestWordVecsMap.entrySet()) {
            List<WordVec> nns = entry.getValue();
            String word = entry.getKey();
            StringBuffer buff = new StringBuffer(word);
            buff.append(" ");
            for (WordVec nn : nns) {
                buff.append(nn.word).append(":").append(nn.querySim);
            }
            System.out.println("<");
            System.out.println(buff.toString());
            System.out.println(">");
        }
    }

    static public WordVecs createInstance(Properties prop) throws Exception {
        if(singleTon == null) {
            singleTon = new WordVecs(prop);
            singleTon.loadPrecomputedNNs();
            System.out.println("Precomputed NNs loaded");
        }
        return singleTon;
    }

    /**
     * compute the similar words for each of the words and store them in a file 
     */
    public void computeAndStoreNNs() throws FileNotFoundException {
        String nnDumpPath = prop.getProperty("nnDumpPath");
        if(nnDumpPath!=null) {
            File f = new File(nnDumpPath);
        }
        else {
            System.err.println("nnDumpPath missing in properties file");
            return;
        }

        System.out.println("Dumping the NNs in: "+ nnDumpPath);
        PrintWriter pout = new PrintWriter(nnDumpPath);

        System.out.println("Precomputing NNs for each word");

        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            System.out.println("Precomputing "+k+" NNs for " + wv.word);
            List<WordVec> nns = computeNNs(wv.word);
            if (nns != null) {
                pout.print(wv.word + "\t");
                for (WordVec nn : nns) {
                    pout.print(nn.word + ":" + nn.querySim + "\t");
                }
                pout.print("\n");
            }
        }
        pout.close();
    }

    /**
     * compute the similar words for each of query words and store them in a file.
     * NOTE: This function is only for fastening the process
     */
    public void computeAndStoreQueryNNs() throws FileNotFoundException, SAXException, Exception {

        EnglishAnalyzerWithSmartStopword engAnalyzer = new EnglishAnalyzerWithSmartStopword("/home/dwaipayan/smart-stopwords");
        Analyzer analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        String queryPath = prop.getProperty("queryPath");
        TRECQueryParser trecQueryparser;
        List<TRECQuery> queries;
        /* constructing the query */
        trecQueryparser = new TRECQueryParser(queryPath, analyzer);
        trecQueryparser.queryFileParse();
        queries = trecQueryparser.queries;
        /* constructed the query */

        String nnDumpPath = prop.getProperty("nnDumpPath");
        if(nnDumpPath!=null) {
            File f = new File(nnDumpPath);
        }
        else {
            System.err.println("nnDumpPath missing in properties file");
            return;
        }

        System.out.println("Dumping the NNs in: "+ nnDumpPath);
        PrintWriter pout = new PrintWriter(nnDumpPath);

        System.out.println("Precomputing NNs for each query word");

        for (TRECQuery query : queries) {
            String[] analyzedQuery = trecQueryparser.getAnalyzedQuery(query).toString("content").split(" ");
//            for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            for (String str : analyzedQuery) {
                str = str.replace("(", "").replace(")", "");
                System.out.println(str);
                WordVec wv = wordvecmap.get(str);
                if(null != wv) {
                    System.out.println("Precomputing "+k+" NNs for " + wv.word);
                    List<WordVec> nns = computeNNs(wv.word);
                    if (nns != null) {
                        pout.print(wv.word + "\t");
                        for (WordVec nn : nns) {
                            pout.print(nn.word + ":" + nn.querySim + "\t");
                        }
                        pout.print("\n");
                    }
                }
            }
        }
        pout.close();
    }

    /* returns the already computed similar words of 'queryWord' */
    public List<WordVec> getPrecomputedNNs(String queryWord) {
        return nearestWordVecsMap.get(queryWord);
    }


    /**
     * Compute the similar words of 'queryWord'
     * @param queryWord
     * @return 
     */
    public List<WordVec> computeNNs(String queryWord) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        
        WordVec queryVec = wordvecmap.get(queryWord);   // vector of the corresponding word is find out
        if (queryVec == null)
        // if the word has no vector embedded with it.
            return null;
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
        // calculate similarity with all the words of the collection
            WordVec wv = entry.getValue();
            if (wv.word.equals(queryWord))
                continue;
            wv.querySim = queryVec.cosineSim(wv);
            distList.add(wv);
        }

        // sort the list of words according to similarity with queryWord (descending)
        Collections.sort(distList);

        // return a list of min(k, distList.size()) items which are similar to queryWord
        return distList.subList(0, Math.min(k, distList.size()));
    }

    /**
     * Compute list of similar terms of the WordVec w and return
     * @param w
     * @return 
     */
    public List<WordVec> computeNNs(WordVec w) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        
        if (w == null)
            return null;
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            if (wv.word.equals(w.word)) // ignoring computing similarity with itself
                continue;
            wv.querySim = w.cosineSim(wv);
            distList.add(wv);
        }
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));        
    }

    /**
     * Compute list of similar terms of the WordVec w from feedbackTerms and return
     * @param w
     * @return 
     */
    public List<WordVec> computeNNs(WordVec w, HashMap<String, WordVec> feedbackTerms) {
        ArrayList<WordVec> distList = new ArrayList<>(feedbackTerms.size());
        
        if (w == null)
            return null;
        
        for (Map.Entry<String, WordVec> entry : feedbackTerms.entrySet()) {
            WordVec wv = entry.getValue();
            if (wv.word.equals(w.word)) // ignoring computing similarity with itself
                continue;
            wv.querySim = w.cosineSim(wv);
            distList.add(wv);
        }
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));        
    }

    public double getSim(String u, String v) {
        WordVec uVec = wordvecmap.get(u);
        WordVec vVec = wordvecmap.get(v);
        if (uVec == null || vVec == null) {
//            System.err.println("words not found...<" + ((uVec == null)?u:v) + ">");
            return 0;
        }

        return uVec.cosineSim(vVec);
    }

    private boolean isLegalToken(String word) {
        boolean flag = true;
        for ( int i=0; i< word.length(); i++) {
//            if(isDigit(word.charAt(i))) {
//                flag = false;
//                break;
//            }
            if(isLetter(word.charAt(i))) {
                continue;
            }
            else {
                flag = false;
                break;
            }
        }
        return flag;
    }
    
    /**
     * load the precomputed NNs into hash table 
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void loadPrecomputedNNs() throws FileNotFoundException, IOException {
        nearestWordVecsMap = new HashMap<>();
        String nnDumpPath = prop.getProperty("nnDumpPath");
        if (nnDumpPath == null) {
            System.out.println("NNDumpPath Null while reading");
            return;
        }
        System.out.println("Reading from the NN dump at: "+ nnDumpPath);
        File nnDumpFile = new File(nnDumpPath);
        
        try (FileReader fr = new FileReader(nnDumpFile);
            BufferedReader br = new BufferedReader(fr)) {
            String line;
            
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, " \t:");
                List<String> tokens = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    tokens.add(token);
                }
                List<WordVec> nns = new LinkedList();
                int len = tokens.size();
                //System.out.print(tokens.get(0)+" > ");
                for (int i=1; i < len-1; i+=2) {
                    nns.add(new WordVec(tokens.get(i), Float.parseFloat(tokens.get(i+1))));
                    //System.out.print(tokens.get(i) + ":" + tokens.get(i+1));
                }
                //System.out.println();
                nearestWordVecsMap.put(tokens.get(0), nns);
            }
            System.out.println("NN dump has been reloaded");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {

        String usage = "Usage: java WordVecs <properties-path>\n"
            + "Properties file must contain:\n"
            + "1. vectorPath = path of the word2vec trained .vec file\n"
            + "2. nnDumpPath = path of the file, in which the precomputed NNs will be stored\n"
            + "3. k = number of NNs to precompute and store\n"
            + "4. [queryPath] = path of the query file";

        /*
        args = new String[1];
        args[0] = "/home/dwaipayan/preComputedNNs.properties";
        //*/

        if(args.length == 0) {
            System.out.println(usage);
            System.exit(1);
	}
        try {
	    WordVecs wv = new WordVecs(args[0]);
            if(prop.containsKey("queryPath"))
                wv.computeAndStoreQueryNNs();
            else
                wv.computeAndStoreNNs();

            /* // to print all precomputed NNs
            qe.loadPrecomputedNNs();
            List<WordVec> nwords = qe.computeNNs("conclus");
            for (WordVec word : nwords)
                System.out.println(word.word + "\t" + word.querySim);
            */
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

    

