package VelichkoA;

import ConfigParser.ConfigParser;
import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

public class Reader implements IReader {
    private int mChunkSize;
    private FileInputStream mInput;
    private final Logger mLogger;

    private final TYPE[] mOutputTypes = { TYPE.BYTE, TYPE.SHORT, TYPE.CHAR };

    private INotifier mConsumerNotifier;
    private HashMap<Integer, byte[]> mReadChunks;

    public Reader(Logger logger) {
        mLogger = logger;
        mReadChunks = new HashMap<>();
    }

    @Override
    public RC setInputStream(FileInputStream fileInputStream) {
        mInput = fileInputStream;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConfig(String configPath) {
        ConfigParser parser = new ConfigParser(new ReaderGrammar());
        RC res = parser.parseConfig(configPath);
        if (res != RC.CODE_SUCCESS) {
            mLogger.warning("Failed to parse the writer config");
            return res;
        }
        
        String chunkSizeToken = ReaderTokens.CHUNK_SIZE.toString();
        String chunkSizeStr = parser.getParam(chunkSizeToken, 0);
        if (chunkSizeStr == null) {
            mLogger.warning("Failed to read writer chunk size\n");
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        }

        try {
            mChunkSize = Integer.parseInt(chunkSizeStr);
        } catch (NumberFormatException e) {
            mLogger.warning("Chunk size must be represented by integer");
            return RC.CODE_INVALID_ARGUMENT;
        }
        return RC.CODE_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return mOutputTypes;
    }

    private byte[] removeChunk(int chunkId) {
        synchronized (mReadChunks) {
            return mReadChunks.remove(chunkId);
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

    private RC readFile() {
        byte[] buffer = new byte[mChunkSize];

        int readBytes = 0;
        for (int chunkId = 0; readBytes != -1; chunkId++) {
            try {
                readBytes = mInput.read(buffer);
            } catch (IOException e) {
                mLogger.warning("Failed to read chunk #" + chunkId +"\n");
                return RC.CODE_FAILED_TO_READ;
            }
            
            byte[] chunk;
            if (readBytes == -1) {
                chunk = null;
            } else {
                chunk = Arrays.copyOf(buffer, readBytes);
            }
            
            synchronized (mReadChunks) {
                mReadChunks.put(chunkId, chunk);
                mConsumerNotifier.notify(chunkId);
            }
        }


        try {
            mInput.close();
        } catch (IOException e) {
            mLogger.warning("Failed to close file\n");
            return RC.CODE_FAILED_TO_READ;
        }
        return RC.CODE_SUCCESS;
    }

    @Override
    public void run() {
        readFile();
    }
}
