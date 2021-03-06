package com.redhorse.minihorse;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

//Need the following import to get access to the app resources, since this
//class is in a sub-package.

public class weibo extends Activity implements OnItemClickListener {

	private EditText weibocontent;
	private ImageView fileupload;
	private EditText username;
	private EditText password;
	private TextView wordcount;
	private String uploadfile;

	private String strUsername;
	private String strPassword;
	private SharedPreferences share;
	
	private TextWatcher watcher = new TextWatcher(){  
		  
        @Override  
        public void afterTextChanged(Editable s) {  
            // TODO Auto-generated method stub  
        	wordcount = (TextView) findViewById(R.id.tv_charcount);
        	wordcount.setText("可输入字数："+(140-s.length()));
        }  
  
        @Override  
        public void beforeTextChanged(CharSequence s, int start, int count,  
                int after) {  
            // TODO Auto-generated method stub  
              
        }  
  
        @Override  
        public void onTextChanged(CharSequence s, int start, int before,  
                int count) {  
            Log.d("TAG","[TextWatcher][onTextChanged]"+s);  
              
        }

    }; 
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.weibo);
		Button button = (Button) findViewById(R.id.btn_weibo_send);
		button.setOnClickListener(SendListener);
		button = (Button) findViewById(R.id.btn_weibo_quit);
		button.setOnClickListener(QuitListener);
		weibocontent = (EditText) findViewById(R.id.et_weibo);
		weibocontent.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View arg0, boolean isFocused) {
				if (isFocused == true) {
					weibocontent.selectAll();
				} else {
				}
			}
		});
		weibocontent.addTextChangedListener(watcher);
		username = (EditText) findViewById(R.id.et_weibo_username);
		username.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View arg0, boolean isFocused) {
				if (isFocused == true) {
					// username.selectAll();
				} else {
				}
			}
		});
		password = (EditText) findViewById(R.id.et_weibo_password);
		password.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View arg0, boolean isFocused) {
				if (isFocused == true) {
					// password.selectAll();
				} else {
				}
			}
		});
		
		share = this.getPreferences(MODE_PRIVATE);
		strUsername = share.getString("strUsername", "");
		strPassword = share.getString("strPassword", "");
		username.setText(strUsername);
		password.setText(strPassword);

		Bundle bundle = getIntent().getExtras();
		String title = bundle.getString("title");
		String url = bundle.getString("url");
		uploadfile = bundle.getString("uploadfile");
		fileupload = (ImageView) findViewById(R.id.et_weibo_filename);
		try {
			Drawable da = Drawable.createFromPath(uploadfile);
			if (da != null) {
				fileupload.setImageDrawable(da);
			} else {
			
			}
			} catch(Exception e) {
		}

		weibocontent.setText("\n" + url);

	}

	private OnClickListener QuitListener = new OnClickListener() {
		public void onClick(View v) {
			weibo.this.finish();
		}
	};

	private OnClickListener SendListener = new OnClickListener() {
		public void onClick(final View v) {
	        long l1 = System.currentTimeMillis();
	        weibocontent = (EditText) findViewById(R.id.et_weibo);
			username = (EditText) findViewById(R.id.et_weibo_username);
			password = (EditText) findViewById(R.id.et_weibo_password);
			
			if (username.getText().toString().equalsIgnoreCase("") || password.getText().toString().equalsIgnoreCase("")) {
            	Toast.makeText(v.getContext(), 
                		"请输入用户名和密码！", 
                        Toast.LENGTH_LONG) 
                     .show();
			}
			else if (weibocontent.length()>140) {
            	Toast.makeText(v.getContext(), 
                		"内容太长，请删除一些内容！", 
                        Toast.LENGTH_LONG) 
                     .show();
			}
			else {
				Editor edUsername = share.edit();
				edUsername.putString("strUsername", username.getText().toString());
				edUsername.commit();
				Editor edPassword = share.edit();
				edPassword.putString("strPassword", password.getText().toString());
				edPassword.commit();
	
				ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);  
	            NetworkInfo networkinfo = manager.getActiveNetworkInfo();  
	            if (networkinfo == null || !networkinfo.isAvailable()) {  
	                 // 当前网络不可用 你该干嘛干嘛
	            	Toast.makeText(v.getContext(), 
	                		"当前网络不可用, 请检查网络！", 
	                        Toast.LENGTH_LONG) 
	                     .show();
	            } else {
        			weiboTask task = new weiboTask();  
         			Log.e("weibo ", uploadfile);
        			task.execute(username.getText().toString(), password.getText().toString(), weibocontent.getText().toString(), v.getContext(), uploadfile);  
		        }
			}
		}
	};

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub

	}
}
