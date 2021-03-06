/*
 * Copyright 2015-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.utils.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for creating completed and exceptional futures.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public final class Futures {

  /**
   * Gets a future result with a default timeout.
   *
   * @param future the future to block
   * @param <T> the future result type
   * @return the future result
   * @throws RuntimeException if a future exception occurs
   */
  public static <T> T get(final Future<T> future) {
    return get(future, 30, TimeUnit.SECONDS);
  }

  /**
   * Gets a future result with a default timeout.
   *
   * @param future the future to block
   * @param timeout the future timeout
   * @param timeUnit the future timeout time unit
   * @param <T> the future result type
   * @return the future result
   * @throws RuntimeException if a future exception occurs
   */
  public static <T> T get(final Future<T> future, final long timeout, final TimeUnit timeUnit) {
    try {
      return future.get(timeout, timeUnit);
    } catch (final InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a future that is synchronously completed.
   *
   * @param result The future result.
   * @return The completed future.
   */
  public static <T> CompletableFuture<T> completedFuture(final T result) {
    return CompletableFuture.completedFuture(result);
  }

  /**
   * Creates a future that is synchronously completed exceptionally.
   *
   * @param t The future exception.
   * @return The exceptionally completed future.
   */
  public static <T> CompletableFuture<T> exceptionalFuture(final Throwable t) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(t);
    return future;
  }

  /**
   * Returns a new CompletableFuture completed with a list of computed values when all of the given
   * CompletableFuture complete.
   *
   * @param futures the CompletableFutures
   * @param <T> value type of CompletableFuture
   * @return a new CompletableFuture that is completed when all of the given CompletableFutures
   *     complete
   */
  public static <T> CompletableFuture<Stream<T>> allOf(final Stream<CompletableFuture<T>> futures) {
    final CompletableFuture<T>[] futuresArray = futures.toArray(CompletableFuture[]::new);
    return AtomixFuture.wrap(
        CompletableFuture.allOf(futuresArray)
            .thenApply(v -> Stream.of(futuresArray).map(CompletableFuture::join)));
  }

  /**
   * Returns a new CompletableFuture completed with a list of computed values when all of the given
   * CompletableFuture complete.
   *
   * @param futures the CompletableFutures
   * @param <T> value type of CompletableFuture
   * @return a new CompletableFuture that is completed when all of the given CompletableFutures
   *     complete
   */
  public static <T> CompletableFuture<List<T>> allOf(final List<CompletableFuture<T>> futures) {
    return AtomixFuture.wrap(
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
            .thenApply(
                v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList())));
  }

  /**
   * Returns a new CompletableFuture completed by reducing a list of computed values when all of the
   * given CompletableFuture complete.
   *
   * @param futures the CompletableFutures
   * @param reducer reducer for computing the result
   * @param emptyValue zero value to be returned if the input future list is empty
   * @param <T> value type of CompletableFuture
   * @return a new CompletableFuture that is completed when all of the given CompletableFutures
   *     complete
   */
  public static <T> CompletableFuture<T> allOf(
      final List<CompletableFuture<T>> futures,
      final BinaryOperator<T> reducer,
      final T emptyValue) {
    return allOf(futures)
        .thenApply(resultList -> resultList.stream().reduce(reducer).orElse(emptyValue));
  }
}
