package VelichkoA;

import ru.spbstu.pipeline.RC;

import java.util.logging.Logger;

public class Compressor extends Encoder {
    public Compressor(Logger logger) {
        super(logger);
    }

    private void writeWithPending(BitOutputStream dest, boolean bit, int pendingCount)  {
        dest.putBit(bit);
        for (int i = 0 ; i < pendingCount; i++) {
            dest.putBit(!bit);
        }
    }

    @Override
    RC encode(byte[] bytes, int chunkId) {
        mModel.reset();

        BitOutputStream buf = new BitOutputStream();
        ModelMetrics metrics = mModel.getMetrics();

        int pendingCount = 0;
        long low = 0;
        long high = metrics.maxCode;

        for (int i = 0; i <= bytes.length; i++) {
            int c = i == bytes.length ? metrics.EOF : bytes[i] & 0xFF;

            Probability p = mModel.getFreqSegment(c);
            long range = high - low + 1;
            high = low + (range * p.high / p.count) - 1;
            low = low + (range * p.low / p.count);

            while (true) {
                if (high < metrics.oneHalf) {
                    writeWithPending(buf, false, pendingCount);
                    pendingCount = 0;

                } else if (low >= metrics.oneHalf) {
                    writeWithPending(buf, true, pendingCount);
                    pendingCount = 0;

                } else if (low >= metrics.oneFourth && high < metrics.threeFourths) {
                    pendingCount++;
                    low -= metrics.oneFourth;
                    high -= metrics.oneFourth;

                } else
                    break;

                high <<= 1;
                high++;
                low <<= 1;
                high &= metrics.maxCode;
                low &= metrics.maxCode;
            }
        }

        pendingCount++;
        if ( low < metrics.oneFourth )
            writeWithPending(buf, false, pendingCount);
        else
            writeWithPending(buf, true, pendingCount);

        synchronized (mEncodedChunks) {
            mEncodedChunks.put(chunkId, buf.toByteArray());
        }
        return RC.CODE_SUCCESS;
    }
}
