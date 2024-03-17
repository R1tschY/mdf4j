/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Scope implements Closeable {
  private final List<Closeable> closeables;

  public Scope() {
    closeables = new ArrayList<>();
  }

  public Scope(Collection<Closeable> closeables) {
    this();
    this.closeables.addAll(closeables);
  }

  public Scope(Closeable... closeables) {
    this(Arrays.asList(closeables));
  }

  public void add(Closeable closeable) {
    synchronized (closeables) {
      closeables.add(closeable);
    }
  }

  @Override
  public void close() throws IOException {
    Throwable exception = null;

    synchronized (closeables) {
      for (Closeable closeable : closeables) {
        try {
          closeable.close();
        } catch (Throwable e) {
          if (exception == null) {
            exception = e;
          } else {
            try {
              exception.addSuppressed(e);
            } catch (Throwable ignore) { /* ignore */ }
          }
        }
      }
      closeables.clear();

      if (exception != null) {
        if (exception instanceof IOException) {
          throw (IOException) exception;
        } else if (exception instanceof RuntimeException) {
          throw (RuntimeException) exception;
        } else {
          throw new RuntimeException(exception);
        }
      }
    }
  }

  public void closeUnchecked() {
    try {
      close();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
