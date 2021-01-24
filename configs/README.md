## Примечания по поводу параметров компрессора
Количество кодовых битов (*MODEL_VALUE_BITS*) и количество частотных 
битов (*MODEL_FREQUENCY_BITS*) должны удовлетворять следующим неравенствам:

*MODEL_VALUE_BITS* &ge; *MODEL_FREQUENCY_BITS* + 2

*PRECISION* &ge; *MODEL_FREQUENCY_BITS* + *MODEL_VALUE_BITS*, где *PRECISION* - 
количество беззнаковых битов используемого типа хранения.
В программе используется тип *long*, поэтому *PRECISION* = 63.
