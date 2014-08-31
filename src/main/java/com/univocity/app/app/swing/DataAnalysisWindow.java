/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.app.app.swing;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.border.*;

import com.univocity.app.app.data.*;
import com.univocity.app.utils.*;

public class DataAnalysisWindow extends JFrame {

	private static final long serialVersionUID = -4770569557233947796L;

	private DataAnalysisPanel dataAnalysisPanel;
	private final DataIntegrationConfig config;

	private JPanel processPanel;
	private JComboBox processList;
	private JLabel processListLabel;
	private JButton executeProcessButton;
	private GlassPane glass;
	private JPanel statusPanel;
	private JLabel statusLabel;
	private JLabel statusInformation;
	private JPanel logoPanel;
	private TableSearchField searchField;
	private JLabel searchLabel;
	private JButton btSearchNext;
	private JButton btSearchPrevious;

	public DataAnalysisWindow(DataIntegrationConfig config) {
		setLookAndFeel();

		this.config = config;
		this.setTitle("uniVocity data integration: " + config.getSourceDatabaseConfig().getDatabaseName() + " -> " + config.getDestinationDatabaseConfig().getDatabaseName());
		this.setGlassPane(getGlass());
		this.setIconImage(getIcon());
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		Container container = getContentPane();
		container.setLayout(new BorderLayout());

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(getProcessPanel(), BorderLayout.CENTER);
		northPanel.add(getLogoPanel(), BorderLayout.NORTH);

		container.add(northPanel, BorderLayout.NORTH);
		container.add(getDataAnalysisPanel(), BorderLayout.CENTER);
		container.add(getStatusPanel(), BorderLayout.SOUTH);

		setSize(Toolkit.getDefaultToolkit().getScreenSize());
		setLocationRelativeTo(null);
	}

