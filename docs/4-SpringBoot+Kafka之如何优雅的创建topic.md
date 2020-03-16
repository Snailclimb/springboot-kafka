在我们之前的代码中，我们是通过注入 `NewTopic` 类型的对象来创建 Kafka 的 topic 的。当我们的项目需要创建的 topic 逐渐变多的话，通过这种方式创建就不是那么友好了，我觉得主要带来的问题有两个：

1. Topic 信息不清晰；
2. 代码量变的庞大；

```java
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
```

今天说一下我对于这个问题的解决办法：

在 `application.xml` 配置文件中配置 Kafka 连接信息以及我们项目中用到的 topic。

```yaml
server:
  port: 9090
spring:
  kafka:
    bootstrap-servers: localhost:9092
kafka:
  topics:
    - name: topic1
      num-partitions: 3
      replication-factor: 1
    - name: topic2
      num-partitions: 1
      replication-factor: 1
    - name: topic3
      num-partitions: 2
      replication-factor: 1
```

`TopicConfigurations` 类专门用来读取我们的 topic 配置信息：

```java

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "kafka")
@Setter
@Getter
@ToString
class TopicConfigurations {
    private List<Topic> topics;

    @Setter
    @Getter
    @ToString
    static class Topic {
        String name;
        Integer numPartitions = 3;
        Short replicationFactor = 1;

        NewTopic toNewTopic() {
            return new NewTopic(this.name, this.numPartitions, this.replicationFactor);
        }

    }
}

```

在 `TopicAdministrator` 类中我们手动将 topic 对象注册到容器中。

```java

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.GenericWebApplicationContext;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author shuang.kou
 */
@Configuration
public class TopicAdministrator {
    private final TopicConfigurations configurations;
    private final GenericWebApplicationContext context;

    public TopicAdministrator(TopicConfigurations configurations, GenericWebApplicationContext genericContext) {
        this.configurations = configurations;
        this.context = genericContext;
    }

    @PostConstruct
    public void init() {
        initializeBeans(configurations.getTopics());
    }

    private void initializeBeans(List<TopicConfigurations.Topic> topics) {
        topics.forEach(t -> context.registerBean(t.name, NewTopic.class, t::toNewTopic));
    }


}

```

这样的话，当我们运行项目之后，就会自动创建 3 个名为：topic1、topic2 和 topic3 的主题了。