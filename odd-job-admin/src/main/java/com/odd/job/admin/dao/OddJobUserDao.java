package com.odd.job.admin.dao;

import com.odd.job.admin.core.model.OddJobUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface OddJobUserDao {

	public List<OddJobUser> pageList(@Param("offset") int offset,
                                     @Param("pagesize") int pagesize,
                                     @Param("username") String username,
									 @Param("role") int role);

	public int pageListCount(@Param("offset") int offset,
							 @Param("pagesize") int pagesize,
							 @Param("username") String username,
							 @Param("role") int role);

	public OddJobUser loadByUserName(@Param("username") String username);

	public int save(OddJobUser oddJobUser);

	public int update(OddJobUser oddJobUser);
	
	public int delete(@Param("id") int id);

}
