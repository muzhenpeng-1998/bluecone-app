package com.bluecone.app.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TenantPlatformBindRequest {

    @NotBlank
    private String platformType;

    @NotBlank
    private String platformAccountId;

    private String accountName;

    private String credential;

    @NotNull
    private Byte status;

    private LocalDateTime expireAt;

    private Long operatorId;
}
