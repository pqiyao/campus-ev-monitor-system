package com.example.evmonitorplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
public class AiKnowledgeInitializer implements ApplicationRunner {
    private final AiGovernanceSchema schema;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!schema.ensureTablesExist()) return;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM policy_document", Integer.class);
        if (count != null && count > 0) return;
        String title = "校园电动车安全治理规则（平台演示版）";
        String content = """
                第一条 驾乘校园电动车应正确佩戴安全头盔，保持车牌清晰，不得遮挡或污损。
                第二条 校园道路禁止违规载人、超速行驶；进入教学核心区等禁行区域须遵守围栏和现场标识。
                第三条 电动车必须在划定区域有序停放，不得占用消防通道、疏散通道和建筑出入口。
                第四条 违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。
                第五条 正式扣分、处罚、车辆冻结和申诉结论必须由管理员确认，AI仅提供研判建议。
                第六条 用户可在规定期限内对违规记录提出申诉，并提交图片、视频、轨迹或其他证明材料。
                第七条 对疑似盗抢、异常移动事件，应优先通知车主和安保人员，不直接作为交通违规扣分依据。
                """;
        jdbcTemplate.update("INSERT INTO policy_document (title, category, version, effective_date, content, source, status) VALUES (?, 'CAMPUS_RULE', 'DEMO-1.0', ?, ?, '平台内置演示规则', 'ACTIVE')",
                title, Date.valueOf(LocalDate.now()), content);
        Long documentId = jdbcTemplate.queryForObject("SELECT document_id FROM policy_document WHERE title=? ORDER BY document_id DESC LIMIT 1", Long.class, title);
        List<String[]> chunks = List.of(
                new String[]{"RULE-01", "头盔与车牌", "驾乘校园电动车应正确佩戴安全头盔，保持车牌清晰，不得遮挡或污损。", "头盔,车牌,遮挡"},
                new String[]{"RULE-02", "道路通行", "校园道路禁止违规载人、超速行驶；进入禁行区域须遵守电子围栏和现场标识。", "载人,超速,禁行,围栏"},
                new String[]{"RULE-03", "车辆停放", "车辆必须在划定区域有序停放，不得占用消防通道、疏散通道和建筑出入口。", "停车,违停,消防通道"},
                new String[]{"RULE-04", "证据与人工复核", "违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。", "证据,复核,身份,轨迹"},
                new String[]{"RULE-05", "处罚边界", "正式扣分、处罚、车辆冻结和申诉结论必须由管理员确认，AI仅提供研判建议。", "扣分,处罚,冻结,管理员"},
                new String[]{"RULE-06", "申诉权利", "用户可在规定期限内提出申诉，并提交图片、视频、轨迹或其他证明材料。", "申诉,材料,图片,视频"},
                new String[]{"RULE-07", "盗抢风险处置", "疑似盗抢、异常移动事件应优先通知车主和安保人员，不直接作为交通违规扣分依据。", "盗抢,异常移动,安保,通知"}
        );
        for (String[] chunk : chunks) {
            jdbcTemplate.update("INSERT INTO policy_chunk (document_id, section_code, heading, content, keywords) VALUES (?, ?, ?, ?, ?)",
                    documentId, chunk[0], chunk[1], chunk[2], chunk[3]);
        }
    }
}
