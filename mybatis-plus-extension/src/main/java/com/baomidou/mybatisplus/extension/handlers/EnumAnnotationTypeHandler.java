/*
 * Copyright (c) 2011-2020, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.extension.handlers;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.core.toolkit.EnumUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EnumValue 注解自定义枚举属性转换器
 *
 * @author yuxiaobin
 * @date 2018-08-30
 */
public class EnumAnnotationTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> type;

    public EnumAnnotationTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }

    private static final Log LOGGER = LogFactory.getLog(EnumAnnotationTypeHandler.class);

    private static final Map<Class<?>, Method> TABLE_FIELD_OF_ENUM_TYPES = new ConcurrentHashMap<>();

    public static void addEnumType(Class<?> clazz, Method method) {
        TABLE_FIELD_OF_ENUM_TYPES.put(clazz, method);
    }


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Enum parameter, JdbcType jdbcType) throws SQLException {
        Method method = getField(type);
        try {
            method.setAccessible(true);
            if (jdbcType == null) {
                ps.setObject(i, method.invoke(parameter));
            } else {
                // see r3589
                ps.setObject(i, method.invoke(parameter), jdbcType.TYPE_CODE);
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("unrecognized jdbcType, failed to set StringValue for type=" + parameter);
        } catch (InvocationTargetException e) {
            throw ExceptionUtils.mpe("Error: NoSuchMethod in %s.  Cause:", e, type.getName());
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getEnumResult(rs.getObject(columnName));
    }

    private E getEnumResult(Object s) {
        if (s == null) {
            return null;
        }
        return EnumUtils.valueOf(type, s, getField(type));
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getEnumResult(rs.getObject(columnIndex));
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getEnumResult(cs.getObject(columnIndex));
    }

    public static Optional<Field> dealEnumType(Class<?> clazz) {
        return clazz.isEnum() ? Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(EnumValue.class)).findFirst() : Optional.empty();
    }

    private Method getField(Class<?> clazz) {
        return Optional.ofNullable(TABLE_FIELD_OF_ENUM_TYPES.get(type)).orElseGet(() -> {
            Field field = dealEnumType(clazz).orElseThrow(() -> new IllegalArgumentException("当前[" + type.getName() + "]枚举类未找到标有@EnumValue注解的字段"));
            Method method = ReflectionKit.getMethod(clazz, field);
            addEnumType(clazz, method);
            return method;
        });
    }
}
