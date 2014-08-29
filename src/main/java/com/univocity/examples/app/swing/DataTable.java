/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app.swing;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.undo.*;

import com.univocity.examples.app.data.*;
import com.univocity.examples.utils.*;

public class DataTable extends JRootPane {

	private static final long serialVersionUID = -1382052887720597635L;
	protected final Database database;

	protected Data data;
	protected Queries queries;

	private JPanel inputPanel;
	private JTextArea queryInput;
	private DefaultTableModel dataModel;
	private JTable dataTable;
	private JScrollPane dataTableScroll;
	private JScrollPane queryInputScroll;
	private JPanel queryInputPanel;
	private JButton executeQueryButton;
	private JPanel commandPanel;
	private DefaultTableColumnModel columnModel;
	private DataTableCellRenderer cellRenderer;

	private JPanel queryListPanel;
	private JComboBox queryList;

	private JSplitPane splitPane;
	private GlassPane glass;

	public DataTable(DatabaseAccessor accessor) {
		this.database = accessor.getDatabase();
		this.queries = accessor.getQueries();

		setGlassPane(getGlass());
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(getSplitPane(), BorderLayout.CENTER);
		getContentPane().add(getCommandPanel(), BorderLayout.SOUTH);

	}

	protected GlassPane getGlass() {
		if (glass == null) {
			glass = new GlassPane();
		}
		return glass;
	}

	protected JSplitPane getSplitPane() {
		if (splitPane == null) {
			splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
			splitPane.setTopComponent(getInputPanel());
			splitPane.setBottomComponent(getDataTableScroll());
			splitPane.setResizeWeight(0.3);
		}
		return splitPane;
	}

	private JPanel getQueryListPanel() {
		if (queryListPanel == null) {
			queryListPanel = new JPanel();
			queryListPanel.setLayout(new GridBagLayout());

			addListToQueryListPanel(getQueryList(), "Queries:");
		}
		return queryListPanel;
	}

