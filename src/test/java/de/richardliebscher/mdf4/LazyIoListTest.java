package de.richardliebscher.mdf4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LazyIoListTest {

  @Nested
  class JavaIterable {
    @Test
    void testStopAfterErrorResult_ForEach() {
      // ARRANGE
      final LazyIoList<Void> list = FailingIoIterator::new;
      final var results = new ArrayList<Result<Void, IOException>>();

      // ACT
      list.forEach(results::add);

      // ASSERT
      assertThat(results).hasSize(1).first().isInstanceOf(Result.Err.class);
    }
  }

  @Nested
  class JavaIterator {
    @Test
    void testStopAfterErrorResult() {
      // ARRANGE
      final LazyIoList<Void> list = FailingIoIterator::new;
      final var iterator = list.iterator();

      // ACT
      final var failed = iterator.next();
      final var hasNext = iterator.hasNext();

      // ASSERT
      assertTrue(failed.isErr());
      assertFalse(hasNext);
    }

    @Test
    void testStopAfterErrorResult_ForEach() {
      // ARRANGE
      final LazyIoList<Void> list = FailingIoIterator::new;
      final var iterator = list.iterator();
      final var results = new ArrayList<Result<Void, IOException>>();

      // ACT
      iterator.forEachRemaining(results::add);

      // ASSERT
      assertThat(results).hasSize(1).first().isInstanceOf(Result.Err.class);
    }

    @Test
    void testStopAfterErrorResult_ForEach_DoubleCall() {
      // ARRANGE
      final LazyIoList<Void> list = FailingIoIterator::new;
      final var iterator = list.iterator();
      final var results = new ArrayList<Result<Void, IOException>>();

      // ACT
      iterator.forEachRemaining(results::add);
      iterator.forEachRemaining(results::add);

      // ASSERT
      assertThat(results).hasSize(1).first().isInstanceOf(Result.Err.class);
    }
  }

  @Nested
  class JavaStream {
    @Test
    void testStopAfterErrorResult() {
      // ARRANGE
      final LazyIoList<Void> list = FailingIoIterator::new;
      final var stream = list.stream();

      // ACT
      final var resultList = stream.limit(2).collect(Collectors.toList());

      // ASSERT
      assertThat(resultList).hasSize(1).first().isInstanceOf(Result.Err.class);
    }

    @Test
    void testStopAfterErrorResult_SpliteratorForEach() {
      // ARRANGE
      final LazyIoList<Void> list = FailingIoIterator::new;
      final var spliterator = list.spliterator();
      final var results = new ArrayList<Result<Void, IOException>>();

      // ACT
      spliterator.forEachRemaining(results::add);

      // ASSERT
      assertThat(results).hasSize(1).first().isInstanceOf(Result.Err.class);
    }


    @Test
    void testStopAfterErrorResult_SpliteratorForEach_DoubleCall() {
      // ARRANGE
      final LazyIoList<Void> list = FailingIoIterator::new;
      final var spliterator = list.spliterator();
      final var results = new ArrayList<Result<Void, IOException>>();

      // ACT
      spliterator.forEachRemaining(results::add);
      spliterator.forEachRemaining(results::add);

      // ASSERT
      assertThat(results).hasSize(1).first().isInstanceOf(Result.Err.class);
    }

    @Test
    void testStopAfterErrorResult_SpliteratorAdvance() {
      // ARRANGE
      final LazyIoList<Void> list = FailingIoIterator::new;
      final var spliterator = list.spliterator();
      final var results = new ArrayList<Result<Void, IOException>>();

      // ACT
      spliterator.tryAdvance(results::add);
      final var remaining = spliterator.tryAdvance(results::add);

      // ASSERT
      assertThat(results).hasSize(1).first().isInstanceOf(Result.Err.class);
      assertFalse(remaining);
    }
  }

  private static class FailingIoIterator<T> implements LazyIoIterator<T> {

    @Override
    public boolean hasNext() {
      return true;
    }

    @Override
    public T next() throws IOException {
      throw new IOException();
    }
  }
}
