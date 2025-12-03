package com.bluecone.app.tenant.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

/**
 * 基于 MyBatis-Plus 3.5.7 的代码生成器
 */
public class CodeGenerator {

    private static String scanner(String tip) {
        System.out.printf("请输入%s：", tip);
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNext()) {
            String ipt = scanner.next().trim();
            if (!ipt.isEmpty()) {
                return ipt;
            }
        }
        throw new IllegalArgumentException("请输入正确的" + tip + "！");
    }

    public static void main(String[] args) {
        String projectPath = "/Users/zhenpengmu/Desktop/code/project/bluecone-app/app-tenant";

        FastAutoGenerator.create(
                "jdbc:mysql://rm-bp1xld3504q1gtf6b9o.mysql.rds.aliyuncs.com:3306/app?useUnicode=true&useSSL=false&characterEncoding=utf8",
                "muzhenpeng",
                "MUzhiwei123")
            .globalConfig(builder -> builder
                .author("muzhenpeng")
                .outputDir(projectPath + "/src/main/java")
                .disableOpenDir())
            .packageConfig(builder -> builder
                .parent("com.bluecone.app.tenant.dao")
                .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper")))
            .templateConfig(builder -> builder
                .disable(TemplateType.CONTROLLER)
                .entity("/templates/entity.java.ftl"))
            .strategyConfig(builder -> {
                builder.addInclude(Arrays.asList(scanner("表名，多个用英文逗号分隔").split(",")));
                builder.controllerBuilder()
                    .enableHyphenStyle()
                    .enableRestStyle();
                builder.entityBuilder()
                    .naming(NamingStrategy.underline_to_camel)
                    .columnNaming(NamingStrategy.underline_to_camel)
                    .logicDeleteColumnName("is_delete")
                    .enableColumnConstant()
                    .versionColumnName("version")
                    .enableLombok()
                    .idType(IdType.AUTO)
                    ;
            })
            .templateEngine(new FreemarkerTemplateEngine())
            .execute();
    }

}
