/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xlend.orm.tools.dbgen;

import com.xlend.orm.tools.dbgen.dbstructure.Column;
import com.xlend.orm.tools.dbgen.dbstructure.NotForeignKeyDescribingLineException;
import com.xlend.orm.tools.dbgen.dbstructure.NotTableDescribingLineException;
import com.xlend.orm.tools.dbgen.dbstructure.Type;
import com.xlend.orm.tools.dbgen.dbstructure.ForeignKey;
import com.xlend.orm.tools.dbgen.dbstructure.Table;
import java.io.*;
import java.util.Date;
import java.util.Iterator;

/**
 *
 * @author Nick Mukhin
 */
public class ORMGenerator {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage:\n\tjava com.xlend.orm.tools.dbgen.ORMGenerator <creating_database.sql> <output directory> [package_name]");
        } else {
            try {
                BufferedReader in = new BufferedReader(new FileReader(args[0]));
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.toLowerCase();
                    if (line.trim().startsWith("create ") && line.indexOf("table ") > 0) {
                        Table table = new Table(line, in);
                    } else if (line.trim().indexOf("foreign key") > 0) {
                        new ForeignKey(line, in);
                    }
                }
                generateClasses(args[1], args.length > 2 ? args[2] : "");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NotTableDescribingLineException e) {
                e.printStackTrace();
            } catch (NotForeignKeyDescribingLineException e) {
                e.printStackTrace();
            }
        }
    }

    private static PrintWriter getJavaOutputWriter(
            Table table, String outputDir, String packageName) throws IOException {
        final String JAVA = "java";
        PrintWriter output;
        String packagePath = packageName.replace('.', File.separatorChar);
        String dirName = outputDir + File.separatorChar + //JAVA +
                File.separatorChar + packagePath;
        String filePath = dirName + File.separatorChar + capitalize(table.getName()) + "." + JAVA;
        File dir = new File(dirName);
        dir.mkdirs();
//        File oldVersion = new File(filePath);
//        if (oldVersion.exists()) {
//            oldVersion.renameTo(new File(oldVersion + ".bak"));
//        }
        System.out.println("...generating " + capitalize(table.getName()) + "." + JAVA);
        output = new PrintWriter(new FileWriter(filePath));
        javaHeader(output, packageName, table.getName());
        return output;
    }

    private static void javaHeader(PrintWriter output, String packageName, String tableName) {
        Date now = new java.util.Date();
//        output.println("// $Id: DbGenerator.java,v 1.3 2009-10-16 12:34:51 nick Exp $");
        output.println("// Generated by com.xlend.orm.tools.dbgen.DbGenerator.class at " + now);
        output.println("// generated file: do not modify");
        output.println("package " + packageName + ";");
        output.println();
    }

    private static void generateClasses(String outputDir, String packageName) throws IOException {
        for (Iterator it = Table.allTables.values().iterator(); it.hasNext();) {
            Table table = (Table) it.next();
            generateClass(table, outputDir, packageName);
        }
    }

    private static void generateClass(Table table, String outputDir, String packageName) throws IOException {
        Iterator it;
        Column col;
        Column pkColumn = (Column) table.getColumns().get(table.getPrimaryKeyColumnName());
        PrintWriter output = getJavaOutputWriter(table, outputDir, packageName);
        output.println("import "+packageName+".dbobject.DbObject;");
        output.println("import "+packageName+".dbobject.ForeignKeyViolationException;");
        output.println("import "+packageName+".dbobject.Triggers;");
        output.println("import java.sql.*;");
        output.println("import java.util.ArrayList;");
        output.println();
        output.println("public class " + capitalize(table.getName()) + " extends DbObject " + table.getImplementsInterfaces() + " {");
        output.println("    private static Triggers activeTriggers = null;");
        if (pkColumn != null) {
            output.println("    private " + pkColumn.getType().getJavaType() + " "
                    + pkColumn.getJavaName() + " = null;");
        }
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.println("    private " + col.getType().getJavaType() + " "
                        + col.getJavaName() + " = null;");
            }
        }
        output.println();
        output.println("    public " + capitalize(table.getName()) + "(Connection connection) {");
        output.println("        super(connection, \"" + table.getName() + "\", \""
                + table.getPrimaryKeyColumnName() + "\");");
        output.print("        setColumnNames(new String[]{");
        if (pkColumn != null) {
            output.print("\"" + pkColumn.getName() + "\"");
        }
        int n = 0;
        for (it = table.getSortedColumns().iterator(); it.hasNext(); n++) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.print(", \"" + col.getName() + "\"");
            }
        }
        output.println("});");
        output.println("    }");
        output.println();
        output.print("    public " + capitalize(table.getName()) + "(Connection connection");
        if (pkColumn != null) {
            output.print(", " + pkColumn.getType().getJavaType() + " "
                    + pkColumn.getJavaName());
        }
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.print(", " + col.getType().getJavaType() + " " + col.getJavaName());
            }
        }
        output.println(") {");
        output.println("        super(connection, \"" + table.getName() + "\", \""
                + table.getPrimaryKeyColumnName() + "\");");

        output.println("        setNew(" + pkColumn.getJavaName() + ".intValue() <= 0);");
        output.println("//        if (" + pkColumn.getJavaName() + ".intValue() != 0) {");
        output.println("            this." + pkColumn.getJavaName() + " = " + pkColumn.getJavaName() + ";");
        output.println("//        }");

        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.println("        this." + col.getJavaName() + " = " + col.getJavaName() + ";");
            }
        }
        output.println("    }");
        output.println();
        String lowerTabName = uncapitalize(table.getName());
        output.println("    public DbObject loadOnId(int id) throws SQLException, ForeignKeyViolationException {");
        output.println("        " + capitalize(table.getName()) + " " + lowerTabName + " = null;");
        output.println("        PreparedStatement ps = null;");
        output.println("        ResultSet rs = null;");
        output.print("        String stmt = \"SELECT " + table.getPrimaryKeyColumnName());
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.print("," + col.getName());
            }
        }
        output.println(" FROM " + table.getSQLName() + " WHERE " + table.getPrimaryKeyColumnName() + "=\" + id;");
        output.println("        try {");
        output.println("            ps = getConnection().prepareStatement(stmt);");
        output.println("            rs = ps.executeQuery();");
        output.println("            if (rs.next()) {");
        output.println("                " + lowerTabName + " = new " + capitalize(table.getName()) + "(getConnection());");
        output.println("                " + lowerTabName + ".set" + capitalize(pkColumn.getJavaName()) + "(new Integer(rs.getInt(1)));");
        n = 2;
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.println("                " + lowerTabName + ".set" + capitalize(col.getJavaName()) + "("
                        + Type.getODBCfuncGetName("rs.get", col.getType().getJavaType(), null, n) + ");");
                n++;
            }
        }
        output.println("                " + lowerTabName + ".setNew(false);");
        output.println("            }");
        output.println("        } finally {");
        output.println("            try {");
        output.println("                if (rs != null) rs.close();");
        output.println("            } finally {");
        output.println("                if (ps != null) ps.close();");
        output.println("            }");
        output.println("        }");
        output.println("        return " + lowerTabName + ";");

        output.println("    }");
        output.println();
        output.println("    protected void insert() throws SQLException, ForeignKeyViolationException {");
        output.println("         if (getTriggers() != null) {");
        output.println("             getTriggers().beforeInsert(this);");
        output.println("         }");
        output.println("         PreparedStatement ps = null;");
        output.println("         String stmt =");
        output.print("                \"INSERT INTO " + table.getSQLName() + " (");// + table.getPrimaryKeyColumnName());

        output.print("\"+(get" + capitalize(pkColumn.getJavaName()) + "().intValue()!=0?\"" + pkColumn.getName() + ",\":\"\")+\"");
        for (n = 0, it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.print((n > 0 ? "," : "") + col.getName());
                n++;
            }
        }
        output.print(") ");
        output.print("values(");
        output.print("\"+(get" + capitalize(pkColumn.getJavaName()) + "().intValue()!=0?\"?,\":\"\")+\"");
        for (n = 0, it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.print((n > 0 ? "," : "") + "?");
                n++;
            }
        }
        output.println(")\";");
        output.println("         try {");
        output.println("             ps = getConnection().prepareStatement(stmt);");
        output.println("             int n = 0;");
        n = 0;
        output.println("             if (get" + capitalize(pkColumn.getJavaName()) + "().intValue()!=0) {");
        output.println("                 ps.setObject(++n, get" + capitalize(pkColumn.getJavaName()) + "());");
        output.println("             }");
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            String delim = (col.getType().getJavaType().equals("String") ? "'" : "");
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.println("             "
                        + "ps.setObject(++n, get" + capitalize(col.getJavaName()) + "());");
            }
        }
        output.println("             ps.execute();");
        output.println("         } finally {");
        output.println("             if (ps != null) ps.close();");
        output.println("         }");
        output.println("         ResultSet rs = null;");
        output.println("         if (get" + capitalize(pkColumn.getJavaName()) + "().intValue()==0) {");
        output.println("             stmt = \"SELECT max(" + table.getPrimaryKeyColumnName() + ") FROM " + table.getSQLName() + "\";");
        output.println("             try {");
        output.println("                 ps = getConnection().prepareStatement(stmt);");
        output.println("                 rs = ps.executeQuery();");
        output.println("                 if (rs.next()) {");
        output.println("                     set" + capitalize(pkColumn.getJavaName()) + "(new Integer(rs.getInt(1)));");
        output.println("                 }");
        output.println("             } finally {");
        output.println("                 try {");
        output.println("                     if (rs != null) rs.close();");
        output.println("                 } finally {");
        output.println("                     if (ps != null) ps.close();");
        output.println("                 }");
        output.println("             }");
        output.println("         }");
        output.println("         setNew(false);");
        output.println("         setWasChanged(false);");
        output.println("         if (getTriggers() != null) {");
        output.println("             getTriggers().afterInsert(this);");
        output.println("         }");
        output.println("    }");
        output.println();
        output.println("    public void save() throws SQLException, ForeignKeyViolationException {");
        output.println("        if (isNew()) {");
        output.println("            insert();");
        output.println("        } else {");
        output.println("            if (getTriggers() != null) {");
        output.println("                getTriggers().beforeUpdate(this);");
        output.println("            }");
        output.println("            PreparedStatement ps = null;");
        output.println("            String stmt =");
        output.println("                    \"UPDATE " + table.getSQLName() + " \" +");
        output.print("                    \"SET ");
        n = 0;
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            String delim = (col.getType().getJavaType().equals("String") ? "'" : "");
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                if (n > 0) {
                    output.print(", ");
                }
                output.print(col.getName() + " = ?");
                n++;
            }
        }
        output.println("\" + ");
        output.println("                    \" WHERE " + table.getPrimaryKeyColumnName() + " = \" + get" + capitalize(pkColumn.getJavaName()) + "();");
        output.println("            try {");
        output.println("                ps = getConnection().prepareStatement(stmt);");
        n = 0;
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            String delim = (col.getType().getJavaType().equals("String") ? "'" : "");
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.println("                "
                        + "ps.setObject(" + (++n) + ", get" + capitalize(col.getJavaName()) + "());");
            }
        }
        output.println("                ps.execute();");
        output.println("            } finally {");
        output.println("                if (ps != null) ps.close();");
        output.println("            }");
        output.println("            setWasChanged(false);");
        output.println("            if (getTriggers() != null) {");
        output.println("                getTriggers().afterUpdate(this);");
        output.println("            }");
        output.println("        }");
        output.println("    }");
        output.println();
        output.println("    public void delete() throws SQLException, ForeignKeyViolationException {");
        if (!table.getForeignKeys().isEmpty()) {
            // check for dependant records existance
            for (it = table.getForeignKeys().values().iterator(); it.hasNext();) {
                ForeignKey fk = (ForeignKey) it.next();
                col = fk.getFkColumn();
                if (fk.getTableTo().getName().equals(table.getName())) {
                    if (!fk.isDeleteCascade() && !fk.isDeleteSetNull()) {
                        output.println("        if (" + capitalize(fk.getTableFrom().getName()) + ".exists(getConnection(),\"" + col.getName() + " = \" + get" + capitalize(pkColumn.getJavaName()) + "())) {");
                        output.println("            throw new ForeignKeyViolationException(\"Can't delete, foreign key violation: " + fk.getName() + "\");");
                        output.println("        }");
                    }
                }
            }
            output.println("        if (getTriggers() != null) {");
            output.println("            getTriggers().beforeDelete(this);");
            output.println("        }");
            // now delete cascade & delete setnull
            for (it = table.getForeignKeys().values().iterator(); it.hasNext();) {
                ForeignKey fk = (ForeignKey) it.next();
                col = fk.getFkColumn();
                if (fk.getTableTo().getName().equals(table.getName())) {
                    if (fk.isDeleteCascade()) {
                        output.println("        {// delete cascade from " + fk.getTableFrom().getName());
                        output.println("            " + capitalize(fk.getTableFrom().getName()) + "[] records = (" + capitalize(fk.getTableFrom().getName()) + "[])"
                                + "" + capitalize(fk.getTableFrom().getName()) + ".load(getConnection(),\"" + col.getName() + " = \" + get" + capitalize(pkColumn.getJavaName()) + "(),null);");
                        output.println("            for (int i = 0; i<records.length; i++) {");
                        output.println("                " + capitalize(fk.getTableFrom().getName()) + " " + uncapitalize(fk.getTableFrom().getName()) + " = records[i];");
                        output.println("                " + uncapitalize(fk.getTableFrom().getName()) + ".delete();");
                        output.println("            }");
                        output.println("        }");
                    } else if (fk.isDeleteSetNull()) {
                        output.println("        {// on delete set null at " + fk.getTableFrom().getName());
                        output.println("            " + capitalize(fk.getTableFrom().getName()) + "[] records = (" + capitalize(fk.getTableFrom().getName()) + "[])"
                                + "" + capitalize(fk.getTableFrom().getName()) + ".load(getConnection(),\"" + col.getName() + " = \" + get" + capitalize(pkColumn.getJavaName()) + "(),null);");
                        output.println("            for (int i = 0; i<records.length; i++) {");
                        output.println("                " + capitalize(fk.getTableFrom().getName()) + " " + uncapitalize(fk.getTableFrom().getName()) + " = records[i];");
                        output.println("                " + uncapitalize(fk.getTableFrom().getName()) + ".set" + capitalize(col.getJavaName()) + "(null);");
                        output.println("                " + uncapitalize(fk.getTableFrom().getName()) + ".save();");
                        output.println("            }");
                        output.println("        }");
                    }
                }
            }
        }
        output.println("        PreparedStatement ps = null;");
        output.println("        String stmt =");
        output.println("                \"DELETE FROM " + table.getSQLName() + " \" +");
        output.println("                \"WHERE " + table.getPrimaryKeyColumnName() + " = \" + get" + capitalize(pkColumn.getJavaName()) + "();");
        output.println("        try {");
        output.println("            ps = getConnection().prepareStatement(stmt);");
        output.println("            ps.execute();");
        output.println("        } finally {");
        output.println("            if (ps != null) ps.close();");
        output.println("        }");
        output.println("        set" + capitalize(pkColumn.getJavaName()) + "(new Integer(-get" + capitalize(pkColumn.getJavaName()) + "().intValue()));");
        output.println("        if (getTriggers() != null) {");
        output.println("            getTriggers().afterDelete(this);");
        output.println("        }");
        output.println("    }");
        output.println();
        output.println("    public boolean isDeleted() {");
        output.println("        return (get" + capitalize(pkColumn.getJavaName()) + "().intValue() < 0);");
        output.println("    }");
        output.println();
        output.println("    public static DbObject[] load(Connection con,String whereCondition,String orderCondition) throws SQLException {");
        output.println("        ArrayList lst = new ArrayList();");
        output.println("        PreparedStatement ps = null;");
        output.println("        ResultSet rs = null;");
        output.print("        String stmt = \"SELECT " + table.getPrimaryKeyColumnName());
