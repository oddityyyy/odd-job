package com.odd.job.admin.dao;

import com.odd.job.admin.core.model.OddJobRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OddJobRegistryDaoTest {

    @Resource
    private OddJobRegistryDao oddJobRegistryDao;

    @Test
    public void test(){
        int ret = oddJobRegistryDao.registryUpdate("g1", "k1", "v1", new Date());
        if (ret < 1) {
            ret = oddJobRegistryDao.registrySave("g1", "k1", "v1", new Date());
        }

        List<OddJobRegistry> list = oddJobRegistryDao.findAll(1, new Date());

        int ret2 = oddJobRegistryDao.removeDead(Arrays.asList(1));
    }

}
