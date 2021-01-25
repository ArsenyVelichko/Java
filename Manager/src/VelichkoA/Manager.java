package VelichkoA;

import ConfigParser.ConfigParser;
import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Manager implements IConfigurable {
    private IReader mReader;
    private ArrayList<IExecutor> mExecutors;
    private IWriter mWriter;
    private Logger mLogger;

    private final Class[] mConstructArgs = { Logger.class };
    
    private RC setupLogger() {
        String logFile = "logs/log.txt";
        mLogger = Logger.getLogger(Manager.class.toString());
        FileHandler handler;

        try {
            handler = new FileHandler(logFile);
        } catch (IOException e) {
            mLogger.warning("Wrong path to log file\n");
            return RC.CODE_INVALID_OUTPUT_STREAM;
        }

        SimpleFormatter formatter = new SimpleFormatter();
        handler.setFormatter(formatter);
        mLogger.addHandler(handler);
        return RC.CODE_SUCCESS;
    }

    private RC setupReader(ConfigParser parser) {
        String readerToken = ManagerTokens.READER_NAME.toString();
        String readerName = parser.getParam(readerToken, 0);

        String configToken = ManagerTokens.READER_CONFIG.toString();
        String config = parser.getParam(configToken, 0);

        String inputToken = ManagerTokens.INPUT.toString();
        String inputStreamName = parser.getParam(inputToken, 0);

        if (readerName == null || config == null || inputStreamName == null) {
            mLogger.warning("There are not enough parameters for reader in config\n");
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        }

        IReader reader;
        try {
            Class reflected = Class.forName(readerName);
            reader = (IReader) reflected.getConstructor(mConstructArgs)
                    .newInstance(mLogger);
        } catch (Exception e) {
            mLogger.warning("Failed to create a reader instance\n");
            return RC.CODE_INVALID_ARGUMENT;
        }
        
        RC res = reader.setConfig(config);
        if (res != RC.CODE_SUCCESS) {
            return res;
        }
        
        try {
            reader.setInputStream(new FileInputStream(inputStreamName));
        } catch (FileNotFoundException e) {
            mLogger.warning("Reader config was not found\n");
            return RC.CODE_INVALID_INPUT_STREAM;
        }

        mReader = reader;
        return RC.CODE_SUCCESS;
    }

    private RC setupExecutors(ConfigParser parser) {
        mExecutors = new ArrayList<>();
        
        String executorToken = ManagerTokens.EXECUTOR_NAME.toString();
        String configToken = ManagerTokens.EXECUTOR_CONFIG.toString();

        IPipelineStep prev = mReader;
        for (int i = 0; i < parser.getValuesNumber(executorToken); i++) {
            String executorName = parser.getParam(executorToken, i);
            IExecutor executor;

            try {
                Class reflected = Class.forName(executorName);
                executor = (IExecutor) reflected.getConstructor(mConstructArgs)
                        .newInstance(mLogger);
            } catch (Exception e) {
                mLogger.warning("Failed to create a executor " + executorName + "\n");
                return RC.CODE_INVALID_ARGUMENT;
            }

            prev.addNotifier(executor.getNotifier());
            executor.setProducer((IProducer) prev);

            String config = parser.getParam(configToken, i);
            if (config == null) {
                mLogger.warning("Config of the executor " + executorName + " was not found\n");
                return RC.CODE_CONFIG_SEMANTIC_ERROR;
            }
            RC res = executor.setConfig(config);
            if (res != RC.CODE_SUCCESS) {
                return res;
            }

            mExecutors.add(executor);
            prev = executor;
        }
        return RC.CODE_SUCCESS;
    }

    private RC setupWriter(ConfigParser parser) {
        String writerToken = ManagerTokens.WRITER_NAME.toString();
        String writerName = parser.getParam(writerToken, 0);

        String configToken = ManagerTokens.WRITER_CONFIG.toString();
        String config = parser.getParam(configToken, 0);

        String outputToken = ManagerTokens.OUTPUT.toString();
        String outputStreamName = parser.getParam(outputToken, 0);
        
        if (writerName == null || config == null || outputStreamName == null) {
            mLogger.warning("There are not enough parameters for writer in config\n");
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        }
        
        IWriter writer;
        try {
            Class reflected = Class.forName(writerName);
            writer = (IWriter) reflected.getConstructor(mConstructArgs)
                    .newInstance(mLogger);
        } catch (Exception e) {
            mLogger.warning("Failed to create a writer instance\n");
            return RC.CODE_INVALID_ARGUMENT;
        }

        IPipelineStep prevStep;
        if (mExecutors.isEmpty()) {
            prevStep = mReader;
        } else {
            prevStep = mExecutors.get(mExecutors.size() - 1);
        }

        prevStep.addNotifier(writer.getNotifier());
        writer.setProducer((IProducer) prevStep);
        RC res = writer.setConfig(config);
        if (res != RC.CODE_SUCCESS) {
            return res;
        }

        try {
            writer.setOutputStream(new FileOutputStream(outputStreamName));
        } catch (FileNotFoundException e) {
            mLogger.warning("Writer config was not found\n");
            return RC.CODE_INVALID_INPUT_STREAM;
        }

        mWriter = writer;
        return RC.CODE_SUCCESS;
    }
    
    @Override
    public RC setConfig(String config) {
        ConfigParser parser = new ConfigParser(new ManagerGrammar());
        parser.parseConfig(config);
        
        RC res;
        if ((res = setupLogger()) != RC.CODE_SUCCESS || 
            (res = setupReader(parser)) != RC.CODE_SUCCESS ||
            (res = setupExecutors(parser)) != RC.CODE_SUCCESS ||
            (res = setupWriter(parser)) != RC.CODE_SUCCESS) {
            mLogger.warning(res.toString());
        }
        return res;
    }

    public void start() {
        ArrayList<Thread> threads = new ArrayList<>();

        threads.add(new Thread(mReader));
        for (IExecutor executor : mExecutors) {
            threads.add(new Thread(executor));
        }
        threads.add(new Thread(mWriter));
        
        for (Thread thread: threads) {
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                mLogger.warning("Thread " + thread.getName() + " was not joined\n");
                return;
            }
        }
        mLogger.info("Pipeline work complete");
    }
}
