package com.bluecone.app.platform.archkit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * 微信 SDK 隔离架构规则。
 * <p>
 * 确保只有 app-wechat 模块可以直接依赖 WxJava SDK，其他模块必须通过 facade 访问。
 * </p>
 * <p>
 * Phase 3 硬约束：
 * 1) 除 app-wechat 外，任何模块不得 import WxJava open/miniapp/pay SDK
 * 2) 除 app-wechat 外，任何入参 DTO 不允许出现 authorizerAppId/subAppid/subMchid
 * </p>
 */
public class WeChatSdkIsolationArchRule {

    /**
     * 规则：只有 app-wechat 模块可以依赖 WxJava SDK。
     * <p>
     * 禁止的包：
     * - me.chanjar.weixin.* (WxJava 微信 SDK)
     * - cn.binarywang.wx.* (WxJava 旧包名)
     * - com.github.binarywang.wxpay.* (WxJava 微信支付 SDK)
     * </p>
     */
    public static final ArchRule ONLY_WECHAT_MODULE_CAN_USE_WXJAVA_SDK = ArchRuleDefinition
            .noClasses()
            .that()
            .resideOutsideOfPackages("com.bluecone.app.wechat..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "me.chanjar.weixin..",
                    "cn.binarywang.wx..",
                    "com.github.binarywang.wxpay.."
            )
            .because("只有 app-wechat 模块可以直接依赖 WxJava SDK，其他模块必须通过 facade 访问");

    /**
     * 验证所有规则。
     *
     * @param classes 要检查的类集合
     */
    public static void checkAll(JavaClasses classes) {
        ONLY_WECHAT_MODULE_CAN_USE_WXJAVA_SDK.check(classes);
    }
}

