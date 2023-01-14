/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xlend.orm.tools.dbgen.dbstructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;


/**
 *
 * @author Nick Mukhin
 */

public class ForeignKey {
    private String name;
    private Column fkColumn;
    private Table tableFrom;
    private Table tableTo;
    private boolean deleteCascade = false;
    private boolean deleteSetNull = false;

    public ForeignKey(String inLine, BufferedReader in) throws NotForeignKeyDescribingLineException, IOException {
        if (inLine.trim().indexOf("constraint") != 0 || inLine.indexOf("foreign key") == -1) {
            throw new NotForeignKeyDescribingLineException(inLine);
        }
        deleteCascade = (inLine.toUpperCase().indexOf("DELETE CASCADE") > 0);
        deleteSetNull = (inLine.toUpperCase().indexOf("DELETE SET NULL") > 0);
        String token = null;
        StringTokenizer tok;
        int n = 0;
        for (tok = new StringTokenizer(inLine," ,.\t\n"); tok.hasMoreTokens(); n++) {
            token = tok.nextToken();
            if (n==1) {
                name = token;
            } else if (token.equalsIgnoreCase("references")) {
                String refTable = tok.nextToken();
                tableTo = (Table) Table.allTables.get(refTable);
            }
        }
        n = 0;
        for (tok = new StringTokenizer(inLine, "_"); tok.hasMoreTokens(); n++) {
            token = tok.nextToken();
            if (n == 0) {
                int pos = token.lastIndexOf(' ');
                String tabname = token.substring(pos + 1);
                tableFrom = (Table) Table.allTables.get(tabname);
            //} else if (n == 1) {
            //    tableTo = (Table) Table.allTables.get(token);
            }
        }
        String fkColname;
        int p = inLine.indexOf('(');
        if (null == getTableFrom()) {
            tableFrom = Table.currentTable;
        }
        if (p >= 0) {
            fkColname = inLine.substring(p+1, inLine.indexOf(')',p));
            fkColumn = (Column) tableFrom.getColumns().get(fkColname);
        }
        getTableFrom().getForeignKeys().put(name, this);
        getTableTo().getForeignKeys().put(name, this);
    }

    public String toString() {
        return "foreign key on " + getTableFrom().getName() + "(" + getFkColumn().getName() + ") to " + getTableTo().getName();
    }

    public Table getTableFrom() {
        return tableFrom;
    }

    public Table getTableTo() {
        return tableTo;
    }

    public String getName() {
        return name;
    }

    public Column getFkColumn() {
        return fkColumn;
    }

    public boolean isDeleteCascade() {
        return deleteCascade;
    }

    public boolean isDeleteSetNull() {
        return deleteSetNull;
    }    
}