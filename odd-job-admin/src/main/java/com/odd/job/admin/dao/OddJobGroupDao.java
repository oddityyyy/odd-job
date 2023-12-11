package com.odd.job.admin.dao;

import com.odd.job.admin.core.model.OddJobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by xuxueli on 16/9/30.
 */
@Mapper
public interface OddJobGroupDao {

    public List<OddJobGroup> findAll();

    public List<OddJobGroup> findByAddressType(@Param("addressType") int addressType);

    public int save(OddJobGroup oddJobGroup);

    public int update(OddJobGroup oddJobGroup);

    public int remove(@Param("id") int id);

    public OddJobGroup load(@Param("id") int id);

    public List<OddJobGroup> pageList(@Param("offset") int offset,
                                      @Param("pagesize") int pagesize,
                                      @Param("appname") String appname,
                                      @Param("title") String title);

    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("appname") String appname,
                             @Param("title") String title);

}
