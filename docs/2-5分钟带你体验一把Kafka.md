

前置条件：你的电脑已经安装 Docker 

主要内容：

1. 使用 Docker 安装
2. 使用命令行测试消息队列的功能
3. zookeeper和kafka可视化管理工具
4. Java 程序中简单使用Kafka

### 使用 Docker 安装搭建Kafka环境

#### 单机版

**下面使用的单机版的 Kafka 来作为演示，推荐先搭建单机版的Kafka来学习。**

> 以下使用 Docker 搭建Kafka基本环境来自开源项目：https://github.com/simplesteph/kafka-stack-docker-compose  。当然，你也可以按照官方提供的来：https://github.com/wurstmeister/kafka-docker/blob/master/docker-compose.yml 。

新建一个名为 `zk-single-kafka-single.yml` 的文件，文件内容如下：

```yaml
version: '2.1'

services:
  zoo1:
    image: zookeeper:3.4.9
    hostname: zoo1
    ports:
      - "2181:2181"
    environment:
      ZOO_MY_ID: 1
      ZOO_PORT: 2181
      ZOO_SERVERS: server.1=zoo1:2888:3888
    volumes:
      - ./zk-single-kafka-single/zoo1/data:/data
      - ./zk-single-kafka-single/zoo1/datalog:/datalog

  kafka1:
    image: confluentinc/cp-kafka:5.3.1
    hostname: kafka1
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: LISTENER_DOCKER_INTERNAL://kafka1:19092,LISTENER_DOCKER_EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: LISTENER_DOCKER_INTERNAL:PLAINTEXT,LISTENER_DOCKER_EXTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: LISTENER_DOCKER_INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zoo1:2181"
      KAFKA_BROKER_ID: 1
      KAFKA_LOG4J_LOGGERS: "kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

    volumes:
      - ./zk-single-kafka-single/kafka1/data:/var/lib/kafka/data
    depends_on:
      - zoo1

```

运行以下命令即可完成环境搭建(会自动下载并运行一个 zookeeper 和 kafka )

```shell
docker-compose -f zk-single-kafka-single.yml up
```

如果需要停止Kafka相关容器的话，运行以下命令即可：

```shell
docker-compose -f zk-single-kafka-single.yml down
```

####  集群版

> 以下使用 Docker 搭建Kafka基本环境来自开源项目：https://github.com/simplesteph/kafka-stack-docker-compose  。

新建一个名为 `zk-single-kafka-multiple.yml` 的文件，文件内容如下：

```java
version: '2.1'

services:
  zoo1:
    image: zookeeper:3.4.9
    hostname: zoo1
    ports:
      - "2181:2181"
    environment:
        ZOO_MY_ID: 1
        ZOO_PORT: 2181
        ZOO_SERVERS: server.1=zoo1:2888:3888
    volumes:
      - ./zk-single-kafka-multiple/zoo1/data:/data
      - ./zk-single-kafka-multiple/zoo1/datalog:/datalog

  kafka1:
    image: confluentinc/cp-kafka:5.4.0
    hostname: kafka1
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: LISTENER_DOCKER_INTERNAL://kafka1:19092,LISTENER_DOCKER_EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: LISTENER_DOCKER_INTERNAL:PLAINTEXT,LISTENER_DOCKER_EXTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: LISTENER_DOCKER_INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zoo1:2181"
      KAFKA_BROKER_ID: 1
      KAFKA_LOG4J_LOGGERS: "kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO"
    volumes:
      - ./zk-single-kafka-multiple/kafka1/data:/var/lib/kafka/data
    depends_on:
      - zoo1

  kafka2:
    image: confluentinc/cp-kafka:5.4.0
    hostname: kafka2
    ports:
      - "9093:9093"
    environment:
      KAFKA_ADVERTISED_LISTENERS: LISTENER_DOCKER_INTERNAL://kafka2:19093,LISTENER_DOCKER_EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: LISTENER_DOCKER_INTERNAL:PLAINTEXT,LISTENER_DOCKER_EXTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: LISTENER_DOCKER_INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zoo1:2181"
      KAFKA_BROKER_ID: 2
      KAFKA_LOG4J_LOGGERS: "kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO"
    volumes:
      - ./zk-single-kafka-multiple/kafka2/data:/var/lib/kafka/data
    depends_on:
      - zoo1


  kafka3:
    image: confluentinc/cp-kafka:5.4.0
    hostname: kafka3
    ports:
      - "9094:9094"
    environment:
      KAFKA_ADVERTISED_LISTENERS: LISTENER_DOCKER_INTERNAL://kafka3:19094,LISTENER_DOCKER_EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9094
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: LISTENER_DOCKER_INTERNAL:PLAINTEXT,LISTENER_DOCKER_EXTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: LISTENER_DOCKER_INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zoo1:2181"
      KAFKA_BROKER_ID: 3
      KAFKA_LOG4J_LOGGERS: "kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO"
    volumes:
      - ./zk-single-kafka-multiple/kafka3/data:/var/lib/kafka/data
    depends_on:
      - zoo1

```

运行以下命令即可完成 1个节点 Zookeeper+3个节点的 Kafka 的环境搭建。

```shell
docker-compose -f zk-single-kafka-multiple.yml up
```

如果需要停止Kafka相关容器的话，运行以下命令即可：

```shell
docker-compose -f zk-single-kafka-multiple.yml down
```

