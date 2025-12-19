package com.bluecone.app.store.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.store.api.dto.StoreStaffView;
import com.bluecone.app.store.application.command.AddStoreStaffCommand;
import com.bluecone.app.store.application.command.BatchBindStoreStaffCommand;
import com.bluecone.app.store.application.command.ChangeStoreStaffRoleCommand;
import com.bluecone.app.store.application.command.RemoveStoreStaffCommand;
import com.bluecone.app.store.application.query.StoreStaffListQuery;
import com.bluecone.app.store.dao.entity.BcStoreStaff;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.dao.service.IBcStoreStaffService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 门店员工应用服务。
 * <p>职责：维护门店与用户的员工关系，确保多租户隔离与逻辑删除。</p>
 * <p>扩展点：后续可接入权限校验、角色变更事件等。</p>
 */
@Service
public class StoreStaffAppService {

    private final IBcStoreStaffService bcStoreStaffService;
    private final StoreStaffAssembler storeStaffAssembler;

    public StoreStaffAppService(IBcStoreStaffService bcStoreStaffService,
                                StoreStaffAssembler storeStaffAssembler) {
        this.bcStoreStaffService = bcStoreStaffService;
        this.storeStaffAssembler = storeStaffAssembler;
    }

    public List<StoreStaffView> list(StoreStaffListQuery query) {
        LambdaQueryWrapper<BcStoreStaff> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcStoreStaff::getTenantId, query.getTenantId())
                .eq(BcStoreStaff::getStoreId, query.getStoreId())
                .eq(BcStoreStaff::getIsDeleted, false);
        if (query.getUserId() != null) {
            wrapper.eq(BcStoreStaff::getUserId, query.getUserId());
        }
        if (query.getRole() != null) {
            wrapper.eq(BcStoreStaff::getRole, query.getRole());
        }
        return bcStoreStaffService.list(wrapper).stream()
                .map(storeStaffAssembler::toView)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void addStaff(AddStoreStaffCommand command) {
        long existing = bcStoreStaffService.lambdaQuery()
                .eq(BcStoreStaff::getTenantId, command.getTenantId())
                .eq(BcStoreStaff::getStoreId, command.getStoreId())
                .eq(BcStoreStaff::getUserId, command.getUserId())
                .eq(BcStoreStaff::getIsDeleted, false)
                .count();
        if (existing > 0) {
            throw new BusinessException(StoreErrorCode.STAFF_ALREADY_EXISTS);
        }
        BcStoreStaff entity = new BcStoreStaff();
        entity.setTenantId(command.getTenantId());
        entity.setStoreId(command.getStoreId());
        entity.setUserId(command.getUserId());
        entity.setRole(command.getRole());
        entity.setIsDeleted(false);
        bcStoreStaffService.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeStaff(RemoveStoreStaffCommand command) {
        boolean updated = bcStoreStaffService.lambdaUpdate()
                .eq(BcStoreStaff::getTenantId, command.getTenantId())
                .eq(BcStoreStaff::getStoreId, command.getStoreId())
                .eq(BcStoreStaff::getUserId, command.getUserId())
                .set(BcStoreStaff::getIsDeleted, true)
                .update();
        if (!updated) {
            throw new BusinessException(StoreErrorCode.STAFF_NOT_FOUND, "移除门店员工失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeRole(ChangeStoreStaffRoleCommand command) {
        boolean updated = bcStoreStaffService.lambdaUpdate()
                .eq(BcStoreStaff::getTenantId, command.getTenantId())
                .eq(BcStoreStaff::getStoreId, command.getStoreId())
                .eq(BcStoreStaff::getUserId, command.getUserId())
                .eq(BcStoreStaff::getIsDeleted, false)
                .set(BcStoreStaff::getRole, command.getNewRole())
                .update();
        if (!updated) {
            throw new BusinessException(StoreErrorCode.STAFF_NOT_FOUND, "调整员工角色失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchBindStaff(BatchBindStoreStaffCommand command) {
        if (command.getStaffList() == null || command.getStaffList().isEmpty()) {
            return;
        }
        for (BatchBindStoreStaffCommand.StaffItem item : command.getStaffList()) {
            long exists = bcStoreStaffService.lambdaQuery()
                    .eq(BcStoreStaff::getTenantId, command.getTenantId())
                    .eq(BcStoreStaff::getStoreId, command.getStoreId())
                    .eq(BcStoreStaff::getUserId, item.getUserId())
                    .eq(BcStoreStaff::getIsDeleted, false)
                    .count();
            if (exists > 0) {
                bcStoreStaffService.lambdaUpdate()
                        .eq(BcStoreStaff::getTenantId, command.getTenantId())
                        .eq(BcStoreStaff::getStoreId, command.getStoreId())
                        .eq(BcStoreStaff::getUserId, item.getUserId())
                        .eq(BcStoreStaff::getIsDeleted, false)
                        .set(BcStoreStaff::getRole, item.getRole())
                        .update();
            } else {
                BcStoreStaff entity = new BcStoreStaff();
                entity.setTenantId(command.getTenantId());
                entity.setStoreId(command.getStoreId());
                entity.setUserId(item.getUserId());
                entity.setRole(item.getRole());
                entity.setIsDeleted(false);
                bcStoreStaffService.save(entity);
            }
        }
    }
}
