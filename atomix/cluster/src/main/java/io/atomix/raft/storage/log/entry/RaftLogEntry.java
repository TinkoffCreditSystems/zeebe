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

import static com.google.common.base.MoreObjects.toStringHelper;

import io.atomix.raft.storage.log.RaftLog;
import java.util.Objects;

/** Stores a state change in a {@link RaftLog}. */
public class RaftLogEntry {

  private final long term;
  private final RaftEntry entry;

  public RaftLogEntry(final long term, final RaftEntry entry) {
    this.term = term;
    this.entry = entry;
  }

  public RaftEntry entry() {
    return entry;
  }

  public boolean isApplicationEntry() {
    return entry instanceof ApplicationEntry;
  }

  public ApplicationEntry getApplicationEntry() {
    return (ApplicationEntry) entry;
  }

  public boolean isConfigurationEntry() {
    return entry instanceof ConfigurationEntry;
  }

  public ConfigurationEntry getConfigurationEntry() {
    return (ConfigurationEntry) entry;
  }

  public boolean isInitialEntry() {
    return entry instanceof InitialEntry;
  }

  public InitialEntry getInitialEntry() {
    return (InitialEntry) entry;
  }

  /**
   * Returns the entry term.
   *
   * @return The entry term.
   */
  public long term() {
    return term;
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("term", term).add("entry", entry).toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final RaftLogEntry that = (RaftLogEntry) o;
    return term == that.term && Objects.equals(entry, that.entry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(term, entry);
  }
}
