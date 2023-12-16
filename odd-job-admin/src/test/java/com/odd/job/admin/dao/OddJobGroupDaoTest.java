package com.odd.job.admin.dao;

import com.odd.job.admin.core.model.OddJobGroup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OddJobGroupDaoTest {

    @Resource
    private OddJobGroupDao oddJobGroupDao;

    @Test
    public void test(){
        List<OddJobGroup> list = oddJobGroupDao.findAll();

        List<OddJobGroup> list2 = oddJobGroupDao.findByAddressType(0);

        OddJobGroup group = new OddJobGroup();
        group.setAppname("setAppName");
        group.setTitle("setTitle");
        group.setAddressType(0);
        group.setAddressList("setAddressList");
        group.setUpdateTime(new Date());

        int ret = oddJobGroupDao.save(group);

        OddJobGroup group2 = oddJobGroupDao.load(group.getId());
        group2.setAppname("setAppName2");
        group2.setTitle("setTitle2");
        group2.setAddressType(2);
        group2.setAddressList("setAddressList2");
        group2.setUpdateTime(new Date());

        int ret2 = oddJobGroupDao.update(group2);

        int ret3 = oddJobGroupDao.remove(group.getId());
    }

}
