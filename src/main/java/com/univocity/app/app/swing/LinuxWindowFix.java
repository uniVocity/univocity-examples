/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.app.swing;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

class LinuxWindowFix implements WindowStateListener {

	private final String desktop;
	private Field metacity_wm;
	private Field awt_wmgr;
	private boolean applyFix;

	private static LinuxWindowFix instance = new LinuxWindowFix();

	public static LinuxWindowFix getInstance() {
		return instance;
	}

	private LinuxWindowFix() {
		applyFix = false;

		List<String> linuxDesktops = Arrays.asList("gnome-shell", "mate", "cinnamon"); //add more desktop names here.

		desktop = System.getenv("DESKTOP_SESSION");
		if (desktop != null && linuxDesktops.contains(desktop.toLowerCase())) {
			try {
				Class<?> xwm = Class.forName("sun.awt.X11.XWM");
				awt_wmgr = xwm.getDeclaredField("awt_wmgr");
				awt_wmgr.setAccessible(true);
				Field other_wm = xwm.getDeclaredField("OTHER_WM");
				other_wm.setAccessible(true);
				if (awt_wmgr.get(null).equals(other_wm.get(null))) {
					metacity_wm = xwm.getDeclaredField("METACITY_WM");
					metacity_wm.setAccessible(true);
					applyFix = true;
				}
			} catch (Exception ex) {
				//ignore
			}
		}
	}

	@Override
	public void windowStateChanged(WindowEvent e) {
		try {
			awt_wmgr.set(null, metacity_wm.get(null));
		} catch (Exception ex) {
			//ignore
		}
	}

	public void apply(Window w) {
		if (!applyFix) {
			return;
		}
		w.removeWindowStateListener(this);
		w.addWindowStateListener(this);
	}
}
