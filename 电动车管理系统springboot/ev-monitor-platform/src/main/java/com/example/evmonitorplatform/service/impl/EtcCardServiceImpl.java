package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.ETCard;
import com.example.evmonitorplatform.mapper.ETCardMapper;
import com.example.evmonitorplatform.service.EtcCardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class EtcCardServiceImpl extends ServiceImpl<ETCardMapper, ETCard> implements EtcCardService {

    /**
     * 库表 {@code etc_card.vehicle_id} 为 NOT NULL + UNIQUE：不能将 vehicle_id 置空，只能删掉原车绑定的行再绑新卡，
     * 或将已存在的卡号行 update 到目标车辆（卡从旧车挪到新车）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindEtcToVehicle(String etcNumber, Integer vehicleId) {
        if (vehicleId == null) {
            throw new BusinessException("车辆ID不能为空");
        }
        if (!StringUtils.hasText(etcNumber)) {
            throw new BusinessException("RFID/ETC 卡号不能为空");
        }
        String num = etcNumber.trim();

        remove(new LambdaQueryWrapper<ETCard>().eq(ETCard::getVehicleId, vehicleId));

        ETCard byNumber = lambdaQuery().eq(ETCard::getEtcNumber, num).one();
        if (byNumber != null) {
            byNumber.setVehicleId(vehicleId);
            if (byNumber.getBalance() == null) {
                byNumber.setBalance(BigDecimal.ZERO);
            }
            if (!StringUtils.hasText(byNumber.getStatus())) {
                byNumber.setStatus("normal");
            }
            if (byNumber.getIssueDate() == null) {
                byNumber.setIssueDate(LocalDate.now());
            }
            updateById(byNumber);
        } else {
            ETCard card = new ETCard();
            card.setEtcNumber(num);
            card.setVehicleId(vehicleId);
            card.setBalance(BigDecimal.ZERO);
            card.setStatus("normal");
            card.setIssueDate(LocalDate.now());
            save(card);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueToVehicle(Integer vehicleId, String etcNumber, LocalDate expireDate) {
        bindEtcToVehicle(etcNumber, vehicleId);
        if (expireDate == null) {
            return;
        }
        ETCard card = lambdaQuery().eq(ETCard::getEtcNumber, etcNumber.trim()).one();
        if (card != null) {
            card.setExpireDate(expireDate);
            updateById(card);
        }
    }

    @Override
    public PageResponse<List<ETCard>> listCards(String keyword, String status, String userType,
                                                Integer pageNum, Integer pageSize) {
        Page<ETCard> page = new Page<>(pageNum, pageSize);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String st = StringUtils.hasText(status) ? status.trim() : null;
        String ut = StringUtils.hasText(userType) ? userType.trim() : null;
        Page<ETCard> result = (Page<ETCard>) baseMapper.selectCardPage(page, kw, st, ut);
        return new PageResponse<>(200, "success", result.getRecords(),
                (int) result.getCurrent(), (int) result.getSize(), result.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCardStatus(Integer etcId, String status) {
        if (etcId == null || !StringUtils.hasText(status)) {
            throw new BusinessException("参数不完整");
        }
        String s = status.trim();
        if (!Set.of("normal", "lost", "disabled").contains(s)) {
            throw new BusinessException("无效的状态值");
        }
        ETCard card = getById(etcId);
        if (card == null) {
            throw new BusinessException("卡不存在");
        }
        card.setStatus(s);
        updateById(card);
    }

    @Override
    public Map<String, Object> cardStatusStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("total", count());
        m.put("normal", lambdaQuery().eq(ETCard::getStatus, "normal").count());
        m.put("lost", lambdaQuery().eq(ETCard::getStatus, "lost").count());
        m.put("disabled", lambdaQuery().eq(ETCard::getStatus, "disabled").count());
        m.put("unbound", 0L);
        return m;
    }
}
