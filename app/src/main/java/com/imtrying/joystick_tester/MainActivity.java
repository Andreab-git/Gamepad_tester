package com.imtrying.joystick_tester;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final float SAFE_AREA_LEFT_KNOB  = (float) 0.010;
    public static final float SAFE_AREA_RIGHT_KNOB = (float) 0.010;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    msg("USB Ready");
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    msg("USB Permission not granted");
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    msg("No USB connected");
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    msg("USB disconnected");
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    msg("USB device not supported");
                    break;
            }
        }
    };

    private UsbService usbService;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    TextView axis_X, axis_Y, axis_RX, axis_RY, action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);


        axis_X  = (TextView) findViewById(R.id.textX);
        axis_Y  = (TextView) findViewById(R.id.textY);
        axis_RX = (TextView) findViewById(R.id.textRX);
        axis_RY = (TextView) findViewById(R.id.textRY);
        action  = (TextView) findViewById(R.id.action);
    }

    @SuppressLint("SetTextI18n")
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if ((motionEvent.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) == 0)
            return false;

        float axisRX = motionEvent.getAxisValue(MotionEvent.AXIS_RX); //Right Stick
        float axisRY = motionEvent.getAxisValue(MotionEvent.AXIS_RY); //Left Stick
        float axisX = motionEvent.getAxisValue(MotionEvent.AXIS_X); //Right Stick
        float axisY = motionEvent.getAxisValue(MotionEvent.AXIS_Y); //Right Stick

        if (axisRX  > -SAFE_AREA_LEFT_KNOB && axisRX <  SAFE_AREA_LEFT_KNOB) {
            axisRX = 0;
        }

        if (axisRY > -SAFE_AREA_RIGHT_KNOB && axisRY <  SAFE_AREA_RIGHT_KNOB) {
            axisRY = 0;
        }

        if (axisX > -SAFE_AREA_RIGHT_KNOB && axisX <  SAFE_AREA_RIGHT_KNOB) {
            axisX = 0;
        }

        if (axisY > -SAFE_AREA_RIGHT_KNOB && axisY <  SAFE_AREA_RIGHT_KNOB) {
            axisY = 0;
        }

        // Neutech - ARDUINO
        String RX = String.format("AXIS_RX : %.3f", -axisRX);
        String RY = String.format("AXIS_RY : %.3f", axisRY);
        String X = String.format("AXIS_X : %.3f", axisX);
        String Y = String.format("AXIS_Y : %.3f", -axisY);

        axis_RX.setText(RX);
        axis_RY.setText(RY);
        axis_X.setText(X);
        axis_Y.setText(Y);

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.END, 700, 30);
        toast.show();
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode==KeyEvent.KEYCODE_BACK)
            msg ("back press");

        switch(keyCode){
            case KeyEvent.KEYCODE_BACK:
                action.setText("KEYCODE_BACK");
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                action.setText("DPAD_UP");
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                action.setText("DPAD_DOWN");
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                action.setText("DPAD_LEFT");
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                action.setText("DPAD_RIGHT");
                return true;
            case KeyEvent.KEYCODE_BUTTON_L1:
                action.setText("BUTTON_L1");
                return true;
            case KeyEvent.KEYCODE_BUTTON_L2:
                action.setText("BUTTON_L2");
                return true;
            case KeyEvent.KEYCODE_BUTTON_R1:
                action.setText("BUTTON_R1");
                return true;
            case KeyEvent.KEYCODE_BUTTON_R2:
                action.setText("BUTTON_R2");
                return true;
            case KeyEvent.KEYCODE_BUTTON_Y:
                action.setText("BUTTON_Y DOWN");
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:
                action.setText("BUTTON_B DOWN");
                return true;
            case KeyEvent.KEYCODE_BUTTON_A:
                action.setText("BUTTON_A DOWN");
                return true;
            case KeyEvent.KEYCODE_BUTTON_X:
                action.setText("BUTTON_X DOWN");
                return true;
            case KeyEvent.KEYCODE_BUTTON_THUMBR:
                action.setText("BUTTON_THUMBL");
                return true;
             case KeyEvent.KEYCODE_BUTTON_1:
                action.setText("BUTTON 1");
                return true;
            case KeyEvent.KEYCODE_BUTTON_2:
                action.setText("BUTTON 2");
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @SuppressLint("SetTextI18n")
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode){
            case KeyEvent.KEYCODE_BUTTON_Y:
                action.setText("BUTTON_Y UP");
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:
                action.setText("BUTTON_B UP");
                return false;
            case KeyEvent.KEYCODE_BUTTON_A:
                action.setText("BUTTON_A UP");
                return false;
            case KeyEvent.KEYCODE_BUTTON_X:
                action.setText("BUTTON_X UP");
                return true;
            case KeyEvent.KEYCODE_BUTTON_R1:
                action.setText("BUTTON_R1 UP");
                return true;
            case KeyEvent.KEYCODE_BUTTON_L1:
                action.setText("BUTTON_L1 UP");
                return true;
            case KeyEvent.KEYCODE_BUTTON_THUMBR:
                action.setText("BUTTON_THUMBR UP");
                return true;
            case KeyEvent.KEYCODE_BUTTON_THUMBL:
                action.setText("BUTTON_THUMBL UP");
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    };


}