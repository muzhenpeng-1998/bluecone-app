package com.bluecone.app.store.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.store.api.dto.StorePrintRuleView;
import com.bluecone.app.store.application.command.ChangeStorePrintRuleStatusCommand;
import com.bluecone.app.store.application.command.UpsertStorePrintRulesCommand;
import com.bluecone.app.store.application.query.StorePrintRuleListQuery;
import com.bluecone.app.store.dao.entity.BcStorePrintRule;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.dao.service.IBcStorePrintRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 打印规则应用服务。
 * <p>职责：提供规则的查询与简单配置，后续可扩展校验/路由/事件。</p>
 */
@Service
public class StorePrintRuleAppService {

    private final IBcStorePrintRuleService bcStorePrintRuleService;
    private final StorePrintRuleAssembler storePrintRuleAssembler;

    public StorePrintRuleAppService(IBcStorePrintRuleService bcStorePrintRuleService,
                                    StorePrintRuleAssembler storePrintRuleAssembler) {
        this.bcStorePrintRuleService = bcStorePrintRuleService;
        this.storePrintRuleAssembler = storePrintRuleAssembler;
    }

    public List<StorePrintRuleView> list(StorePrintRuleListQuery query) {
        LambdaQueryWrapper<BcStorePrintRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcStorePrintRule::getTenantId, query.getTenantId())
                .eq(BcStorePrintRule::getStoreId, query.getStoreId())
                .eq(BcStorePrintRule::getIsDeleted, false);
        if (query.getEventType() != null) {
            wrapper.eq(BcStorePrintRule::getEventType, query.getEventType());
        }
        // status 字段实体中暂无，预留扩展
        return bcStorePrintRuleService.list(wrapper).stream()
                .map(storePrintRuleAssembler::toView)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void upsertRules(UpsertStorePrintRulesCommand command) {
        // 简化策略：先全部逻辑删除，再按入参插入/更新
        bcStorePrintRuleService.lambdaUpdate()
                .eq(BcStorePrintRule::getTenantId, command.getTenantId())
                .eq(BcStorePrintRule::getStoreId, command.getStoreId())
                .set(BcStorePrintRule::getIsDeleted, true)
                .update();

        if (command.getRules() == null || command.getRules().isEmpty()) {
            return;
        }

        List<BcStorePrintRule> toSave = new ArrayList<>();
        for (UpsertStorePrintRulesCommand.PrintRuleItem item : command.getRules()) {
            if (item.getRuleId() != null) {
                // 尝试更新已有记录
                boolean updated = bcStorePrintRuleService.lambdaUpdate()
                        .eq(BcStorePrintRule::getTenantId, command.getTenantId())
                        .eq(BcStorePrintRule::getStoreId, command.getStoreId())
                        .eq(BcStorePrintRule::getId, item.getRuleId())
                        .set(BcStorePrintRule::getEventType, item.getEventType())
                        .set(BcStorePrintRule::getTargetDeviceId, item.getTargetDeviceId())
                        .set(BcStorePrintRule::getTemplateCode, item.getTemplateCode())
                        .set(BcStorePrintRule::getConfigJson, item.getConfigJson())
                        .set(BcStorePrintRule::getIsDeleted, false)
                        .update();
                if (updated) {
                    continue;
                }
            }
            BcStorePrintRule entity = new BcStorePrintRule();
            entity.setTenantId(command.getTenantId());
            entity.setStoreId(command.getStoreId());
            entity.setEventType(item.getEventType());
            entity.setTargetDeviceId(item.getTargetDeviceId());
            entity.setTemplateCode(item.getTemplateCode());
            entity.setConfigJson(item.getConfigJson());
            entity.setIsDeleted(false);
            toSave.add(entity);
        }
        if (!toSave.isEmpty()) {
            bcStorePrintRuleService.saveBatch(toSave);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(ChangeStorePrintRuleStatusCommand command) {
        // 实体未包含 status 字段，暂用 configJson 里状态或直接逻辑删除
        boolean updated = bcStorePrintRuleService.lambdaUpdate()
                .eq(BcStorePrintRule::getTenantId, command.getTenantId())
                .eq(BcStorePrintRule::getStoreId, command.getStoreId())
                .eq(BcStorePrintRule::getId, command.getRuleId())
                .set(BcStorePrintRule::getIsDeleted, "DISABLED".equalsIgnoreCase(command.getTargetStatus()))
                .update();
        if (!updated) {
            throw new BusinessException(StoreErrorCode.PRINT_RULE_NOT_FOUND, "更新打印规则状态失败");
        }
    }
}
