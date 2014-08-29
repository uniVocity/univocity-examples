/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app.swing;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

import com.univocity.examples.utils.*;

public class DataAnalysisPanel extends JPanel {

	private static final long serialVersionUID = -1850024827814851918L;
	private DaoTable sourceTable;
	private DaoTable destinationTable;

	private final DatabaseAccessor sourceConfig;
	private final DatabaseAccessor destinationConfig;

	private JPanel sourcePanel;
	private JPanel destinationPanel;

	private JSplitPane splitPane;

	public DataAnalysisPanel(DatabaseAccessor sourceConfig, DatabaseAccessor destinationConfig) {
		this.sourceConfig = sourceConfig;
		this.destinationConfig = destinationConfig;

		this.setLayout(new BorderLayout());
		this.add(getSplitPane(), BorderLayout.CENTER);
	}

	protected JPanel getSourcePanel() {
		if (sourcePanel == null) {
			sourcePanel = newPanel(getSourceTable(), "Source - " + sourceConfig.getDatabaseName());
		}
		return sourcePanel;
	}

	protected JPanel getDestinationPanel() {
		if (destinationPanel == null) {
			destinationPanel = newPanel(getDestinationTable(), "Destination - " + destinationConfig.getDatabaseName());
		}
		return destinationPanel;
	}

	protected JPanel newPanel(DaoTable table, String title) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(table, BorderLayout.CENTER);
		panel.setBorder(new TitledBorder(title));
		return panel;
	}

	protected JSplitPane getSplitPane() {
		if (splitPane == null) {
			splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
			splitPane.setLeftComponent(getSourcePanel());
			splitPane.setRightComponent(getDestinationPanel());
			splitPane.setResizeWeight(0.5);
		}
		return splitPane;
	}

	protected DaoTable getSourceTable() {
		if (sourceTable == null) {
			sourceTable = newDaoTable(sourceConfig);
		}
		return sourceTable;
	}

	protected DaoTable getDestinationTable() {
		if (destinationTable == null) {
			destinationTable = newDaoTable(destinationConfig);
		}
		return destinationTable;
	}

	protected DaoTable newDaoTable(DatabaseAccessor config) {
		return new DaoTable(config);
	}

	public static void main(String... args) {
		DatabaseAccessor source = new DatabaseAccessor("usda_db", "source/db", "source/db/queries.properties");
		DatabaseAccessor destination = new DatabaseAccessor("destination", "destination/db", "destination/db/queries.properties");

		WindowUtils.displayInExecutableWindow(new DataAnalysisPanel(source, destination));
	}
}
