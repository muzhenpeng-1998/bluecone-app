package ${package.Entity};

<#list table.importPackages as pkg>
import ${pkg};
</#list>
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/**
 * ${table.comment!}
 *
 * @author ${author}
 * @since ${date}
 */
@Data
@Schema(name = "${entity}", description = "${table.comment!''}")
public class ${entity} implements Serializable {
    private static final long serialVersionUID = 1L;

<#list table.fields as field>
    <#if field.comment!?length gt 0>
    @Schema(description = "${field.comment}")
    </#if>
    <#if field.keyFlag>
    @TableId(value = "${field.name}", type = IdType.${field.idType?default("AUTO")})
    </#if>
    private ${field.propertyType} ${field.propertyName};

</#list>
}
