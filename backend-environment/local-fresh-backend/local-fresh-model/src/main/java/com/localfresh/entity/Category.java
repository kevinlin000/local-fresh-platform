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

    // 類型: 1商品分類 2直送箱分類
    private Integer type;

    // 分類名稱
    private String name;

    // 順序
    private Integer sort;

    // 分類狀態 0 停用 1 啟用
    private Integer status;

    // 建立時間
    private LocalDateTime createTime;

    // 更新時間
    private LocalDateTime updateTime;

    //建立人
    private Long createUser;

    //修改人
    private Long updateUser;
}
