package com.bluecone.app.controller;

import com.bluecone.app.core.log.annotation.ApiLog;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
public class ExceptionDemoController {

    @GetMapping("/biz-ex")
    @ApiLog("业务异常验证")
    public Map<String, Object> businessException() {
        throw BusinessException.of(
                ErrorCode.STOCK_NOT_ENOUGH.getCode(),
                ErrorCode.STOCK_NOT_ENOUGH.getMessage()
        );
    }

    @GetMapping("/sys-ex")
    @ApiLog("系统异常验证")
    public Map<String, Object> systemException() {
        Integer x = null;
        x.toString();
        return new HashMap<>();
    }
}
