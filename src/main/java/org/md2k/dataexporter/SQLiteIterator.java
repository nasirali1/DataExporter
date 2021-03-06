package org.md2k.dataexporter;/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center 
 * - Timothy Hnat <twhnat@memphis.edu>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import org.md2k.datakitapi.datatype.DataType;

import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class SQLiteIterator implements Iterator<List<DataType>> {
    private ResultSet rs;
    private int bufferSize;
    private Kryo kryo = new Kryo();

    public SQLiteIterator(Statement statement, Integer id, int bufferSize) {
        this.bufferSize = bufferSize;
        try {
            rs = statement.executeQuery("Select _id, datasource_id, datetime, sample from data where datasource_id = " + id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        boolean result = false;
        try {
            result = !rs.isAfterLast();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public List<DataType> next() {
        List<DataType> result = new ArrayList<DataType>();
        byte[] b;
        Input input;
        DataType dt;
        try {
            while (result.size() < bufferSize && rs.next()) {
                b = rs.getBytes("sample");
                input = new Input(new ByteArrayInputStream(b));
                try {
                    dt = (DataType) kryo.readClassAndObject(input);
                    result.add(dt);
                } catch (KryoException ke) {
                    Date date = new Date(rs.getLong("dateTime"));
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));

                    System.err.println("KryoException: (" + rs.getLong("_id") + ", " + rs.getString("datasource_id") + ", " + rs.getLong("dateTime") + ", " + format.format(date) + ")");
//                    ke.printStackTrace();
                }


            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}
