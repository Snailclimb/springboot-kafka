### Step1:创建项目

直接通过Spring 官方提供的 [Spring Initializr](https://start.spring.io/) 创建或者直接使用 IDEA 创建皆可。

![](https://imgkr.cn-bj.ufileos.com/946d907d-f983-4bb0-ad32-76deb15057cb.jpg)

### Step2: 配置 Kafka

通过 application.yml 配置文件配置 Kafka 基本信息

```yml
server:
  port: 9090

spring:
  kafka:
    consumer:
      bootstrap-servers: localhost:9092
      # 配置消费者消息offset是否自动重置(消费者重连会能够接收最开始的消息)
      auto-offset-reset: earliest
    producer:
      bootstrap-servers: localhost:9092
      # 发送的对象信息变为json格式
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
kafka:
  topic:
    my-topic: my-topic
    my-topic2: my-topic2
```

Kafka 额外配置类：

```java
package cn.javaguide.springbootkafka01sendobjects.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

/**
 * @author shuang.kou
 */
@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.my-topic}")
    String myTopic;
    @Value("${kafka.topic.my-topic2}")
    String myTopic2;

    /**
     * JSON消息转换器
     */
    @Bean
    public RecordMessageConverter jsonConverter() {
        return new StringJsonMessageConverter();
    }

    /**
     * 通过注入一个 NewTopic 类型的 Bean 来创建 topic，如果 topic 已存在，则会忽略。
     */
    @Bean
    public NewTopic myTopic() {
        return new NewTopic(myTopic, 2, (short) 1);
    }

    @Bean
    public NewTopic myTopic2() {
        return new NewTopic(myTopic2, 1, (short) 1);
    }
}

```

当我们到了这一步之后，你就可以试着运行项目了，运行成功后你会发现 Spring Boot 会为你创建两个topic:

1. my-topic: partition 数为 2, replica 数为 1
2. my-topic2:partition 数为 1, replica 数为 1

> 通过上一节说的：`kafka-topics --describe --zookeeper zoo1:2181` 命令查看或者直接通过IDEA 提供的 Kafka 可视化管理插件-Kafkalytic 来查看

### Step3:创建要发送的消息实体类

```java
package cn.javaguide.springbootkafka01sendobjects.entity;

public class Book {
    private Long id;
    private String name;

    public Book() {
    }

    public Book(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    省略 getter/setter以及 toString方法
}

```

### Step4:创建发送消息的生产者

> 这一步内容比较长，会一步一步优化生产者的代码。

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookProducerService {

    private static final Logger logger = LoggerFactory.getLogger(BookProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BookProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, Object o) {
        kafkaTemplate.send(topic, o);
    }
}
```

我们使用Kafka 提供的  `KafkaTemplate `  调用 `send()`方法出入要发往的topic和消息内容即可很方便的完成消息的发送:

```java
  kafkaTemplate.send(topic, o);
```

如果我们想要知道消息发送的结果的话，`sendMessage`方法这样写：

```java
    public void sendMessage(String topic, Object o) {
        try {
            SendResult<String, Object> sendResult = kafkaTemplate.send(topic, o).get();
            if (sendResult.getRecordMetadata() != null) {
                logger.info("生产者成功发送消息到" + sendResult.getProducerRecord().topic() + "-> " + sendResult.getProducerRecord().value().toString());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
```

但是这种属于同步的发送方式并不推荐，没有利用到 `Future`对象的特性。

 `KafkaTemplate `  调用 `send()`方法实际上返回的是`ListenableFuture` 对象。

`send()`方法源码如下：

```java
	@Override
	public ListenableFuture<SendResult<K, V>> send(String topic, @Nullable V data) {
		ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, data);
		return doSend(producerRecord);
	}
```

`ListenableFuture` 是Spring提供了继承自`Future` 的接口。

`ListenableFuture`方法源码如下：

```java
public interface ListenableFuture<T> extends Future<T> {
    void addCallback(ListenableFutureCallback<? super T> var1);

    void addCallback(SuccessCallback<? super T> var1, FailureCallback var2);

    default CompletableFuture<T> completable() {
        CompletableFuture<T> completable = new DelegatingCompletableFuture(this);
        this.addCallback(completable::complete, completable::completeExceptionally);
        return completable;
    }
}
```

继续优化`sendMessage`方法

```java
    public void sendMessage(String topic, Object o) {

        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, o);
        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {

            @Override
            public void onSuccess(SendResult<String, Object> sendResult) {
                logger.info("生产者成功发送消息到" + topic + "-> " + sendResult.getProducerRecord().value().toString());
            }
            @Override
            public void onFailure(Throwable throwable) {
                logger.error("生产者发送消息：{} 失败，原因：{}", o.toString(), throwable.getMessage());
            }
        });
    }
```

使用lambda表达式再继续优化：

```java
    public void sendMessage(String topic, Object o) {

        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, o);
        future.addCallback(result -> logger.info("生产者成功发送消息到topic:{} partition:{}的消息", result.getRecordMetadata().topic(), result.getRecordMetadata().partition()),
                ex -> logger.error("生产者发送消失败，原因：{}", ex.getMessage()));
    }
```

再来简单研究一下 `send(String topic, @Nullable V data)` 方法。

我们使用`send(String topic, @Nullable V data)`方法的时候实际会new 一个`ProducerRecord`对象发送，

```java
	@Override
	public ListenableFuture<SendResult<K, V>> send(String topic, @Nullable V data) {
		ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, data);
		return doSend(producerRecord);
	}
