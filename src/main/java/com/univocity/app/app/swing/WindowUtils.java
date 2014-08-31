/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.app.swing;

import java.awt.*;

import javax.swing.*;

public class WindowUtils {

	public static void configureAsExecutableWindow(JFrame window) {
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(1024, 768);
		window.setLocationRelativeTo(null);
	}

	public static JFrame getExecutableWindow(Component c) {
		JFrame window = new JFrame();
		configureAsExecutableWindow(window);
		window.setLayout(new BorderLayout());
		window.add(c, BorderLayout.CENTER);
		return window;
	}

	public static void displayWindow(final JFrame window) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				window.setVisible(true);
			}
		});
	}

	public static void displayInExecutableWindow(Component c) {
		final JFrame window = getExecutableWindow(c);
		displayWindow(window);
	}

	public static void showErrorMessage(Component parent, Throwable error) {
		String message;
		if (error.getMessage() != null) {
			message = error.getMessage().replaceAll(":", ":\n");
		} else {
			message = error.getClass().getName();
		}
		error.printStackTrace();
		JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);

	}

	public static void fixDisplayOnLinux(Window w) {
		LinuxWindowFix.getInstance().apply(w);
	}

}
