package VelichkoA;

import ru.spbstu.pipeline.*;

enum ReaderTokens {
    CHUNK_SIZE
}

public class ReaderGrammar extends BaseGrammar {
    ReaderGrammar() {
        super(getTokenNames());
    }

    static String[] getTokenNames() {
        ReaderTokens[] tokens = ReaderTokens.values();
        String[] names = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            names[i] = tokens[i].toString();
        }
        return names;
    }
}
