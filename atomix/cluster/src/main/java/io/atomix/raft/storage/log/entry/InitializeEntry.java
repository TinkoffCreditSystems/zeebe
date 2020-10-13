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
package io.atomix.raft.storage.log.entry;

import io.atomix.storage.protocol.EntryType;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * Indicates a leader change has occurred.
 *
 * <p>The {@code InitializeEntry} is logged by a leader at the beginning of its term to indicate
 * that a leadership change has occurred. Importantly, initialize entries are logged with a {@link
 * #timestamp() timestamp} which can be used by server state machines to reset session timeouts
 * following leader changes. Initialize entries are always the first entry to be committed at the
 * start of a leader's term.
 */
public class InitializeEntry implements EntryValue {
  private final long term;
  private final long timestamp;

  public InitializeEntry(final long term, final long timestamp) {
    this.term = term;
    this.timestamp = timestamp;
  }

  @Override
  public long term() {
    return term;
  }

  @Override
  public long timestamp() {
    return timestamp;
  }

  @Override
  public EntryType type() {
    return EntryType.INITIALIZE;
  }

  @Override
  public int serialize(
      final EntrySerializer serializer, final MutableDirectBuffer dest, final int offset) {
    return 0;
  }
}
