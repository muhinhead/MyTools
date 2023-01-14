/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xlend.orm.tools.dbgen.dbstructure;

import java.util.StringTokenizer;

/**
 *
 * @author Nick Mukhin
 */
public class Column {

    private int number;
    private String name = null;
    private Type type = null;
    private Integer length = null;
    private Integer precision = null;
    private boolean isNullable = true;
    private boolean isPrimary = true;

    public Column(String inLine, int number) throws NotColumnDescribingLineException {
        this.number = number;
        if (inLine.trim().length() == 0 || inLine.charAt(0) != ' '
                || inLine.indexOf("primary key") > 0 || inLine.indexOf("foreign key") > 0) {
            throw new NotColumnDescribingLineException(inLine);
        }
        int n = 0, p;//, r;
        boolean isIdentity = false;
        boolean isLength = ((p = inLine.indexOf('(')) > 0);
        for (StringTokenizer tok = new StringTokenizer(inLine, " \t,()"); tok.hasMoreTokens(); n++) {
            String s = tok.nextToken();
            if (s.equalsIgnoreCase("identity")) {
                isIdentity = true;
            } else if (n == 0) {
                name = s;
            } else if (n == 1) {
                type = new Type(s);
            } else if (n == 2 && isLength) {
                try {
                    length = new Integer(s);
                } catch (NumberFormatException ne) {
                }
            } else if (n == 3 && isLength) {
                try {
                    precision = new Integer(s);
                } catch (NumberFormatException ne) {
                }
            }
        }
        isNullable = isIdentity ? false : (inLine.indexOf("not null") == -1);
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public String getName() {
        return name;
    }

    public String getJavaName() {
        String javaName = "";
        char[] arr = name.toCharArray();
        boolean nextBig = false;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '_') {
                nextBig = true;
            } else if (nextBig) {
                javaName += ("" + arr[i]).toUpperCase();
                nextBig = false;
            } else {
                javaName += arr[i];
            }
        }
        return javaName;
    }

    public Type getType() {
        return type;
    }

    public Integer getLength() {
        return length;
    }

    public Integer getPrecision() {
        return precision;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public String toString() {
        String out = "Column:" + getName() + " type:(" + getType() + ") length:" + getLength() + " precision:" + getPrecision();
        return out;
    }

    public int getNumber() {
        return number;
    }
}
