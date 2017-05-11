package com.example.acitvity;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;

import java.util.HashMap;

/**
 * Created by 49005 on 2017/5/3.
 */

public class Detial extends ActionBarActivity {
        private String testData="START 35.53 28.91 24.75 17.88 100.12 12.09 21.00 38.00 37.793 316.073 END";
        AnaylizeString anaylizeString=null;
        Thread thread;
        Simplelinechart detialActivity=null;
        Handler handler;
        @Override
        protected void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.detial);
            handler=new Handler();
            detialActivity = (Simplelinechart) findViewById(R.id.detial);
            String[] xItem = {"NO2","CO","CO2","O3","SO2","tep","wet","PM25"};
            String[] yItem = {"10","20","30","40","50","60","70","80","90","100","110"};
            detialActivity.setXItem(xItem);
            detialActivity.setYItem(yItem);
            anaylizeString=new AnaylizeString();
            thread=new Thread(backWork);
            thread.start();
        }
        private Runnable backWork=new Runnable() {
            @Override
            public void run() {
                //解析初始值
                anaylizeString.Analize(testData);
                //更新ui
                UpDateUI();
            }

        private void UpDateUI() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    HashMap<Integer,Integer> pointMap = new HashMap();
                    pointMap.put(0,(int)(anaylizeString.NO2/10));
                    pointMap.put(1,(int)(anaylizeString.CO/10));
                    pointMap.put(2,(int)(anaylizeString.CO2/10));
                    pointMap.put(3,(int)(anaylizeString.O3/10));
                    pointMap.put(4,(int)(anaylizeString.SO2/10));
                    pointMap.put(5,(int)(anaylizeString.tep/10));
                    pointMap.put(6,(int)(anaylizeString.wet/10));
                    pointMap.put(7,(int)(anaylizeString.PM25/10));
                    detialActivity.setData(pointMap);
                }
            });


        }
    };
}
