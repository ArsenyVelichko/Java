package VelichkoA;

import ConfigParser.ConfigParser;
import ru.spbstu.pipeline.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

public abstract class Encoder implements IExecutor {
    protected AdaptiveModel mModel;
    protected final Logger mLogger;

    private final TYPE[] mInputTypes = { TYPE.BYTE };
    private final TYPE[] mOutputTypes = { TYPE.BYTE, TYPE.SHORT, TYPE.CHAR };
    private IMediator mProducerMediator;

    private INotifier mConsumerNotifier;
    private LinkedList<Integer> mWaitingChunks;
    protected HashMap<Integer, byte[]> mEncodedChunks;

    public Encoder(Logger logger) {
        mLogger = logger;

        mWaitingChunks = new LinkedList<>();
        mEncodedChunks = new HashMap<>();
    }

    @Override
    public RC setConfig(String configPath) {
        ConfigParser parser = new ConfigParser(new EncoderGrammar());
        RC res = parser.parseConfig(configPath);
        if (res != RC.CODE_SUCCESS) {
            mLogger.warning("Failed to parse the encoder config");
            return res;
        }

        String valueBitsToken = EncoderTokens.MODEL_VALUE_BITS.toString();
        String valueBitsStr = parser.getParam(valueBitsToken, 0);
        
        String freqBitsToken = EncoderTokens.MODEL_VALUE_BITS.toString();
        String freqBitsStr = parser.getParam(freqBitsToken, 0);
        if (freqBitsStr == null || valueBitsStr == null) {
            mLogger.warning("Failed to read model parameters\n");
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        }

        int codeValueBits, freqBits;
        try {
            codeValueBits = Integer.parseInt(valueBitsStr);
            freqBits = Integer.parseInt(freqBitsStr);
        } catch (NumberFormatException e) {
            mLogger.warning("Model parameters must be represented by integers\n");
            return RC.CODE_INVALID_ARGUMENT;
        }
        mModel = new AdaptiveModel(codeValueBits, freqBits);
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer producer) {
        for (TYPE outputType : producer.getOutputTypes()) {
            for (TYPE inputType : mInputTypes) {
                if (outputType == inputType) {
                    mProducerMediator = producer.getMediator(inputType);
                    return RC.CODE_SUCCESS;
                }
            }
        }
        mLogger.warning("Encoder does not support any of the producer's output formats\n");
        return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return mOutputTypes;
    }


    private byte[] removeChunk(int chunkId) {
        synchronized (mEncodedChunks) {
            return mEncodedChunks.remove(chunkId);
        }
    }

    class ByteMediator implements IMediator {
        @Override
        public Object getData(int chunkId) {
            return removeChunk(chunkId);
        }
    }

    class ShortMediator implements IMediator {
        @Override
        public Object getData(int chunkId) {
            byte[] byteData = removeChunk(chunkId);
            if (byteData == null) { return null; }

            short[] shortData = new short[byteData.length / 2];
            ByteBuffer.wrap(byteData)
                    .asShortBuffer()
                    .get(shortData);
            return shortData;
        }
    }

    class CharMediator implements IMediator {
        @Override
        public Object getData(int chunkId) {
            byte[] byteData = removeChunk(chunkId);
            if (byteData == null) { return null; }

            char[] charData = new char[byteData.length / 2];
            ByteBuffer.wrap(byteData)
                    .asCharBuffer()
                    .get(charData);
            return charData;
        }
    }

    @Override
    public IMediator getMediator(TYPE type) {
        switch (type) {
            case BYTE:
                return new ByteMediator();

            case SHORT:
                return new ShortMediator();

            case CHAR:
                return new CharMediator();

            default:
                return null;
        }
    }

    @Override
    public RC addNotifier(INotifier iNotifier) {
        mConsumerNotifier = iNotifier;
        return RC.CODE_SUCCESS;
    }

    class EncoderNotifier implements INotifier {
        @Override
        public RC notify(int chunkId) {
            synchronized (mWaitingChunks) {
                mWaitingChunks.add(chunkId);
            }
            
            synchronized (Encoder.this) {
                Encoder.this.notify();
            }
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public INotifier getNotifier() {
        return new EncoderNotifier();
    }

    abstract RC encode(byte[] bytes, int chunkId);

    @Override
    public void run() {

        while (true) {

            Integer chunkId;
            synchronized (mWaitingChunks) {
                chunkId = mWaitingChunks.poll();
            }

            if (chunkId == null) {
                try {
                    synchronized (this) {
                        wait();
                    }
                    continue;

                } catch (InterruptedException e) {
                    mLogger.warning(RC.CODE_SYNCHRONIZATION_ERROR.toString());
                }
            }

            byte[] bytes = (byte[]) mProducerMediator.getData(chunkId);
            if (bytes == null) {
                mConsumerNotifier.notify(chunkId);
                break;
            }

            RC res = encode(bytes, chunkId);
            if (res != RC.CODE_SUCCESS) {
                mLogger.warning(res.toString());
            } else {
                mConsumerNotifier.notify(chunkId);
            }
        }
    }
}
