package com.bluecone.app.store.api.impl;

import com.bluecone.app.store.api.StorePrintRuleFacade;
import com.bluecone.app.store.api.dto.StorePrintRuleView;
import com.bluecone.app.store.application.command.ChangeStorePrintRuleStatusCommand;
import com.bluecone.app.store.application.command.UpsertStorePrintRulesCommand;
import com.bluecone.app.store.application.query.StorePrintRuleListQuery;
import com.bluecone.app.store.application.service.StorePrintRuleAppService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 打印规则 Facade 实现。
 * <p>职责：承接外部调用并委派应用服务，保持接口简洁。</p>
 */
@Service
public class StorePrintRuleFacadeImpl implements StorePrintRuleFacade {

    private final StorePrintRuleAppService appService;

    public StorePrintRuleFacadeImpl(StorePrintRuleAppService appService) {
        this.appService = appService;
    }

    @Override
    public List<StorePrintRuleView> list(StorePrintRuleListQuery query) {
        return appService.list(query);
    }

    @Override
    public void upsertRules(UpsertStorePrintRulesCommand command) {
        appService.upsertRules(command);
    }

    @Override
    public void changeStatus(ChangeStorePrintRuleStatusCommand command) {
        appService.changeStatus(command);
    }
}
