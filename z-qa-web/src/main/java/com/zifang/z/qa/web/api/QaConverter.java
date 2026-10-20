package com.zifang.z.qa.web.api;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO ↔ DO 转换（lead 007 步骤 6）。DO 不出 web 层，只在这里做单向拷贝。
 *
 * <p>两边字段同名（DO 用 {@code @TableField} 映射列名，Java 属性仍是驼峰），故可直接
 * copyProperties；列表另有一份拷贝，避免把 DO 集合直接序列化出去。
 */
final class QaConverter {

    private QaConverter() {
    }

    static <T, R> R to(Object doObj, Class<R> respType) {
        if (doObj == null) {
            return null;
        }
        R resp = BeanUtils.instantiateClass(respType);
        BeanUtils.copyProperties(doObj, resp);
        return resp;
    }

    static <D, R> List<R> toList(List<D> doList, Class<R> respType) {
        if (doList == null) {
            return new ArrayList<>();
        }
        List<R> out = new ArrayList<>(doList.size());
        for (D d : doList) {
            out.add(to(d, respType));
        }
        return out;
    }
}
