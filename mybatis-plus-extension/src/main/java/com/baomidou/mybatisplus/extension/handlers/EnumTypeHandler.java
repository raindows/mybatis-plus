/*
 * Copyright (c) 2011-2020, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.mybatisplus.extension.handlers;

import com.baomidou.mybatisplus.core.enums.IEnum;
import com.baomidou.mybatisplus.core.toolkit.EnumUtils;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 自定义枚举属性转换器
 *
 * @author hubin
 * @since 2017-10-11
 */
public class EnumTypeHandler<E extends Enum<?> & IEnum> extends BaseTypeHandler<IEnum> {

    private Class<E> type;

    private Method method;

    public EnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        if (!IEnum.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("当前[" + type.getName() + "]枚举类未实现" + IEnum.class.getName() + "接口");
        }
        this.type = type;
        try {
            method = type.getMethod("getValue");
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("当前[" + type.getName() + "]枚举类未找到getValue方法");
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, IEnum parameter, JdbcType jdbcType)
        throws SQLException {
        if (jdbcType == null) {
            ps.setObject(i, parameter.getValue());
        } else {
            ps.setObject(i, parameter.getValue(), jdbcType.TYPE_CODE);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        if (null == rs.getObject(columnName) && rs.wasNull()) {
            return null;
        }
        return EnumUtils.valueOf(type, rs.getObject(columnName), method);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        if (null == rs.getObject(columnIndex) && rs.wasNull()) {
            return null;
        }
        return EnumUtils.valueOf(type, rs.getObject(columnIndex), method);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        if (null == cs.getObject(columnIndex) && cs.wasNull()) {
            return null;
        }
        return EnumUtils.valueOf(type, cs.getObject(columnIndex), method);
    }
}
