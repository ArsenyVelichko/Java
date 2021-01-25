package VelichkoA;

import ru.spbstu.pipeline.RC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class Decompressor extends Encoder {
    public Decompressor(Logger logger) {
        super(logger);
    }

    @Override
    RC encode(byte[] bytes, int chunkId) {
        mModel.reset();

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ModelMetrics metrics = mModel.getMetrics();
        BitInputStream source = new BitInputStream(bytes, metrics.codeValueBits);

        long high = metrics.maxCode;
        long low = 0;
        long value = 0;

        for (int i = 0; i < metrics.codeValueBits; i++) {
            value <<= 1;

            try {
                value += source.getBit() ? 1 : 0;
            } catch (IOException e) {
                mLogger.warning("No end-of-file code was encountered\n");
                return RC.CODE_INVALID_INPUT_STREAM;
            }
        }

        while (true) {
            long range = high - low + 1;
            long scaledVal = ((value - low + 1) * mModel.getSumFreq() - 1) / range;

            int c = mModel.getChar(scaledVal);
            if (c == metrics.EOF)
                break;
            buf.write(c);

            Probability p = mModel.getFreqSegment(c);
            high = low + (range * p.high) / p.count - 1;
            low = low + (range * p.low) / p.count;

            while (true) {
                if (high < metrics.oneHalf) {

                } else if (low >= metrics.oneHalf) {
                    value -= metrics.oneHalf;
                    low -= metrics.oneHalf;
                    high -= metrics.oneHalf;

                } else if (low >= metrics.oneFourth && high < metrics.threeFourths) {
                    value -= metrics.oneFourth;
                    low -= metrics.oneFourth;
                    high -= metrics.oneFourth;

                } else
                    break;

                low <<= 1;
                high <<= 1;
                high++;
                value <<= 1;

                try {
                    value += source.getBit() ? 1 : 0;
                } catch (IOException e) {
                    mLogger.warning("No end-of-file code was encountered in chunk #" + chunkId +"\n");
                    return RC.CODE_INVALID_INPUT_STREAM;
                }
            }
        }

        synchronized (mEncodedChunks) {
            mEncodedChunks.put(chunkId, buf.toByteArray());
        }
        return RC.CODE_SUCCESS;
    }
}
