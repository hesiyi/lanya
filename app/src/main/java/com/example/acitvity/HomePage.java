package com.example.acitvity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.services.weather.LocalWeatherForecastResult;
import com.amap.api.services.weather.LocalWeatherLive;
import com.amap.api.services.weather.LocalWeatherLiveResult;
import com.amap.api.services.weather.WeatherSearch;
import com.amap.api.services.weather.WeatherSearchQuery;

import static com.example.acitvity.R.id.txtAQI;


public class HomePage extends ActionBarActivity implements AMapLocationListener, WeatherSearch.OnWeatherSearchListener {
    //声明AMapLocationClient类对象，定位发起端
    private AMapLocationClient mLocationClient = null;
    //声明mLocationOption对象，定位参数
    public AMapLocationClientOption mLocationOption = null;
    //标识，用于判断是否只显示一次定位信息和用户重新定位
    public AMapLocation aMapLocation;
    //显示城市信息
    public TextView cityview;
    public TextView temperature;
    public TextView wind;
    //天气
    WeatherSearchQuery mquery =null;
    WeatherSearch   mweathersearch=null;
    LocalWeatherLive weatherlive=null;
    Detial detial=null;

    private Thread thread;
    private Handler handler;
    private Button Refrash;
    private Button btnShare;
    private Button btndetial;
    private TextView ResultTxt;
    private TextView weather;
    MyDatabaseHelper dbHelper=null;
    SQLiteDatabase sqldb=null;
    //test data  显示页面的初始测试值
    protected static String  testData="START 35.53 28.91 24.75 17.88 100.12 12.09 21.00 38.00 37.793 316.073 END";
    //数据存储
//    private String []StoreData=
//            {"START 357.53 234.91 24.75 17.88 100.12 12.09 21.00 38.00 37.793 316.073 END",
//            "START 56.53 442.91 99.75 34.88 100.12 56.09 21.00 34.00 24.793 316.073 END",
//            "START 79.53 167.91 56.75 17.88 45.12 12.09 55.00 35.00 45.793 316.073 END",
//            "START 188.53 283.91 34.75 356.88 134.12 23.09 44.00 38.00 37.793 316.073 END",
//            "START 63.53 42.91 34.75 17.88 100.12 45.09 77.00 38.00 37.793 316.073 END"};
    //进度条
    public ProgressBar btn_NO,btn_CO,btn_CO2,btn_O3,btn_SO2,btn_temperature,btn_wet,btn_PM25;
    private AnaylizeString anaylizeString;
    public TextView vNO,vCO,vCO2,vO3,vSO2,vTem,vWet,vPM25,vAQI;
    String AQI=null;

