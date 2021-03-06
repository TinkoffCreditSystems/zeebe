/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.camunda.zeebe.model.bpmn.instance.bpmndi;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.di.LabeledShape;

/**
 * The BPMNDI BPMNShape element
 *
 * @author Sebastian Menski
 */
public interface BpmnShape extends LabeledShape {

  BaseElement getBpmnElement();

  void setBpmnElement(BaseElement bpmnElement);

  boolean isHorizontal();

  void setHorizontal(boolean isHorizontal);

  boolean isExpanded();

  void setExpanded(boolean isExpanded);

  boolean isMarkerVisible();

  void setMarkerVisible(boolean isMarkerVisible);

  boolean isMessageVisible();

  void setMessageVisible(boolean isMessageVisible);

  ParticipantBandKind getParticipantBandKind();

  void setParticipantBandKind(ParticipantBandKind participantBandKind);

  BpmnShape getChoreographyActivityShape();

  void setChoreographyActivityShape(BpmnShape choreographyActivityShape);

  BpmnLabel getBpmnLabel();

  void setBpmnLabel(BpmnLabel bpmnLabel);
}
