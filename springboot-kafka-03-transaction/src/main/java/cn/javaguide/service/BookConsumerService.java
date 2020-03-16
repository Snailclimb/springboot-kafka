package cn.javaguide.service;

import cn.javaguide.entity.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class BookConsumerService {

    @Value("${kafka.topic.topic-test-transaction}")
    String topicTestTransaction;

    private final Logger logger = LoggerFactory.getLogger(BookProducerService.class);


    @KafkaListener(topics = {"${kafka.topic.topic-test-transaction}"}, id = "bookGroup")
    public void consumeMessage(Book book) {
        logger.info("消费者消费{}的消息 -> {}", topicTestTransaction, book.toString());
        throw new RuntimeException("dlt");
    }

    @KafkaListener(topics = {"${kafka.topic.topic-test-transaction}"}, id = "dltGroup")
    public void dltConsumeMessage(Book book) {
        logger.info("消费者消费{}的消息 -> {}", topicTestTransaction, book.toString());
    }
}
