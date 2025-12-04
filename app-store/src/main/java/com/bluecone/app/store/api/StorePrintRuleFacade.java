package com.bluecone.app.store.api;

import com.bluecone.app.store.api.dto.StorePrintRuleView;
import com.bluecone.app.store.application.command.ChangeStorePrintRuleStatusCommand;
import com.bluecone.app.store.application.command.UpsertStorePrintRulesCommand;
import com.bluecone.app.store.application.query.StorePrintRuleListQuery;

import java.util.List;

/**
 * 门店打印规则 Facade（事件 → 设备 → 模板）。
 * <p>职责：对外暴露打印规则的查询与配置接口。</p>
 * <p>高隔离：外部仅依赖接口与 DTO，隐藏领域实现。</p>
 */
public interface StorePrintRuleFacade {

    List<StorePrintRuleView> list(StorePrintRuleListQuery query);

    /**
     * 批量新增/更新打印规则。
     * <p>策略：前端传递全量规则，由后端做 upsert。</p>
     */
    void upsertRules(UpsertStorePrintRulesCommand command);

    void changeStatus(ChangeStorePrintRuleStatusCommand command);
}
