# Spring Integration Scrutinized

A detailed look at the different concepts in Spring Integration 5.

For each concept, there is a test-class containing a number of tests.
These tests can be used as exercises by removing the, in IntelliJ IDEA folded, Answer Section and attempting a solution.
The provided solution to the tests are what I have been able to come up with and no absolute truth.
There are more ways of solving the same problem and probably even better ways.

The following concepts are scrutinized in the project.

## Message Channels
Examines the different types of message channels in Spring Integration.
Located in the package se.ivankrizsan.springintegration.messagechannels.

### DirectChannelTests
Examines the direct message channel, which is the default message channel in Spring Integration.
This message channel allows for multiple subscribers but only one subscriber will receive a message.
Dispatches messages in the sender thread.

### ExecutorChannelTests
Examines the executor message channel.
This message channel allows for multiple subscribers but only one subscriber will receive a message.
Can dispatch messages in a separate thread.

### FluxMessageChannelTests
Examines the flux message channel which uses reactive streams.
Messages sent to a flux message channel are published to all subscribers of the message channel.

### MessageChannelCommonTests
Examines concepts and functionality common to all Spring Integration message channels.

### NullChannelTests
Examines the null message channel, which is the /dev/null of message channels.

### PriorityChannelTests
Examines the priority message channel, which allows for messages to be prioritized thus bypass strict first-in-first-out ordering of messages.
This message channel allows for multiple subscribers but only one subscriber will receive a message.

### PublishSubscribeChannelTests
Messages sent to a publish-subscribe message channel are published to all subscribers of the message channel.

### QueueChannelTests
The queue message channel retains messages in a queue for clients to poll when ready to consume a message.

### RendezvousChannelTests
This type of message channel cause the producer of messages to block until a consumer has polled the message channel for the message.

## Message Channel Interceptors
Message channel interceptors can be used to intercept the sending and receiving of message to/from a message channel.
Located in the package se.ivankrizsan.springintegration.channelinterceptors.

### ExecutorChannelInterceptorTests
Message channel interceptor that makes it possible to intercept asynchronous sending of messages to a subscriber.

### WireTapInterceptorTests
Wire-taps messages from the intercepted message channel, sending them to an additional message channel.
