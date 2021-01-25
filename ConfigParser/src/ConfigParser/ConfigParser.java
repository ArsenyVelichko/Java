package ConfigParser;

import ru.spbstu.pipeline.BaseGrammar;
import ru.spbstu.pipeline.RC;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ConfigParser {
    private HashMap<String, ArrayList<String>> mParams;
    private final String mDelimiter;

    public ConfigParser(BaseGrammar grammar) {
        mParams = new HashMap<>();
        for (int i = 0; i < grammar.numberTokens(); i++) {
            mParams.put(grammar.token(i), new ArrayList<>());
        }
        mDelimiter = grammar.delimiter();
    }

    public RC parseConfig(String configPath) {
        BufferedReader buffReader;
        try {
            buffReader = new BufferedReader(new FileReader(configPath));
        } catch (FileNotFoundException e) {
            return RC.CODE_INVALID_INPUT_STREAM;
        }

        while (true) {
            String line;
            try {
                line = buffReader.readLine();
            } catch (IOException e) {
                return RC.CODE_FAILED_TO_READ;
            }

            if (line == null) { break; }
            String[] lineTokens = line.split(mDelimiter);
            if (lineTokens.length != 2) { return RC.CODE_CONFIG_GRAMMAR_ERROR; }
            mParams.get(lineTokens[0]).add(lineTokens[1]);
        }
        return RC.CODE_SUCCESS;
    }

    public String getParam(String token, int index) {
        ArrayList<String> values = mParams.get(token);
        if (index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    public int getValuesNumber(String token) {
        return mParams.get(token).size();
    }
}
