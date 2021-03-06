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

package io.camunda.zeebe.model.bpmn.impl.instance;

import static io.camunda.zeebe.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static io.camunda.zeebe.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CORRELATION_PROPERTY_REF;
import static io.camunda.zeebe.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CORRELATION_PROPERTY_BINDING;

import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.CorrelationProperty;
import io.camunda.zeebe.model.bpmn.instance.CorrelationPropertyBinding;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.camunda.bpm.model.xml.type.child.ChildElement;
import org.camunda.bpm.model.xml.type.child.SequenceBuilder;
import org.camunda.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN correlationPropertyBinding element
 *
 * @author Sebastian Menski
 */
public class CorrelationPropertyBindingImpl extends BaseElementImpl
    implements CorrelationPropertyBinding {

  protected static AttributeReference<CorrelationProperty> correlationPropertyRefAttribute;
  protected static ChildElement<DataPath> dataPathChild;

  public CorrelationPropertyBindingImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(CorrelationPropertyBinding.class, BPMN_ELEMENT_CORRELATION_PROPERTY_BINDING)
            .namespaceUri(BPMN20_NS)
            .extendsType(BaseElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<CorrelationPropertyBinding>() {
                  @Override
                  public CorrelationPropertyBinding newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new CorrelationPropertyBindingImpl(instanceContext);
                  }
                });

    correlationPropertyRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_CORRELATION_PROPERTY_REF)
            .required()
            .qNameAttributeReference(CorrelationProperty.class)
            .build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    dataPathChild = sequenceBuilder.element(DataPath.class).required().build();

    typeBuilder.build();
  }

  @Override
  public CorrelationProperty getCorrelationProperty() {
    return correlationPropertyRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setCorrelationProperty(final CorrelationProperty correlationProperty) {
    correlationPropertyRefAttribute.setReferenceTargetElement(this, correlationProperty);
  }

  @Override
  public DataPath getDataPath() {
    return dataPathChild.getChild(this);
  }

  @Override
  public void setDataPath(final DataPath dataPath) {
    dataPathChild.setChild(this, dataPath);
  }
}
