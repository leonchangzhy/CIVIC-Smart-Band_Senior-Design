package leon.civicv3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BleActivity extends AppCompatActivity {
    private Handler mHandler;
    private static final String TAG = "BLE";

    private static final String BLE_THERMOMETER_SERVICE_UUID = UUIDUtil.UUID_16bit_128bit("8453", true);
    private static final UUID[] TemperServiceUuids = {UUID.fromString(BLE_THERMOMETER_SERVICE_UUID)};
    private static final String All_Char_UUID = UUIDUtil.UUID_16bit_128bit("4B31", true);
    private static final String GPS_Char_UUID = UUIDUtil.UUID_16bit_128bit("4B32", true);

    private BluetoothAdapter mBtAdapter = null;
    private BluetoothManager mBtManager = null;
    private BluetoothDevice mDevice;

    private BluetoothGatt gatt;
    DatabaseHelper SensorDb;
    DatabaseHelperGPS GPSDb;

    private LineGraphSeries<DataPoint> series1;
    private LineGraphSeries<DataPoint> series2;




    private BluetoothGattCharacteristic gpsCharacteristic;
    private BluetoothGattCharacteristic allCharacteristic;





    private String DeviceName;
    private String DeviceMacAddr;

    private String showTempStr = "";
    private String HR;
    private String Steps;
    private String Temp;
    private String Battery;

    private String GPSLat;
    private String GPSLong;
    private String GPSHour;
    private String GPSMin;
    private String GPSTime;

    public Double GPSLatdoub;
    public Double GPSLongdoub;





    public Integer HRint;
    public Integer Stepsint;
    public Double Tempdoub;

    @BindView(R.id.scanBtn)
    Button scanBtn;
    @BindView(R.id.connBtn)
    Button connBtn;
    @BindView(R.id.readBtn)
    Button readBtn;
    @BindView(R.id.readGPSBtn)
    Button readGPSBtn;
    @BindView(R.id.ViewSensorBtn)
    Button ViewSensorBtn;
    @BindView(R.id.ViewGPSBtn)
    Button ViewGPSBtn;


    @BindView(R.id.actionBtn)
    LinearLayout actionBtn;
    @BindView(R.id.DatabaseBtn)
    LinearLayout DatabaseBtn;

    @BindView(R.id.deviceName)
    TextView deviceName;
    @BindView(R.id.connState)
    TextView connState;
    @BindView(R.id.temperTv)
    TextView temperTv;
    @BindView(R.id.displayZone)
    LinearLayout displayZone;
    @BindView(R.id.activity_ble)
    RelativeLayout activityBle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        ButterKnife.bind(this);


        SensorDb = new DatabaseHelper(this);
        GPSDb = new DatabaseHelperGPS(this);

        mHandler = new Handler();

        mBtManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBtManager.getAdapter();

        int HRintPlot,idplot;
        double tempdouplot;



        GraphView graph = (GraphView)findViewById(R.id.graph);
        GraphView graphTemp = (GraphView)findViewById(R.id.graphTemp);
        series1 = new LineGraphSeries<>();
        series2 = new LineGraphSeries<>();

        Cursor res2 = SensorDb.getAllData();
        if(res2.getCount() == 0) {
            // show message
            return;
        }

        while (res2.moveToNext()) {
            HRintPlot =  res2.getInt(1);
            tempdouplot = res2.getDouble(3);
            idplot = res2.getInt(0);
            series1.appendData(new DataPoint(idplot,HRintPlot),true,100);
            series2.appendData(new DataPoint(idplot,tempdouplot),true,100);

        }
        series1.setTitle("Heart Rate");
        graph.addSeries(series1);
        graph.getViewport().setScrollable(true);
        graph.getLegendRenderer().setVisible(true);
        graph.getViewport().setXAxisBoundsManual(true);

        series2.setTitle("Temperature");
        series2.setColor(Color.RED);
        graphTemp.addSeries(series2);
        graphTemp.getViewport().setScrollable(true);
        graphTemp.getLegendRenderer().setVisible(true);
        graphTemp.getViewport().setXAxisBoundsManual(true);


    }

    @OnClick({R.id.scanBtn, R.id.connBtn, R.id.readBtn, R.id.readGPSBtn,R.id.ViewSensorBtn,R.id.ViewGPSBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scanBtn:
                if (mBtAdapter.isEnabled())
                    mBtAdapter.startLeScan(TemperServiceUuids, mLeScanCallback);
                else
                    Toast.makeText(this, "Please turn on Bluetooth!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.connBtn:
                gatt = mDevice.connectGatt(this, false, mGattCallback);
                break;
            case R.id.readBtn:
                gatt.readCharacteristic(allCharacteristic);
                break;
            case R.id.readGPSBtn:
                gatt.readCharacteristic(gpsCharacteristic);
                break;
            case R.id.ViewSensorBtn:
                Cursor res = SensorDb.getAllData();
                if(res.getCount() == 0) {
                    // show message
                    showMessage("Empty","Nothing found");
                    return;
                }

                StringBuffer buffer = new StringBuffer();
                while (res.moveToNext()) {
                    buffer.append("Id :"+ res.getString(0)+"\n");
                    buffer.append("Heart Rate :"+ res.getString(1)+"bpm\n");
                    buffer.append("Steps :"+ res.getString(2)+"\n");
                    buffer.append("Temperature :"+ res.getString(3)+"ºC\n\n");
                }

                // Show all data
                showMessage("Data",buffer.toString());
                break;
            case R.id.ViewGPSBtn:
                Cursor resgps = GPSDb.getAllGPS();
                if(resgps.getCount() == 0) {
                    // show message
                    showMessage("Empty","Nothing found");
                    return;
                }

                StringBuffer buffergps = new StringBuffer();
                while (resgps.moveToNext()) {
                    buffergps.append("Time :"+ resgps.getString(3)+"\n");
                    buffergps.append("Latitude :"+ resgps.getString(1)+"\n");
                    buffergps.append("Longitude :"+ resgps.getString(2)+"\n\n");
                }

                // Show all data
                showMessage("Data",buffergps.toString());
                break;

        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            mDevice = device;
            DeviceName = device.getName();
            DeviceMacAddr = device.getAddress();

            // 子线程不能操作 UI！
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    deviceName.setText("Device Name: " + DeviceName + ", MAC Address: " + DeviceMacAddr);
                    connBtn.setEnabled(true);
                    connState.setText("Disconnected");
                }
            });

            // 已扫描到 device! 停止扫描!
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Looper.prepare();
                Toast.makeText(BleActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();
                Looper.loop();
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 子线程不能操作 UI！
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        connState.setText("Connected");
                        connBtn.setEnabled(false);
                        showTempStr = "";

                        gatt.discoverServices();
                    }
                });

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 子线程不能操作 UI！
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        connState.setText("Disconnected");
                        connBtn.setEnabled(true);
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            List<BluetoothGattService> bleGattServiceList = gatt.getServices();
            for (BluetoothGattService bleGattService : bleGattServiceList) {
                Log.i(TAG, "发现服务: " + bleGattService.getUuid());

                List<BluetoothGattCharacteristic> bleGattCharacteristicList = bleGattService.getCharacteristics();
                for (BluetoothGattCharacteristic bleGattCharacteristic : bleGattCharacteristicList) {
                    Log.i(TAG, "发现服务有以下特征: " + bleGattCharacteristic.getUuid());
                    if (All_Char_UUID.equals(bleGattCharacteristic.getUuid().toString())) {
                        allCharacteristic = bleGattCharacteristic;
                        Log.i(TAG, "onServicesDiscovered: " + allCharacteristic);
                    }
                    if (GPS_Char_UUID.equals(bleGattCharacteristic.getUuid().toString())) {
                        gpsCharacteristic = bleGattCharacteristic;
                        Log.i(TAG, "onServicesDiscovered: " + gpsCharacteristic);
                    }
                }
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    readBtn.setEnabled(true);
                    readGPSBtn.setEnabled(true);
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (All_Char_UUID.equals(characteristic.getUuid().toString())) {
                byte[] allVal = characteristic.getValue();
                handleAll(allVal);
            }else if(GPS_Char_UUID.equals(characteristic.getUuid().toString())){
                byte [] gpsVal = characteristic.getValue();
                handleGPS(gpsVal);
            }




        }
    };

    private void handleAll(byte[] val){
        double temperature = getCharTempValue(val);
        int Heartrate = getCharHRValue(val);
        int Step = getCharStepValue(val);
        int Batt = getCharBattValue(val);


        Tempdoub = temperature;
        HRint = Heartrate;
        Stepsint = Step;

        Temp = Double.toString(temperature);
        HR = Integer.toString(Heartrate);
        Steps = Integer.toString(Step);
        Battery = Integer.toString(Batt);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                showTempStr += "Temperature:"+Temp+"ºC\n"+"HR:"+HR+"bpm\n"+"Step:"+Steps+"steps\n"+"Battery:"+Battery+"%\n\n";
                temperTv.setText(showTempStr);
                AddData();
            }
        });

    }

    private void handleGPS(byte[] val){
        double gpslat = getCharLatValue(val);
        double gpslong = getCharLongValue(val);
        int gpshour = getCharHourValue(val);
        int gpsmin = getCharMinValue(val);


        GPSLat = String.format("%.6f",gpslat);
        GPSLong = String.format("%.6f",gpslong);
        GPSHour = Integer.toString(gpshour);
        GPSMin = Integer.toString(gpsmin);
        GPSTime = GPSHour + " : " + GPSMin;

        GPSLatdoub = Double.parseDouble(GPSLat);
        GPSLongdoub = Double.parseDouble(GPSLong);



        mHandler.post(new Runnable() {
            @Override
            public void run() {
                showTempStr += "At "+ GPSTime+"\n" + "Latitude:"+GPSLat+"\n"+"Longitude:"+GPSLong+"\n\n";
                temperTv.setText(showTempStr);
                AddGPS();

            }
        });

    }


