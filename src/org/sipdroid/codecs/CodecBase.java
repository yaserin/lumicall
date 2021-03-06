/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.sipdroid.codecs;

import java.util.logging.Logger;

import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Sipdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

abstract class CodecBase implements Preference.OnPreferenceChangeListener, Codec {
	Logger logger = Logger.getLogger(getClass().getCanonicalName());
	protected String CODEC_NAME;
	protected String CODEC_USER_NAME;
	protected int CODEC_NUMBER;
	protected int CODEC_SAMPLE_RATE=8000;		// default for most narrow band codecs
	protected int CODEC_FRAME_SIZE=160;		// default for most narrow band codecs
	protected String CODEC_DESCRIPTION;
	protected String CODEC_DEFAULT_SETTING = "never";
	protected int CODEC_FRAMES_PER_PACKET = 1;
	protected String CODEC_JNI_LIB = null;

	private boolean loaded = false,failed = false;
	private boolean enabled = false;
	private boolean wlanOnly = false,wlanOr3GOnly = false;
	private String value;

	public void update() {
		if (Receiver.mContext != null) {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
			value = sp.getString(key(), CODEC_DEFAULT_SETTING);
			updateFlags(value);		
		}
	}
	
	public String getValue() {
		return value;
	}
	
	void load() {
		try {
			if(CODEC_JNI_LIB != null) {
				logger.info("trying to load shared object: " + CODEC_JNI_LIB);
				System.loadLibrary(CODEC_JNI_LIB);
			} else {
				logger.info("no shared object to be loaded");
			}
			update();
			loaded = true;
		} catch (Throwable e) {
			logger.severe("Loading failed: " + e.getClass().getCanonicalName()
					+ ": " + e.getMessage());
			if (!Sipdroid.release) e.printStackTrace();
		}  
	}
	
	public abstract int open();
	
	public void init() {
		logger.info("trying to init()");
		if(!isLoaded())
			load();
		if (isLoaded())
			open();
	}
	
	public int samp_rate() {
		return CODEC_SAMPLE_RATE;
	}
	
	public int frame_size() {
		return CODEC_FRAME_SIZE;
	}
	
	public int framesPerPacket() {
		return CODEC_FRAMES_PER_PACKET;
	}

	public boolean isLoaded() {
		return loaded;
	}
    
	public boolean isFailed() {
		return failed;
	}
	
	public void fail() {
		logger.warning("fail() invoked, codec will not be used");
		update();
		failed = true;
	}
	
	public void enable(boolean e) {
		enabled = e;
	}

	public boolean isEnabled() {
		return enabled;
	}

	TelephonyManager tm;
	int nt;
	
	public boolean isValid() {
		if (!isEnabled())
			return false;
		if (Receiver.on_wlan)
			return true;
		if (wlanOnly())
			return false;
		if (tm == null) tm = (TelephonyManager) Receiver.mContext.getSystemService(Context.TELEPHONY_SERVICE);
		nt = tm.getNetworkType();
		if (wlanOr3GOnly() && nt < TelephonyManager.NETWORK_TYPE_UMTS)
			return false;
		if (nt < TelephonyManager.NETWORK_TYPE_EDGE)
			return false;
		return true;
	}
		
	private boolean wlanOnly() {
		return enabled && wlanOnly;
	}
	
	private boolean wlanOr3GOnly() {
		return enabled && wlanOr3GOnly;
	}

	public String name() {
		return CODEC_NAME;
	}

	public String key() {
		return CODEC_NAME+"_new";
	}
	
	public String userName() {
		return CODEC_USER_NAME;
	}

	public String getTitle() {
		return CODEC_NAME + " (" + CODEC_DESCRIPTION + ")";
	}

	public int number() {
		return CODEC_NUMBER;
	}

	public void setListPreference(ListPreference l) {
		l.setOnPreferenceChangeListener(this);
		l.setValue(value);
	}

	public boolean onPreferenceChange(Preference p, Object newValue) {
		ListPreference l = (ListPreference)p;
		value = (String)newValue;

		updateFlags(value);

		l.setValue(value);
		l.setSummary(l.getEntry());

		return true;
	}

	private void updateFlags(String v) {

		if (v.equals("never")) {
			enabled = false;
		} else {
			enabled = true;
			if (v.equals("wlan"))
				wlanOnly = true;
			else
				wlanOnly = false;
			if (v.equals("wlanor3g"))
				wlanOr3GOnly = true;
			else
				wlanOr3GOnly = false;
		}
	}

	public String toString() {
		return "CODEC{ " + CODEC_NUMBER + ": " + getTitle() + "}";
	}

	@Override
	public boolean isLicensed() {
		// Most codecs are free and don't need a license
		// codecs that do need a license should override this method
		return true;
	}
}
