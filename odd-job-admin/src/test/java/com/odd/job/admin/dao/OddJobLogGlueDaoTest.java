package com.odd.job.admin.dao;

import com.odd.job.admin.core.model.OddJobLogGlue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OddJobLogGlueDaoTest {

    @Resource
    private OddJobLogGlueDao oddJobLogGlueDao;

    @Test
    public void test(){
        OddJobLogGlue logGlue = new OddJobLogGlue();
        logGlue.setJobId(1);
        logGlue.setGlueType("1");
        logGlue.setGlueSource("1");
        logGlue.setGlueRemark("1");

        logGlue.setAddTime(new Date());
        logGlue.setUpdateTime(new Date());
        int ret = oddJobLogGlueDao.save(logGlue);

        List<OddJobLogGlue> list = oddJobLogGlueDao.findByJobId(1);

        int ret2 = oddJobLogGlueDao.removeOld(1, 1);

        int ret3 =oddJobLogGlueDao.deleteByJobId(1);
    }

}
