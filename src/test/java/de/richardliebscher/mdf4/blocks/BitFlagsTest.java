package de.richardliebscher.mdf4.blocks;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BitFlagsTest {
  enum TestFlag implements BitFlag {
    FIRST(0),
    SECOND(1);

    private final int bitNumber;

    TestFlag(int bitNumber) {
      this.bitNumber = bitNumber;
    }

    @Override
    public int bitNumber() {
      return bitNumber;
    }
  }

  @Test
  void testToString_KnownFlags() {
    assertThat(BitFlags.of(TestFlag.class, TestFlag.FIRST, TestFlag.SECOND).toString())
        .isEqualTo("FIRST,SECOND");
  }

  @Test
  void testToString_OneFlags() {
    assertThat(BitFlags.of(TestFlag.class, TestFlag.FIRST).toString())
        .isEqualTo("FIRST");
  }

  @Test
  void testToString_NoFlags() {
    assertThat(BitFlags.of(TestFlag.class).toString())
        .isEqualTo("");
  }

  @Test
  void testToString_UnknownFlags() {
    assertThat(BitFlags.of(0xF0, TestFlag.class).toString())
        .isEqualTo("UnknownBit4,UnknownBit5,UnknownBit6,UnknownBit7");
  }

  @Test
  void testToString_MixedFlags() {
    assertThat(BitFlags.of(0x0F, TestFlag.class).toString())
        .isEqualTo("FIRST,SECOND,UnknownBit2,UnknownBit3");
  }
}
