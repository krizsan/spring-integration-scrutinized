# Spring Integration Scrutinized
[![Build Status](https://travis-ci.org/krizsan/spring-integration-scrutinized.svg?branch=master)](https://travis-ci.org/krizsan/spring-integration-scrutinized)

A detailed look at the different concepts in Spring Integration 5.

For each concept, there is a test-class containing a number of tests.
These tests can be used as exercises by removing the, in IntelliJ IDEA folded, Answer Section and attempting a solution.
The provided solution to the tests are what I have been able to come up with and no absolute truth.
There are more ways of solving the same problem and probably even better ways.<br/><br/>

To find an example related to a certain area, start from the `se.ivankrizsan.springintegration`
package in the `src/main/test/java` directory and navigate to the appropriate package.
For instance, examples showing how to use different types of message channels are located
in the package `se.ivankrizsan.springintegration.messagechannels`.

## Notes
A bug (https://bugs.openjdk.java.net/browse/JDK-8212586) in the OpenJDK 11 compiler causes compilation failure in IntelliJ IDEA.
This bug has been fixed in JDK 12 and later.
