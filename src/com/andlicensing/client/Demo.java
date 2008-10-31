package com.andlicensing.client;

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
	  * The key used to decode licenses.
	  */
	 
	private static final byte[] LICENSE_DECODING_KEY = {
		48,-126,1,34,48,13,6,9,42,-122,72,-122,-9,13,1,1,1,5,0,3,-126,
		1,15,0,48,-126,1,10,2,-126,1,1,0,-101,-108,-79,90,-46,-68,-62,51,
		108,-1,126,-85,-57,72,-41,113,37,68,48,37,-8,30,-33,-21,-122,-21,-37,97,
		-98,-47,-87,-122,-11,86,-54,39,94,-127,55,27,-67,78,73,-101,-78,95,-111,-11,
		20,18,-31,118,76,69,77,75,-18,27,-34,76,-45,-47,-121,32,46,125,68,50,
		34,-126,-37,66,-28,76,5,-56,21,-1,58,-37,96,-110,-127,-64,112,-119,80,-17,
		96,-26,-120,-64,39,79,-119,-5,-35,99,-105,96,-124,95,123,112,71,61,10,-14,
		-118,79,-49,62,56,99,43,-125,-2,-22,-114,60,-19,-41,122,-60,4,3,-71,95,
		-11,90,49,-98,-57,-30,-34,62,24,100,-128,26,-55,90,80,78,-86,36,123,84,
		-95,-50,123,117,54,20,-50,24,96,-44,21,-22,-46,-23,103,-104,74,-98,126,-76,
		-104,-118,97,-98,-43,31,-86,125,77,123,-30,95,-81,127,-40,121,55,-101,59,93,
		-76,-36,28,-58,-91,-47,-11,-86,-121,-73,-117,-57,-94,-113,-91,118,-128,-32,-44,34,
		-9,120,-72,-76,-7,-25,59,-80,43,-55,53,-22,89,-37,-23,-7,34,81,-86,25,
		50,80,-55,32,-7,-36,76,-58,54,-73,-91,91,-30,116,28,70,123,127,-12,-49,
		-88,-106,-107,-3,-10,-1,-85,3,2,3,1,0,1};

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

        new AlertDialog.Builder(this).setTitle("AndLicensing Demo")
          .setMessage("(c)Copright 2008 Al Sutton\nAll Rights Reserved.")
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
	        
			Properties license = readLicenseFile();
			if(license == null) {
				textBuffer.append("-= No License Present =-");
			} else {
		        textBuffer.append("-= License Details =-");

		        textBuffer.append("\nExpiry = ");
		        String x = license.getProperty("x");
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
		byte[] license = new byte[256];

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

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(LICENSE_DECODING_KEY);
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
        uri.path("/AndroidApplicationLicensing/license!generate");
        
		boolean phoneLock = preferenceList.isItemChecked(0);
        if(phoneLock) {
        	uri.appendQueryParameter("p", getPhoneNumber());
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
        		uri.appendQueryParameter("d", getDeviceID());
        	}
		}
        
		boolean expiryLock = preferenceList.isItemChecked(2);
        if(expiryLock) {
        	Calendar expiryCal = Calendar.getInstance();
        	expiryCal.add(Calendar.DAY_OF_MONTH, 30);
        	SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        	uri.appendQueryParameter("x", sf.format(expiryCal.getTime()));
		}            

        HttpEntity entity = null;
//        HttpHost host = new HttpHost("192.168.219.97", 8080, "http");
        HttpHost host = new HttpHost("ls01.andlicensing.com", 80, "http");
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