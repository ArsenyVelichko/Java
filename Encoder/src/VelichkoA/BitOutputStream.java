package VelichkoA;

import java.io.ByteArrayOutputStream;

public class BitOutputStream {
    public BitOutputStream() {
        mOutput = new ByteArrayOutputStream();
        mNextByte = 0;
        mBitInd = Byte.SIZE;
    }

    public void putBit(boolean bit) {
        mBitInd--;

        if (bit) {
            int mask = 1 << mBitInd;
            mNextByte |= mask;
        }

        if (mBitInd == 0) {
            mOutput.write(mNextByte);
            mNextByte = 0;
            mBitInd = Byte.SIZE;
        }
    }

    public byte[] toByteArray() {
        if (mBitInd != Byte.SIZE)
            mOutput.write(mNextByte);

        if (mOutput.size() % 2 == 1) {
            mOutput.write(0);
        }

        return mOutput.toByteArray();
    }

    private ByteArrayOutputStream mOutput;
    private byte mNextByte;
    private int mBitInd;
}