/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.Link;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import lombok.NonNull;

public final class Links<E> implements Serializable, List<Link<E>> {
  private final long[] links;

  public Links(long[] links) {
    this.links = links;
  }

  public Links(List<Link<E>> links) {
    this.links = links.stream().mapToLong(Link::asLong).toArray();
  }

  @Override
  public int size() {
    return links.length;
  }

  @Override
  public boolean isEmpty() {
    return links.length == 0;
  }

  @Override
  public boolean contains(Object o) {
    if (o instanceof Long) {
      final long other = (Long) o;
      return Arrays.stream(links).anyMatch(l -> l == other);
    }
    return false;
  }

  @Override
  public @NonNull Iterator<Link<E>> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object @NonNull [] toArray() {
    return Arrays.stream(links).mapToObj(Link::of).toArray();
  }

  @Override
  public <T> T[] toArray(T @NonNull [] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(Link<E> link) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, Link<E> element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Link<E> remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(@NonNull Collection<? extends Link<E>> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(int index, @NonNull Collection<? extends Link<E>> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(@NonNull Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(@NonNull Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Link<E> get(int index) {
    return Link.of(links[index]);
  }

  @Override
  public Link<E> set(int index, Link<E> element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull ListIterator<Link<E>> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull ListIterator<Link<E>> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull List<Link<E>> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }
}
