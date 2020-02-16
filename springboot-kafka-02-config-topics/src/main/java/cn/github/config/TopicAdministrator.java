package cn.github.config;

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
