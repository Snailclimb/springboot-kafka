package cn.javaguide.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shuang.kou
 */
@Service
public class BookProducerService {

    private List<Object> sendedBooks = new ArrayList<>();

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BookProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(String topic, Object o) {
        // 发送消息
        kafkaTemplate.send(topic, o);
        // 模拟发生异常
        int a = 1 / 0;
        // 模拟业务操作
        sendedBooks.add(o);
    }
}
