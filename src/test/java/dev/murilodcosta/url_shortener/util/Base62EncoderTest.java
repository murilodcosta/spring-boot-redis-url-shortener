package dev.murilodcosta.url_shortener.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @ParameterizedTest
    @CsvSource({
            "1, 1",
            "10, a",
            "61, Z",
            "62, 10",
            "1000, g8",
            "123456, w7e"
    })
    @DisplayName("Deve codificar numeros inteiros positivos em Base62 corretamente")
    void shouldEncodeNumberToBase62(long input, String expected) {
        String encoded = Base62Encoder.encode(input);
        assertEquals(expected, encoded);
    }

    @Test
    @DisplayName("Deve decodificar strings em Base62 de volta para o numero original")
    void shouldDecodeBase62ToOriginalNumber() {
        long original = 123456789L;
        String encoded = Base62Encoder.encode(original);
        long decoded = Base62Encoder.decode(encoded);

        assertEquals(original, decoded);
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar codificar numero menor ou igual a zero")
    void shouldThrowExceptionForZeroOrNegativeNumber() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(0));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(-5));
    }
}
