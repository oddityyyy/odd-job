package com.odd.job.admin.dao;

import com.odd.job.admin.core.model.OddJobLogReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;


@Mapper
public interface OddJobLogReportDao {

	public int save(OddJobLogReport oddJobLogReport);

	public int update(OddJobLogReport oddJobLogReport);

	public List<OddJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
												@Param("triggerDayTo") Date triggerDayTo);

	public OddJobLogReport queryLogReportTotal();

}
