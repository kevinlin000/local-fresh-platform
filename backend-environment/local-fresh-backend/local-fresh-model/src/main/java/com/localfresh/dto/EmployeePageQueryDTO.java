package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeePageQueryDTO implements Serializable {

    //员工姓名
    private String name;

    //頁碼
    private int page;

    //每页显示紀錄数
    private int pageSize;

}
