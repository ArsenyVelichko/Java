package VelichkoA;

import ru.spbstu.pipeline.BaseGrammar;

enum ManagerTokens {
    INPUT,
    OUTPUT,
    READER_NAME,
    WRITER_NAME,
    EXECUTOR_NAME,
    WRITER_CONFIG,
    READER_CONFIG,
    EXECUTOR_CONFIG,
}

public class ManagerGrammar extends BaseGrammar {
    ManagerGrammar() {
        super(getTokenNames());
    }

    static String[] getTokenNames() {
        ManagerTokens[] tokens = ManagerTokens.values();
        String[] names = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            names[i] = tokens[i].toString();
        }
        return names;
    }
}
