package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

public class TestBenchmark {


  @State(Scope.Benchmark)
  public static class F16State {
    int[] values;

    @Setup(Level.Iteration)
    public void setup() {
      final var values = IntStream.rangeClosed(0, 1000000)
          .boxed()
          .collect(Collectors.toList());
      Collections.shuffle(values);
      this.values = values.stream().mapToInt(x -> x).toArray();
    }
  }

  private Integer generic(int t) {
    return t + 1;
  }

  private void genericConsumer(int t, IntConsumer mutRef) {
    mutRef.accept(t + 1);
  }
  private int nonGeneric(int t) {
    return t + 1;
  }

//  @Benchmark
//  @Warmup(iterations = 1, time = 1, batchSize = 1)
//  @Fork(value = 1, warmups = 0)
//  @Measurement(iterations = 3)
//  public int[] IntegerDeserialize(F16State values) throws IOException {
//    int[] res = new int[values.values.length];
//    for (int i = 0; i < values.values.length; i++) {
//      res[i] = new IntegerDeserialize().deserialize(new IntDeserializer(res[i]));
//    }
//    return res;
//  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] IntegerDeserialize2(F16State values) throws IOException {
    int[] res = new int[values.values.length];
    final var integerDeserialize = new IntegerDeserialize();
    final var deserializer = new IntArrayDeserializer(values.values);
    for (int i = 0; i < values.values.length; i++) {
      res[i] = integerDeserialize.deserialize(deserializer);
    }
    return res;
  }

