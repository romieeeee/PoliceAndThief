package com.pnt.pnt_spring.global.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // 상수 정의 (GameResultServiceImpl 등에서 사용하는 이름과 일치해야 함)
    public static final String NEWS_REQUEST_QUEUE = "game.news.request";
    public static final String NEWS_EXCHANGE = "game.news.exchange";
    public static final String NEWS_ROUTING_KEY = "game.news.request";

    /**
     * 1. 큐 생성 (AI 서버가 구독할 큐)
     * durable = true : RabbitMQ 서버가 재시작되어도 큐가 유지됨
     */
    @Bean
    public Queue gameNewsRequestQueue() {
        return new Queue(NEWS_REQUEST_QUEUE, true);
    }

    /**
     * 2. Exchange 생성 (Direct Exchange)
     * 특정 Routing Key로 큐에 1:1 전달
     */
    @Bean
    public DirectExchange gameNewsExchange() {
        return new DirectExchange(NEWS_EXCHANGE);
    }

    /**
     * 3. 바인딩 (Exchange <-> Queue 연결)
     * "game.news.exchange"로 "game.news.request" 키를 달고 온 메시지를 큐에 넣음
     */
    @Bean
    public Binding bindingGameNews(Queue gameNewsRequestQueue, DirectExchange gameNewsExchange) {
        return BindingBuilder.bind(gameNewsRequestQueue)
                .to(gameNewsExchange)
                .with(NEWS_ROUTING_KEY);
    }

    /**
     * 4. JSON 메시지 컨버터 (필수)
     * Java 객체(AiNewsRequest)를 JSON 문자열로 자동 변환해 줌
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 5. RabbitTemplate 설정
     * 위에서 만든 JSON 컨버터를 템플릿에 적용
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}