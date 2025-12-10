package com.bluecone.app.infra.wechat;

import lombok.Data;

/**
 * 查询注册结果入参。
 *
 * 典型 fastregisterweapp.search 参数：
 * - name
 * - legal_persona_wechat
 * - legal_persona_name
 *
 * 也可以携带本地注册任务 ID，方便日志关联。
 */
@Data
public class WeChatRegisterStatusQuery {

    private String name;

    private String legalPersonaWechat;

    private String legalPersonaName;

    /**
     * 本地注册任务 ID（例如 bc_wechat_register_task.id），便于排查问题。
     */
    private Long localTaskId;
}

