package com.redhorse.minihorse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import com.yy.ah.util.HttpRequestParser;
import com.yy.ah.util.HttpRequestParser.Request;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CacheManager;
import android.webkit.CacheManager.CacheResult;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class minihorse extends Activity {

	private dbConfigKeyValueHelper dbConfigKeyValue = null;
	private dbBookmarksAdapter dbBookmarks = null;
	private Cursor dbConfigKeyValueCursor;
	private ProgressBar circleProgressBar;
	private ImageView iv;
	private String sharedimageurl;
	private String uploadfile;
	private String filename;
	private File tempFile;
	private int sdk;

	private final static int ITEM_ID_GOBACK = 1;
	private final static int ITEM_ID_GOFORWARD = 2;
	private final static int ITEM_ID_GOSTOP = 3;
	private final static int ITEM_ID_GOHOME = 4;
	private final static int ITEM_ID_GODOWNLOADMANAGER = 5;
	private final static int ITEM_ID_GOQUIT = 6;
	private final static int ITEM_ID_BOOKMARKS = 7;
	private final static int ITEM_ID_ADDBOOKMARK = 8;
	private final static int ITEM_ID_NEWURL = 9;
	private final static int ITEM_ID_REFRESH = 10;
	private final static int ITEM_ID_SETTING = 11;
	private final static int ITEM_ID_ABOUT = 12;
	private final static int ITEM_ID_DOWNLOADFILES = 13;
	private final static int ITEM_ID_SHARE = 14;

	private static final int BOOKMARKS_REQUEST = 0;
	private static final int WALLPAPER_REQUEST = 1;
	private static final int SELECTPIC_REQUEST = 2;

	private final static String STRING_HOMEPAGEURL = "http://redhorse4you.appspot.com/";
	private final static String STRING_SAVETODIR = "/sdcard/download";

	final Context myApp = this;

	private WebView testWebView = null;
	private String homepageurl;
	private String savetodir;
	private long bookmarkid;

	private NotificationManager mNM;

	private void notification() {
		try {
			mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			Intent intent = new Intent(this, bookmarkslist.class);
			CharSequence appName = getString(R.string.app_name);
			Notification notification = new Notification(R.drawable.icon,
					appName, System.currentTimeMillis());
			notification.flags = Notification.FLAG_NO_CLEAR;
			CharSequence appDescription = "";
			notification.setLatestEventInfo(minihorse.this, appName,
					appDescription, PendingIntent.getActivity(getBaseContext(),
							0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
			mNM.notify(0, notification);
		} catch (Exception e) {
			mNM = null;
		}
	}

	@Override
	protected void onNewIntent(Intent i) {
		// TODO Auto-generated method stub
		super.onNewIntent(i);
		String url = "";
		try {
			url = i.getData().toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!url.equals(""))
			testWebView.loadUrl(url);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sdk = new Integer(Build.VERSION.SDK).intValue();

		Log.e("onCreate", "onCreate");
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);
		
		this.tempFile=new File("/sdcard/minihorse_tmp.jpg"); 

		SharedPreferences share = this.getPreferences(MODE_PRIVATE);
		this.homepageurl = share.getString("homepageurl", "");
		if (this.homepageurl == "") {
			this.homepageurl = STRING_HOMEPAGEURL;
			Editor editor = share.edit();// 取得编辑器
			editor.putString("homepageurl", this.homepageurl);
			editor.commit();// 提交刷新数据
		}
		this.savetodir = share.getString("savetodir", "");
		if (this.savetodir == "") {
			// this.savetodir = STRING_SAVETODIR;
			java.io.File sdcardDir = android.os.Environment
					.getExternalStorageDirectory();
			this.savetodir = sdcardDir.getAbsolutePath() + "/download";
			Editor editor = share.edit();// 取得编辑器
			editor.putString("savetodir", this.savetodir);
			editor.commit();// 提交刷新数据
		}

		dbBookmarks = new dbBookmarksAdapter(this);
		dbBookmarks.open();
		// bookmarkid =
		// dbBookmarks.insertTitle("","redhorse主页",this.homepageurl);

		testWebView = (WebView) this.findViewById(R.id.WebView01);
		testWebView.getSettings().setSaveFormData(true);
		testWebView.getSettings().setSavePassword(true);
		testWebView.getSettings().setSupportZoom(true);
		testWebView.getSettings().setBuiltInZoomControls(true);
		testWebView.getSettings().setJavaScriptEnabled(true);

		Intent i = getIntent();
		String url = "";
		try {
			url = i.getData().toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (url.equals(""))
			url = homepageurl;
		testWebView.loadUrl(url);

		testWebView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (!testWebView.hasFocus()) {
						testWebView.requestFocus();
					}
					break;
				default:
					break;
				}

				// 点击关闭软键盘
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(
						((EditText) findViewById(R.id.urlText))
								.getWindowToken(), 0);
				return false;
			}
		});

		// Button btn_loadUrl = (Button) this.findViewById(R.id.loadUrl);
		final EditText urlText = (EditText) this.findViewById(R.id.urlText);
		urlText.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View arg0, boolean isFocused) {
				// TODO Auto-generated method stub

				if (isFocused == true) {
					urlText.selectAll();
				} else {
				}
			}
		});

		urlText.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub

				// 如果按的是回车键则加载url
				if (event.getAction() == KeyEvent.ACTION_UP
						&& keyCode == KeyEvent.KEYCODE_ENTER) {
					loadUrl(urlText);
				}
				return false;
			}
		});

		// 设置webview为一个单独的client, 这样可以使加载url不调用系统的browser
		testWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// Log.i("",
				// ".......EXPID_LOCAL.. shouldOverrideUrlLoading......url=="+url);
				float zoomlevel = view.getScale();
				view.setInitialScale(Math.round(zoomlevel * 100));
				view.loadUrl(url);
				return true;
			}

			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				Log.e("url", "finish url is " + url);
				((EditText) findViewById(R.id.urlText)).setText(url);
				testWebView.requestFocus();
				circleProgressBar.setVisibility(View.INVISIBLE);
				// iv.setVisibility(View.VISIBLE);
			}
		});

		testWebView.setDownloadListener(new WebDownloadListener());
		testWebView.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				Looper looper = Looper.myLooper();// 取得当前线程里的looper
				MyHandler mHandler = new MyHandler(looper);// 构造一个handler使之可与looper通信
				// buton等组件可以由mHandler将消息传给looper后,再放入messageQueue中,同时mHandler也可以接受来自looper消息
				mHandler.removeMessages(0);
				String msgStr = "";
				Message m = mHandler.obtainMessage(1, 1, 1, msgStr);// 构造要传递的消息
				testWebView.requestImageRef(m);
				// mHandler.sendMessage(m);//
				// 发送消息:系统会自动调用handleMessage方法来处理消息
				// testWebView.requestImageRef(msg);

				AlertDialog opDialog = new AlertDialog.Builder(minihorse.this)
						.setTitle(R.string.select_dialog)
						.setItems(R.array.select_dialog_shareimage,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {

										/* User clicked so do some stuff */
										String[] items = getResources()
												.getStringArray(
														R.array.select_dialog_items);
										// new
										// AlertDialog.Builder(downloadlist.this)
										// .setMessage("You selected: " + which
										// + " , " + items[which])
										// .show();
										Intent intent = null;
										CacheResult cs = null;
										File cachedir = null;
										filename = null;

										Bundle mBundle = new Bundle();
										switch (which) {
										case 0:
											Log.e("debug",
													Integer.toString(which));
											intent = new Intent();
											intent.setClass(minihorse.this,
													weibo.class);
											mBundle.putString("url",
													testWebView.getUrl());
											intent.putExtras(mBundle);
											startActivity(intent);
											break;
										case 1:
											Log.e("debug",
													Integer.toString(which));
											if (sharedimageurl != null) {
												intent = new Intent();
												intent.setClass(minihorse.this,
														weibo.class);
												mBundle.putString("url", "");
												cs = CacheManager.getCacheFile(
														sharedimageurl,
														new HashMap());
												cachedir = CacheManager
														.getCacheFileBaseDir();
												filename = cs.getLocalPath();
												mBundle.putString("uploadfile",
														cachedir.getPath()
																+ "/"
																+ filename);
												Log.e("uploadfile",
														cachedir.getPath()
																+ "/"
																+ filename);
												intent.putExtras(mBundle);
												startActivity(intent);
											} else {
												Toast.makeText(minihorse.this,
														"没有选中的图片",
														Toast.LENGTH_LONG)
														.show();
											}
											break;
										case 2:
											Log.e("debug",
													Integer.toString(which));
											intent = new Intent(
													Intent.ACTION_SEND);
											intent.setType("text/plain");
											intent.putExtra(
													Intent.EXTRA_SUBJECT, "分享");
											intent.putExtra(Intent.EXTRA_TEXT,
													testWebView.getUrl());
											startActivity(Intent.createChooser(
													intent, getTitle()));
											break;
										case 3:
											Log.e("debug",
													Integer.toString(which));
											if (sharedimageurl != null) {
												cs = CacheManager.getCacheFile(
														sharedimageurl,
														new HashMap());
												cachedir = CacheManager
														.getCacheFileBaseDir();
												filename = cs.getLocalPath();
												uploadfile = cachedir.getPath()
														+ "/" + filename;
												LayoutInflater factory = LayoutInflater
														.from(myApp);
												final View savetoView = factory
														.inflate(
																R.layout.dialog_save_download_to,
																null);
												((EditText) savetoView
														.findViewById(R.id.dialog_saveto_edit)).setText(URLUtil.guessFileName(
														sharedimageurl, null,
														cs.getMimeType()));
												((EditText) savetoView
														.findViewById(R.id.dialog_savetopath_edit)).setText("savedpic");
												AlertDialog savetoDialog = new AlertDialog.Builder(
														myApp)
														.setIcon(
																R.drawable.alert_dialog_icon)
														.setTitle(
																R.string.dialog_saveto)
														.setView(savetoView)
														.setPositiveButton(
																R.string.dialog_ok,
																savetodirclick)
														.setNegativeButton(
																R.string.dialog_cancel,
																new DialogInterface.OnClickListener() {
																	public void onClick(
																			DialogInterface dialog,
																			int whichButton) {
																	}
																}).create();
												savetoDialog.show();
											} else {
												Toast.makeText(minihorse.this,
														"没有选中的图片",
														Toast.LENGTH_LONG)
														.show();
											}
											break;
										case 4:
											Log.e("debug",
													Integer.toString(which));
											Picture picture = testWebView
													.capturePicture();

											Bitmap b = Bitmap.createBitmap(
													picture.getWidth(),
													picture.getHeight(),
													Bitmap.Config.ARGB_8888);

											Canvas c = new Canvas(b);

											picture.draw(c);

											FileOutputStream fos = null;

											// String tmpsavetodir =
											// getSDPath()+"/DCIM/Camera/";
											String tmpsavetodir = getSDPath()
													+ "/download/";
											try {
												mkdir(tmpsavetodir);
												fos = new FileOutputStream(
														tmpsavetodir
																+ "minihorse_"
																+ System.currentTimeMillis()
																+ ".jpg");

												if (fos != null) {
													b.compress(
															Bitmap.CompressFormat.JPEG,
															90, fos);

													fos.close();
												}

											} catch (Exception e) {
												// ...
											}
											break;
										case 5:
											if (sharedimageurl != null) {
												cs = CacheManager.getCacheFile(
														sharedimageurl,
														new HashMap());
												cachedir = CacheManager
														.getCacheFileBaseDir();
												filename = cs.getLocalPath();
												uploadfile = cachedir.getPath()
														+ "/" + filename;
//												tmpsavetodir = getSDPath()+ "/DCIM/100ANDRO";
												tmpsavetodir = getSDPath()+ "/download/wallpaper";
												mkdir(tmpsavetodir);
												String tmpuploadfile = tmpsavetodir	+ "/" + filename + ".jpg";
												try {
													FileCopyforTransfer(
															uploadfile,
															tmpuploadfile);
												} catch (Exception e) {
													// TODO Auto-generated catch
													// block
													e.printStackTrace();
												}
//												intent = new Intent();  
//												intent.setClassName("com.android.camera", "com.android.camera.CropImage");  
//												intent.setData(Uri.fromFile(new File(tmpuploadfile)));  
//												intent.putExtra("crop", "true");  
//												intent.putExtra("aspectX", 4);  
//												intent.putExtra("aspectY", 3);  
////												intent.putExtra("outputX", 96);  
////												intent.putExtra("outputY", 96);  
//												intent.putExtra("noFaceDetection", true);
//												intent.putExtra("setWallpaper",    true);
//												intent.putExtra("return-data", true);
												//输出文件
//												intent.putExtra("output", Uri.fromFile(new File(tmpuploadfile))); 
//								                intent.putExtra("outputFormat", "JPEG");												intent.putExtra("setWallpaper", true);
//								                startActivityForResult(intent,WALLPAPER_REQUEST); 
												
												//2.1以上打开图库选择器
//												Cursor cursor = getContentResolver().query(Uri.parse("content://media/external/images/media"), null, null, null, null);
//												SimpleCursorAdapter adapter = new SimpleCursorAdapter(minihorse.this,0,null,null,null);
//												adapter.notifyDataSetChanged(); 												
//								                intent = new Intent(Intent.ACTION_GET_CONTENT); 
//								                intent.setType("image/*"); 
//								                intent.putExtra("crop", "true");
//								                intent.putExtra("aspectX", 800); 
//								                intent.putExtra("aspectY", 480);
//								                intent.putExtra("output", Uri.fromFile(tempFile)); 
//								                intent.putExtra("outputFormat", "JPEG");
//								                startActivityForResult(Intent.createChooser(intent, "选择图片"), 
//								                        SELECTPIC_REQUEST); 
												//2.1系统以下
//												Intent it = new Intent(
//														Intent.ACTION_VIEW);
//												Uri uri = Uri.parse("file://"
//														+ tmpuploadfile);
//												intent = new Intent(
//														"com.android.camera.action.CROP");
//												intent.setClassName(
//														"com.android.camera",
//														"com.android.camera.CropImage");
//
//												intent.setData(uri);
//												intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);												intent.putExtra("outputX", 96);
//												intent.putExtra("outputY", 96);
//												intent.putExtra("aspectX", 800);
//												intent.putExtra("aspectY", 480);
//												intent.putExtra("scale", true);
//												intent.putExtra("return-data",
//														true);
//												startActivityForResult(intent,WALLPAPER_REQUEST);
												// 启动图片察看选择器
												// it.setFlags(0x3000000);
												// it.setType("image/*");
												// it.setData(uri);
												// Intent wrapperIntent =
												// Intent.createChooser(it,
												// null);
												// startActivityForResult(wrapperIntent,
												// 0);
												// 通过照片查看程序设置壁纸
												// Intent it = new
												// Intent(Intent.ACTION_VIEW);
												// Uri uri =
												// Uri.parse("file://"+tmpuploadfile);
												// it.setDataAndType(uri,
												// cs.getMimeType());
												// startActivity(it);
												// 直接设置壁纸
												 final Intent intent1 = new
												 Intent();
												 intent1.setAction("android.intent.action.ATTACH_DATA");
												 String wallpaperpkg = "com.android.camera"; 
												 if (sdk > 5) wallpaperpkg = "com.android.gallery";
												 final ComponentName cn = new
												 ComponentName(wallpaperpkg,"com.android.camera.Wallpaper");
												 intent1.setComponent(cn);
												 intent1.setFlags(0x3000000);
												 intent1.setType(cs.getMimeType());
												 intent1.setData(Uri.parse("file://"+tmpuploadfile));
												 startActivity(intent1);
											} else {
												Toast.makeText(minihorse.this,
														"没有选中的图片",
														Toast.LENGTH_LONG)
														.show();
											}
											break;
										}
									}
								}).create();
				opDialog.show();
				return false;
			}
		});

		/* WebChromeClient must be set BEFORE calling loadUrl! */
		testWebView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onJsAlert(WebView view, String url, String message,
					final android.webkit.JsResult result) {
				new AlertDialog.Builder(myApp)
						.setTitle(R.string.onjsalert)
						.setMessage(message)
						.setPositiveButton(android.R.string.ok,
								new AlertDialog.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										result.confirm();
									}
								}).setCancelable(false).create().show();

				return true;
			};
		});
		circleProgressBar = (ProgressBar) findViewById(R.id.circleProgressBar);
		iv = (ImageView) findViewById(R.id.searchIcon);
		// circleProgressBar.setVisibility(View.VISIBLE);
	}

	public OnClickListener savetodirclick = new OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			/* User clicked OK so do some stuff */
			try {
				String forder = ((EditText) ((AlertDialog) dialog)
						.findViewById(R.id.dialog_savetopath_edit)).getText()
						.toString();
				String file = ((EditText) ((AlertDialog) dialog)
						.findViewById(R.id.dialog_saveto_edit)).getText()
						.toString();
				String tmpsavetodir = savetodir;
				if (forder != "") {
					tmpsavetodir = savetodir + "/" + forder;
				}
				mkdir(tmpsavetodir);
				Log.e("forder", "uploadfile is " + uploadfile);
				Log.e("forder", "tmpsavetodir is " + tmpsavetodir + "/" + file);
				FileCopyforTransfer(uploadfile, tmpsavetodir + "/" + file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (testWebView == null) {
			return true;
		}
		// 按下BACK键回到历史页面中
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	// 加载url同时关闭软键盘
	private void loadUrl(EditText urlText) {
		String url = "";
		url = urlText.getText().toString();
		if (!url.toLowerCase().startsWith("http")) {
			url = "http://" + url;
		}
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(urlText.getWindowToken(), 0);

		// iv.setVisibility(View.INVISIBLE);
		circleProgressBar.setVisibility(View.VISIBLE);

		float zoomlevel = testWebView.getScale();
		testWebView.setInitialScale(Math.round(zoomlevel * 100));
		testWebView.loadUrl(url);
	}

	// 创建菜单
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(1, ITEM_ID_SHARE, 0, R.string.share).setIcon(
				R.drawable.menu_sharepage);
		menu.add(1, ITEM_ID_GOBACK, 0, R.string.back).setIcon(
				R.drawable.controlbar_backward_enable);
		menu.add(1, ITEM_ID_GOFORWARD, 0, R.string.go).setIcon(
				R.drawable.controlbar_forward_enable);
		menu.add(1, ITEM_ID_GOSTOP, 0, R.string.stop).setIcon(
				R.drawable.controlbar_stop);
		menu.add(1, ITEM_ID_GOHOME, 0, R.string.home).setIcon(
				R.drawable.controlbar_homepage);
		menu.add(1, ITEM_ID_GODOWNLOADMANAGER, 0, R.string.downloadmanager)
				.setIcon(R.drawable.menu_downmanager);
		menu.add(1, ITEM_ID_DOWNLOADFILES, 0, R.string.downloadfile).setIcon(
				R.drawable.menu_redownload);
		menu.add(1, ITEM_ID_NEWURL, 0, R.string.newurl).setIcon(
				R.drawable.menu_newurl);
		menu.add(1, ITEM_ID_REFRESH, 0, R.string.refresh).setIcon(
				R.drawable.menu_refresh);
		menu.add(1, ITEM_ID_ADDBOOKMARK, 0, R.string.addbookmark).setIcon(
				R.drawable.menu_add_to_bookmark);
		menu.add(1, ITEM_ID_BOOKMARKS, 0, R.string.bookmarks).setIcon(
				R.drawable.menu_bookmark);
		// menu.add(1, ITEM_ID_SETTING, 0, R.string.setting).setIcon(
		// R.drawable.menu_syssettings);
		menu.add(1, ITEM_ID_ABOUT, 0, R.string.about).setIcon(
				R.drawable.menu_help);
		menu.add(1, ITEM_ID_GOQUIT, 0, R.string.quit).setIcon(
				R.drawable.menu_quit);
		return true;
	}

	// 给菜单加事件
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case ITEM_ID_GOBACK:
			if (testWebView.canGoBack()) {
				testWebView.goBack();
			}
			break;
		case ITEM_ID_GOFORWARD:
			if (testWebView.canGoForward()) {
				testWebView.goForward();
			}
			break;
		case ITEM_ID_GOSTOP:
			testWebView.stopLoading();
			break;
		case ITEM_ID_GOHOME:
			float zoomlevel = testWebView.getScale();
			testWebView.setInitialScale(Math.round(zoomlevel * 100));
			testWebView.loadUrl(homepageurl);
			break;
		case ITEM_ID_GOQUIT:
			finish();
			break;
		case ITEM_ID_ADDBOOKMARK:
			bookmarkid = dbBookmarks.insertTitle("", testWebView.getTitle()
					+ "", testWebView.getUrl());
			Toast.makeText(this, R.string.info_addbookmark, Toast.LENGTH_LONG)
					.show();
			break;
		case ITEM_ID_BOOKMARKS:
			Intent it = new Intent();
			it.setClass(minihorse.this, bookmarkslist.class);
			startActivityForResult(it, BOOKMARKS_REQUEST);
			break;
		case ITEM_ID_REFRESH:
			testWebView.reload();
			break;
		case ITEM_ID_NEWURL:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("打开新页面");
			alert.setMessage("网址");

			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			alert.setView(input);
			alert.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							loadUrl(input);
						}
					});

			alert.setNegativeButton("取消",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Canceled.
						}
					});

			alert.show();
			break;
		case ITEM_ID_GODOWNLOADMANAGER:
			Intent itdownloadmgr = new Intent();
			itdownloadmgr.setClass(minihorse.this, downloadlist.class);
			startActivity(itdownloadmgr);
			break;
		case ITEM_ID_SETTING:
			Intent setting = new Intent();
			setting.setClass(minihorse.this, AppGrid.class);
			startActivity(setting);
			break;
		case ITEM_ID_ABOUT:
			// Toast.makeText(this, R.string.info_about, Toast.LENGTH_LONG)
			// .show();
			Intent itfeedback = new Intent();
			itfeedback.setClass(minihorse.this, Feedback.class);
			startActivity(itfeedback);
			break;
		case ITEM_ID_DOWNLOADFILES:
			// Log.e("minihorse", "start ansrozip");
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			List<ResolveInfo> mAllApps = getPackageManager()
					.queryIntentActivities(mainIntent, 0);
			boolean found = false;
			Iterator it1 = mAllApps.iterator();
			while (it1.hasNext()) {
				ResolveInfo info = (ResolveInfo) it1.next();
				if (("com.agilesoftresource")
						.equals(info.activityInfo.packageName)) {
					found = true;
					break;
				}
			}
			Intent intent = new Intent();
			PackageManager packageManager = this.getPackageManager();
			if (found)
				try {
					intent = packageManager
							.getLaunchIntentForPackage("com.agilesoftresource");
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else
				intent.setClass(minihorse.this, FileList.class);
			startActivity(intent);
			break;
		case ITEM_ID_SHARE:
			Intent itShare = new Intent();
			itShare.setClass(minihorse.this, weibo.class);
			Bundle mBundle = new Bundle();
			mBundle.putString("title", testWebView.getTitle() + "");
			mBundle.putString("url", testWebView.getUrl());
			itShare.putExtras(mBundle);
			startActivity(itShare);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case BOOKMARKS_REQUEST:
			switch (resultCode) {
			case RESULT_OK:
				Bundle b = data.getExtras();
				String url = b.getString("URL");
				float zoomlevel = testWebView.getScale();
				testWebView.setInitialScale(Math.round(zoomlevel * 100));
				testWebView.loadUrl(url);
				Log.e("bookmarkmenu", "url is " + url);
				break;
			}
		case WALLPAPER_REQUEST:
			switch (resultCode) {
			case RESULT_OK:
				Bundle extras = data.getExtras();  
				if(extras != null ) {  
				    Bitmap photo = extras.getParcelable("data");  
					FileOutputStream fos = null;
					String tmpsavetodir = getSDPath()
							+ "/wallpapers/";
					try {
						mkdir(tmpsavetodir);
						String filename = tmpsavetodir + "minihorse_"	+ System.currentTimeMillis()+ ".jpg";
						fos = new FileOutputStream(filename);

						if (fos != null) {
							photo.compress(
									Bitmap.CompressFormat.JPEG,
									80, fos);
							fos.close();
							getApplicationContext().setWallpaper(photo);
						}

					} catch (Exception e) {
						// ...
					}
				} 
				break;
			}
		default:
			break;
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	class WebDownloadListener implements DownloadListener {
		public void onDownloadStart(final String url, String userAgent,
				String contentDisposition, String mimeType, long contentLength) {
			Log.e("Download", "url is " + url);
			Log.e("Download", "userAgent is " + userAgent);
			Log.e("Download", "contentDisposition is " + contentDisposition);
			Log.e("Download", "mimetype is " + mimeType);
			Log.e("Download", "contentLength is " + contentLength);
			LayoutInflater factory = LayoutInflater.from(myApp);
			final View savetoView = factory.inflate(
					R.layout.dialog_save_download_to, null);
			((EditText) savetoView.findViewById(R.id.dialog_saveto_edit))
					.setText(URLUtil.guessFileName(url, contentDisposition,
							mimeType));
			Request urlrequest = HttpRequestParser.parse(url);
			((EditText) savetoView.findViewById(R.id.dialog_savetopath_edit))
					.setText(urlrequest.getParameter("forder"));
			AlertDialog savetoDialog = new AlertDialog.Builder(myApp)
					.setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.dialog_saveto)
					.setView(savetoView)
					.setPositiveButton(R.string.dialog_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									/* User clicked OK so do some stuff */
									try {
										String forder = ((EditText) ((AlertDialog) dialog)
												.findViewById(R.id.dialog_savetopath_edit))
												.getText().toString();
										String tmpsavetodir = savetodir;
										if (forder != "") {
											tmpsavetodir = savetodir + "/"
													+ forder;
										}
										mkdir(tmpsavetodir);
										Log.e("forder", "forder is " + forder);
										Log.e("forder", "tmpsavetodir is "
												+ tmpsavetodir);
										new DownloadFilesTask()
												.execute(
														url,
														tmpsavetodir,
														((EditText) ((AlertDialog) dialog)
																.findViewById(R.id.dialog_saveto_edit))
																.getText()
																.toString(),
														String.valueOf(1),
														myApp.getApplicationContext());
										// Intent itShare = new Intent();
										// itShare.setClass(minihorse.this,
										// downloadlist.class);
										// Bundle mBundle = new Bundle();
										// mBundle.putString("title", "");
										// mBundle.putString("url", url);
										// mBundle.putString("savetodir",
										// tmpsavetodir);
										// mBundle.putString(
										// "filename",
										// ((EditText) ((AlertDialog) dialog)
										// .findViewById(R.id.dialog_saveto_edit))
										// .getText().toString());
										// itShare.putExtras(mBundle);
										// startActivity(itShare);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							})
					.setNegativeButton(R.string.dialog_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
			savetoDialog.show();
		}
	}

	private class MyHandler extends Handler {
		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {// 处理消息
			sharedimageurl = msg.getData().getString("url");
			Log.e("minihorse", "url is " + sharedimageurl);
		}
	}

	public String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
		}
		return sdDir.toString();

	}

	public boolean mkdir(String tmpsavetodir) {
		StringTokenizer st = new StringTokenizer(tmpsavetodir, "/");
		String path1 = st.nextToken() + "/";
		String path2 = path1;
		while (st.hasMoreTokens()) {
			path1 = st.nextToken() + "/";
			path2 += path1;
			File inbox = new File(path2);
			if (!inbox.exists())
				inbox.mkdir();
		}
		return true;
	}

	public long FileCopyforTransfer(String f1, String f2) throws Exception {
		long time = new Date().getTime();
		int length = 2097152;
		FileInputStream in = new FileInputStream(f1);
		FileOutputStream out = new FileOutputStream(f2);
		byte[] buffer = new byte[length];
		while (true) {
			int ins = in.read(buffer);
			if (ins == -1) {
				in.close();
				out.flush();
				out.close();
//				Toast.makeText(minihorse.this, "图片保持完成！", Toast.LENGTH_LONG).show();
				return new Date().getTime() - time;
			} else
				out.write(buffer, 0, ins);
		}
	}
}
