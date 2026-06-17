package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //類型: 1商品分類 2直送箱分類
    private Integer type;

    //分類名称
    private String name;

    //顺序
    private Integer sort;

    //分類狀態 0标识停用 1表示啟用
    private Integer status;

    //建立時间
    private LocalDateTime createTime;

    //更新時间
    private LocalDateTime updateTime;

    //建立人
    private Long createUser;

    //修改人
    private Long updateUser;
}
