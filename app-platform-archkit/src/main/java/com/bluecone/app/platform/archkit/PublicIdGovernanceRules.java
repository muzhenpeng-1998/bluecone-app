package com.bluecone.app.platform.archkit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * Public ID 治理架构规则，强制执行 API 层不暴露内部主键。
 * 
 * <p>核心约束：</p>
 * <ul>
 *   <li>Controller 方法参数：禁止使用 Long/long 类型的 id/storeId/productId 等（除非标记 @ResolvePublicId）</li>
 *   <li>DTO/View 响应字段：禁止暴露名为 id/storeId/productId 的 Long/long 字段</li>
 *   <li>Controller 层：禁止直接依赖 Mapper（保持分层）</li>
 * </ul>
 * 
 * <p>白名单：</p>
 * <ul>
 *   <li>内部管理接口（..admin..）可放宽限制</li>
 *   <li>平台侧接口（..platform..）可放宽限制</li>
 * </ul>
 */
public class PublicIdGovernanceRules {

    /**
     * Controller 方法参数禁止使用 Long 类型的 id/storeId/productId 等，必须使用 publicId（String）或标记 @ResolvePublicId。
     * 
     * <p>违规示例：</p>
     * <pre>
     * &#64;GetMapping("/stores/{id}")
     * public Result detail(&#64;PathVariable Long id) { ... }  // ❌ 禁止
     * </pre>
     * 
     * <p>正确示例：</p>
     * <pre>
     * &#64;GetMapping("/stores/{storeId}")
     * public Result detail(&#64;PathVariable &#64;ResolvePublicId(type=STORE) Long storePk) { ... }  // ✅ 允许
     * </pre>
     */
    public static final ArchRule CONTROLLER_PARAMS_NO_LONG_ID =
            methods()
                    .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
                    .and().areDeclaredInClassesThat().resideOutsideOfPackages("..admin..", "..platform..")
                    .and().arePublic()
                    .should(new ArchCondition<JavaMethod>("not have Long/long parameters named id/storeId/productId/skuId without @ResolvePublicId") {
                        @Override
                        public void check(JavaMethod method, ConditionEvents events) {
                            // Note: ArchUnit's JavaParameter doesn't expose parameter names directly
                            // This is a simplified check - for full validation, use runtime reflection or bytecode analysis
                            for (JavaParameter param : method.getParameters()) {
                                JavaClass paramType = param.getRawType();
                                
                                // 检查参数类型是否为 Long/long
                                boolean isLongType = paramType.isEquivalentTo(Long.class)
                                        || paramType.isEquivalentTo(long.class);
                                
                                // 检查是否标记了 @ResolvePublicId
                                boolean hasResolveAnnotation = param.getAnnotations().stream()
                                        .anyMatch(ann -> ann.getRawType().getSimpleName().equals("ResolvePublicId"));
                                
                                // 如果是 Long 类型但没有 @ResolvePublicId 注解，发出警告
                                if (isLongType && !hasResolveAnnotation) {
                                    String message = String.format(
                                            "Controller 方法 %s.%s 有 Long 类型参数未标记 @ResolvePublicId，可能违反 Public ID 治理规则。" +
                                            "请确认参数是否应该使用 String publicId 或标记 @ResolvePublicId 注解。",
                                            method.getOwner().getSimpleName(),
                                            method.getName()
                                    );
                                    events.add(SimpleConditionEvent.violated(method, message));
                                }
                            }
                        }
                    });

    /**
     * DTO/View 响应类禁止暴露名为 id 的 Long/long 字段，应使用 publicId（String）。
     * 
     * <p>违规示例：</p>
     * <pre>
     * public class StoreView {
     *     private Long id;  // ❌ 禁止
     *     private String name;
     * }
     * </pre>
     * 
     * <p>正确示例：</p>
     * <pre>
     * public class StoreView {
     *     private String storePublicId;  // ✅ 允许
     *     private String name;
     * }
     * </pre>
     */
    public static final ArchRule DTO_FIELDS_NO_LONG_ID =
            fields()
                    .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("View")
                    .or().areDeclaredInClassesThat().haveSimpleNameEndingWith("DTO")
                    .or().areDeclaredInClassesThat().haveSimpleNameEndingWith("Response")
                    .and().haveName("id")
                    .should(new ArchCondition<JavaField>("not be Long or long type") {
                        @Override
                        public void check(JavaField field, ConditionEvents events) {
                            JavaClass fieldType = field.getRawType();
                            boolean isLongType = fieldType.isEquivalentTo(Long.class)
                                    || fieldType.isEquivalentTo(long.class);
                            
                            if (isLongType) {
                                String message = String.format(
                                        "DTO/View 类 %s 的字段 'id' 类型为 Long，违反 Public ID 治理规则。" +
                                        "应使用 String 类型的 publicId 字段。",
                                        field.getOwner().getSimpleName()
                                );
                                events.add(SimpleConditionEvent.violated(field, message));
                            }
                        }
                    })
                    .because("DTO/View 响应类不应暴露 Long 类型的 id 字段，应使用 publicId（String）");