////////////////////////////////////////////////////////////////////////////

    private double getCharTempValue(byte [] val){
        byte Thermometer_low = val[0];
        double low = Thermometer_low & 0xff;

        return low;
    }

    private int getCharHRValue(byte [] val){
        byte HR_low = val[1];
        int low = HR_low & 0xff;

        return low;
    }

    private int getCharBattValue(byte [] val){
        byte Batt_low = val[2];
        int low = Batt_low & 0xff;

        return low;
    }

    private int getCharStepValue(byte [] val){
        byte Step_low = val[4];
        int low = Step_low & 0xff;
        byte Step_high = val[3];
        int high = Step_high & 0xff;

        return high * 256 + low;
    }

    private double getCharLatValue(byte [] val){
        byte GPSLat_high = val[0];
        double high = GPSLat_high & 0xff;

        byte GPSLat_low1 = val[1];
        double low1 = GPSLat_low1 & 0xff;

        byte GPSLat_low2 = val[2];
        double low2 = GPSLat_low2 & 0xff;

        byte GPSLat_low3 = val[3];
        double low3 = GPSLat_low3 & 0xff;

        byte GPSLat_NS = val[4];
        int ns = GPSLat_NS & 0xff;

        double LatMag = high + (low1 + low2 * 0.01 + low3 * 0.0001)/60;


       if (ns == 0){
           LatMag = LatMag * (1);
       }else if(ns == 1){
           LatMag = LatMag *(-1);
       }else{
           LatMag = 0.0;
       }
       return LatMag;
    }

    private double getCharLongValue(byte [] val){
        byte GPSLong_high = val[5];
        double high = GPSLong_high & 0xff;

        byte GPSLong_low1 = val[6];
        double low1 = GPSLong_low1 & 0xff;

        byte GPSLong_low2 = val[7];
        double low2 = GPSLong_low2 & 0xff;

        byte GPSLong_low3 = val[8];
        double low3 = GPSLong_low3 & 0xff;

        byte GPSLong_WE = val[9];
        int we = GPSLong_WE & 0xff;

        double LongMag = high + (low1 + low2 * 0.01 + low3 * 0.0001)/60;


        if (we == 0){
            LongMag = LongMag * (1);
        }else if(we == 1){
            LongMag = LongMag *(-1);
        }else{
            LongMag = 0.0;
        }
        return LongMag;
    }

    private int getCharHourValue(byte [] val){
        byte GPSHour = val[10];
        int Hour = GPSHour & 0xff;

        if(Hour >= 5){
            Hour = Hour - 5;
        }else if (Hour < 5){
            Hour = Hour - 5 + 24;
        }

        return Hour;
    }

    private int getCharMinValue(byte [] val) {
        byte GPSMin = val[11];
        int Min = GPSMin & 0xff;

        return Min;
    }



//////////////////////////////////////////////////////////////////////////////////

    public  void AddData() {

        boolean isInserted = SensorDb.insertData(HRint,
                Stepsint,
                Tempdoub );
        if(isInserted == true)
            Toast.makeText(BleActivity.this,"Data Inserted",Toast.LENGTH_LONG).show();
        else
            Toast.makeText(BleActivity.this,"Data not Inserted",Toast.LENGTH_LONG).show();

    }

    public  void AddGPS() {

        boolean isInserted = GPSDb.insertGPS(GPSLatdoub,
                GPSLongdoub,GPSTime);
        if(isInserted == true)
            Toast.makeText(BleActivity.this,"Data Inserted",Toast.LENGTH_LONG).show();
        else
            Toast.makeText(BleActivity.this,"Data not Inserted",Toast.LENGTH_LONG).show();

    }

    public void showMessage(String title,String Message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }



}

