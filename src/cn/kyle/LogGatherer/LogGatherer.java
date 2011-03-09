package cn.kyle.LogGatherer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LogGatherer extends Activity {
	Button btnStart = null;
	TextView tvTipTime = null;
	Handler handler = new Handler();
	public long startTime = 0;
	public File logFile = null;
	public boolean bUpdateTime = false;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btnStart = (Button)findViewById(R.id.btnStart);
        tvTipTime = (TextView)findViewById(R.id.tvTipTime);
        final TextView tvTip = (TextView)findViewById(R.id.tvTip); 
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
        	refreshDisplay();
        	btnStart.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					if (isRunning()){
						stop();
						btnStart.setText("开始收集");
						bUpdateTime = false;
					}else{
						btnStart.setText("停止收集");
						startTime = System.currentTimeMillis();
						bUpdateTime = true;
		        		setTipTime();
		        		start();
						tvTip.setText("日志文件：/sdcard/loggather.log."+startTime+" \n" +
								"收集开始于："+new java.util.Date().toString());
					}
				}
        	});
        }else{
        	AlertDialog ad = new AlertDialog.Builder(this)
        		.setTitle("提示").setMessage("SD卡不可用，程序即将退出")
        		.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						LogGatherer.this.finish();
					}
				}).create();
        	ad.show();
        	
        }
        
    }
    
    public void refreshDisplay(){
    	if (isRunning()){
    		btnStart.setText("停止收集");
    		bUpdateTime = true;
    		try {
				BufferedReader fr = new BufferedReader(new FileReader(getFlagFile()));
				startTime = Long.parseLong(fr.readLine());
				fr.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    		setTipTime();
    	}else{
    		btnStart.setText("开始收集");
    	}
    }
    
    public boolean isRunning(){
    	return new File(this.getFilesDir(),"flag").exists();
    }
    
	protected void onResume() {
		refreshDisplay();
		super.onResume();
	}

	protected void onPause() {
		this.bUpdateTime = false;
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		if (isRunning()){
			AlertDialog ad = new AlertDialog.Builder(this)
	    		.setTitle("提示").setMessage("如果要后台运行，按小房子键；如果要退出，请先停止收集")
	    		.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						
					}
				}).create();
	    	ad.show();
		}else
			super.onBackPressed();
		
	}

	public void setTipTime(){
    	new Thread(){
    		public void run() {
		    	while(bUpdateTime){
		    		handler.post(new Runnable(){
						public void run() {
							tvTipTime.setText("已经收集："+((System.currentTimeMillis()-startTime)/1000)+" 秒\n"+
									"目前文件大小："+
									(
									logFile==null?" 未知 ":
										logFile.length()/1024 < 1024 ? logFile.length()/1024 +" KB":
										(logFile.length()/1024/1024)+" MB"
									)
									);
						}
			    	});
		    		try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		    	}
    		}
    	}.start();
    }
    
    public boolean bStop = false;
    public File getFlagFile(){
    	return new File(this.getFilesDir(),"flag");
    }
    public void start(){
    	this.getFilesDir().mkdirs();
    	try {
    		new File(this.getFilesDir(),"flag").createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		final File flag = new File(this.getFilesDir(),"flag");
		bStop = false;
		new Thread(){
			public void run(){
				while(flag.exists()&&!bStop){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				bStop = true;
			}
		}.start();
		new Thread(){
			public void run(){
				try {
					Process p = Runtime.getRuntime().exec("logcat 2>&1");
					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line = null;
					logFile = new File("/sdcard/loggather.log."+startTime);
					FileWriter f1 = new FileWriter(getFlagFile());
					f1.write(""+startTime);
					f1.close();
					FileWriter fw = new FileWriter(logFile);
					int count = 0;
					while ( (line= br.readLine())!=null ){
						fw.write(line+"\n");
						count++;
						if (count>10){
							fw.flush();
							count=0;
						}
						if (bStop) break;
					}
					
					fw.flush();
					bStop = true;
					p.destroy();
					br.close();
					//追加系统信息
					fw.write("\n---System info ---------------------------");
					fw.write("\nBuild.MODEL="+Build.MODEL);
					fw.write("\nBuild.PRODUCT="+Build.PRODUCT);
					fw.write("\nBuild.MANUFACTURER="+Build.MANUFACTURER);
					fw.write("\nBuild.DEVICE="+Build.DEVICE);
					fw.write("\nBuild.DISPLAY="+Build.DISPLAY);
					fw.write("\nBuild.VERSION.CODENAME="+Build.VERSION.CODENAME);
					fw.write("\nBuild.VERSION.RELEASE="+Build.VERSION.RELEASE);
					fw.write("\nBuild.FINGERPRINT="+Build.FINGERPRINT);
					fw.write("\nBuild.CPU_ABI="+Build.CPU_ABI);
					fw.write("\nBuild.ID="+Build.ID);
					fw.write("\n---end -----------------------------------\n");
					//
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
    }
    
    public void stop(){
    	new File(this.getFilesDir(),"flag").delete();
    }
}