	private void setLookAndFeel() {
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");
		WindowUtils.fixDisplayOnLinux(this);

		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			//keep the default
		}
	}

	private TableSearchField getSearchField() {
		if (searchField == null) {
			searchField = new TableSearchField();
			searchField.addTable(getDataAnalysisPanel().getSourceTable().getDataTable());
			searchField.addTable(getDataAnalysisPanel().getDestinationTable().getDataTable());
			searchField.setFont(new Font("Arial", Font.PLAIN, 10));
		}
		return searchField;
	}

	private JPanel getLogoPanel() {
		if (logoPanel == null) {
			logoPanel = new JPanel();

			logoPanel.setLayout(new BorderLayout());
			logoPanel.setBackground(new Color(35, 39, 42));

			ImageIcon image = new ImageIcon(getClass().getResource("/images/univocity_logo_250x86.png"));
			JLabel logo = new JLabel(image);

			logoPanel.add(logo, BorderLayout.CENTER);
			logoPanel.setPreferredSize(new Dimension(250, 100));
		}
		return logoPanel;
	}

	private Image getIcon() {
		try {
			return new ImageIcon(getClass().getResource("/icon128x128.png")).getImage();
		} catch (Exception ex) {
			return null;
		}
	}

	protected JPanel getStatusPanel() {
		if (statusPanel == null) {
			statusPanel = new JPanel();
			statusPanel.setLayout(new GridBagLayout());

			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.insets = new Insets(0, 5, 0, 5);
			statusPanel.add(getSearchLabel(), c);

			c.gridx++;
			c.ipadx = 350;
			statusPanel.add(getSearchField(), c);

			c.gridx++;
			c.ipadx = 0;
			statusPanel.add(getBtSearchNext(), c);

			c.gridx++;
			c.insets = new Insets(0, 5, 0, 20);
			statusPanel.add(getBtSearchPrevious(), c);

			c.gridx++;
			c.insets = new Insets(0, 5, 0, 5);
			statusPanel.add(getStatusLabel(), c);

			c.gridx++;
			statusPanel.add(getStatusInformation(), c);

			c.gridx++;
			c.weightx = 1.0;
			statusPanel.add(new JPanel(), c);

		}
		return statusPanel;
	}

	private JButton getBtSearchNext() {
		if (btSearchNext == null) {
			btSearchNext = new JButton("Next");
			btSearchNext.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getSearchField().searchNext();
				}
			});
		}
		return btSearchNext;
	}

	private JButton getBtSearchPrevious() {
		if (btSearchPrevious == null) {
			btSearchPrevious = new JButton("Previous");
			btSearchPrevious.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getSearchField().searchPrevious();
				}
			});
		}
		return btSearchPrevious;
	}

	protected JLabel getStatusLabel() {
		if (statusLabel == null) {
			statusLabel = new JLabel();
			statusLabel.setFont(new Font("Arial", Font.BOLD, 10));
		}
		return statusLabel;
	}

	protected JLabel getSearchLabel() {
		if (searchLabel == null) {
			searchLabel = new JLabel("Search:");
			searchLabel.setFont(new Font("Arial", Font.BOLD, 10));
		}
		return searchLabel;
	}

	protected JLabel getStatusInformation() {
		if (statusInformation == null) {
			statusInformation = new JLabel();
			statusInformation.setFont(new Font("Arial", Font.PLAIN, 11));
		}
		return statusInformation;
	}

	protected JPanel getProcessPanel() {
		if (processPanel == null) {
			processPanel = new JPanel();
			processPanel.setBorder(new TitledBorder("Process selection"));
			processPanel.setLayout(new BoxLayout(processPanel, BoxLayout.X_AXIS));
			addWithSpace(processPanel, getProcessListLabel(), 2);
			addWithSpace(processPanel, getProcessList(), 2);
			addWithSpace(processPanel, getExecuteProcessButton(), 2);
		}
		return processPanel;
	}

	private GlassPane getGlass() {
		if (glass == null) {
			glass = new GlassPane();
		}
		return glass;
	}

	private void addWithSpace(JPanel panel, Component c, int width) {
		panel.add(Box.createRigidArea(new Dimension(width, 0)));
		panel.add(c);
		panel.add(Box.createRigidArea(new Dimension(width, 0)));
	}

	protected JComboBox getProcessList() {
		if (processList == null) {
			Object[] processNames = config.getProcessNames().toArray();
			processList = new JComboBox(processNames);
		}
		return processList;
	}

	protected JLabel getProcessListLabel() {
		if (processListLabel == null) {
			processListLabel = new JLabel("Processes:");
		}
		return processListLabel;
	}

	protected JButton getExecuteProcessButton() {
		if (executeProcessButton == null) {
			executeProcessButton = new JButton("Execute process");
			executeProcessButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					executeSelectedProcess();
				}
			});
		}
		return executeProcessButton;
	}

	protected DataAnalysisPanel getDataAnalysisPanel() {
		if (dataAnalysisPanel == null) {
			dataAnalysisPanel = new DataAnalysisPanel(config.getSourceDatabaseConfig(), config.getDestinationDatabaseConfig());
			dataAnalysisPanel.setBorder(new TitledBorder("Data analysis"));
		}
		return dataAnalysisPanel;
	}

	protected void executeSelectedProcess() {
		Object selection = getProcessList().getSelectedItem();
		if (selection != null) {
			String processName = String.valueOf(selection);
			executeProcess(processName);
		}
	}

	protected void executeProcess(String processName) {
		final Runnable process = config.getProcess(processName);
		getGlass().activate("Executing process: '" + processName + "'...");

		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					long start = System.currentTimeMillis();
					SwingUtilities.invokeAndWait(process);
					long timeTaken = System.currentTimeMillis() - start;
					setStatus("Completed.", "Took " + timeTaken / 1000 + " seconds (" + timeTaken + " ms)");
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					setStatus("Interrupted", "");
				} catch (InvocationTargetException ie) {
					WindowUtils.showErrorMessage(DataAnalysisWindow.this, ie.getCause());
					setStatus("Interrupted", "");
				} finally {
					getGlass().deactivate();
				}
				System.gc();
			}
		};
		thread.start();
	}

	private void setStatus(String label, String message) {
		if (label != null) {
			getStatusLabel().setText(label);
		}
		if (message != null) {
			getStatusInformation().setText(message);
		}
	}

	public static void main(String... args) {
		DatabaseAccessor source = new DatabaseAccessor("usda_db", "source/db", "source/db/queries.properties");
		DatabaseAccessor destination = new DatabaseAccessor("destination", "destination/db", "destination/db/queries.properties");

		DataIntegrationConfig config = new DataIntegrationConfig();
		config.setSourceDatabaseConfig(source);
		config.setDestinationDatabaseConfig(destination);

		config.addProcess("Sleep and do nothing", new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});

		DataAnalysisWindow main = new DataAnalysisWindow(config);
		WindowUtils.displayWindow(main);
	}
}
