package com.bluecone.app.store.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对应 bc_store_staff 的领域模型，描述门店员工及角色。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStaffModel {

    private Long userId;

    private String role;
}
