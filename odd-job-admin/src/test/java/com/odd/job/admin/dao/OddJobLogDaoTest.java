package com.odd.job.admin.dao;
;
import com.odd.job.admin.core.model.OddJobLog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OddJobLogDaoTest {

    @Resource
    private OddJobLogDao oddJobLogDao;

    @Test
    public void test(){
        List<OddJobLog> list = oddJobLogDao.pageList(0, 10, 1, 1, null, null, 1);
        int list_count = oddJobLogDao.pageListCount(0, 10, 1, 1, null, null, 1);

        OddJobLog log = new OddJobLog();
        log.setJobGroup(1);
        log.setJobId(1);

        long ret1 = oddJobLogDao.save(log);
        OddJobLog dto = oddJobLogDao.load(log.getId());

        log.setTriggerTime(new Date());
        log.setTriggerCode(1);
        log.setTriggerMsg("1");
        log.setExecutorAddress("1");
        log.setExecutorHandler("1");
        log.setExecutorParam("1");
        ret1 = oddJobLogDao.updateTriggerInfo(log);
        dto = oddJobLogDao.load(log.getId());


        log.setHandleTime(new Date());
        log.setHandleCode(2);
        log.setHandleMsg("2");
        ret1 = oddJobLogDao.updateHandleInfo(log);
        dto = oddJobLogDao.load(log.getId());


        List<Long> ret4 = oddJobLogDao.findClearLogIds(1, 1, new Date(), 100, 100);

        int ret2 = oddJobLogDao.delete(log.getJobId());

    }

}
