package VelichkoA;

class ModelMetrics {
    public final int codeValueBits;
    public final int frequencyBits;
    public final long maxCode;
    public final long maxFreq;
    public final long oneFourth;
    public final long oneHalf;
    public final long threeFourths;
    public final int EOF = 256;

    ModelMetrics(int codeValueBits, int frequencyBits) {
        this.codeValueBits = codeValueBits;
        this.frequencyBits = frequencyBits;
        maxCode = (1l << codeValueBits) - 1;
        maxFreq = (1l << frequencyBits) - 1;
        oneFourth = 1l << (codeValueBits - 2);
        oneHalf = 2 * oneFourth;
        threeFourths = 3 * oneFourth;
    }
}

class Probability {
    public long low;
    public long high;
    public long count;

    public Probability(long low, long high, long count) {
        this.low = low;
        this.high = high;
        this.count = count;
    }
}

public class AdaptiveModel {
    private long mCumulativeFreq[];
    private boolean mFrozen;
    private ModelMetrics mMetrics;

    public AdaptiveModel(int codeValueBits, int frequencyBits) {
        mCumulativeFreq = new long[256 + 2];
        mMetrics = new ModelMetrics(codeValueBits, frequencyBits);
        reset();
    }

    public ModelMetrics getMetrics() {
        return mMetrics;
    }

    public void reset() {
        for ( int i = 0 ; i < mCumulativeFreq.length ; i++ )
            mCumulativeFreq[i] = i;
        mFrozen = false;
    }

    private void update(int c) {
        int len = mCumulativeFreq.length;
        for ( int i = c + 1 ; i < len ; i++ )
            mCumulativeFreq[i]++;

        if (mCumulativeFreq[len - 1] >= mMetrics.maxFreq) {
            mFrozen = true;
        }
    }

    public Probability getFreqSegment(int c) {
        Probability s = new Probability(mCumulativeFreq[c], mCumulativeFreq[c + 1], getSumFreq());
        if (!mFrozen)
            update(c);
        return s;
    }

    public int getChar(long scaledVal) throws IllegalArgumentException {
        for (int i = 0; i < mCumulativeFreq.length - 1; i++)
            if (scaledVal < mCumulativeFreq[i + 1] ) {
                return i;
            }
        throw new IllegalArgumentException("Scaled value is incorrect");
    }

    public long getSumFreq() {
        return mCumulativeFreq[mCumulativeFreq.length - 1];
    }
}
