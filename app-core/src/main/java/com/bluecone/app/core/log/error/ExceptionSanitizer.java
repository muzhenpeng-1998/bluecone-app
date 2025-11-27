package com.bluecone.app.core.log.error;

import com.bluecone.app.core.log.util.LogMaskUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 异常事件脱敏与裁剪，避免敏感信息泄露和日志膨胀。
 */
@Component
public class ExceptionSanitizer {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_ROOT_CAUSE_LENGTH = 500;
    private static final int MAX_STACK_LINE_LENGTH = 512;
    private static final int MAX_STACK_LINES = 10;
    private static final int MAX_REQUEST_PARAM_LENGTH = 2000;

    public ExceptionEvent sanitize(ExceptionEvent event) {
        if (event == null) {
            return null;
        }
        event.setMessage(trim(LogMaskUtil.mask(event.getMessage()), MAX_MESSAGE_LENGTH));
        event.setRootCause(trim(LogMaskUtil.mask(event.getRootCause()), MAX_ROOT_CAUSE_LENGTH));
        event.setRequestParams(trim(LogMaskUtil.mask(event.getRequestParams()), MAX_REQUEST_PARAM_LENGTH));
        if (StringUtils.hasText(event.getRequestBodyDigest())) {
            event.setRequestBodyDigest(trim(event.getRequestBodyDigest(), 256));
        }

        List<String> stackTop = event.getStackTop();
        if (!CollectionUtils.isEmpty(stackTop)) {
            List<String> sanitized = stackTop.stream()
                    .limit(MAX_STACK_LINES)
                    .map(LogMaskUtil::mask)
                    .map(line -> trim(line, MAX_STACK_LINE_LENGTH))
                    .collect(Collectors.toList());
            event.setStackTop(sanitized);
        }
        return event;
    }

    private String trim(String source, int maxLength) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        return source.length() > maxLength ? source.substring(0, maxLength) + "...[truncated]" : source;
    }
}
