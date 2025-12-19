package com.bluecone.app.api.advice;

import com.bluecone.app.core.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ApiResponseAdvice 测试。
 * 验证自动包装逻辑：
 * 1. 普通 JSON 接口自动包装为 ApiResponse
 * 2. 回调接口不包装
 */
@WebMvcTest(controllers = {
        ApiResponseAdviceTest.TestController.class,
        ApiResponseAdviceTest.CallbackController.class
})
@Import(ApiResponseAdvice.class)
class ApiResponseAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 测试普通 JSON 接口自动包装为 ApiResponse。
     */
    @Test
    void shouldWrapNormalJsonResponseAutomatically() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/data"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(content, Map.class);

        // 验证包含 ApiResponse 必须字段
        assertThat(responseMap).containsKeys("code", "message", "data", "traceId", "timestamp");
        assertThat(responseMap.get("code")).isEqualTo("OK");
        assertThat(responseMap.get("message")).isEqualTo("success");
        
        // 验证原始数据在 data 字段中
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        assertThat(data).containsEntry("name", "test");
        assertThat(data).containsEntry("value", 123);
    }

    /**
     * 测试已经是 ApiResponse 的接口不重复包装。
     */
    @Test
    void shouldNotDoubleWrapApiResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/already-wrapped"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(content, Map.class);

        // 验证只有一层 ApiResponse
        assertThat(responseMap).containsKeys("code", "message", "data", "traceId", "timestamp");
        assertThat(responseMap.get("code")).isEqualTo("OK");
        
        // data 应该是原始字符串，不是嵌套的 ApiResponse
        assertThat(responseMap.get("data")).isEqualTo("already wrapped");
    }

    /**
     * 测试空返回值自动包装为 ApiResponse（无 data）。
     */
    @Test
    void shouldWrapVoidResponseAsApiResponseWithoutData() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/void"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(content, Map.class);

        assertThat(responseMap).containsKeys("code", "message", "data", "traceId", "timestamp");
        assertThat(responseMap.get("code")).isEqualTo("OK");
        assertThat(responseMap.get("data")).isNull();
    }

    /**
     * 测试回调接口不包装（通过路径判断）。
     */
    @Test
    void shouldNotWrapCallbackResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/payment/notify"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(content, Map.class);

        // 验证返回原始格式，不是 ApiResponse 格式
        assertThat(responseMap).containsKeys("code", "message");
        assertThat(responseMap).doesNotContainKey("traceId");
        assertThat(responseMap).doesNotContainKey("timestamp");
        assertThat(responseMap.get("code")).isEqualTo("SUCCESS");
    }

    /**
     * 测试 @NoApiResponseWrap 注解排除包装。
     */
    @Test
    void shouldNotWrapWhenAnnotatedWithNoWrap() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/no-wrap"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // 验证返回原始字符串
        assertThat(content).isEqualTo("raw response");
    }

    // ========== Test Controllers ==========

    /**
     * 测试用 Controller。
     */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/data")
        public Map<String, Object> getData() {
            return Map.of("name", "test", "value", 123);
        }

        @GetMapping("/already-wrapped")
        public ApiResponse<String> getAlreadyWrapped() {
            return ApiResponse.ok("already wrapped");
        }

        @GetMapping("/void")
        public void doNothing() {
            // 返回 void
        }

        @GetMapping("/no-wrap")
        @NoApiResponseWrap
        public String getNoWrap() {
            return "raw response";
        }
    }

    /**
     * 模拟回调 Controller（路径包含 /notify）。
     */
    @RestController
    @RequestMapping("/api/payment")
    static class CallbackController {

        @GetMapping("/notify")
        public Map<String, String> onNotify() {
            return Map.of("code", "SUCCESS", "message", "ok");
        }
    }
}
