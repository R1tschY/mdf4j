package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.extract.de.Half;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

public class HalfBenchmark {
  @State(Scope.Benchmark)
  public static class F16State {
    int[] values;

    @Setup(Level.Iteration)
    public void setup() {
      final var values = IntStream.rangeClosed(Short.MIN_VALUE, Short.MAX_VALUE)
          .boxed()
          .collect(Collectors.toList());
      Collections.shuffle(values);
      this.values = values.stream().mapToInt(x -> x).toArray();
    }
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public float[] own(F16State values) {
    float[] res = new float[values.values.length];
    for (int i = 0; i < values.values.length; i++) {
      res[i] = Half.shortBitsToFloat((short) values.values[i]);
    }
    return res;
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public float[] js(F16State values) {
    float[] res = new float[values.values.length];
    for (int i = 0; i < values.values.length; i++) {
      res[i] = convertFromBits((short) values.values[i]);
    }
    return res;
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public float[] x4u(F16State values) {
    float[] res = new float[values.values.length];
    for (int i = 0; i < values.values.length; i++) {
      res[i] = convertFromBits4((short) values.values[i]);
    }
    return res;
  }

  public static float convertFromBits(short value) {
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

  public static float convertFromBits4(short hbits) {
    var mant = hbits & 0x03ff;
    var exp = hbits & 0x7c00;

    if (exp == 0x7c00) {
      exp = 0x3fc00;
    } else if (exp != 0) {
      exp += 0x1c000;
      if (mant == 0 && exp > 0x1c400) {
        return Float.intBitsToFloat((hbits & 0x8000) << 16 | exp << 13);
      }
    } else if (mant != 0) {
      exp = 0x1c400;
      do {
        mant <<= 1;
        exp -= 0x400;
      } while ((mant & 0x400) == 0);
      mant &= 0x3ff;
    }
    return Float.intBitsToFloat((hbits & 0x8000) << 16 | (exp | mant) << 13);
  }
}
