
package common;

import static common.CommonVariables.FIELD_BOW;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */


public class CollectionStatistics {

    /**
     * Path of the directory in which the index is stored.
     */
    String      indexPath;

    /**
     * The IndexReader that will be used for reading the index.
     */
    IndexReader indexReader;

    /**
     * Total number of documents in the collection.
     */
    private long         docCount;
    /**
     * Total number of terms in the collection.
     */
    private long        colSize;
    /**
     * Field for which, the statistics will be made.
     */
    public String      field;
    /**
     * Total number of unique terms in the collection 'in that field'.
     * NOTE: This value is different from luke-Number Of Terms 
     *       Luke-Number Of Terms = number of terms in all fields taken together.
     */
    private int         uniqTermCount;

    /**
     * perTerm statistics of all the terms of collection.
     */
    public HashMap<String, PerTermStat> perTermStat;

    public long getDocCount() {return docCount;}
    public long getColSize() {return colSize;}
    public int getUniqueTermCount() {return uniqTermCount;}

    /**
     * Constructor
     * @param indexPath Path of the index
     * @param field The field of the index which will be searched
     * @throws IOException 
     */
    public CollectionStatistics(String indexPath, String field) throws IOException {
        indexReader = DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath()));
        this.field = field;
        perTermStat = new HashMap<>();
    }

    /**
     * Default constructor.
     */
    public CollectionStatistics() {
        perTermStat = new HashMap<>();
    }

    /**
     * Initialize collectionStat:<p>
     * docCount      - total-number-of-docs-in-index<p>
     * colSize       - collection-size<p>
     * uniqTermCount - unique terms in collection<p>
     * perTermStat   - cf, df of each terms in the collection <p>
     * @throws IOException 
     */
    public void buildCollectionStat() throws IOException {

        docCount = indexReader.maxDoc();      // total number of documents in the index

        Fields fields = MultiFields.getFields(indexReader);
        Terms terms = fields.terms(field);
        if(null == terms) {
            System.err.println("Field: "+field);
            System.err.println("Error buildCollectionStat(): terms Null found");
        }
        colSize = terms.getSumTotalTermFreq();  // total number of terms in the index in that field
        TermsEnum iterator = terms.iterator();
        BytesRef byteRef = null;

        while((byteRef = iterator.next()) != null) {
        //* for each word in the collection
            String t = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            int df = iterator.docFreq();           // df of 't'
            long cf = iterator.totalTermFreq();    // cf of 't'
            // idf = log(#docCount / (df+1) )
            double idf = Math.log((float)(docCount)/(float)(df+1));
            double norm_cf = (double)cf / (double)colSize;
            perTermStat.put(t, new PerTermStat(t, cf, df, idf, norm_cf));
        }
        uniqTermCount = perTermStat.size();

        System.out.println("Collection statistics built");
    }

    /**
     * Prints the basic statistics of the collection in the screen:<p>
     * Collection size, #documents in collection, #unique terms in collection,
     * Individual term df, cf in the collection.
     */
    public void showCollectionStat() {

        System.out.println("Collection Size: " + colSize);
        System.out.println("Number of documents in collection: " + docCount);
        System.out.println("NUmber of unique terms in collection: " + uniqTermCount);

        ///*
        for (Map.Entry<String, PerTermStat> entrySet : perTermStat.entrySet()) {
            String key = entrySet.getKey();
            PerTermStat value = entrySet.getValue();
            System.out.println("Term: <"+key + "> " + 
                "df: "+value.getDF() +" cf: "+value.getCF());
        }
        //*/
    }

    public float getCollectionProbability(String term) {
        long cf = perTermStat.get(term).getCF();
        long collSize = getColSize();

        System.out.print(term + " CF: "+cf+" CollSize: "+collSize);
        return (float)(cf+1)/(float)(collSize+1);
    }

    public void getQueryDocumentVector(int luceneDocid, String[] qTerms, float lambda) throws IOException {

        DocumentVector dv = new DocumentVector();
        if(this == null)
            System.out.println("CollectionStatistics is null");
        dv = dv.getDocumentVector(luceneDocid, this);

        // Term vector for this document and field, or null if term vectors were not indexed
        Terms terms = indexReader.getTermVector(luceneDocid, FIELD_BOW);
        if(null == terms) {
            System.err.println("Error: getQueryDocumentVector() Term vectors not indexed: "+luceneDocid);
            System.exit(1);
        }

        float totalScore = 0;
        for(String qTerm : qTerms) {
            PerTermStat perQueryStat = dv.docPerTermStat.get(qTerm);
            //System.out.println(qTerm);
            if(null != perQueryStat) {
                long tf = perQueryStat.getCF();
                long docSize = dv.getDocSize();
                System.out.print(qTerm+" TF: "+tf+" DocLen: "+docSize/*+" CF: "+cf+" CollSize: "+collSize*/);

                float singleTermScore = (float) Math.log(1+(float)((float)(1.0-lambda)*(float)tf/(float)docSize)/lambda * getCollectionProbability(qTerm));
                totalScore += singleTermScore;
                System.out.println(" Score: "+singleTermScore);
            }
//        (float)Math.log(1 + ((1 - lambda) * freq / docLen) /(lambda * ((LMStats)stats).getCollectionProbability()));
        }
        System.out.println("Total score: " + totalScore+"\n");
    }
    /**
     * Displays the vector of the document with luceneDocId
     * @param luceneDocid The lucene doc id of the corresponding document
     * @param indexReader The index reader
     * @throws IOException 
     */
    public void showDocumentVector(int luceneDocid, IndexReader indexReader) throws IOException {

        int docSize = 0;

        if(indexReader==null) {
            System.out.println("Error: null == indexReader in showDocumentVector(int,IndexReader)");
            System.exit(1);
        }

        // Term vector for this document and field, or null if term vectors were not indexed
        Terms terms = indexReader.getTermVector(luceneDocid, FIELD_BOW);
        if(null == terms) {
            System.err.println("Error: Term vectors not indexed: "+luceneDocid);
            System.exit(1);
        }

        System.out.println("Unique term count: " + terms.size());
        TermsEnum iterator = terms.iterator();
        BytesRef byteRef = null;

        while((byteRef = iterator.next()) != null) {
        //* for each word in the document
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            long docFreq = perTermStat.get(term).getDF();            // df of 't'
            long colFreq = perTermStat.get(term).getCF();            // cf of 't'
            long termFreq = iterator.totalTermFreq();    // tf of 't'
            System.out.println(term+": tf: "+termFreq + " df: "+docFreq
                + " cf: " + colFreq);
            docSize += termFreq;
        }
        System.out.println("DocSize: "+docSize);
    }

    
    public static void main(String[] args) throws IOException {

        String indexPath = "/store/collections/indexed/trec678";
        String field = "content";

        CollectionStatistics cs = new CollectionStatistics(indexPath, field);

        cs.buildCollectionStat();
        //cs.showCollectionStat();

        //* show the document vector of the document with 1th lucene-docid
        cs.showDocumentVector(1, cs.indexReader);
        DocumentVector dv = new DocumentVector();
        dv = dv.getDocumentVector(1, cs);
        dv.printDocumentVector();
    }
}

