package cn.javaguide.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.GenericWebApplicationContext;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
public class TopicAdministrator {
    private final TopicConfigurations configurations;
    private final GenericWebApplicationContext context;

    public TopicAdministrator(TopicConfigurations configurations,	GenericWebApplicationContext genericContext) {
        this.configurations = configurations;
        this.context = genericContext;
    }

    @PostConstruct
    public void createTopics() {
        configurations
                .getTopics()
                .ifPresent(this::initializeBeans);
    }

    private void initializeBeans(List<TopicConfigurations.TopicConfiguration> topics) {
        topics.forEach(t -> {
            context.registerBean(t.name, NewTopic.class, t::toNewTopic);
        });
    }
}
