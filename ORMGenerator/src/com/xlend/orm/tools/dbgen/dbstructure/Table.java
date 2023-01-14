/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xlend.orm.tools.dbgen.dbstructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 *
 * @author Nick Mukhin
 */
public class Table {
    public static HashMap<String, Table> allTables = new HashMap<String, Table>();
    public static Table currentTable;
    private String name;
    private String primaryKeyColumnName;
    private HashMap columns; //Column objects
    private HashMap foreignKeys; //ForeignKey objects of both directions
    private String implementsInterfaces = "";

    public Table(String inLine, BufferedReader in)
            throws NotTableDescribingLineException, IOException, NotForeignKeyDescribingLineException {
        if (inLine.trim().indexOf("create ") == -1 || inLine.indexOf("table") == -1) {
            throw new NotTableDescribingLineException(inLine);
        }
        currentTable = this;
        columns = new HashMap(); //Column objects
        foreignKeys = new HashMap(); //ForeignKey objects

        int p = inLine.toLowerCase().indexOf("cached");
        if (p>0) {
            inLine.replaceAll("cached", "");
        }
        
        if ((p = inLine.indexOf("--")) > 0) {
            String cmnt = inLine.substring(p + 2);
            if (cmnt.trim().startsWith("implements")) {
                implementsInterfaces = cmnt.trim();
            }
            inLine = inLine.substring(0, p);
        }

        String token = null;
        StringTokenizer tok;
        for (tok = new StringTokenizer(inLine); tok.hasMoreTokens();) {
            token = tok.nextToken();
        }
        name = token.replace('.', '_'); //toJavaStyle(token);
        String line;
        int number = 0;
        while ((line = in.readLine()) != null) {
            if (line.trim().length()>0 && !line.trim().startsWith("--") && !line.trim().startsWith("check") && !line.trim().startsWith("key ")) {
                int cmntStart = line.indexOf("--");
                line = cmntStart > -1 ? line.substring(0, cmntStart) : line;
                try {
                    Column col = new Column(line, number++);
                    columns.put(col.getName(), col);
                    if (line.indexOf("auto_increment") > 0) {
                        primaryKeyColumnName = col.getName();
                    }
                } catch (NotColumnDescribingLineException e) {
                    if (line.trim().indexOf("primary key") >= 0) {
                        int n = 0;
                        for (tok = new StringTokenizer(line, "()"); tok.hasMoreTokens(); n++) {
                            token = tok.nextToken();
                            if (n == 1) {
                                primaryKeyColumnName = token;
                                break;
                            }
                        }
                    } else if (line.trim().indexOf("foreign key") >= 0) {
                        new ForeignKey(line, in);
                        break;
                    }
                    if (line.trim().startsWith(")")) {
                        break;
                    }
                }
            }
        }
        allTables.put(name, this);
    }

//    public static String toJavaStyle(String token) {
//        String ans = "";
//        for (int i=0; i<token.length(); i++) {
//            if (i>0 && token.charAt(i-1)=='_') {
//                ans += token.substring(i,i+1).toUpperCase();
//            } else if (token.charAt(i)!='_') {
//                ans += token.substring(i,i+1);
//            }
//        }
//        return ans;
//    }

    public String getName() {
        return name;
    }

    public String getSQLName() {
        return name.replace('_', '.');
    }

    public HashMap getColumns() {
        return columns;
    }

    public SortedSet getSortedColumns() {
        SortedSet ss = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                Column c1 = (Column) o1;


                Column c2 = (Column) o2;
                return c1.getNumber() - c2.getNumber();
            }

            public boolean equals(Object obj) {
                return false;
            }
        });
        ss.addAll(columns.values());
        return ss;
    }

    public HashMap getForeignKeys() {
        return foreignKeys;
    }

    public String toString() {
        Iterator it;
        String out = "---Table:" + getName() + "---\n";
        out += "columns:\n";
        for (it = getSortedColumns().iterator(); it.hasNext();) {
            Column column = (Column) it.next();
            out += ("\t" + column.toString() + "\n");
        }
        out += "foreign keys:\n";
        for (it = getForeignKeys().values().iterator(); it.hasNext();) {
            ForeignKey fk = (ForeignKey) it.next();
            out += ("\t" + fk.toString()) + "\n";
        }
        return out;
    }

    public String getPrimaryKeyColumnName() {
        return primaryKeyColumnName;
    }

    public String getImplementsInterfaces() {
        return implementsInterfaces;
    }
}
