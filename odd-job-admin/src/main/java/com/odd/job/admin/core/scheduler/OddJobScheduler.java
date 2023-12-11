package com.odd.job.admin.core.scheduler;

import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.core.enums.ExecutorBlockStrategyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author oddity
 * @create 2023-12-11 19:38
 */

//TODO
public class OddJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OddJobScheduler.class);

    public void init() throws Exception {
        // init i18n
        initI18n();
    }

    // ---------------------- I18n ----------------------

    private void initI18n(){
        for (ExecutorBlockStrategyEnum item :ExecutorBlockStrategyEnum.values()){
            item.setTitle(I18nUtil.get);
        }
    }
}
