
package common;

import static common.CommonVariables.FIELD_BOW;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */
public class DocumentVector {

    /**
     * PerTermStat of the Document.
     */
    public HashMap<String, PerTermStat>     docPerTermStat;
    /**
     * Size of the Document.
     */
    private int                              size;
    /**
     * The retrieval score of the document after a retrieval. *Mostly unused*
     */
    private float                            docScore;   // retrieval score

    public DocumentVector() {
        docPerTermStat = new HashMap<>();
    }

    public DocumentVector(HashMap<String, PerTermStat> docVec, int size) {
        this.docPerTermStat = docVec;
        this.size = size;
    }

    public DocumentVector(HashMap<String, PerTermStat> docVec, int size, float docScore) {
        this.docPerTermStat = docVec;
        this.size = size;
        this.docScore = docScore;
    }

    public HashMap getDocPerTermStat() {return docPerTermStat;}
    public int getDocSize() {return size;}
    public float getDocScore() {return docScore;}

    /**
     * Returns the document vector for a document with lucene-docid=luceneDocId
       Returns dv containing 
      1) docPerTermStat: a HashMap of (t,PerTermStat) type
      2) size : size of the document
     * @param luceneDocId
     * @param cs
     * @return document vector
     * @throws IOException 
     */
    public DocumentVector getDocumentVector(int luceneDocId, CollectionStatistics cs) throws IOException {

        DocumentVector dv = new DocumentVector();
        int docSize = 0;

        if(cs.indexReader==null) {
            System.out.println("Error: null == indexReader in showDocumentVector(int,IndexReader)");
            System.exit(1);
        }

        // t vector for this document and field, or null if t vectors were not indexed
        Terms terms = cs.indexReader.getTermVector(luceneDocId, FIELD_BOW);
        if(null == terms) {
            System.err.println("Error getDocumentVector(): Term vectors not indexed: "+luceneDocId);
            return null;
        }

        TermsEnum iterator = terms.iterator(null);
        BytesRef byteRef = null;

        //* for each word in the document
        while((byteRef = iterator.next()) != null) {
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            //int docFreq = iterator.docFreq();            // df of 't'
            long termFreq = iterator.totalTermFreq();    // tf of 't'
            //System.out.println(t+": tf: "+termFreq);
            docSize += termFreq;

            //* termFreq = cf, in a document; df = 1, in a document
            //dv.docPerTermStat.put(t, new PerTermStat(t, termFreq, 1));
            dv.docPerTermStat.put(term, new PerTermStat(term, termFreq, 1, cs.perTermStat.get(term).getIDF(), (double)termFreq/(double)cs.getColSize()));
        }
        dv.size = docSize;
        //System.out.println("DocSize: "+docSize);

        return dv;
    }

    /**
     * Returns true if the docPerTermStat is not-zero; else, return false.
     * @return 
     */
    public boolean printDocumentVector() {

        if(this == null) {
            System.err.println("Error: printing document vector. Calling docVec null");
            System.exit(1);
        }
        if(0 == this.docPerTermStat.size()) {
            System.out.println("Error: printing document vector. Calling docVec zero");
            return false;
        }
        for (Map.Entry<String, PerTermStat> entrySet : this.docPerTermStat.entrySet()) {
            String key = entrySet.getKey();
            PerTermStat value = entrySet.getValue();
            System.out.println(key + " : " + value.getCF());
        }
        return true;
    }

    /** 
     * Returns the TF of 'term' in 'dv'.
     * @param term The term
     * @param dv Document vector
     * @return Returns the TF of 'term' in 'dv'
     */
    public long getTf(String term, DocumentVector dv) {
        PerTermStat t = dv.docPerTermStat.get(term);
        if(null != t)
            return t.getCF();
        else
            return 0;
    }
}