### 使用命令行测试消息的生产和消费

一般情况下我们很少会用到 Kafka 的命令行操作。

**1.进入 Kafka container 内部执行 Kafka 官方自带了一些命令**

```shell
docker exec -ti docker_kafka1_1 bash
```

**2.列出所有 Topic**

```shell
root@kafka1:/# kafka-topics --describe --zookeeper zoo1:2181
```

**3.创建一个 Topic**

```shell
root@kafka1:/# kafka-topics --create --topic test --partitions 3 --zookeeper zoo1:2181 --replication-factor 1
Created topic test.
```

我们创建了一个名为 test  的 Topic, partition 数为 3, replica 数为 1。

**4.消费者订阅主题**

```shell
root@kafka1:/# kafka-console-consumer --bootstrap-server localhost:9092 --topic test
send hello from console -producer
```

我们订阅了 名为 test  的 Topic。

**5.生产者向 Topic 发送消息**

```shell
root@kafka1:/# kafka-console-producer --broker-list localhost:9092 --topic test
>send hello from console -producer
>
```

我们使用 `kafka-console-producer ` 命令向名为 test  的 Topic 发送了一条消息，消息内容为：“send hello from console -producer”

这个时候，你会发现消费者成功接收到了消息：

```shell
root@kafka1:/# kafka-console-consumer --bootstrap-server localhost:9092 --topic test
send hello from console -producer
```

### IDEA相关插件推荐

#### Zoolytic-Zookeeper tool

这是一款 IDEA 提供的 Zookeeper 可视化工具插件，非常好用！ 我们可以通过它：

1. 可视化ZkNodes节点信息
2. ZkNodes节点管理-添加/删除
3. 编辑zkNodes数据
4. ......

实际使用效果如下：

<img src="https://my-blog-to-use.oss-cn-beijing.aliyuncs.com/2019-11/zookeeper-kafka.jpg" style="zoom:50%;" />

使用方法：

1. 打开工具：View->Tool windows->Zoolytic；
2. 点击 “+” 号后在弹出框数据：“127.0.0.1:2181” 连接 zookeeper；
3. 连接之后点击新创建的连接然后点击“+”号旁边的刷新按钮即可！

#### Kafkalytic

IDEA 提供的 Kafka 可视化管理插件。这个插件为我们提供了下面这写功能：

1. 多个集群支持
2. 主题管理:创建/删除/更改分区
3. 使用正则表达式搜索主题
4. 发布字符串/字节序列化的消息
5. 使用不同的策略消费消息

实际使用效果如下：

<img src="https://my-blog-to-use.oss-cn-beijing.aliyuncs.com/2019-11/kafkalytic.jpg" style="zoom:50%;" />

使用方法：

1. 打开工具：View->Tool windows->kafkalytic；

2. 点击 “+” 号后在弹出框数据：“127.0.0.1:9092” 连接；

### Java 程序中简单使用Kafka

> 代码地址：https://github.com/Snailclimb/springboot-kafka/tree/master/kafka-intro-maven-demo

**Step 1:新建一个Maven项目**

**Step2:  `pom.xml` 中添加相关依赖**

```
        <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-clients</artifactId>
            <version>2.2.0</version>
        </dependency>
```

**Step 3：初始化消费者和生产者**

`KafkaConstants`常量类中定义了Kafka一些常用配置常量。

```java
public class KafkaConstants {
    public static final String BROKER_LIST = "localhost:9092";
    public static final String CLIENT_ID = "client1";
    public static String GROUP_ID_CONFIG="consumerGroup1";
    private KafkaConstants() {

    }
}

```

`ProducerCreator` 中有一个 `createProducer()` 方法方法用于返回一个  `KafkaProducer`对象

```java
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * @author shuang.kou
 */
public class ProducerCreator {


    public static Producer<String, String> createProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.BROKER_LIST);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, KafkaConstants.CLIENT_ID);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }
}

```

ConsumerCreator 中有一个` createConsumer()` 方法方法用于返回一个  `KafkaConsumer` 对象

```java
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;

public class ConsumerCreator {

    public static Consumer<String, String> createConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.BROKER_LIST);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaConstants.GROUP_ID_CONFIG);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(properties);
    }
}

```

**Step 4：发送和消费消息**

生产者发送消息：

 ```java
private static final String TOPIC = "test-topic";
Producer<String, String> producer = ProducerCreator.createProducer();
ProducerRecord<String, String> record =
  new ProducerRecord<>(TOPIC, "hello, Kafka!");
try {
  //send message
  RecordMetadata metadata = producer.send(record).get();
  System.out.println("Record sent to partition " + metadata.partition()
                     + " with offset " + metadata.offset());
} catch (ExecutionException | InterruptedException e) {
  System.out.println("Error in sending record");
  e.printStackTrace();
}
producer.close();
 ```

消费者消费消息：

```java
Consumer<String, String> consumer = ConsumerCreator.createConsumer();
// 循环消费消息
while (true) {
  //subscribe topic and consume message
  consumer.subscribe(Collections.singletonList(TOPIC));

  ConsumerRecords<String, String> consumerRecords =
    consumer.poll(Duration.ofMillis(1000));
  for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
    System.out.println("Consumer consume message:" + consumerRecord.value());
  }
}
```

**Step 5：测试**

运行程序控制台打印出：

```j
Record sent to partition 0 with offset 20
Consumer consume message:hello, Kafka!
```

