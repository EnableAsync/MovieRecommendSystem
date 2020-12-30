package com.atguigu.business.service;

import com.atguigu.business.utils.KafkaStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {
    @Autowired
    private KafkaStream kafkaStream;

    public void sendMessage(String msg) {
        kafkaStream.sendMessage(msg);
    }
}
