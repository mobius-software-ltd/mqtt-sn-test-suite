# Performance MQTT-SN Test Suite

## Getting started

Now you have an opportunity to independently evaluate the performance of **IoTBroker.Cloud**. 
Besides this test suite can be used to measure the performance of your own software. The following instruction will 
explain how to run the performance tests by yourself.

### Prerequisites

The following programs should be installed before starting to clone the project:

* **JDK (version 8+)**;
* **Maven**.

### Installation

First of all, you should clone [Performance MQTT-SN Test Suite](https://github.com/mobius-software-ltd/mqtt-sn-test-suite).

Then you have to build the project. For this purpose run in console "mvn clean install -Dgpg.skip=true" 

Now you have the controller (in _mqtt-test-suite/controllmqtt-test-suite/controller_ folder) and the test runner 
(in _mqtt-test-suite/runner/target_ folder) jar files on your computer.
To make the work more convenient, create _performance_test_ folder which will contain
`controller-jar-with-dependencies.jar` and `runner-jar-with-dependencies.jar`.
Also you should add [JSON files](https://github.com/mobius-software-ltd/mqtt-sn-test-suite/blob/master/runner/src/test/resources/json) and config.properties to this very performance_test folder. 
Modify scenario file by setting "controller.1.ip" and "broker.ip" with public IP addresses used on controller and broker.
In [config.properties](https://github.com/mobius-software-ltd/mqtt-sn-test-suite/blob/master/controller/src/main/resources/config.properties) 
set "localHostname" property with local ip address of the machine running the controller.

### Test run

First you should open the terminal and `cd` to _performance_test_ folder. You should start the controller by running
the command which is given below (do not forget to indicate your path):
 

Now you can start the controller by running the following command :

```
java -Xmx1024m -Xms1024m -jar mqttsn-controller.jar http://127.0.0.1:9998/ /home/username/performance_test/controller.params
 
```
Here is a brief explanation:

**Xmx1024m** – maximum memory allocation;

**Xmx1024m** – initial memory allocation;

**controller.jar** – controller which is inside the _performance_test_ folder;

**http://192.168.1.1:9998/** - IP address and port of controller;

**/home/username/performance_test/controller.params** – path to controller.params file.

Now you should open the second terminal window and `cd` to _performance_test_ folder. 
Now you can run the test by running the following command:
```
java -jar test-runner.jar pipeline.json
```
The command mentioned above is an example of running the test scenario which is described in `pipeline.json` file.

Each [JSON file](https://github.com/mobius-software-ltd/mqtt-test-suite/tree/master/docs/docs-suite/src/main/asciidoc/samples) contains different test scenarios. You can separately run each test scenario by indicating the name of a specific [JSON file](https://github.com/mobius-software-ltd/mqtt-test-suite/tree/master/docs/docs-suite/src/main/asciidoc/samples). When the test is over you will get the report for each test scenario:
```
+---------- Scenario-ID:  8bfe7e26-f2af-4980-a403-59b82e07188c ---------- Result: SUCCESS ----------+ 

| Start Time                      | 2019-01-04 15:08:49.871        | 1546607329871                  | 

| Finish Time                     | 2019-01-04 15:09:21.112        | 1546607361112                  | 

| Current Time                    | 2019-01-04 15:09:23.234        | 1546607363234                  | 

+---------------------------------+--------------------------------+--------------------------------+ 

| Total clients                 1 | Total commands               3 | Errors occured               0 | 

| Successfuly finished          1 | Successfuly finished         3 | Duplicates received          0 | 

| Failed                        0 | Failed                       0 | Duplicates sent              0 | 

+--------------- Outgoing counters ---------------+--------------- Incoming counters ---------------+ 

|      Counter Name      |      Counter Value     |      Counter Name      |      Counter Value     | 

|         CONNECT        |            1           |         CONNECT        |            0           | 

|         CONNACK        |            0           |         CONNACK        |            1           | 

|        SUBSCRIBE       |            1           |        SUBSCRIBE       |            0           | 

|         SUBACK         |            0           |         SUBACK         |            1           | 

|         PUBLISH        |            0           |         PUBLISH        |          10000         | 

|         PUBACK         |          10000         |         PUBACK         |            0           | 

|         PINGREQ        |            5           |         PINGREQ        |            0           | 

|        PINGRESP        |            0           |        PINGRESP        |            5           | 

|       DISCONNECT       |            1           |       DISCONNECT       |            1           | 

+------------------------+------------------------+------------------------+------------------------+ 
  
+---------- Scenario-ID:  c519e67c-5c37-4c33-b086-2f298a90d6b5 ---------- Result: SUCCESS ----------+ 

| Start Time                      | 2019-01-04 15:08:51.006        | 1546607331006                  | 

| Finish Time                     | 1970-01-01 15:09:21.109        | 1546607361109                  | 

| Current Time                    | 2019-01-04 15:09:23.237        | 1546607363237                  | 

+---------------------------------+--------------------------------+--------------------------------+ 

| Total clients              1000 | Total commands           13000 | Errors occured               0 | 

| Successfuly finished       1000 | Successfuly finished     13000 | Duplicates received          0 | 

| Failed                        0 | Failed                       0 | Duplicates sent              0 | 

+--------------- Outgoing counters ---------------+--------------- Incoming counters ---------------+ 

|      Counter Name      |      Counter Value     |      Counter Name      |      Counter Value     | 

|         CONNECT        |          1000          |         CONNECT        |            0           | 

|         CONNACK        |            0           |         CONNACK        |          1000          | 

|        REGISTER        |          1000          |        REGISTER        |            0           | 

|         REGACK         |            0           |         REGACK         |          1000          | 

|         PUBLISH        |          10000         |         PUBLISH        |            0           | 

|         PUBACK         |            0           |         PUBACK         |          10000         | 

|         PINGREQ        |          2000          |         PINGREQ        |            0           | 

|        PINGRESP        |            0           |        PINGRESP        |          2000          | 

|       DISCONNECT       |          1000          |       DISCONNECT       |          1000          | 

+------------------------+------------------------+------------------------+------------------------+
```
Each test can be run in its current form.
Besides you can change the existing test scenarios or add the new ones.

Performance MQTT-SN Test Suite is developed by [Mobius Software](http://mobius-software.com).

## [License](LICENSE.md)

