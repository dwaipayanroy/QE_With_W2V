
package common;

/**
 *
 * @author dwaipayan
 */
public class PerTermStat {

    /**
     * The actual t for which the statistics is getting calculated and stored
     */
    public String  t;

    /**
     * cf with respect to collection; tf with respect to document
     */
    private long    cf;

    /**
     * df with respect to collection; 1 with respect to document
     */
    private long    df;

    /**
     * idf = log(#-of-Document-in-collection / (df+1) )
     */
    private double  idf;

    /**
     * Normalized collection frequency <p>
     * Collection: norm_cf(w,C) = cf(w) / |colSize|<p>
     * Document: norm_cf(w,d) = tf(w,d) / |docCount|<p>
     */
    private double  norm_cf;

    public long getCF() {return cf;}
    public long getDF() {return df;}
    public double getIDF() {return idf;}
    public double getNormalizedCF() {return norm_cf;}

    public void setCF(long cf) {this.cf = cf;}
    public void setDF(long df) {this.df = df;}
    public void setIDF(double idf) {this.idf = idf;}
    public void setNormalizedCF(double norm_cf) {this.norm_cf = norm_cf;}

    public PerTermStat() {        
    }

    public PerTermStat(String term, long cf, long df) {
        this.t = term;
        this.cf = cf;
        this.df = df;
    }

    public PerTermStat(String term, long cf, long df, double idf, double rcf) {
        this.t = term;
        this.cf = cf;
        this.df = df;
        this.idf = idf;
        this.norm_cf = rcf;
    }
}