    /**
     * DTO/View 响应类禁止暴露名为 storeId/productId/skuId 的 Long/long 字段。
     * 
     * <p>说明：允许 tenantId 为 Long（内部标识，不对外暴露）</p>
     */
    public static final ArchRule DTO_FIELDS_NO_LONG_RESOURCE_ID =
            fields()
                    .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("View")
                    .or().areDeclaredInClassesThat().haveSimpleNameEndingWith("DTO")
                    .or().areDeclaredInClassesThat().haveSimpleNameEndingWith("Response")
                    .and().areDeclaredInClassesThat().resideOutsideOfPackages("..admin..", "..platform..")
                    .should(new ArchCondition<JavaField>("not be Long/long type with name storeId/productId/skuId/orderId/userId") {
                        @Override
                        public void check(JavaField field, ConditionEvents events) {
                            String fieldName = field.getName();
                            JavaClass fieldType = field.getRawType();
                            
                            // 检查字段名是否为资源 ID
                            boolean isResourceIdName = fieldName.equals("storeId")
                                    || fieldName.equals("productId")
                                    || fieldName.equals("skuId")
                                    || fieldName.equals("orderId")
                                    || fieldName.equals("userId");
                            
                            // 检查字段类型是否为 Long/long
                            boolean isLongType = fieldType.isEquivalentTo(Long.class)
                                    || fieldType.isEquivalentTo(long.class);
                            
                            if (isResourceIdName && isLongType) {
                                String message = String.format(
                                        "DTO/View 类 %s 的字段 '%s' 类型为 Long，违反 Public ID 治理规则。" +
                                        "请使用 String 类型的 publicId 字段（如 storePublicId）。",
                                        field.getOwner().getSimpleName(),
                                        fieldName
                                );
                                events.add(SimpleConditionEvent.violated(field, message));
                            }
                        }
                    })
                    .because("DTO/View 响应类不应暴露 Long 类型的资源 ID 字段，应使用 publicId（String）");

    /**
     * Controller 层禁止直接依赖 Mapper（保持分层）。
     * 
     * <p>说明：Controller 应通过 Facade/ApplicationService 访问数据，不应直接注入 Mapper。</p>
     */
    public static final ArchRule CONTROLLER_NO_DIRECT_MAPPER =
            ContextRules.CONTROLLERS_NO_DIRECT_MAPPER_ACCESS;

    /**
     * 检查所有 Public ID 治理规则。
     * 
     * @param classes 待检查的类集合
     */
    public static void checkAll(JavaClasses classes) {
        CONTROLLER_PARAMS_NO_LONG_ID.check(classes);
        DTO_FIELDS_NO_LONG_ID.check(classes);
        DTO_FIELDS_NO_LONG_RESOURCE_ID.check(classes);
        CONTROLLER_NO_DIRECT_MAPPER.check(classes);
    }

    /**
     * 检查 Controller 参数规则（轻量级，适用于快速验证）。
     * 
     * @param classes 待检查的类集合
     */
    public static void checkControllerParams(JavaClasses classes) {
        CONTROLLER_PARAMS_NO_LONG_ID.check(classes);
    }

    /**
     * 检查 DTO 字段规则（轻量级，适用于快速验证）。
     * 
     * @param classes 待检查的类集合
     */
    public static void checkDtoFields(JavaClasses classes) {
        DTO_FIELDS_NO_LONG_ID.check(classes);
        DTO_FIELDS_NO_LONG_RESOURCE_ID.check(classes);
    }
}

