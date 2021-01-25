package VelichkoA;

import ConfigParser.ConfigParser;
import ru.spbstu.pipeline.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Logger;

public class Writer implements IWriter {
    private FileOutputStream mOutput;
    private int mChunkSize;
    private final Logger mLogger;

    private final TYPE[] mInputTypes = { TYPE.BYTE };
    private IMediator mProducerMediator;

    private LinkedList<Integer> mWaitingChunks;

    public Writer(Logger logger) {
        mLogger = logger;
        mWaitingChunks = new LinkedList<>();
    }

    @Override
    public RC setConfig(String configPath) {
        ConfigParser parser = new ConfigParser(new WriterGrammar());
        RC res = parser.parseConfig(configPath);
        if (res != RC.CODE_SUCCESS) {
            mLogger.warning("Failed to parse the writer config");
            return res;
        }
        
        String chunkSizeToken = WriterTokens.CHUNK_SIZE.toString();
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
    public RC setOutputStream(FileOutputStream fileOutputStream) {
        mOutput = fileOutputStream;
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
        mLogger.warning("Writer does not support any of the producer's output formats\n");
        return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
    }

    @Override
    public RC addNotifier(INotifier iNotifier) { return RC.CODE_SUCCESS; }

    class WriterNotifier implements INotifier {
        @Override
        public RC notify(int chunkId) {
            synchronized (mWaitingChunks) {
                mWaitingChunks.add(chunkId);
            }
            
            synchronized (Writer.this) {
                Writer.this.notify();
            }
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public INotifier getNotifier() {
        return new WriterNotifier();
    }

    private RC write(byte[] bytes) {
        for (int currByte = 0; currByte < bytes.length; currByte += mChunkSize) {
            int chunkSize = Math.min(mChunkSize, bytes.length - currByte);
            byte[] chunk = Arrays.copyOfRange(bytes, currByte, currByte + chunkSize);

            try {
                mOutput.write(chunk);
            } catch (IOException e) {
                mLogger.warning("Chunk was not written");
                return RC.CODE_FAILED_TO_WRITE;
            }
        }
        return RC.CODE_SUCCESS;
    }

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
                break; 
            }

            RC res = write(bytes);
            if (res != RC.CODE_SUCCESS){
                mLogger.warning(res.toString());
            }
        }
    }
}
