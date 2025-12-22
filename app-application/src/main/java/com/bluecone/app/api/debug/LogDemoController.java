package com.bluecone.app.controller;

import com.bluecone.app.core.log.annotation.ApiLog;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "🛠️ 开发调试 > 其他调试接口", description = "日志系统测试接口")
@RestController
public class LogDemoController {

    @GetMapping("/log-demo")
    @ApiLog("日志系统示例")
    public Map<String, Object> demo() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "BlueCone 日志体系运行正常");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("phone", "13812345678");
        result.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
        return result;
    }
}
