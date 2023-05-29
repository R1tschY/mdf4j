package de.richardliebscher.mdf4.extract.de;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HalfTest {

  static Stream<Arguments> values() {
    return Stream.of(
        Arguments.of(0x3C00, 1.f),
        Arguments.of(0xC000, -2.f),
        Arguments.of(0x0001, 0.000000059604645f),
        Arguments.of(0x03FF, 0.000060975552f),
        Arguments.of(0x0400, 0.00006103515625f),
        Arguments.of(0x3555, 0.33325195f),
        Arguments.of(0x3BFF, 0.99951172f),
        Arguments.of(0x3C01, 1.00097656f),
        Arguments.of(0x7BFF, 65504.f),
        Arguments.of(0x0000, 0f),
        Arguments.of(0x8000, -0f),
        Arguments.of(0x7C00, Float.POSITIVE_INFINITY),
        Arguments.of(0xFC00, Float.NEGATIVE_INFINITY),
        Arguments.of(0x3555, 0.33325195f),
        Arguments.of(0x7C01, Float.NaN)
    );
  }

  @ParameterizedTest
  @MethodSource("values")
  void testSomeValues(int bits, float expected) {
    assertEquals(expected, Half.shortBitsToFloat((short) bits));
  }

  @Test
  void testAgainstReference() {
    for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
      assertEquals(reference((short) i), Half.shortBitsToFloat((short) i));
    }
  }

  public static float reference(short value) {
    final var exponent = (value & 0x7C00) >>> 10;
    final var mantissa = (value & 0x03FF);

    if (exponent == 0) {
      return (value < 0 ? -1.f : 1.f)
          * (float) Math.pow(2, -14)
          * (mantissa / (float) Math.pow(2, 10));
    } else if (exponent == 0x1F) {
      return mantissa != 0 ? Float.NaN
          : (value < 0 ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY);
    }

    return (value < 0 ? -1.f : 1.f)
        * (float) Math.pow(2, exponent - 15)
        * (1 + mantissa / (float) Math.pow(2, 10));
  }
}