/*
 * Copyright 2017-2019 Ivan Krizsan
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

package se.ivankrizsan.springintegration.messagechannels.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Beans used in the {@code MessageChannelsCommonTests} tests.
 *
 * @author Ivan Krizsan
 */
@Configuration
public class MessageChannelsCommonTestsConfiguration implements SpringIntegrationExamplesConstants {

    /**
     * Thread scoped queue message channel bean.
     * Need to be defined as a Spring bean in order to set the scope of the bean
     * to thread scope.
     *
     * @return Queue message channel.
     */
    @Bean
    @Scope("thread")
    public QueueChannel scopedQueueChannel() {
        final QueueChannel theQueueChannel = new QueueChannel();
        theQueueChannel.setComponentName(QUEUE_CHANNEL_NAME);
        return theQueueChannel;
    }

    /**
     * Atomic reference to contain any message received from the thread-local queue
     * message channel.
     *
     * @return Atomic reference to contain any received message.
     */
    @Bean
    public AtomicReference<Message> scopedChannelMessageReference() {
        return new AtomicReference<>();
    }

    /**
     * Thread that receives a message from the thread-scoped queue message channel
     * and stores it in the supplied message reference.
     *
     * @param inApplicationContext Application context used to retrieve message channel
     * bean from.
     * @param inScopedChannelMessageReference Message reference in which any received
     * message will be stored.
     * @return Message receiver thread.
     */
    @Bean
    public Thread messageReceiverThread(
        @Autowired final ApplicationContext inApplicationContext,
        @Autowired final AtomicReference<Message> inScopedChannelMessageReference) {
        final Thread theReceiverThread = new Thread(() -> {
            /*
             * Note that the message channel bean must be retrieved by this very thread in order
             * to be truly thread-local (since it is thread scoped).
             * If the bean is autowired then another thread will retrieve the bean instance
             * and it will be the same instance as injected elsewhere.
             */
            final QueueChannel theQueueChannel =
                (QueueChannel)inApplicationContext.getBean("scopedQueueChannel");
            final Message<?> theOutputMessage =
                theQueueChannel.receive(RECEIVE_TIMEOUT_500_MILLISECONDS);
            inScopedChannelMessageReference.set(theOutputMessage);
        });

        return theReceiverThread;
    }

    /**
     * Custom Spring bean scope configurer with the Spring thread-scope.
     *
     * @return Custom Spring bean scope configurer.
     */
    @Bean
    public CustomScopeConfigurer scopeConfigurer() {
        final CustomScopeConfigurer theScopeConfigurer = new CustomScopeConfigurer();
        theScopeConfigurer.addScope("thread", new SimpleThreadScope());

        return theScopeConfigurer;
    }
}
