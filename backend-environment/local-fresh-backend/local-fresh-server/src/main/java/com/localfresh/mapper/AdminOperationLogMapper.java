package com.localfresh.mapper;

import com.github.pagehelper.Page;
import com.localfresh.dto.AdminOperationLogPageQueryDTO;
import com.localfresh.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface AdminOperationLogMapper {

    @Insert("insert into admin_operation_log " +
            "(action, target_type, target_id, before_value, after_value, reason, operator_type, operator_id, created_at) " +
            "values (#{action}, #{targetType}, #{targetId}, #{beforeValue}, #{afterValue}, #{reason}, #{operatorType}, #{operatorId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AdminOperationLog adminOperationLog);

    Page<AdminOperationLog> pageQuery(AdminOperationLogPageQueryDTO queryDTO);
}