//  private static final IntegerDeserialize INTEGER_DESERIALIZE = new IntegerDeserialize();
//  @Benchmark
//  @Warmup(iterations = 1, time = 1, batchSize = 1)
//  @Fork(value = 1, warmups = 0)
//  @Measurement(iterations = 3)
//  public int[] IntegerDeserializeStatic(F16State values) throws IOException {
//    int[] res = new int[values.values.length];
//    final var deserializer = new IntArrayDeserializer(values.values);
//    for (int i = 0; i < values.values.length; i++) {
//      res[i] = INTEGER_DESERIALIZE.deserialize(deserializer);
//    }
//    return res;
//  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] IntCellDeserialize(F16State values) throws IOException {
    int[] res = new int[values.values.length];
    final var deserializer = new IntArrayDeserializer(values.values);
    for (int i = 0; i < values.values.length; i++) {
      res[i] = new IntCellDeserialize().deserialize(deserializer).getValue();
    }
    return res;
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] IntCellDeserialize2(F16State values) throws IOException {
    int[] res = new int[values.values.length];
    final var intCellDeserialize = new IntCellDeserialize();
    final var deserializer = new IntArrayDeserializer(values.values);
    for (int i = 0; i < values.values.length; i++) {
      res[i] = intCellDeserialize.deserialize(deserializer).getValue();
    }
    return res;
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] IntCellDeserialize3(F16State values) throws IOException {
    int[] res = new int[values.values.length];
    final var intCellDeserialize = new IntCellDeserialize3();
    final var deserializer = new IntArrayDeserializer(values.values);
    for (int i = 0; i < values.values.length; i++) {
      res[i] = intCellDeserialize.deserialize(deserializer).getValue();
    }
    return res;
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] IntCellDeserialize4(F16State values) throws IOException {
    int[] res = new int[values.values.length];
    final var intCellDeserialize = new IntCellDeserialize4();
    final var deserializer = new IntArrayDeserializer(values.values);
    for (int i = 0; i < values.values.length; i++) {
      res[i] = intCellDeserialize.deserialize(deserializer).getValue();
    }
    return res;
  }

  private static final IntCellThreadLocalDeserialize INT_CELL_DESERIALIZE = new IntCellThreadLocalDeserialize();

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] IntCellDeserializeStatic(F16State values) throws IOException {
    int[] res = new int[values.values.length];
    final var deserializer = new IntArrayDeserializer(values.values);
    for (int i = 0; i < values.values.length; i++) {
      res[i] = INT_CELL_DESERIALIZE.deserialize(deserializer).getValue();
    }
    return res;
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] IntDeserialize(F16State values) throws IOException {
    final var deserializer = new IntArrayDeserializer(values.values);
    final var arrayBuilder = new IntArrayBuilder(values.values.length);
    for (int i = 0; i < values.values.length; i++) {
      new IntDeserialize(arrayBuilder)
          .deserialize(deserializer);
    }
    return arrayBuilder.getRes();
  }


  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] IntDeserializeInto(F16State values) throws IOException {
    final var arrayBuilder = new IntArrayBuilder(values.values.length);
    final var intDeserializeInto = new IntDeserializeInto();
    final var deserializer = new IntArrayDeserializer(values.values);
    for (int i = 0; i < values.values.length; i++) {
      intDeserializeInto.deserializeInto(deserializer, arrayBuilder);
    }
    return arrayBuilder.res;
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] notBoxed(F16State values) {
    int[] res = new int[values.values.length];
    for (int i = 0; i < values.values.length; i++) {
      res[i] = nonGeneric(values.values[i]);
    }
    return res;
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] boxed(F16State values) {
    int[] res = new int[values.values.length];
    for (int i = 0; i < values.values.length; i++) {
      res[i] = generic(values.values[i]);
    }
    return res;
  }

  @Benchmark
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  @Measurement(iterations = 3)
  public int[] clone(F16State values) {
    return values.values.clone();
  }

  @RequiredArgsConstructor
  static final class IntArrayDeserializer implements Deserializer {
    private final int[] values;
    private int index;

    @Override
    public <R> R deserialize_value(Visitor<R> visitor) throws IOException {
      return visitor.visitI32(values[index++]);
    }
  }

  static final class IntArrayBuilder implements IntConsumer {
    @Getter
    private final int[] res;
    private int index;

    public IntArrayBuilder(int len) {
      res = new int[len];
    }

    @Override
    public void accept(int value) {
      res[index++] = value;
    }
  }

  @RequiredArgsConstructor
  static final class IntDeserialize implements Deserialize<Void> {
    private final IntConsumer consumer;

    @Override
    public Void deserialize(Deserializer deserializer) throws IOException {
      return deserializer.deserialize_value(new Visitor<Void>() {
        @Override
        public String expecting() {
          return "int";
        }

        @Override
        public Void visitI32(int value) throws IOException {
          consumer.accept(value + 1);
          return null;
        }
      });
    }
  }

  @RequiredArgsConstructor
  static final class IntDeserializeInto implements DeserializeInto<IntConsumer> {

    @Override
    public void deserializeInto(Deserializer deserializer, IntConsumer dest) throws IOException {
      deserializer.deserialize_value(new Visitor<Object>() {
        @Override
        public String expecting() {
          return null;
        }

        @Override
        public Object visitI32(int value) throws IOException {
          dest.accept(value + 1);
          return null;
        }
      });
    }
  }

  @Data
  static final class IntCell {
    private int value;
  }

  static final class IntCellThreadLocal extends ThreadLocal<IntCell> {
    @Override
    protected IntCell initialValue() {
      return new IntCell();
    }
  }

  @RequiredArgsConstructor
  static final class IntCellDeserialize implements Deserialize<IntCell> {
    private final IntCell cell = new IntCell();

    @Override
    public final IntCell deserialize(Deserializer deserializer) throws IOException {
      return deserializer.deserialize_value(new Visitor<IntCell>() {
        @Override
        public String expecting() {
          return "int";
        }

        @Override
        public IntCell visitI32(int value) throws IOException {
          cell.setValue(value + 1);
          return cell;
        }
      });
    }
  }

  @RequiredArgsConstructor
  static final class IntCellDeserialize3 implements Deserialize<IntCell> {
    private final IntCell cell = new IntCell();
    private final IntCellVisitor visitor = new IntCellVisitor();

    @Override
    public final IntCell deserialize(Deserializer deserializer) throws IOException {
      return deserializer.deserialize_value(visitor);
    }

    private class IntCellVisitor implements Visitor<IntCell> {

      @Override
      public String expecting() {
        return "int";
      }

      @Override
      public IntCell visitI32(int value) throws IOException {
        cell.setValue(value + 1);
        return cell;
      }
    }
  }

  @RequiredArgsConstructor
  @Getter
  static final class IntCellDeserialize4 implements Deserialize<IntCellDeserialize4>, Visitor<IntCellDeserialize4> {
    private int value;

    @Override
    public final IntCellDeserialize4 deserialize(Deserializer deserializer) throws IOException {
      return deserializer.deserialize_value(this);
    }

    @Override
    public String expecting() {
      return null;
    }

    @Override
    public IntCellDeserialize4 visitI32(int value) throws IOException {
      this.value = value + 1;
      return this;
    }
  }

  @RequiredArgsConstructor
  static final class IntCellThreadLocalDeserialize implements Deserialize<IntCell> {

    private static final IntCellThreadLocal cell = new IntCellThreadLocal();
    private static final IntCellVisitor VISITOR = new IntCellVisitor();

    @Override
    public IntCell deserialize(Deserializer deserializer) throws IOException {
      return deserializer.deserialize_value(VISITOR);
    }

    private static final class IntCellVisitor implements Visitor<IntCell> {

      @Override
      public String expecting() {
        return "int";
      }

      @Override
      public IntCell visitI32(int value) throws IOException {
        final var intCell = cell.get();
        intCell.setValue(value + 1);
        return intCell;
      }
    }
  }

  @RequiredArgsConstructor
  static final class IntegerDeserialize implements Deserialize<Integer> {
    @Override
    public Integer deserialize(Deserializer deserializer) throws IOException {
      return deserializer.deserialize_value(new Visitor<Integer>() {
        @Override
        public String expecting() {
          return "integer";
        }

        @Override
        public Integer visitI32(int value) throws IOException {
          return value + 1;
        }
      });
    }
  }
}
