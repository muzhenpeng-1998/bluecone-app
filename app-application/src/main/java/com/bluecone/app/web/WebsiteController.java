package com.bluecone.app.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 官网路由控制器：
 * - 根路径 "/" 访问时跳转到静态官网首页 /website/index.html
 * - 仅负责页面跳转，不包含任何业务逻辑。
 */
@Controller
public class WebsiteController {

    /**
     * 根路径访问时，直接重定向到官网首页静态页面。
     * 这样在微信开放平台审核时，可以直接填写域名根路径作为官网地址。
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/website/index.html";
    }
}

