/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xlend.orm.tools.dbgen.dbstructure;

import java.util.HashMap;

/**
 *
 * @author Nick Mukhin
 */
public class Type {
    private static HashMap sql2java = null;
    private String sqlType;
    private String javaType;

    private static void buildMap() {
        sql2java = new HashMap();
        sql2java.put("int", "Integer");
        sql2java.put("bigint", "Integer");
        sql2java.put("bit", "Integer");
        sql2java.put("smallint", "Integer");
        sql2java.put("tinyint", "Integer");
        sql2java.put("numeric", "Integer");
        sql2java.put("number", "Integer");
        sql2java.put("varchar", "String");
        sql2java.put("varchar2", "String");
        sql2java.put("varchar_ignorecase", "String");
        sql2java.put("longvarchar", "String");
        sql2java.put("varchar_ignorecase", "String");
        sql2java.put("text", "String");
        sql2java.put("char", "String");
        sql2java.put("date", "Date");
        sql2java.put("time", "Timestamp");
        sql2java.put("datetime", "Timestamp");
        sql2java.put("timestamp", "Timestamp");
        sql2java.put("year", "Integer");
        sql2java.put("tinyint", "Integer");
        sql2java.put("boolean", "Boolean");
        sql2java.put("decimal", "Double");
        sql2java.put("float", "Float");
        sql2java.put("double", "Double");
        sql2java.put("mediumblob", "Object");
        sql2java.put("blob", "Object");
        sql2java.put("other", "Object");
    }

    public static String getODBCfuncGetName(String prefix, String javaType, String postFix, int n) {
        if (javaType.equals("Integer")) {
            if (prefix.endsWith("get"))
                return "new Integer(" + prefix + "Int(" + n + "))";
            else
                return prefix + "Int(" + n + (postFix == null ? "" : postFix) + ")";
        }
        return prefix + javaType + "(" + n + (postFix == null ? "" : postFix) + ")";
    }

    public Type(String sqlType) {
        if (sql2java == null) {
            buildMap();
        }
        this.setSqlType(sqlType);
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
        javaType = (String) sql2java.get(sqlType);
    }

    public String getJavaType() {
        return javaType;
    }

//    public String getJavaType(boolean isNullable) {
//        return isNullable && javaType.equals("Integer") ? "Object" : javaType;
//    }

    public String toString() {
        return "sql:" + getSqlType() + " java:" + getJavaType();
    }
}
