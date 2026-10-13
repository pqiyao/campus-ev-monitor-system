package com.example.evmonitorplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the optional local DeepSeek Harness runtime.
 * When {@code managed} is true, the Spring Boot process will launch and
 * supervise
 * the DSH CLI via {@code npx @deepseek-ai/dsh web} rather than requiring an
 * external operator to start it. The HTTP RPC boundary remains unchanged.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.agent.dsh")
public class AgentDshProperties {
    private boolean enabled = true;
    /**
     * When true, Spring Boot launches {@code npx dsh web --host --port}
     * automatically.
     */
    private boolean managed = true;
    private String baseUrl = "http://127.0.0.1:8091";
    private String host = "127.0.0.1";
    private int port = 8091;
    /**
     * Override the {@code node} executable path; resolved from {@code PATH}
     * otherwise.
     */
    private String nodePath = "";
    /**
     * Override the {@code npx(.cmd)} executable path; resolved from {@code PATH}
     * otherwise.
     */
    private String npxPath = "";
    /** The npm package name fed to npx. */
    private String npmPackage = "@deepseek-ai/dsh";
    /**
     * Optional extra arguments after the profile host/port/no-open (e.g.
     * {@code "--trusted-host 0.0.0.0"}).
     */
    private String extraArgs = "";
    private String profile = "web";
    private String workspaceRoot = "";
    private String agentPreset = "xiaoye";
    private int timeoutMs = 5000;
    /**
     * Interval between HTTP health checks while waiting for DSH to become ready.
     */
    private int healthCheckMs = 1500;
    /** Maximum wait time before giving up on DSH startup. */
    private int maxStartupMs = 120_000;
    /** Absolute path for DSH stdout/stderr. Empty means {@code <temp>/dsh.log}. */
    private String logFile = "";
    private String promptVersion = "campus-spatiotemporal-agent-v1";
    /**
     * Override with DSH_SYSTEM_PROMPT when a deployment needs a local policy
     * variant.
     */
    private String systemPrompt = """
            你是校园电动车监测管理系统的时空治理智能体，服务对象是校园管理人员。
            你的任务是理解管理员目标，基于平台提供的摄像头、车辆定位、电子围栏、AI事件、事件融合、历史违规和巡检任务事实进行分析。
            你必须区分观测事实、融合证据、模型推断和人工待确认事项；数据缺失时明确说明，不得补造数据。
            你可以输出风险热点、时空关联、巡检优先级、复核清单和报告草稿，但不能直接处罚、发送通知、冻结账号、修改申诉状态、删除数据或声称已完成平台写操作。
            任何正式处置都必须回到校园平台的原有 AI 治理和人工审核流程。
            默认使用中文，先给结论，再给证据、置信度、风险、不确定性和下一步建议。
            """;
}