    private TextView testView;
    private int flag;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);
        cityview=(TextView)findViewById(R.id.cityview);
        weather=(TextView) findViewById(R.id.weather);
        wind=(TextView) findViewById(R.id.wind);
        locationInit();

        Refrash=(Button)findViewById(R.id.btnRefrash);
        btnShare=(Button) findViewById(R.id.btnShare);
        btndetial=(Button) findViewById(R.id.detial);
        handler=new Handler();
        Init();
        flag=0;

        mquery=new WeatherSearchQuery("杭州",WeatherSearchQuery.WEATHER_TYPE_LIVE);
        mweathersearch=new WeatherSearch(this);
        mweathersearch.setOnWeatherSearchListener(this);
        mweathersearch.setQuery(mquery);
        mweathersearch.searchWeatherAsyn(); //异步搜索

        anaylizeString=new AnaylizeString();
        //更新ui的线程
        thread=new Thread(backWork);
        thread.start();
        dbHelper=new MyDatabaseHelper(this,"test.db",1);
        sqldb=dbHelper.getReadableDatabase();
        AQI=vAQI.getText().toString();
        //刷新跳转功能
        Refrash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    save(sqldb, testData,AQI);
                    Log.e("HomePage", "保存数据");
                }catch (Exception e){
                    e.printStackTrace();
                }finally {

                    Intent intent = new Intent(HomePage.this, BluetoothReceive.class);
                    startActivityForResult(intent, 1);
                }
            }
        });
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, "数据包："+testData+"\nAQI："+anaylizeString.AQI_Final);
                shareIntent.setType("text/plain");
                //设置分享列表的标题，并且每次都显示分享列表
                startActivity(Intent.createChooser(shareIntent, "分享到"));
            }
        });
        btndetial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(HomePage.this,Detial.class);
                startActivity(intent);
            }
        });
    }

    /**
     * 定位的主要初始化函数
     */
    private void locationInit() {
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置返回地址信息，默认为true
        mLocationOption.setNeedAddress(true);
        //设置定位监听
        mLocationClient.setLocationListener(this);
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为1000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        //启动定位
        mLocationClient.startLocation();
        //天气初始化

    }
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                StringBuilder sb=new StringBuilder();
                sb.append(aMapLocation.getCity());
                cityview.setText(sb.toString());
            }
        } else {
            //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
            Log.e("AmapError", "location Error, ErrCode:"
                    + aMapLocation.getErrorCode() + ", errInfo:"
                    + aMapLocation.getErrorInfo());
            Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_LONG).show();
        }
        mLocationClient.stopLocation();
    }

    /**
     * 高德天气
     * @param weatherLiveResult
     * @param rCode
     */
    @Override
    public void onWeatherLiveSearched(LocalWeatherLiveResult weatherLiveResult, int rCode) {
        if (rCode == 1000) {
            if (weatherLiveResult != null && weatherLiveResult.getLiveResult() != null) {
                weatherlive = weatherLiveResult.getLiveResult();
//                reporttime1.setText(weatherlive.getReportTime()+"发布");
                weather.setText(weatherlive.getWeather());
                wind.setText(weatherlive.getWindDirection()+"风");
//                humidity.setText("湿度         "+weatherlive.getHumidity()+"%");
            }else {
                Toast.makeText(HomePage.this, "",Toast.LENGTH_LONG).show();
            }
        }else {
            Toast.makeText(HomePage.this,rCode,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onWeatherForecastSearched(LocalWeatherForecastResult localWeatherForecastResult, int i) {

    }


    //调用startActitivyForResult之后立刻调用onActivityResult,在BluetoothReceive销毁之前的信息
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(resultCode==RESULT_OK)   
        {
            Uri uridata=data.getData();
            String resultdata=uridata.toString();
            String[] numdata=resultdata.split(" ");
            double num=0;
            for (int i = 1; i <numdata.length-2 ; i++) {
                num+=Double.valueOf(numdata[i]);
            }
            ResultTxt.setText("总和："+num+"ppm");
            testData=uridata.toString();
            //EnterBtn.setVisibility(View.VISIBLE);

            //如果线程死了，重新开启线程
            if (!thread.isAlive()) {
                thread = new Thread(backWork);
                thread.start();
            }

        }
    }


    protected Runnable backWork=new Runnable() {
        @Override
        public void run() {
            //解析初始值
            anaylizeString.Analize(testData);
            //更新ui
            UpDateUI();
        }
    };

    private void UpDateUI()
    {
        handler.post(new Runnable() {
            @Override
            public void run() {

                int MAX=(int)anaylizeString.MAXC+10;

                btn_CO.setMax(MAX);
                btn_CO2.setMax(MAX);
                btn_NO.setMax(MAX);
                btn_SO2.setMax(MAX);
                btn_O3.setMax(MAX);
                btn_temperature.setMax(MAX);
                btn_wet.setMax(MAX);
                btn_PM25.setMax(MAX);

                //设置显示的进度
                btn_CO.setProgress((int) anaylizeString.CO);
                btn_CO2.setProgress((int) anaylizeString.CO2);
                btn_NO.setProgress((int) anaylizeString.NO2);
                btn_SO2.setProgress((int) anaylizeString.SO2);
                btn_O3.setProgress((int) anaylizeString.O3);
                btn_temperature.setProgress((int) anaylizeString.tep);
                btn_wet.setProgress((int) anaylizeString.wet);
                btn_PM25.setProgress((int) anaylizeString.PM25);


                vCO.setText(String.valueOf((int) anaylizeString.CO));
                vCO2.setText(String.valueOf((int) anaylizeString.CO2));
                vNO.setText(String.valueOf((int) anaylizeString.NO2));
                vSO2.setText(String.valueOf((int) anaylizeString.SO2));
                vO3.setText(String.valueOf((int) anaylizeString.O3));
                vTem.setText(String.valueOf((int) anaylizeString.tep));
                vWet.setText(String.valueOf((int) anaylizeString.wet));
                vPM25.setText(String.valueOf((int) anaylizeString.PM25));

                vAQI.setText(String.valueOf((int) anaylizeString.AQI_Final));

                //设置温度的颜色
                int tempAQI = (int) anaylizeString.AQI_Final;
                if (0 < tempAQI && tempAQI <= 100)
                    vAQI.setTextColor(0xFF0DFF41);
                else if (100 < tempAQI && tempAQI <= 200)
                    vAQI.setTextColor(0xFFA4FF27);

                else if (200 < tempAQI && tempAQI <= 300)
                    vAQI.setTextColor(0xFFFFA312);

                else if (300 < tempAQI && tempAQI <= 400)
                    vAQI.setTextColor(0xFFFF4B08);

                else if (400 < tempAQI && tempAQI <= 500)
                    vAQI.setTextColor(0xFFFF0001);
                else
                    vAQI.setTextColor(0xFFFF0001);
            }
        });


    }
    public void save(SQLiteDatabase db,String testData,String AQI){
        db.execSQL("insert into test values(?,?)",new String[] {testData,AQI});
        Toast.makeText(HomePage.this,"插入数据库成功",Toast.LENGTH_LONG).show();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //退出程序时,关闭sqlliteDatabase
        if(dbHelper!=null){
            dbHelper.close();
        }
    }

    private void Init()
    {
        btn_NO = (ProgressBar) findViewById(R.id.seekBar);
        btn_CO = (ProgressBar) findViewById(R.id.seekBar2);
        btn_CO2 = (ProgressBar) findViewById(R.id.seekBar3);
        btn_O3 = (ProgressBar) findViewById(R.id.seekBar4);
        btn_SO2 = (ProgressBar) findViewById(R.id.seekBar5);
        btn_temperature = (ProgressBar) findViewById(R.id.seekBar6);
        btn_wet = (ProgressBar) findViewById(R.id.seekBar7);
        btn_PM25 = (ProgressBar) findViewById(R.id.seekBar8);

        vCO = (TextView) findViewById(R.id.valueCO);
        vCO2 = (TextView) findViewById(R.id.valueCO2);
        vNO = (TextView) findViewById(R.id.valueNO2);
        vO3 = (TextView) findViewById(R.id.valueO3);
        vTem = (TextView) findViewById(R.id.valueTem);
        vWet = (TextView) findViewById(R.id.valueWet);
        vSO2 = (TextView) findViewById(R.id.valueSO2);
        vPM25 = (TextView) findViewById(R.id.valuePM25);

        vAQI = (TextView) findViewById(txtAQI);

        ResultTxt=(TextView)findViewById(R.id.result_text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }else if(id==R.id.share){
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "AQI的值："+testData);
            shareIntent.setType("text/plain");
            //设置分享列表的标题，并且每次都显示分享列表
            startActivity(Intent.createChooser(shareIntent, "分享到"));
        }

        return super.onOptionsItemSelected(item);
    }
}