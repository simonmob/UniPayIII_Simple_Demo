package com.example.unipayiii_sdk_tutorial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.idtechproducts.device.*;
import com.idtechproducts.device.ReaderInfo.CAPTURE_ENCODE_TYPE;
import com.idtechproducts.device.ReaderInfo.CAPTURE_ENCRYPT_TYPE;
import com.idtechproducts.device.ReaderInfo.EVENT_MSR_Types;

public class MainActivity extends Activity implements  OnReceiverListener{

	// declaring the instance of the UniPayReader;
	private IDT_UniPayIII myUniPayReader = null;
	private TextView connectStatusTextView;  
	private TextView textLog;
	private TextView lcdLog;
	private Button btnGetFirmware;
	private Button btnStartSwipe;
	private Button btnCancelSwipe;
	private Button btnStartEMV;
	private Button btnCancelEMV;
    private Button btnConnect;
	private Handler handler = new Handler();
    private boolean isReaderConnected = false;
    private String info = "";
	private String detail = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		handler = new Handler();
        btnConnect=(Button)findViewById(R.id.btnconnect);
		btnStartEMV = (Button)findViewById(R.id.btn_StartEMV);
		btnCancelEMV = (Button)findViewById(R.id.btn_CancelEMV);
		btnGetFirmware = (Button)findViewById(R.id.btn_getFirmware);
		btnStartSwipe = (Button)findViewById(R.id.btn_startSwipe);
		btnCancelSwipe = (Button)findViewById(R.id.btn_cancelSwipe);
	    textLog = (TextView)findViewById(R.id.textLog);
	    lcdLog = (TextView)findViewById(R.id.lcdLog);
	    connectStatusTextView = (TextView)findViewById(R.id.status_text);

        if (myUniPayReader != null) {
            myUniPayReader.unregisterListen();
            myUniPayReader.release();
            myUniPayReader= null;
        }

