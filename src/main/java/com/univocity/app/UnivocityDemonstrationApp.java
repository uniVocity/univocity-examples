/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app;

import javax.swing.*;

import com.univocity.app.swing.*;

public class UnivocityDemonstrationApp extends DataAnalysisWindow {

	private static final long serialVersionUID = -4290799952797464413L;

	public UnivocityDemonstrationApp() {
		super(new SampleAppConfig());
		setIconImage(new ImageIcon(UnivocityDemonstrationApp.class.getResource("/images/icon128x128.png")).getImage());
	}

	public static void main(String... args) {
		UnivocityDemonstrationApp app = new UnivocityDemonstrationApp();
		WindowUtils.displayWindow(app);
	}

}
