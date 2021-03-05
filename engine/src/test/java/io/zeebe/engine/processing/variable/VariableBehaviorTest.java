/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.engine.processing.variable;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.processing.streamprocessor.writers.EventApplyingStateWriter;
import io.zeebe.engine.processing.streamprocessor.writers.StateWriter;
import io.zeebe.engine.state.DefaultZeebeDbFactory;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.ZeebeDbState;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.engine.state.appliers.EventAppliers;
import io.zeebe.engine.state.immutable.VariableState;
import io.zeebe.engine.state.mutable.MutableVariableState;
import io.zeebe.engine.util.RecordingTypedEventWriter;
import io.zeebe.engine.util.RecordingTypedEventWriter.RecordedEvent;
import io.zeebe.protocol.record.intent.VariableIntent;
import io.zeebe.protocol.record.value.VariableRecordValue;
import io.zeebe.protocol.record.value.VariableRecordValueAssert;
import io.zeebe.test.util.MsgPackUtil;
import io.zeebe.util.buffer.BufferUtil;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class VariableBehaviorTest {

  private final RecordingTypedEventWriter eventWriter = new RecordingTypedEventWriter();

  private ZeebeDb<ZbColumnFamilies> db;
  private MutableVariableState state;
  private VariableBehavior behavior;

  @BeforeEach
  void beforeEach(final @TempDir File directory) {
    db = DefaultZeebeDbFactory.defaultFactory().createDb(directory);
    final ZeebeState zeebeState = new ZeebeDbState(db, db.createContext());
    final StateWriter stateWriter =
        new EventApplyingStateWriter(eventWriter, new EventAppliers(zeebeState));

    state = zeebeState.getVariableState();
    behavior = new VariableBehavior(state, stateWriter, zeebeState.getKeyGenerator());
  }

  @AfterEach
  void afterEach() {
    CloseHelper.close(db);
  }

  @Test
  void shouldMergeLocalDocument() {
    // given
    final long processDefinitionKey = 1;
    final long parentScopeKey = 1;
    final long childScopeKey = 2;
    final long childFooKey = 3;
    final Map<String, Object> document = Map.of("foo", "bar", "baz", "buz");
    state.createScope(parentScopeKey, VariableState.NO_PARENT);
    state.createScope(childScopeKey, parentScopeKey);
    setVariable(childFooKey, childScopeKey, processDefinitionKey, "foo", "qux");

    // when
    behavior.mergeLocalDocument(
        childScopeKey, processDefinitionKey, parentScopeKey, MsgPackUtil.asMsgPack(document));

    // then
    final List<RecordedEvent<VariableRecordValue>> events = getFollowUpEvents();
    assertThat(events)
        .satisfiesExactlyInAnyOrder(
            event -> {
              assertThat(event.intent).isEqualTo(VariableIntent.CREATED);
              VariableRecordValueAssert.assertThat(event.value)
                  .hasName("baz")
                  .hasValue("\"buz\"")
                  .hasScopeKey(childScopeKey)
                  .hasProcessDefinitionKey(processDefinitionKey)
                  .hasProcessInstanceKey(parentScopeKey);
            },
            event -> {
              assertThat(event.intent).isEqualTo(VariableIntent.UPDATED);
              assertThat(event.key).isEqualTo(childFooKey);
              VariableRecordValueAssert.assertThat(event.value)
                  .hasName("foo")
                  .hasValue("\"bar\"")
                  .hasScopeKey(childScopeKey)
                  .hasProcessDefinitionKey(processDefinitionKey)
                  .hasProcessInstanceKey(parentScopeKey);
            });
  }

  @Test
  void shouldNotMergeLocalDocumentIfEmpty() {
    // given
    final long processDefinitionKey = 1;
    final long scopeKey = 1;
    final Map<String, Object> document = Map.of();
    setVariable(2, scopeKey, processDefinitionKey, "foo", "qux");

    // when
    behavior.mergeLocalDocument(
        scopeKey, processDefinitionKey, scopeKey, MsgPackUtil.asMsgPack(document));

    // then
    assertThat(getFollowUpEvents()).isEmpty();
  }

  @Test
  void shouldMergeDocumentWithoutPropagatingMoreThanOnce() {
    // given
    final long processDefinitionKey = 1;
    final long rootScopeKey = 1;
    final long parentScopeKey = 2;
    final long childScopeKey = 3;
    final long parentFooKey = 4;
    final Map<String, Object> document = Map.of("foo", "bar");
    state.createScope(rootScopeKey, VariableState.NO_PARENT);
    state.createScope(parentScopeKey, rootScopeKey);
    state.createScope(childScopeKey, parentScopeKey);
    setVariable(parentFooKey, parentScopeKey, processDefinitionKey, "foo", "qux");
    setVariable(5, rootScopeKey, processDefinitionKey, "foo", "biz");

    // when
    behavior.mergeDocument(
        childScopeKey, processDefinitionKey, rootScopeKey, MsgPackUtil.asMsgPack(document));

    // then
    final List<RecordedEvent<VariableRecordValue>> events = getFollowUpEvents();
    assertThat(events)
        .satisfiesExactlyInAnyOrder(
            event -> {
              assertThat(event.intent).isEqualTo(VariableIntent.UPDATED);
              assertThat(event.key).isEqualTo(parentFooKey);
              VariableRecordValueAssert.assertThat(event.value)
                  .hasName("foo")
                  .hasValue("\"bar\"")
                  .hasScopeKey(parentScopeKey)
                  .hasProcessDefinitionKey(processDefinitionKey)
                  .hasProcessInstanceKey(rootScopeKey);
            });
  }

  @Test
  void shouldMergeDocumentPropagatingToRoot() {
    // given
    final long processDefinitionKey = 1;
    final long rootScopeKey = 1;
    final long parentScopeKey = 2;
    final long childScopeKey = 3;
    final Map<String, Object> document = Map.of("foo", "bar", "buz", "baz");
    state.createScope(rootScopeKey, VariableState.NO_PARENT);
    state.createScope(parentScopeKey, rootScopeKey);
    state.createScope(childScopeKey, parentScopeKey);

    // when
    behavior.mergeDocument(
        childScopeKey, processDefinitionKey, rootScopeKey, MsgPackUtil.asMsgPack(document));

    // then
    final List<RecordedEvent<VariableRecordValue>> events = getFollowUpEvents();
    assertThat(events)
        .satisfiesExactlyInAnyOrder(
            event -> {
              assertThat(event.intent).isEqualTo(VariableIntent.CREATED);
              VariableRecordValueAssert.assertThat(event.value)
                  .hasName("foo")
                  .hasValue("\"bar\"")
                  .hasScopeKey(rootScopeKey)
                  .hasProcessDefinitionKey(processDefinitionKey)
                  .hasProcessInstanceKey(rootScopeKey);
            },
            event -> {
              assertThat(event.intent).isEqualTo(VariableIntent.CREATED);
              VariableRecordValueAssert.assertThat(event.value)
                  .hasName("buz")
                  .hasValue("\"baz\"")
                  .hasScopeKey(rootScopeKey)
                  .hasProcessDefinitionKey(processDefinitionKey)
                  .hasProcessInstanceKey(rootScopeKey);
            });
  }

  @Test
  void shouldMergeDocumentWithoutPropagatingExistingVariables() {
    // given
    final long processDefinitionKey = 1;
    final long parentScopeKey = 1;
    final long childScopeKey = 2;
    final long childFooKey = 3;
    final Map<String, Object> document = Map.of("foo", "bar");
    state.createScope(parentScopeKey, VariableState.NO_PARENT);
    state.createScope(childScopeKey, parentScopeKey);
    setVariable(childFooKey, childScopeKey, processDefinitionKey, "foo", "qux");
    setVariable(4, parentScopeKey, processDefinitionKey, "foo", "biz");

    // when
    behavior.mergeDocument(
        childScopeKey, processDefinitionKey, parentScopeKey, MsgPackUtil.asMsgPack(document));

    // then
    final List<RecordedEvent<VariableRecordValue>> events = getFollowUpEvents();
    assertThat(events)
        .satisfiesExactlyInAnyOrder(
            event -> {
              assertThat(event.intent).isEqualTo(VariableIntent.UPDATED);
              assertThat(event.key).isEqualTo(childFooKey);
              VariableRecordValueAssert.assertThat(event.value)
                  .hasName("foo")
                  .hasValue("\"bar\"")
                  .hasScopeKey(childScopeKey)
                  .hasProcessDefinitionKey(processDefinitionKey)
                  .hasProcessInstanceKey(parentScopeKey);
            });
  }

  @Test
  void shouldNotMergeDocumentIfEmpty() {
    // given
    final int processDefinitionKey = 1;
    final int parentScopeKey = 1;
    final int childScopeKey = 2;
    final Map<String, Object> document = Map.of();
    state.createScope(parentScopeKey, VariableState.NO_PARENT);
    state.createScope(childScopeKey, parentScopeKey);
    setVariable(3, parentScopeKey, processDefinitionKey, "foo", "qux");
    setVariable(4, childScopeKey, processDefinitionKey, "foo", "bar");

    // when
    behavior.mergeDocument(
        childScopeKey, processDefinitionKey, parentScopeKey, MsgPackUtil.asMsgPack(document));

    // then
    final List<RecordedEvent<VariableRecordValue>> events = getFollowUpEvents();
    assertThat(events).isEmpty();
  }

  @Test
  void shouldCreateLocalVariable() {
    // given
    final int processDefinitionKey = 1;
    final int parentScopeKey = 1;
    final int childScopeKey = 2;
    final DirectBuffer variableName = BufferUtil.wrapString("foo");
    final DirectBuffer variableValue = packString("bar");
    state.createScope(parentScopeKey, VariableState.NO_PARENT);
    state.createScope(childScopeKey, parentScopeKey);

    // when
    behavior.setLocalVariable(
        childScopeKey,
        processDefinitionKey,
        parentScopeKey,
        variableName,
        variableValue,
        0,
        variableValue.capacity());

    // then
    final List<RecordedEvent<VariableRecordValue>> events = getFollowUpEvents();
    assertThat(events)
        .satisfiesExactlyInAnyOrder(
            event -> {
              assertThat(event.intent).isEqualTo(VariableIntent.CREATED);
              VariableRecordValueAssert.assertThat(event.value)
                  .hasName("foo")
                  .hasValue("\"bar\"")
                  .hasScopeKey(childScopeKey)
                  .hasProcessDefinitionKey(processDefinitionKey)
                  .hasProcessInstanceKey(parentScopeKey);
            });
  }

  @Test
  void shouldUpdateLocalVariable() {
    // given
    final long processDefinitionKey = 1;
    final long parentScopeKey = 1;
    final long childScopeKey = 2;
    final long parentFooKey = 3;
    final DirectBuffer variableName = BufferUtil.wrapString("foo");
    final DirectBuffer variableValue = packString("bar");
    state.createScope(parentScopeKey, VariableState.NO_PARENT);
    state.createScope(childScopeKey, parentScopeKey);
    setVariable(parentFooKey, parentScopeKey, processDefinitionKey, "foo", "qux");

    // when
    behavior.setLocalVariable(
        parentScopeKey,
        processDefinitionKey,
        parentScopeKey,
        variableName,
        variableValue,
        0,
        variableValue.capacity());

    // then
    final List<RecordedEvent<VariableRecordValue>> events = getFollowUpEvents();
    assertThat(events)
        .satisfiesExactlyInAnyOrder(
            event -> {
              assertThat(event.intent).isEqualTo(VariableIntent.UPDATED);
              assertThat(event.key).isEqualTo(parentFooKey);
              VariableRecordValueAssert.assertThat(event.value)
                  .hasName("foo")
                  .hasValue("\"bar\"")
                  .hasScopeKey(parentScopeKey)
                  .hasProcessDefinitionKey(processDefinitionKey)
                  .hasProcessInstanceKey(parentScopeKey);
            });
  }

  @SuppressWarnings("unchecked")
  private List<RecordedEvent<VariableRecordValue>> getFollowUpEvents() {
    return eventWriter.getEvents().stream()
        .filter(e -> e.value instanceof VariableRecordValue)
        .map(e -> (RecordedEvent<VariableRecordValue>) e)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("SameParameterValue")
  private void setVariable(
      final long key,
      final long scopeKey,
      final long processDefinitionKey,
      final String name,
      final String value) {
    final DirectBuffer nameBuffer = BufferUtil.wrapString(name);
    state.setVariableLocal(key, scopeKey, processDefinitionKey, nameBuffer, packString(value));
  }

  private DirectBuffer packString(final String value) {
    return MsgPackUtil.encodeMsgPack(b -> b.packString(value));
  }
}
