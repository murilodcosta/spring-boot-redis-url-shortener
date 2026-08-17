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
    @DisplayName("Should encode positive integers to Base62 correctly")
    void shouldEncodeNumberToBase62(long input, String expected) {
        String encoded = Base62Encoder.encode(input);
        assertEquals(expected, encoded);
    }

    @Test
    @DisplayName("Should decode Base62 strings back to the original positive integer")
    void shouldDecodeBase62ToOriginalNumber() {
        long original = 123456789L;
        String encoded = Base62Encoder.encode(original);
        long decoded = Base62Encoder.decode(encoded);

        assertEquals(original, decoded);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when attempting to encode zero or negative numbers")
    void shouldThrowExceptionForZeroOrNegativeNumber() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(0));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(-5));
    }
}