		initializeReader();
//		if(myUniPayReader!=null){
//			myUniPayReader.unregisterListen();
//			myUniPayReader.release();
//			myUniPayReader = null;
//		}
//		myUniPayReader = new IDT_UniPayIII(this,this);
//		myUniPayReader.registerListen();
//		loadXMLfile();
//
//		runAutoConfig();

//		String filepath = getConfigurationFileFromRaw();
//		if(!isFileExist(filepath)) {
//			filepath = null;
//		}
//		if (ErrorCode.SUCCESS == myUniPayReader.autoConfig_start(filepath))
//			Toast.makeText(this, "AutoConfig started", Toast.LENGTH_SHORT).show();
//		else
//			Toast.makeText(this, "AutoConfig not started", Toast.LENGTH_SHORT).show();

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runAutoConfig();
            }
        });
		

		btnGetFirmware.setOnClickListener(new Button.OnClickListener(){  
	        public void onClick(View v) {
	        	info = "Getting Firmware\n";
	        	detail = "";
	        	handler.post(doUpdateStatus);
	        	StringBuilder sb = new StringBuilder();
	            int ret = myUniPayReader.device_getFirmwareVersion(sb);
	            if (ret == ErrorCode.SUCCESS) {
					info += "Firmware Version: " + sb.toString();
					detail = "";
					handler.post(doUpdateStatus);
				}
				else {
					info += "GetFirmwareVersion: Failed\n";
					info += "Status: "+ myUniPayReader.device_getResponseCodeString(ret)+"";
					detail = "";
					handler.post(doUpdateStatus);							
					}
	        }  
	    });
		
		btnStartEMV.setOnClickListener(new Button.OnClickListener(){  
	        public void onClick(View v) {;
	        detail = "";
	        info = "Starting EMV Transaction\n";
        	handler.post(doUpdateStatus);
	        	IDT_UniPayIII.emv_allowFallback(true);
				myUniPayReader.emv_startTransaction(1.00, 0.00, 0, 100, null, false);
	        }  
	    });
		
		btnCancelEMV.setOnClickListener(new Button.OnClickListener(){  
	        public void onClick(View v) {
	        	detail = "";
		        info = "Canceling EMV Transaction\n";
	        	handler.post(doUpdateStatus);
	        	ResDataStruct resData = new ResDataStruct();
				myUniPayReader.emv_cancelTransaction(resData);
	        }  
	    });
		
		btnStartSwipe.setOnClickListener(new Button.OnClickListener(){  
	        public void onClick(View v) {
	        	detail = "";
		        info = "Starting Swipe/Tap Transaction\n";
	        	handler.post(doUpdateStatus);
				myUniPayReader.msr_startMSRSwipe();
	        }  
	    });
		
		btnCancelSwipe.setOnClickListener(new Button.OnClickListener(){  
	        public void onClick(View v) {
	        	detail = "";
		        info = "Cancelling Swipe/Tap Transaction\n";
	        	handler.post(doUpdateStatus);
				myUniPayReader.msr_cancelMSRSwipe();
	        }  
	    });
		
	}


	public void initializeReader()
	{
		if (myUniPayReader != null){
			releaseSDK();
		}
		myUniPayReader = new IDT_UniPayIII(this, this);
		openReaderSelectDialog();

		String filepath = getXMLFileFromRaw();
		if(!isFileExist(filepath)) {
			filepath = null;
		}
		myUniPayReader.config_setXMLFileNameWithPath(filepath);
		myUniPayReader.config_loadingConfigurationXMLFile(true);
		myUniPayReader.log_setVerboseLoggingEnable(true);
		//fwTool = new FirmwareUpdateTool(this, context);
		displaySdkInfo();

	}
	public void releaseSDK() {
		if (myUniPayReader != null) {
			myUniPayReader.unregisterListen();
			myUniPayReader.release();
			myUniPayReader  = null;
		}
	}

	public void displaySdkInfo() {

		 info = 	"Manufacturer: " + android.os.Build.MANUFACTURER + "\n" +
				"Model: " + android.os.Build.MODEL + "\n" +
				"OS Version: " + android.os.Build.VERSION.RELEASE + " \n" +
				"SDK Version: \n" + myUniPayReader.config_getSDKVersion() + "\n";

		//Toast.makeText(getApplicationContext(),info,Toast.LENGTH_LONG).show();

		detail = "";

		handler.post(doUpdateStatus);
	}

	void openReaderSelectDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select a device:");
		builder.setCancelable(false);
		builder.setItems(R.array.reader_type, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				switch(which) {

					case 0:
						if (myUniPayReader.device_setDeviceType(ReaderInfo.DEVICE_TYPE.DEVICE_UNIPAY_III))
							Toast.makeText(getApplicationContext(), "UniPay III is selected", Toast.LENGTH_SHORT).show();
						else
							Toast.makeText(getApplicationContext(), "Failed. Please disconnect first.", Toast.LENGTH_SHORT).show();
						break;
					case 1:
						if (myUniPayReader.device_setDeviceType(ReaderInfo.DEVICE_TYPE.DEVICE_UNIPAY_III_USB))
							Toast.makeText(getApplicationContext(), "UniPay III (USB-HID) is selected", Toast.LENGTH_SHORT).show();
						else
							Toast.makeText(getApplicationContext(), "Failed. Please disconnect first.", Toast.LENGTH_SHORT).show();
						break;
					case 2:
						if (myUniPayReader.device_setDeviceType(ReaderInfo.DEVICE_TYPE.DEVICE_UNIPAY_III_BT))
						{
							Toast.makeText(getApplicationContext(), "UniPay III (Bluetooth) is selected", Toast.LENGTH_SHORT).show();
						}
						else
							Toast.makeText(getApplicationContext(), "Failed. Please disconnect first.", Toast.LENGTH_SHORT).show();
				}

				//device.setIDT_Device(fwTool);
				if (myUniPayReader.device_getDeviceType() == ReaderInfo.DEVICE_TYPE.DEVICE_UNIPAY_III_BT)
				{
//                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//                        Toast.makeText(context, "Bluetooth LE is not supported\r\n", Toast.LENGTH_LONG).show();
//                        return;
//                    }
//                    final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//                    mBtAdapter = bluetoothManager.getAdapter();
//                    if (mBtAdapter == null)
//                    {
//                        Toast.makeText(getActivity(), "Bluetooth LE is not available\r\n", Toast.LENGTH_LONG).show();
//                        return;
//                    }
//                    btleDeviceRegistered = false;
//                    if (!mBtAdapter.isEnabled()) {
//                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//                    } else {
//                        scanLeDevice(true, BLE_ScanTimeout);
//                    }
				} else {
					myUniPayReader.registerListen();
				}
			}
		});
		builder.create().show();
	}


	void runAutoConfig() {
		//config = null;
		String filepath = getXMLFileFromRaw();
		if(!isFileExist(filepath)) {
			filepath = null;
		}
		if (ErrorCode.SUCCESS == myUniPayReader.autoConfig_start(filepath))
			Toast.makeText(this, "AutoConfig started", Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(this, "AutoConfig not started", Toast.LENGTH_SHORT).show();
	}

	private String getXMLFileFromRaw(){
		//the target filename in the application path
		String fileName = "idt_unimagcfg_default.xml";

		try{
			InputStream in = getResources().openRawResource(R.raw.idt_unimagcfg_default);
			int length = in.available();
			byte [] buffer = new byte[length];
			in.read(buffer);
			in.close();
			this.deleteFile(fileName);
			FileOutputStream fout = this.openFileOutput(fileName, MODE_PRIVATE);
			fout.write(buffer);
			fout.close();

			// to refer to the application path
			File fileDir = this.getFilesDir();
			fileName = fileDir.getParent() + java.io.File.separator + fileDir.getName();
			fileName = fileName+java.io.File.separator+"idt_unimagcfg_default.xml";

		} catch(Exception e){
			e.printStackTrace();
			fileName = null;
		}
		return   fileName;
	}

	@Override
	public void ICCNotifyInfo(byte[] arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void LoadXMLConfigFailureInfo(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void autoConfigCompleted(StructConfigParameters structConfigParameters) {
        Toast.makeText(this, "AutoConfig found a working profile.", Toast.LENGTH_LONG).show();
        myUniPayReader.device_connectWithProfile(structConfigParameters);
        Log.i("CardTransactions","Autoconfig complete");
    }

    @Override
    public void autoConfigProgress(int progressValue) {
        info = "AutoConfig is running: "+progressValue +"%";
        handler.post(doUpdateStatus);
//        Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
//        Log.i("CardTransactions","Autoconfig running");
    }
	
	private Runnable doUpdateLabel = new Runnable()
	{
	    public void run()
	    {
	        if(!isReaderConnected){
	            connectStatusTextView.setText("UNIPAYIII DISCONNECTED");
	        }
	        else{
	            connectStatusTextView.setText("UNIPAYIII CONNECTED");
	        }
	    }
	};
	@Override
	public void deviceConnected() {
        isReaderConnected = true;
        //Log.i("CardTransactions","Connected");
        //Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
        if (!Common.getBootLoaderMode()) {
            String device_name = myUniPayReader.device_getDeviceType().toString();
            info = device_name.replace("DEVICE_", "");
            info += " Reader is connected\r\n";
            if (info.startsWith("UNIPAY_III_BT")) {
                info += "Address: " + "btleDeviceAddress";
            }

            detail = "";
            handler.post(doUpdateStatus);
            //Toast.makeText(context, info, Toast.LENGTH_LONG).show();
            //PutMessage(info);

            handler.post(doUpdateLabel);
        }
    }

    @Override
    public void onDestroy() {
        if (myUniPayReader != null)
            myUniPayReader.unregisterListen();
        super.onDestroy();
    }

	@Override
	public void deviceDisconnected() {
		isReaderConnected = false;
        info = ">>>>> Reader Disconnected.";
        handler.post(doUpdateStatus);
	    handler.post(doUpdateLabel);		
	}
	
	private void printTags(IDTEMVData emvData)
	{
	
	}
	
	@Override
	public void emvTransactionData(final IDTEMVData emvData) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainActivity.this,"Kubaf...." + String.valueOf(emvData.result),Toast.LENGTH_LONG).show();
			}
		});

		detail += Common.emvErrorCodes(emvData.result);
		detail += "\r\n";
		if (emvData.result == IDTEMVData.START_TRANS_SUCCESS)
			detail += "Start transaction response:\r\n";
		else if (emvData.result == IDTEMVData.GO_ONLINE)
			detail += "\r\nAuthentication response:\r\n";
		else 
			detail += "\r\nComplete Transaction response:\r\n";
		if (emvData.unencryptedTags != null && !emvData.unencryptedTags.isEmpty())
		{
			detail += "Unencrypted Tags:\r\n";
			Set<String> keys = emvData.unencryptedTags.keySet();
			for(String key: keys){
				detail += key + ": ";
				byte[] data = emvData.unencryptedTags.get(key);
				detail += Common.getHexStringFromBytes(data) + "\r\n";
			}
		}
		if (emvData.maskedTags != null && !emvData.maskedTags.isEmpty())
		{
			detail += "Masked Tags:\r\n";
			Set<String> keys = emvData.maskedTags.keySet();
			for(String key: keys){
				detail += key + ": ";
				byte[] data = emvData.maskedTags.get(key);
				detail += Common.getHexStringFromBytes(data) + "\r\n";
			}
		}
		if (emvData.encryptedTags != null && !emvData.encryptedTags.isEmpty())
		{
			detail += "Encrypted Tags:\r\n";
			Set<String> keys = emvData.encryptedTags.keySet();
			for(String key: keys){
				detail += key + ": ";
				byte[] data = emvData.encryptedTags.get(key);
				detail += Common.getHexStringFromBytes(data) + "\r\n";
			}
		}
		handler.post(doUpdateStatus);
		 if (emvData.result == IDTEMVData.GO_ONLINE){
			//Auto Complete
			 byte[] response = new byte[]{0x30,0x30};
			 myUniPayReader.emv_completeTransaction(false, response, null, null,null);
		 }
		 else if (emvData.result == IDTEMVData.START_TRANS_SUCCESS){  
				//Auto Authenticate
			 myUniPayReader.emv_authenticateTransaction(null);
			}
	
	}

	public void lcdDisplay(int mode, String[] lines, int timeout) {
		
				if (mode == 0x01) //Menu Display
				{
					//automatically select 1st application
					myUniPayReader.emv_lcdControlResponse((byte)mode, (byte)0x01);
				}
				else if (mode == 0x08) //Language Menu Display
				{
					//automatically select first language
					myUniPayReader.emv_lcdControlResponse((byte)mode, (byte)0x01);
				}
				else{
					ResDataStruct toData = new ResDataStruct();
					info = lines[0];
					handler.post(doUpdateStatus);
				}			
	}


	@Override
	public void msgAudioVolumeAjustFailed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void msgRKICompleted(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void msgToConnectDevice() {
		// TODO Auto-generated method stub
		
	}

	private Runnable doUpdateStatus = new Runnable()
	{
		public void run()
		{		
			lcdLog.setText(info);
			textLog.setText(detail);
		}
	};
	@Override
	public void swipeMSRData(IDTMSRData card) {
		if (card.cardData[0] != (byte)0x01 && card.track1Length == 0 && card.track2Length == 0 && card.track3Length == 0)
			info = "Swipe/Tap data didn't read correctly";
		else
			info = "Swipe/Tap Read Successfully";
		detail = Common.parse_MSRData(myUniPayReader.device_getDeviceType(), card);
		handler.post(doUpdateStatus);
		
	}

	@Override
	public void timeout(int arg0) {
		// TODO Auto-generated method stub
        info = ErrorCodeInfo.getErrorCodeDescription(arg0);
        handler.post(doUpdateStatus);
	}
	
	private String getXMLFileFromRaw(String fileName ,int res){
		//the target filename in the application path
		String fileNameWithPath = null;
		fileNameWithPath = fileName;
		String newFilename = fileName;
	
		try {
			InputStream in = getResources().openRawResource(res);
			int length = in.available();
			byte [] buffer = new byte[length];
			in.read(buffer);    	   
			in.close();
			deleteFile(fileNameWithPath);
			FileOutputStream fout = openFileOutput(fileNameWithPath, MODE_PRIVATE);
			fout.write(buffer);
			fout.close();
    	   
			// to refer to the application path
			File fileDir = this.getFilesDir();
			fileNameWithPath = fileDir.getParent() + java.io.File.separator + fileDir.getName();
			fileNameWithPath += java.io.File.separator+newFilename;
	   	   
		} catch(Exception e){
			e.printStackTrace();
			fileNameWithPath = null;
		}
		return fileNameWithPath;
	}
	
	private String getConfigurationFileFromRaw( ){
		return getXMLFileFromRaw("idt_unimagcfg_default.xml",R.raw.idt_unimagcfg_default);
		}
	private boolean isFileExist(String path) {
    	if(path==null)
    		return false;
	    File file = new File(path);
	    if (!file.exists()) {
	      return false ;
	    }
	    return true;
    }
	private void loadXMLfile(){
		
	    //load the XML configuratin file
	    //String fileNameWithPath = getConfigurationFileFromRaw();
		String fileNameWithPath=getXMLFileFromRaw();
	    if(!isFileExist(fileNameWithPath)) { 
	    	fileNameWithPath = null; 
	    }        	
	    /////////////////////////////////////////////////////////////////////////////////
		// Network operation is prohibited in the UI Thread if target API is 11 or above.
		// If target API is 11 or above, please use AsyncTask to avoid errors.
	    myUniPayReader.config_setXMLFileNameWithPath(fileNameWithPath);
	    Log.d("Demo Info >>>>>","loadingConfigurationXMLFile begin.");
	    myUniPayReader.config_loadingConfigurationXMLFile(true);
		myUniPayReader.log_setVerboseLoggingEnable(true);
	    /////////////////////////////////////////////////////////////////////////////////
	}

	@Override
	public void dataInOutMonitor(byte[] arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void msgBatteryLow() {
		// TODO Auto-generated method stub
		
	}
	
	
	

	
	
}
