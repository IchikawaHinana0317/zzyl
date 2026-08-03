package com.zzyl.nursing.task;

import com.zzyl.nursing.service.IAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlertTask {

    @Autowired
    private IAlertRuleService alertRuleService;

    public void deviceDataAlertFilter() {
        alertRuleService.alertFilter();
    }
}