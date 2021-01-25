package VelichkoA;

import java.io.IOException;

public class BitInputStream {
    public BitInputStream(byte[] source, int codeValueBits) {
        mSource = source;
        mIndex = 0;
        mBitsToGo = 8;
        mCodeValueBits = codeValueBits;
    }

    public boolean getBit() throws IOException {
        if (mBitsToGo == 0) {
            if (mIndex < mSource.length - 1) {
                mIndex++;
            } else {
                if (mCodeValueBits > 0) {
                    mCodeValueBits -= Byte.SIZE;
                } else {
                    throw new IOException("EOF code not found");
                }
            }
            mBitsToGo = Byte.SIZE;
        }

        mBitsToGo--;
        int mask = 1 << mBitsToGo;
        return (mSource[mIndex] & mask) != 0;
    }

    private byte[] mSource;
    private int mIndex;
    private int mBitsToGo;
    private int mCodeValueBits;
}