package de.richardliebscher.mdf4.internal;

import de.richardliebscher.mdf4.blocks.ChannelGroup;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Spliterator;

@RequiredArgsConstructor
public class BlockIter<T> implements Iterable<T> {
    private final long start;
    private final ByteInput input;
    private final FromBytesInput<T> parse;

    @NotNull
    @Override
    public Iterator<T> iterator() {
        throw new RuntimeException();
    }
//        return new Iterator<>() {
//            private long next = start;
//
//            @Override
//            public boolean hasNext() {
//                return start != 0;
//            }
//
//            @Override
//            public ChannelGroup next() {
//                try {
//                    input.seek(next);
//                    final var block = parse.parse(input);
//                    next = block.getLinks()[0];
//                    return block;
//                } catch (IOException e) {
//                    throw new UncheckedIOException(e);
//                }
//            }
//        };;
//    }

    @Override
    public Spliterator<T> spliterator() {
        return Iterable.super.spliterator();
    }
}
