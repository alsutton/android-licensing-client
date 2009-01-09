package com.andappstore.licensing.demo;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.crypto.Cipher;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Demo extends Activity {
	
	//---------------------------------------------------------
	//
	// YOU WILL NEED TO ALTER THESE TO MATCH THE KEY DETAILS
	// FOR YOUR APPLICATION
	//
	//----------------------------------------------------------

	/**
	 * Your master key pair ID
	 */
	
	private static final String KEY_PAIR_ID = "";
	

	/**
	 * The API Key for the key pair ID
	 */
	
	private static final String KEY_PAIR_API_KEY = "";
	
	/**
	 * The APP key as downloaded from Master Key Pair page at AndAppStore.com 
	 */
	
	public static final byte[] APP_KEY= {};
	
	
	//---------------------------------------------------------
	//
	// HERE ENDS THE CONFIGURATION SECTION.
	//
	//----------------------------------------------------------

	/**
	 * Menu item for the "Get new license" option.
	 */
	private MenuItem getNewMenuItem;

	/**
	 * Menu item to switch between console and preferences.
	 */
	
	private MenuItem viewSwitchMenuItem;
	
	/**
	 * The options view
	 */
	
	private ListView preferenceList;
	
	/**
	 * The adapter managing the preferences.
	 */
	
	private ArrayAdapter<String> preferencesAdapter;
	
	/**
	 * The console
	 */
	
	private TextView console;

	/**
	 * The HTTP client for talking to the server.
	 */
	
	private HttpClient httpClient;

	/**
	 * This is a demo license which was generated with the generator key which was generated with the above decoding key
	 */

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
		console = new TextView(this);        
		updateLicenseAndDeviceDetails();
        setContentView(console);
        
        preferenceList = new ListView(this);
        preferenceList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
        String[] options = {"Lock to Phone Number", "Lock to Device", "30 Day Expiry"};
        preferencesAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_multiple_choice, options);
        preferenceList.setAdapter(preferencesAdapter);

        final HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        final SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http",PlainSocketFactory.getSocketFactory(), 80));
        final ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
        httpClient = new DefaultHttpClient(manager, params);

        new AlertDialog.Builder(this).setTitle("AndAppStore Demo")
          .setMessage("(c)Copright 2008\nFunky Android Ltd.\nAll Rights Reserved.\nUse is at users own risk.")
          .setPositiveButton("OK", null)
          .show(); 
    }

    /**
     * Updates the text area to show the license and device details.
     */
    
    private void updateLicenseAndDeviceDetails() {
        String text;
		try {
			StringBuffer textBuffer = new StringBuffer();
	        textBuffer.append("-= Device Details =-");
	        textBuffer.append("\nDate = ");
        	SimpleDateFormat sf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        	Date now = new Date();
        	textBuffer.append(sf.format(now));
	        
	        textBuffer.append("\nPhone Number = ");
	        textBuffer.append(getPhoneNumber());
	        
	        textBuffer.append("\nDevice ID = ");
	        textBuffer.append(getDeviceID());
	        textBuffer.append("\n\n");
	        
	        if (APP_KEY.length == 0) { 
	        	textBuffer.append("-= PLEASE MODIFY THE SOURCE CODE AND INSERT YOUR KEY DETAILS IN Demo.java =-");				
	        } else {	        
				Properties license = readLicenseFile();
				if(license == null) {
					textBuffer.append("-= No License Present =-");
				} if (APP_KEY.length == 0) { 
					textBuffer.append("-= PLEASE MODIFY THE SOURCE CODE AND INSERT YOUR KEY DETAILS IN Demo.java =-");				
				} else {
			        textBuffer.append("-= License Details =-");
	
			        textBuffer.append("\nExpiry = ");
			        String x = license.getProperty("e");
					if( x == null ) {
						textBuffer.append("None");
					} else {
						SimpleDateFormat xSdf = new SimpleDateFormat("yyyyMMdd");
						Date expiry = xSdf.parse(x);
						textBuffer.append(sf.format(expiry));
						if(expiry.before(now)) {
							textBuffer.append("\n** Expiry Check Failed **");
						}
					}
	
			        textBuffer.append("\nPhone Number Lock = ");
			        String p = license.getProperty("p");
					if( p == null ) {
						textBuffer.append("None");
					} else {
						textBuffer.append(p);
						if(!p.equals(getPhoneNumber())) {
							textBuffer.append("\n** Phone Number Check Failed **");
						}
					}
	
			        textBuffer.append("\nDevice Lock = ");
			        String d = license.getProperty("d");
					if( d == null ) {
						textBuffer.append("None");
					} else {
						textBuffer.append(d);
						if(!d.equals(getDeviceID())) {
							textBuffer.append("\n** Device Check Failed **");
						}
					}
				}
	        }
	        text = textBuffer.toString();				
		} catch (Exception e) {
			e.printStackTrace();
			text = "Error during decode : "+e.getMessage();
		}
		
		console.setText(text);
    }
                                                  
    /**
     * Method to read and decode the file license.
     * @throws IOException 
     * @throws GeneralSecurityException 
     */
    
    private Properties readLicenseFile() throws IOException, GeneralSecurityException {
		byte[] license = new byte[128];

		try {
			FileInputStream reader = openFileInput("AndLicense.001");
			try {
				reader.read(license);
			} finally {
				reader.close();
			}
		} catch(FileNotFoundException fnf) {
			return null;
		}

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(APP_KEY);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey key = factory.generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] original = cipher.doFinal(license);
        
        Properties props = new Properties();
        props.clear();
        ByteArrayInputStream bis = new ByteArrayInputStream(original);
        try {
	        props.load(bis);		        
        } finally {
        	bis.close();
        }    	
        
        return props;
    }
    
    /**
     * Set up the menu for the demo app.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
			     
		getNewMenuItem = menu.add("Get License");
		getNewMenuItem.setIcon(android.R.drawable.ic_menu_upload);
		viewSwitchMenuItem = menu.add("Options");
		setOptionsAsSwitchItem();
		
		return true;
	}
    
    /**
     * Set the switch option to be "preferences".
     */
    
    private void setOptionsAsSwitchItem() {
    	viewSwitchMenuItem.setTitle("Options");
    	viewSwitchMenuItem.setIcon(android.R.drawable.ic_menu_preferences);    	
    }
    
    /**
     * Set the switch option to be "preferences".
     */
    
    private void setConsoleAsSwitchItem() {
    	viewSwitchMenuItem.setTitle("Console");
    	viewSwitchMenuItem.setIcon(android.R.drawable.ic_menu_edit);    	
    }
 
    /**
     * Gets the phone number for this device.
     */
    
    public String getPhoneNumber() {
        TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getLine1Number();	        
    }

    /**
     * Gets the phone number for this device.
     */
    
    public String getDeviceID() {
        String id = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        if( id == null ) {
        	return "**Emulator - ID Unavailable**";
        } 
        
        return id;
    }

    /**
     * Handle the selection of a menu option.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if			( item.equals(getNewMenuItem) ) {
    		console.setText("Downloading new license...");
            setContentView(console);
            setOptionsAsSwitchItem();
            
            downloadAndStoreLicense();
            
            updateLicenseAndDeviceDetails();
		} else if 	( item.equals(viewSwitchMenuItem) ) {
			if(viewSwitchMenuItem.getTitle().equals("Options")) {
				setContentView(preferenceList);
				setConsoleAsSwitchItem();
			} else {
				setContentView(console);
				setOptionsAsSwitchItem();
			}
			return true;
		} 
		return super.onOptionsItemSelected(item);
	}
    
    /**
     * Download the license from the server and write it to the license file.
     */
    
    private void downloadAndStoreLicense() {
        final Uri.Builder uri = new Uri.Builder();
        uri.path("/AndroidPhoneApplications/license/!generate");

    	uri.appendQueryParameter("kid", KEY_PAIR_ID);
    	uri.appendQueryParameter("key", KEY_PAIR_API_KEY);
    	uri.appendQueryParameter("o", "TEST APP");
        
		boolean phoneLock = preferenceList.isItemChecked(0);
        if(phoneLock) {
        	uri.appendQueryParameter("pn", getPhoneNumber());
		}            

        boolean deviceLock = preferenceList.isItemChecked(1);
        if(deviceLock) {
        	if(Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID) == null) {
            	new AlertDialog.Builder(this)
            		.setIcon(android.R.drawable.ic_dialog_alert)
            		.setTitle("ID Lock Unavailable")
            		.setMessage("ID Locking to the emulator is not possible.")
            		.setPositiveButton("OK", null)
            		.show();             	
        	} else {
        		uri.appendQueryParameter("did", getDeviceID());
        	}
		}
        
		boolean expiryLock = preferenceList.isItemChecked(2);
        if(expiryLock) {
        	Calendar expiryCal = Calendar.getInstance();
        	expiryCal.add(Calendar.DAY_OF_MONTH, 30);
        	SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        	uri.appendQueryParameter("exp", sf.format(expiryCal.getTime()));
		}            

        HttpEntity entity = null;
        HttpHost host = new HttpHost("licensing.andappstore.com", 80, "http");
    	try {
            final HttpResponse response = httpClient.execute(host, new HttpGet(uri.build().toString()));
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                entity = response.getEntity();
                final InputStream in = entity.getContent();
                FileOutputStream fos = openFileOutput("AndLicense.001", MODE_PRIVATE);
                try {
                	byte[] buffer = new byte[256];
                	int len;
                	while((len = in.read(buffer)) > 0 ) {
                		fos.write(buffer, 0, len);
                	}
                } finally {
                	fos.close();
                }
            }            	
        } catch (Exception ex) {
        	new AlertDialog.Builder(this)
        		.setIcon(android.R.drawable.ic_dialog_alert)
        		.setTitle("Error fetching license")
        		.setMessage(ex.getMessage())
        		.setPositiveButton("OK", null)
        		.show(); 
        } finally {
            if (entity != null) {
            	try {
            		entity.consumeContent(); 
            	} catch(IOException ioe) {
            		// Ignore errors during consumption, there is 
            		// no possible corrective action.
            	}
            }
        }   	
    }
}