```

`ProducerRecord`类中有多个构造方法:

```java
   public ProducerRecord(String topic, V value) {
        this(topic, null, null, null, value, null);
    }
    public ProducerRecord(String topic, Integer partition, Long timestamp, K key, V 
        ......
    }
```

如果我们想在发送的时候带上timestamp（时间戳）、key等信息的话，`sendMessage()`方法可以这样写：

```java
    public void sendMessage(String topic, Object o) {
      // 分区编号最好为 null，交给 kafka 自己去分配
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, null, System.currentTimeMillis(), String.valueOf(o.hashCode()), o);
      
        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(producerRecord);
        future.addCallback(result -> logger.info("生产者成功发送消息到topic:{} partition:{}的消息", result.getRecordMetadata().topic(), result.getRecordMetadata().partition()),
                ex -> logger.error("生产者发送消失败，原因：{}", ex.getMessage()));
    }
```

### Step5:创建消费消息的消费者

通过在方法上使用  `@KafkaListener` 注解监听消息，当有消息的时候就会通过 poll 下来消费。

```java
import cn.javaguide.springbootkafka01sendobjects.entity.Book;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class BookConsumerService {

    @Value("${kafka.topic.my-topic}")
    private String myTopic;
    @Value("${kafka.topic.my-topic2}")
    private String myTopic2;
    private final Logger logger = LoggerFactory.getLogger(BookProducerService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();


    @KafkaListener(topics = {"${kafka.topic.my-topic}"}, groupId = "group1")
    public void consumeMessage(ConsumerRecord<String, String> bookConsumerRecord) {
        try {
            Book book = objectMapper.readValue(bookConsumerRecord.value(), Book.class);
            logger.info("消费者消费topic:{} partition:{}的消息 -> {}", bookConsumerRecord.topic(), bookConsumerRecord.partition(), book.toString());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = {"${kafka.topic.my-topic2}"}, groupId = "group2")
    public void consumeMessage2(Book book) {
        logger.info("消费者消费{}的消息 -> {}", myTopic2, book.toString());
    }
}

```

### Step6:创建一个 Rest Controller

```java
import cn.javaguide.springbootkafka01sendobjects.entity.Book;
import cn.javaguide.springbootkafka01sendobjects.service.BookProducerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author shuang.kou
 */
@RestController
@RequestMapping(value = "/book")
public class BookController {
    @Value("${kafka.topic.my-topic}")
    String myTopic;
    @Value("${kafka.topic.my-topic2}")
    String myTopic2;
    private final BookProducerService producer;
    private AtomicLong atomicLong = new AtomicLong();

    BookController(BookProducerService producer) {
        this.producer = producer;
    }

    @PostMapping
    public void sendMessageToKafkaTopic(@RequestParam("name") String name) {
        this.producer.sendMessage(myTopic, new Book(atomicLong.addAndGet(1), name));
        this.producer.sendMessage(myTopic2, new Book(atomicLong.addAndGet(1), name));
    }
}
```

### Step7:测试

输入命令：

```shell
curl -X POST -F 'name=Java' http://localhost:9090/book
```

控制台打印出的效果如下：

![](https://my-blog-to-use.oss-cn-beijing.aliyuncs.com/2019-11/springboot-kafka-result.jpg)

**my-topic 有2个partition（分区） 当你尝试发送多条消息的时候，你会发现消息会被比较均匀地发送到每个 partion 中。**

### 推荐阅读

- [Spring-Kafka官方文档](https://docs.spring.io/spring-kafka/reference/html/#preface)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka#samples)
- [How to Work with Apache Kafka in Your Spring Boot Application](https://www.confluent.io/blog/apache-kafka-spring-boot-application/)
- [Spring Boot and Kafka – Practical Configuration Examples](https://thepracticaldeveloper.com/2018/11/24/spring-boot-kafka-config/)
- [Spring Kafka – JSON Serializer and Deserializer Example](https://memorynotfound.com/spring-kafka-json-serializer-deserializer-example/)
- [Spring Boot Kafka概览、配置及优雅地实现发布订阅](https://cloud.tencent.com/developer/article/1558924)