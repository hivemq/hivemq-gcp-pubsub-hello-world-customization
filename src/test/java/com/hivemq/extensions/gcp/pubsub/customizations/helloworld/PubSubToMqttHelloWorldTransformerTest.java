package com.hivemq.extensions.gcp.pubsub.customizations.helloworld;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extensions.gcp.pubsub.api.model.InboundPubSubMessage;
import com.hivemq.extensions.gcp.pubsub.api.model.Timestamp;
import com.hivemq.extensions.gcp.pubsub.api.transformers.MqttToPubSubInitInput;
import com.hivemq.extensions.gcp.pubsub.api.transformers.PubSubToMqttInitInput;
import com.hivemq.extensions.gcp.pubsub.api.transformers.PubSubToMqttInput;
import com.hivemq.extensions.gcp.pubsub.api.transformers.PubSubToMqttOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.hivemq.extensions.gcp.pubsub.customizations.helloworld.PubSubToMqttHelloWorldTransformer.MISSING_DATA_COUNTER_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @since 4.9.0
 */
class PubSubToMqttHelloWorldTransformerTest {

    private @NotNull MetricRegistry metricRegistry;
    private @NotNull PubSubToMqttHelloWorldTransformer transformer;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
        transformer = new PubSubToMqttHelloWorldTransformer();
    }

    @Test
    void initTransformer_customSettingsSet() {
        final PubSubToMqttInitInput initInput = mock(PubSubToMqttInitInput.class);
        when(initInput.getPubSubConnection()).thenReturn(new TestPubSubConnection());
        when(initInput.getCustomSettings()).thenReturn(new TestCustomSettings(
                List.of(
                        new TestCustomSetting("settings-1", "value-1"),
                        new TestCustomSetting("settings-2", "value-2")
                )
        ));
        when(initInput.getMetricRegistry()).thenReturn(metricRegistry);

        transformer.init(initInput);

        assertNotNull(transformer.customSettings);
    }

    @Test
    void initTransformer_initFailed_noException() {
        final PubSubToMqttInitInput initInput = mock(PubSubToMqttInitInput.class);
        when(initInput.getPubSubConnection()).thenReturn(new TestPubSubConnection());
        when(initInput.getCustomSettings()).thenThrow(new RuntimeException("TEST_FAILED"));
        when(initInput.getMetricRegistry()).thenReturn(metricRegistry);

        assertDoesNotThrow(() -> transformer.init(initInput));
    }

    @Test
    void transformMessageFailed_noExceptionThrown() {
        final PubSubToMqttOutput output = mock(PubSubToMqttOutput.class);
        final PubSubToMqttInput input = mock(PubSubToMqttInput.class);
        when(input.getInboundPubSubMessage()).thenThrow(new RuntimeException("TEST_FAILED"));

        assertDoesNotThrow(() -> transformer.transformPubSubToMqtt(input, output));
    }

    @Test
    void transformMessageSuccess_publishMessageAsResult() {

        final PubSubToMqttInitInput initInput = mock(PubSubToMqttInitInput.class);
        when(initInput.getPubSubConnection()).thenReturn(new TestPubSubConnection());
        when(initInput.getCustomSettings()).thenReturn(new TestCustomSettings(
                List.of(new TestCustomSetting("qos", "1"))
        ));
        when(initInput.getMetricRegistry()).thenReturn(metricRegistry);
        transformer.init(initInput);

        final PubSubToMqttOutput output = mock(PubSubToMqttOutput.class);
        final PubSubToMqttInput input = mock(PubSubToMqttInput.class);
        final InboundPubSubMessage inboundPubSubMessage = mock(InboundPubSubMessage.class);
        final PublishBuilder publishBuilder = mock(PublishBuilder.class);
        when(input.getInboundPubSubMessage()).thenReturn(inboundPubSubMessage);
        when(output.newPublishBuilder()).thenReturn(publishBuilder);
        when(publishBuilder.topic(any())).thenReturn(publishBuilder);
        when(publishBuilder.build()).thenReturn(mock(Publish.class));
        when(inboundPubSubMessage.getData()).thenReturn(Optional.of(ByteBuffer.wrap(new byte[10])));
        when(inboundPubSubMessage.getAttributes()).thenReturn(Collections.emptyMap());
        final AtomicReference<List<Publish>> publishAtomicReference = new AtomicReference<>();
        doAnswer(invocation -> {
            publishAtomicReference.set(invocation.getArgument(0));
            return null;
        }).when(output).setPublishes(any());

        transformer.transformPubSubToMqtt(input, output);

        assertNotNull(publishAtomicReference.get());
        assertEquals(1, publishAtomicReference.get().size());
    }

    @Test
    void transformMessage_qosNotParseable_defaultQosUsed() {

        final PubSubToMqttInitInput initInput = mock(PubSubToMqttInitInput.class);
        when(initInput.getPubSubConnection()).thenReturn(new TestPubSubConnection());
        when(initInput.getCustomSettings()).thenReturn(new TestCustomSettings(
                List.of(new TestCustomSetting("qos", "ONE"))
        ));
        when(initInput.getMetricRegistry()).thenReturn(metricRegistry);
        transformer.init(initInput);

        final PubSubToMqttOutput output = mock(PubSubToMqttOutput.class);
        final PubSubToMqttInput input = mock(PubSubToMqttInput.class);
        final InboundPubSubMessage inboundPubSubMessage = mock(InboundPubSubMessage.class);
        final PublishBuilder publishBuilder = mock(PublishBuilder.class);
        when(input.getInboundPubSubMessage()).thenReturn(inboundPubSubMessage);
        when(output.newPublishBuilder()).thenReturn(publishBuilder);
        when(publishBuilder.topic(any())).thenReturn(publishBuilder);
        when(publishBuilder.build()).thenReturn(mock(Publish.class));
        when(inboundPubSubMessage.getData()).thenReturn(Optional.of(ByteBuffer.wrap(new byte[10])));
        when(inboundPubSubMessage.getAttributes()).thenReturn(Collections.emptyMap());
        final AtomicReference<List<Publish>> publishAtomicReference = new AtomicReference<>();
        doAnswer(invocation -> {
            publishAtomicReference.set(invocation.getArgument(0));
            return null;
        }).when(output).setPublishes(any());

        transformer.transformPubSubToMqtt(input, output);

        assertNotNull(publishAtomicReference.get());
        assertEquals(1, publishAtomicReference.get().size());
        verify(publishBuilder).qos(Qos.AT_MOST_ONCE);
    }

    @Test
    void transformMessage_noData_metricIncreased() {

        final PubSubToMqttInitInput initInput = mock(PubSubToMqttInitInput.class);
        when(initInput.getPubSubConnection()).thenReturn(new TestPubSubConnection());
        when(initInput.getCustomSettings()).thenReturn(new TestCustomSettings(
                List.of(new TestCustomSetting("qos", "1"))
        ));
        when(initInput.getMetricRegistry()).thenReturn(metricRegistry);
        transformer.init(initInput);

        final PubSubToMqttOutput output = mock(PubSubToMqttOutput.class);
        final PubSubToMqttInput input = mock(PubSubToMqttInput.class);
        final InboundPubSubMessage inboundPubSubMessage = mock(InboundPubSubMessage.class);
        final PublishBuilder publishBuilder = mock(PublishBuilder.class);
        when(input.getInboundPubSubMessage()).thenReturn(inboundPubSubMessage);
        when(output.newPublishBuilder()).thenReturn(publishBuilder);
        when(publishBuilder.topic(any())).thenReturn(publishBuilder);
        when(publishBuilder.build()).thenReturn(mock(Publish.class));
        when(inboundPubSubMessage.getData()).thenReturn(Optional.empty());
        when(inboundPubSubMessage.getAttributes()).thenReturn(Collections.emptyMap());
        final AtomicReference<List<Publish>> publishAtomicReference = new AtomicReference<>();
        doAnswer(invocation -> {
            publishAtomicReference.set(invocation.getArgument(0));
            return null;
        }).when(output).setPublishes(any());

        transformer.transformPubSubToMqtt(input, output);

        assertNotNull(publishAtomicReference.get());
        assertEquals(1, publishAtomicReference.get().size());
        assertEquals(1, metricRegistry.counter(MISSING_DATA_COUNTER_NAME).getCount());
    }
}