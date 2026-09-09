package com.example.tj_project_apicommon.Utils;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*
 * SpEL 表达式解析工具类。
 * 职责：将模板字符串中的 #{expression} 占位符，使用方法参数值动态替换。
 * 使用：SPELUtils.parse("user:#{user.id}", new String[]{"user"}, new Object[]{user})。
 * 注意：占位符必须为 #{expression} 格式，expression 为合法的 SpEL 表达式（如 user.id）。
 *       若模板无占位符或参数不匹配，返回原模板（或null，视场景而定）。
 */
public class SPELUtils {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(#\\{([^}]*)\\})");
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private SPELUtils() {
        // 工具类私有构造
    }

    /**
     * 解析模板，将 #{expression} 替换为参数实际值。
     *
     * @param template    模板字符串（含 #{...} 占位符）
     * @param paramNames  方法参数名称列表（顺序与方法参数对应）
     * @param args        方法参数值列表（顺序与 paramNames 对应）
     * @return 替换后的字符串；若模板为 null 或空，返回 null；若无占位符，返回原模板
     */
    public static String parse(String template, String[] paramNames, Object[] args) {
        if (StringUtils.isBlank(template)) {
            return template;
        }

        // 快速检查是否包含占位符
        if (!template.contains("#{")) {
            return template;
        }

        var matcher = PLACEHOLDER_PATTERN.matcher(template);
        var placeholders = new ArrayList<String>();
        while (matcher.find()) {
            placeholders.add(matcher.group(1)); // 完整占位符 #{xxx}
        }

        if (placeholders.isEmpty()) {
            return template;
        }

        // 构建 SpEL 上下文
        var context = new StandardEvaluationContext();
        if (paramNames != null && args != null && paramNames.length == args.length) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 逐一替换
        var result = template;
        for (var placeholder : placeholders) {
            // 提取 #{ 和 } 之间的表达式
            var expr = placeholder.substring(2, placeholder.length() - 1);
            try {
                var value = PARSER.parseExpression(expr).getValue(context, String.class);
                // 若值为 null，可选择保留原占位符或替换为空字符串（这里替换为空字符串）
                var replacement = (value == null) ? "" : value;
                result = result.replace(placeholder, replacement);
            } catch (Exception e) {
                // 解析失败时保留占位符（或可根据业务需要抛出异常）
                // 这里选择保留原占位符以便排查
                // 也可以替换为 "ERROR" 或直接抛出
            }
        }
        return result;
    }

    // 测试用（可删除）
    static void main(String[] args) {
        var user = new User();
        var result = parse("tj:#{user.id}", new String[]{"user"}, new Object[]{user});
        System.out.println(result); // 输出 tj:1
    }

    @lombok.Data
    public static class User {
        private Long id = 1L;
    }
}