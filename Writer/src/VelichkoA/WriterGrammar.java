package VelichkoA;

import ru.spbstu.pipeline.BaseGrammar;

enum WriterTokens {
    CHUNK_SIZE
}

public class WriterGrammar extends BaseGrammar {
    WriterGrammar() {
        super(getTokenNames());
    }

    static String[] getTokenNames() {
        WriterTokens[] tokens = WriterTokens.values();
        String[] names = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            names[i] = tokens[i].toString();
        }
        return names;
    }
}
