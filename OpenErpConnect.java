/*
 * OpenERP, Open Source Management Solution
 * Copyright (c) 2012 Zikzakmedia S.L. (http://zikzakmedia.com) All Rights Reserved.
 *               Enric Caumons Gou <caumons@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.util.Log;

public class OpenErpConnect {
    /*
     * This class provides access to basic methods in OpenObject, so you can use
     * them from an Android device. The operations supported are:
     * - login
     * - create
     * - search
     * - read
     * - write
     * - unlink
     * - browse
     * - call (This is a generic method to call whatever you need)
     * You can extend OpenErpConnect to implement more specific methods of your need. */
    
    protected String mServer;
    protected Integer mPort;
    protected String mDatabase;
    protected String mUser;
    protected String mPassword; // Stored as a raw String
    protected Integer mId;
    protected URL mUrl;
    
    protected static final String CONECTOR_NAME = "OpenErpConnect";
    protected static OpenErpConnect sConnectionInstance = null; // Singleton class
    
    protected OpenErpConnect(String server, Integer port, String db, String user, String pass, Integer id) throws MalformedURLException {
        mServer = server;
        mPort = port;
        mDatabase = db;
        mUser = user;
        mPassword = pass;
        mId = id;
        mUrl = new URL("http", server, port, "/xmlrpc/object");
    }
    
    public static OpenErpConnect connect(String server, Integer port, String db, String user, String pass) {
        /* Returns an OpenErpConnect instance, which you will use to call the methods, and stores it as a static object. */
        sConnectionInstance = login(server, port, db, user, pass);
        return sConnectionInstance;
    }
    
    public static Boolean testConnection(String server, Integer port, String db, String user, String pass) {
        return login(server, port, db, user, pass) != null;
    }
    
    protected static OpenErpConnect login(String server, Integer port, String db, String user, String pass) {
        OpenErpConnect connection = null;
        try {
            URL loginUrl = new URL("http", server, port, "/xmlrpc/common");
            XMLRPCClient client = new XMLRPCClient(loginUrl);
            Integer id = (Integer)client.call("login", "oerp6_training", "admin", "admin");
            connection = new OpenErpConnect(server, port, db, user, pass, id);
        } catch (XMLRPCException e) {
            Log.d(CONECTOR_NAME, e.toString());
        } catch (MalformedURLException e) {
            Log.d(CONECTOR_NAME, e.toString());
        }
        return connection;
    }
    
    public static OpenErpConnect getConnectionInstance() {
        return sConnectionInstance;
    }
    
    public static Boolean isConnected() {
        return sConnectionInstance != null;
    }
    
    public Integer create(String model, HashMap<String, ?> values, HashMap<String, ?> context) {
        /*
         * Creates a new record for the given model with the values supplied, if
         * you do not need the context, just pass null for it.
         * Remember: In order to add different types in a Collection use Object, e.g.
         * HashMap<String, Object> values = new HashMap<String, Object>();
         * values.put("name", "hello");
         * values.put("number", 10); */
        
        Integer newObjectId = null;
        try {
            XMLRPCClient client = new XMLRPCClient(mUrl);
            newObjectId = (Integer)client.call("execute", mDatabase, mId, mPassword, model, "create", values, context);
        } catch (XMLRPCException e) {
            Log.d(CONECTOR_NAME, e.toString());
        }
        return newObjectId;
    }
    
    public Integer[] search(String model, Object...conditions) {
        return search(model, false, 0, 0, null, conditions);
    }
    
    public Integer[] search(String model, boolean count, Object...conditions) {
        return search(model, false, 0, 0, null, conditions);
    }
    
    public Integer[] search(String model, boolean count, Integer limit, String order, Object...conditions) {
        return search(model, false, 0, limit, order, conditions);
    }
    
    public Integer[] search(String model, boolean count, Integer offset, Integer limit, String order, Object...conditions) {
        /* Retrieves the ids of objects.
         * If count is true the resulting array will only contain the number of
         * matching ids.
         * You can pass new Object[0] to specify an empty list of conditions,
         * which will return all the ids for that model. */
        
        Integer[] result = null;
        try {
            XMLRPCClient client = new XMLRPCClient(mUrl);
            Vector<Object> parameters = new Vector<Object>(11);
            parameters.add(mDatabase);
            parameters.add(mId);
            parameters.add(mPassword);
            parameters.add(model);
            parameters.add("search");
            parameters.add((conditions.length == 1 && conditions[0] instanceof Object[] && ((Object[])conditions[0]).length == 0) ? conditions[0] : conditions);
            parameters.add(offset);
            parameters.add(limit);
            parameters.add(order);
            parameters.add(null);
            parameters.add(count);
            if (count) { // We just want the number of items
                result = new Integer[] { (Integer)client.call("execute", parameters) };
            } else { // Returning the list of matching item id's
                Object[] responseIds = (Object[])client.call("execute", parameters);
                if (responseIds.length > 0) {
                    result = Arrays.copyOf(responseIds, responseIds.length, Integer[].class); // Converting from Object[] to Integer[]
                }
            }
        } catch (XMLRPCException e) {
            Log.d(CONECTOR_NAME, e.toString());
        }
        return result;
     }
    
    @SuppressWarnings("unchecked")
    public List<HashMap<String, Object>> read(String model, Integer[] ids, String[] fields) {
        /* Each HashMap in the List contains the values for the specified fields for each
         * object in the ids (in the same order).
         * Specifying an empty fields array as: new String[0] will return all the fields */
        
        List<HashMap<String, Object>> listOfFieldValues = null;
        try {
            XMLRPCClient client = new XMLRPCClient(mUrl);
            Object[] responseFields = (Object[])client.call("execute", mDatabase, mId, mPassword, model, "read", ids, fields);
            if (responseFields.length > 0) {
                listOfFieldValues = new ArrayList<HashMap<String, Object>>(responseFields.length);
                for (Object objectFields : responseFields) {
                    listOfFieldValues.add((HashMap<String, Object>)objectFields);
                }
            }
        } catch (XMLRPCException e) {
            Log.d(CONECTOR_NAME, e.toString());
        }
        return listOfFieldValues;
    }
    
    public Boolean write(String model, Integer[] ids, HashMap<String, ?> values, HashMap<String, ?> context) {
        /* Used to modify an existing object. */
        
        Boolean writeOk = false;
        try {
            XMLRPCClient client = new XMLRPCClient(mUrl);
            writeOk = (Boolean)client.call("execute", mDatabase, mId, mPassword, model, "write", ids, values, context);
        } catch (XMLRPCException e) {
            Log.d(CONECTOR_NAME, e.toString());
        }
        return writeOk;
    }
    
    public Boolean unlink(String model, Integer[] ids) {
        /* A method to delete the matching records with the ids given*/
        
        Boolean unlinkOk = false;
        try {
            XMLRPCClient client = new XMLRPCClient(mUrl);
            unlinkOk = (Boolean)client.call("execute", mDatabase, mId, mPassword, model, "unlink", ids);
        } catch (XMLRPCException e) {
            Log.d(CONECTOR_NAME, e.toString());
        }
        return unlinkOk;
    }
    
    public <E> void browse(String model, Class<E> modelClass, Integer[] ids, List<String> fields, List<E> resultList) {
        /*
         * The result is stored in the parameter List<E> resultList. The parameter
         * modelClass should look like: MyClass.class Do not expect
         * to use it as in the native method. You will not jump from one model to
         * another just accessing the foreign field! But it is easier to work with
         * E instances than HashMaps ;)
         * The class E MUST define a public constructor with one parameter of type HashMap,
         * which will initialize the attributes with the values inside the
         * Hashmap with the keys corresponding to the fields supplied.
         * It is recommended no to hardcode the fields, instead, program a public
         * static method such as getAtrributeNames() in E that returns a List<String>
         * with the attribute names in the OpenERP table, which match the
         * attributes defined in the class.
         * You can extend classes and call the parent's getAtrributeNames() to
         * add() the new attributes (as it is a List<String>). Also, you can call
         * the super constructor and populate just the new attributes. This may
         * be useful for modules in OpenERP which add fields in existing models
         * e.g. module MyModule adds the field my_module_field to res.partner,
         * so you could define the classes ResPartner and ResPartnerMyModule,
         * if needed.
         */
        
        List<HashMap<String, Object>> listOfFieldValues = read(model, ids, fields.toArray(new String [fields.size()]));
        if (listOfFieldValues != null) {
            try {
                Constructor<E> constructor = modelClass.getConstructor(HashMap.class);
                for (HashMap<String, Object> objectHashmap : listOfFieldValues) {
                    resultList.add(constructor.newInstance(objectHashmap));
                }
            } catch (SecurityException e) {
                Log.d(CONECTOR_NAME, e.toString());
            } catch (NoSuchMethodException e) {
                Log.d(CONECTOR_NAME, e.toString());
            } catch (IllegalArgumentException e) {
                Log.d(CONECTOR_NAME, e.toString());
            } catch (InstantiationException e) {
                Log.d(CONECTOR_NAME, e.toString());
            } catch (IllegalAccessException e) {
                Log.d(CONECTOR_NAME, e.toString());
            } catch (InvocationTargetException e) {
                Log.d(CONECTOR_NAME, e.toString());
            }
        } else {
            resultList = null;
        }
    }
    
    public Object call(String model, String method, Object...parameters) {
        /* This is a generic method to call any WS.
         * Each one of parameters Objects can be one primitive type,
         * object instance, array or List... depending on the WS called. */
        
        Object response = null;
        try {
            Vector<Object> paramsVector = new Vector<Object>(6);
            paramsVector.add(mDatabase);
            paramsVector.add(mId);
            paramsVector.add(mPassword);
            paramsVector.add(model);
            paramsVector.add(method);
            for (Object parameter : parameters) {
                paramsVector.add(parameter);
            }
            XMLRPCClient client = new XMLRPCClient(mUrl);
            response = client.call("execute", paramsVector);
        } catch (XMLRPCException e) {
            Log.d(CONECTOR_NAME, e.toString());
        }
        return response;
    }
    
    public String toString() {
        /* A String representation of the OpenErpConnection instance, good for
         * debugging purposes. You can comment the password if you want. */
        
        StringBuilder stringConn = new StringBuilder();
        stringConn.append("server: " + mServer + "\n");
        stringConn.append("port: " + mPort + "\n");
        stringConn.append("database: " + mDatabase + "\n");
        stringConn.append("user: " + mUser + "\n");
        stringConn.append("password: " + mPassword + "\n");
        stringConn.append("id: " + mId + "\n");
        return stringConn.toString();
    }
}