//        for (it = table.getColumns().values().iterator(); it.hasNext();) {
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.print("," + col.getName());
            }
        }
        output.println(" FROM " + table.getSQLName() + " \" +");
        output.println("                ((whereCondition != null && whereCondition.length() > 0) ?");
        output.println("                \" WHERE \" + whereCondition : \"\") +");
        output.println("                ((orderCondition != null && orderCondition.length() > 0) ?");
        output.println("                \" ORDER BY \" + orderCondition : \"\");");
        output.println("        try {");
        output.println("            ps = con.prepareStatement(stmt);");
        output.println("            rs = ps.executeQuery();");
        output.println("            while (rs.next()) {");
        output.println("                DbObject dbObj;");
        output.print("                lst.add(dbObj=new " + capitalize(table.getName()) + "(con,new Integer(rs.getInt(1))");
        n = 2;
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.print("," + Type.getODBCfuncGetName("rs.get", col.getType().getJavaType(), null, n++));
            }
        }
        output.println("));");
        output.println("                dbObj.setNew(false);");
        output.println("            }");
        output.println("        } finally {");
        output.println("            try {");
        output.println("                if (rs != null) rs.close();");
        output.println("            } finally {");
        output.println("                if (ps != null) ps.close();");
        output.println("            }");
        output.println("        }");
        output.println("        " + capitalize(table.getName()) + "[] objects = new " + capitalize(table.getName()) + "[lst.size()];");
        output.println("        for (int i = 0; i < lst.size(); i++) {");
        output.println("            " + capitalize(table.getName()) + " " + uncapitalize(table.getName()) + " = (" + capitalize(table.getName()) + ") lst.get(i);");
        output.println("            objects[i] = " + uncapitalize(table.getName()) + ";");
        output.println("        }");
        output.println("        return objects;");
        output.println("    }");
        output.println();
        output.println("    public static boolean exists(Connection con, String whereCondition) throws SQLException {");
        output.println("        if (con == null) {");
        output.println("            return true;");
        output.println("        }");
        output.println("        boolean ok = false;");
        output.println("        PreparedStatement ps = null;");
        output.println("        ResultSet rs = null;");
        output.println("        String stmt = \"SELECT " + table.getPrimaryKeyColumnName() + " FROM " + table.getSQLName() + " \" +"); //+" WHERE \" + whereCondition;");
        output.println("                ((whereCondition != null && whereCondition.length() > 0) ?");
        output.println("                \"WHERE \" + whereCondition : \"\");");

        output.println("        try {");
        output.println("            ps = con.prepareStatement(stmt);");
        output.println("            rs = ps.executeQuery();");
        output.println("            ok = rs.next();");
        output.println("        } finally {");
        output.println("            try {");
        output.println("                if (rs != null) rs.close();");
        output.println("            } finally {");
        output.println("                if (ps != null) ps.close();");
        output.println("            }");
        output.println("        }");
        output.println("        return ok;");
        output.println("    }");
        output.println();
        output.println("    //public String toString() {");
        output.println("    //    return get" + capitalize(pkColumn.getJavaName()) + "() + getDelimiter();");
        output.println("    //}");
        output.println();
        output.println("    public Integer getPK_ID() {");
        output.println("        return " + pkColumn.getJavaName() + ";");
        output.println("    }");
        output.println();
        output.println("    public void setPK_ID(Integer id) throws ForeignKeyViolationException {");
        output.println("        boolean prevIsNew = isNew();");
        output.println("        set" + capitalize(pkColumn.getJavaName()) + "(id);");
        output.println("        setNew(prevIsNew);");
        output.println("    }");
        output.println();
        output.println("    public Integer get" + capitalize(pkColumn.getJavaName()) + "() {");
        output.println("        return " + pkColumn.getJavaName() + ";");
        output.println("    }");
        output.println();
        output.println("    public void set" + capitalize(pkColumn.getJavaName()) + "(Integer " + pkColumn.getJavaName() + ") throws ForeignKeyViolationException {");
        output.println("        setWasChanged(this." + pkColumn.getJavaName() + " != null && this." + pkColumn.getJavaName() + " != " + pkColumn.getJavaName() + ");");
        output.println("        this." + pkColumn.getJavaName() + " = " + pkColumn.getJavaName() + ";");
        output.println("        setNew(" + pkColumn.getJavaName() + ".intValue() == 0);");
        output.println("    }");
        for (it = table.getSortedColumns().iterator(); it.hasNext();) {
            col = (Column) it.next();
            if (!col.getName().equals(table.getPrimaryKeyColumnName())) {
                output.println();
                output.println("    public " + col.getType().getJavaType() + " get" + capitalize(col.getJavaName()) + "() {");
                output.println("        return " + col.getJavaName() + ";");
                output.println("    }");
                output.println();
                output.println("    public void set" + capitalize(col.getJavaName()) + "(" + col.getType().getJavaType() + " " + col.getJavaName() + ") throws SQLException, ForeignKeyViolationException {");
                boolean isFirst = true;
                for (Iterator inner = table.getForeignKeys().values().iterator(); inner.hasNext();) {
                    if (isFirst && col.getType().getJavaType().equals("Integer") && col.isNullable()) {
                        output.println("        if (null != " + col.getJavaName() + ")");
                        output.println("            " + col.getJavaName() + " = " + col.getJavaName() + " == 0 ? null : " + col.getJavaName() + ";");
                        isFirst = false;
                    }
                    ForeignKey fk = (ForeignKey) inner.next();
                    if (fk.getTableFrom().getName().equals(table.getName()) && fk.getFkColumn().getName().equals(col.getName())) {
                        Column refCol = (Column) fk.getTableTo().getColumns().get(fk.getTableTo().getPrimaryKeyColumnName());
                        output.println("        if (" + col.getJavaName() + "!=null && !" + capitalize(fk.getTableTo().getName()) + ".exists(getConnection(),\"" + refCol.getName() + " = \" + " + col.getJavaName() + ")) {");
                        output.println("            throw new ForeignKeyViolationException(\"Can't set " + col.getName() + ", foreign key violation: " + fk.getName() + "\");");
                        output.println("        }");
                    }
                }
                output.println("        setWasChanged(this." + col.getJavaName() + " != null && !this." + col.getJavaName() + ".equals(" + col.getJavaName() + "));");
                output.println("        this." + col.getJavaName() + " = " + col.getJavaName() + ";");
                output.println("    }");
            }
        }
        output.println("    public Object[] getAsRow() {");
        output.println("        Object[] columnValues = new Object[" + table.getColumns().size() + "];");
        for (it = table.getSortedColumns().iterator(), n = 0; it.hasNext(); n++) {
            col = (Column) it.next();
            output.println("        columnValues[" + n + "] = get" + capitalize(col.getJavaName()) + "();");
        }
        output.println("        return columnValues;");
        output.println("    }");
        output.println();
        output.println("    public static void setTriggers(Triggers triggers) {");
        output.println("        activeTriggers = triggers;");
        output.println("    }");
        output.println();
        output.println("    public static Triggers getTriggers() {");
        output.println("        return activeTriggers;");
        output.println("    }");
        output.println();
        output.println("    //for SOAP exhange");
        output.println("    @Override");
        output.println("    public void fillFromString(String row) throws ForeignKeyViolationException, SQLException {");
        output.println("        String[] flds = splitStr(row, delimiter);");
        for (it = table.getSortedColumns().iterator(), n = 0; it.hasNext(); n++) {
            col = (Column) it.next();
            if (col.getType().getJavaType().equals("Integer")) {
                output.println("        try {");
                output.println("            set" + capitalize(col.getJavaName()) + "(Integer.parseInt(flds[" + n + "]));");
                output.println("        } catch(NumberFormatException ne) {");
                output.println("            set" + capitalize(col.getJavaName()) + "(null);");
                output.println("        }");
            } else if (col.getType().getJavaType().equals("Date")) {
                output.println("        set" + capitalize(col.getJavaName()) + "(toDate(flds[" + n + "]));");
            } else if (col.getType().getJavaType().equals("Timestamp")) {
                output.println("        set" + capitalize(col.getJavaName()) + "(toTimeStamp(flds[" + n + "]));");
            } else if (col.getType().getJavaType().equals("Double")) {
                output.println("        try {");
                output.println("            set" + capitalize(col.getJavaName()) + "(Double.parseDouble(flds[" + n + "]));");
                output.println("        } catch(NumberFormatException ne) {");
                output.println("            set" + capitalize(col.getJavaName()) + "(null);");
                output.println("        }");
            } else if (col.getType().getJavaType().equals("Float")) {
                output.println("        try {");
                output.println("            set" + capitalize(col.getJavaName()) + "(Float.parseFloat(flds[" + n + "]));");
                output.println("        } catch(NumberFormatException ne) {");
                output.println("            set" + capitalize(col.getJavaName()) + "(null);");
                output.println("        }");
            } else if (col.getType().getJavaType().equals("Timestamp")) {
                output.println("        //Timestamp flds[" + n + "] skipped");
            } else {
                output.println("        set" + capitalize(col.getJavaName()) + "(flds[" + n + "]);");
            }
        }
        output.println("    }");
        output.println("}");
        output.close();
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static String uncapitalize(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }
}
