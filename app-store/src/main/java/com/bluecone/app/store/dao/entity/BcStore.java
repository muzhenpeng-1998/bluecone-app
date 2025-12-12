package com.bluecone.app.store.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;
import com.bluecone.app.core.domain.IndustryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Data
public class BcStore implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String storeCode;

    private String name;

    private String shortName;

    private IndustryType industryType;

    private Long brandId;

    private String provinceCode;

    private String cityCode;

    private String districtCode;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String contactPhone;

    private String logoUrl;

    private String coverUrl;

    private String status;

    // 入驻状态：0-草稿，1-可营业（READY），2-关闭/停业（CLOSED）
    private Integer onboardStatus;

    // 门店级绑定的小程序 appid，当门店与租户默认小程序不一致时可单独覆盖使用。
    private String miniappAppid;

    private Boolean openForOrders;

    private Long configVersion;

    private String extJson;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    private Boolean isDeleted;

}
