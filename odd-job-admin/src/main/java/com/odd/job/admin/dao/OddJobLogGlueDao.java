package com.odd.job.admin.dao;

import com.odd.job.admin.core.model.OddJobLogGlue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface OddJobLogGlueDao {
	
	public int save(OddJobLogGlue oddJobLogGlue);
	
	public List<OddJobLogGlue> findByJobId(@Param("jobId") int jobId);

	public int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

	public int deleteByJobId(@Param("jobId") int jobId);
	
}
