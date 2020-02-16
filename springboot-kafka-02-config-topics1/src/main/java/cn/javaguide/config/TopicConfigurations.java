package cn.javaguide.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = "kafka")
class TopicConfigurations {
    private List<TopicConfiguration> topics;

    public Optional<List<TopicConfiguration>> getTopics() {
        return Optional.of(topics);
    }

    static class TopicConfiguration {
        String name;
        Integer numPartitions = 3;
        Short replicationFactor = 1;

        NewTopic toNewTopic() {
            return new NewTopic(this.name, this.numPartitions, this.replicationFactor);
        }
    }
}
