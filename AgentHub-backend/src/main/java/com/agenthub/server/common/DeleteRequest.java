package com.agenthub.server.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    /**
     * 数据主键
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}


