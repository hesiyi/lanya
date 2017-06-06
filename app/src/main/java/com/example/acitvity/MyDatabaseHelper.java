package com.example.acitvity;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 49005 on 2017/2/21.
 * 数据库
 */

public class MyDatabaseHelper extends SQLiteOpenHelper {

    public static final String CREATE_TABLE_SQL= "create table test(testData ,AQI)";

    //带两个参数的构造函数，调用的其实是带三个参数的构造函数
    public MyDatabaseHelper(Context context,String name,int version)
    {
        super(context,name,null,version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        System.out.println(""+oldVersion+"---->"+newVersion);
    }
}
