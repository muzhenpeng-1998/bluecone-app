package com.bluecone.app.product.dto.view.unified;

/**
 * 选项组类型枚举，用于统一渲染模型。
 * <p>
 * 将商品的规格组（SPEC）、属性组（ATTR）、小料组（ADDON）统一为 OptionGroup 视图，
 * 便于 Admin 回显编辑与 C 端菜单快照渲染。
 */
public enum OptionGroupKind {

    /**
     * 规格组（Spec Group）。
     * <p>
     * 用于定义商品的规格维度（如"容量""温度"），用于 SKU 组合与下单选择。
     */
    SPEC,

    /**
     * 属性组（Attribute Group）。
     * <p>
     * 用于定义商品的可复用属性（如"口味""做法"），通常不影响 SKU，但可能影响价格。
     */
    ATTR,

    /**
     * 小料组（Addon Group）。
     * <p>
     * 用于定义商品的附加小料（如"加料""配料"），通常有独立计价规则。
     */
    ADDON
}

