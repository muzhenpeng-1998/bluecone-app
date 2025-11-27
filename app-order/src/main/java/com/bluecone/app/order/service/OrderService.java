package com.bluecone.app.order.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service("orderHelloService")
public class OrderService {

    public Map<String, Object> hello() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "hello, order");
        result.put("at", Instant.now().toString());
        return result;
    }
}
