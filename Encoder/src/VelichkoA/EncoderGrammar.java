package VelichkoA;

import ru.spbstu.pipeline.BaseGrammar;

enum EncoderTokens {
    MODEL_VALUE_BITS,
    MODEL_FREQUENCY_BITS,
}

public class EncoderGrammar extends BaseGrammar {
    EncoderGrammar() {
        super(getTokenNames());
    }

    static String[] getTokenNames() {
        EncoderTokens[] tokens = EncoderTokens.values();
        String[] names = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            names[i] = tokens[i].toString();
        }
        return names;
    }
}