	private GridBagConstraints getGridBagConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0;
		c.weighty = 0;
		c.anchor = GridBagConstraints.CENTER;
		c.gridy = queryListPanel.getComponentCount() / 2;
		return c;
	}

	protected void addListToQueryListPanel(JComboBox list, String label) {
		GridBagConstraints c = getGridBagConstraints();
		getQueryListPanel().add(new JLabel(label), c);

		c.gridx = 1;
		c.weightx = 1;

		queryListPanel.add(list, c);
	}

	protected JPanel getInputPanel() {
		if (inputPanel == null) {
			inputPanel = new JPanel();
			inputPanel.setLayout(new BorderLayout());
			inputPanel.add(getQueryListPanel(), BorderLayout.NORTH);
			inputPanel.add(getQueryInputPanel(), BorderLayout.CENTER);
		}
		return inputPanel;
	}

	protected JPanel getCommandPanel() {
		if (commandPanel == null) {
			commandPanel = new JPanel();
			commandPanel.setLayout(new FlowLayout());
			commandPanel.add(getExecuteQueryButton());
		}
		return commandPanel;
	}

	protected JPanel getQueryInputPanel() {
		if (queryInputPanel == null) {
			queryInputPanel = new JPanel();
			queryInputPanel.setLayout(new BorderLayout());
			queryInputPanel.setBorder(new TitledBorder("SQL"));
			queryInputPanel.add(getQueryInputScroll(), BorderLayout.CENTER);
		}
		return queryInputPanel;
	}

	protected JScrollPane getQueryInputScroll() {
		if (queryInputScroll == null) {
			queryInputScroll = new JScrollPane(getQueryInput());
		}
		return queryInputScroll;
	}

	protected JTextArea getQueryInput() {
		if (queryInput == null) {
			queryInput = new JTextArea(5, 70);

			final UndoManager undoManager = new UndoManager();

			queryInput.getDocument().addUndoableEditListener(new UndoableEditListener() {
				@Override
				public void undoableEditHappened(UndoableEditEvent e) {
					undoManager.addEdit(e.getEdit());
				}
			});

			InputMap inputMap = queryInput.getInputMap(JComponent.WHEN_FOCUSED);
			ActionMap actionMap = queryInput.getActionMap();

			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Undo");
			actionMap.put("Undo", new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (undoManager.canUndo()) {
						undoManager.undo();
					}
				}
			});

			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Redo");
			actionMap.put("Redo", new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (undoManager.canRedo()) {
						undoManager.redo();
					}
				}
			});

			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "Run");
			actionMap.put("Run", new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					executeQueryFromInput();
				}
			});
		}
		return queryInput;
	}

	protected JScrollPane getDataTableScroll() {
		if (dataTableScroll == null) {
			dataTableScroll = new JScrollPane(getDataTable());
		}
		return dataTableScroll;
	}

	protected JTable getDataTable() {
		if (dataTable == null) {
			dataTable = new JTable(getDataModel());
			dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			dataTable.setColumnModel(getColumnModel());
		}
		return dataTable;
	}

	protected DefaultTableColumnModel getColumnModel() {
		if (columnModel == null) {
			columnModel = new DefaultTableColumnModel();
		}
		return columnModel;
	}

	protected DefaultTableModel getDataModel() {
		if (dataModel == null) {
			dataModel = new DefaultTableModel();
		}
		return dataModel;
	}

	protected JButton getExecuteQueryButton() {
		if (executeQueryButton == null) {
			executeQueryButton = new JButton("Run (F5)");
			executeQueryButton.setToolTipText("Press F5 to execute your SQL command.");
			executeQueryButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					executeQueryFromInput();
				}
			});
		}
		return executeQueryButton;
	}

	protected DataTableCellRenderer getCellRenderer() {
		if (cellRenderer == null) {
			cellRenderer = new DataTableCellRenderer();
		}
		return cellRenderer;
	}

	private void executeQueryFromInput() {
		try {
			String sql = getQueryInput().getSelectedText();
			if (sql == null || sql.trim().isEmpty()) {
				sql = getQueryInput().getText();
			}
			reloadTable(sql);
		} catch (Exception ex) {
			WindowUtils.showErrorMessage(DataTable.this, ex);
		}
	}

	protected void reloadTable(String query) {
		if (query == null || query.trim().isEmpty()) {
			return;
		}

		if (!getQueryInput().getText().contains(query)) {
			getQueryInput().setText(query);
		}
		data = new Data(database, query);

		getGlass().activate("Processing...");

		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					reloadAndDisplayData();
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				} catch (InvocationTargetException ie) {
					WindowUtils.showErrorMessage(DataTable.this, ie.getCause());
				} finally {
					getGlass().deactivate();
				}
			}
		};
		thread.start();
	}

	private void reloadAndDisplayData() throws InterruptedException, InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				executeReloadDataProcess();
			}
		});
	}

	protected void executeReloadDataProcess() {
		data.reloadData();
		String[] columns = data.getColumnNames();
		List<Object[]> rows = data.getRows();

		getDataModel().setDataVector(rows.toArray(new Object[][] {}), columns);
		for (int i = 0; i < columns.length; i++) {
			TableColumn column = getColumnModel().getColumn(i);
			column.setPreferredWidth(100);
			column.setCellRenderer(getCellRenderer());
		}
	}

	protected JComboBox getQueryList() {
		if (queryList == null) {
			Object[] queries = this.queries.getQueryNames().toArray();
			queryList = new JComboBox(queries);
			queryList.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					loadSelectedQuery();
				}
			});
		}
		return queryList;
	}

	protected void loadSelectedQuery() {
		Object selection = queryList.getSelectedItem();
		if (selection != null) {
			String queryName = String.valueOf(selection);
			String query = queries.getQuery(queryName);
			reloadTable(query);
		}
	}

	public static void main(String... args) {
		DatabaseAccessor database = new DatabaseAccessor("usda_db", "source/db", "source/db/queries.properties");
		WindowUtils.displayInExecutableWindow(new DataTable(database));
	}
}
