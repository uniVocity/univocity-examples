/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app.swing;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.univocity.examples.app.data.*;
import com.univocity.examples.utils.*;

public class DaoTable extends DataTable {

	private static final long serialVersionUID = 1645749413804350353L;

	private JComboBox tableList;
	private DefaultTableModel dataModel;
	private JTable dataTable;
	private Dao dao;
	private final Set<Integer> primaryKeyIndexes = new HashSet<Integer>();

	private JButton deleteButton;
	private JButton insertButton;
	private JButton saveButton;
	private JButton editButton;
	private JButton cancelButton;

	private JPanel commandPanel;

	private int editingRow = -1;
	private boolean inserting = false;
	private Object[] dataBeforeUpdate;

	public DaoTable(DatabaseAccessor database) {
		super(database);

		super.addListToQueryListPanel(getTableList(), "Tables:");
		loadTableData();
		enableComponents();
	}

	protected JButton getDeleteButton() {
		if (deleteButton == null) {
			deleteButton = new JButton("Delete");
			deleteButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					deleteSelectedRow();
				}
			});
		}
		return deleteButton;
	}

	protected JButton getInsertButton() {
		if (insertButton == null) {
			insertButton = new JButton("Insert");
			insertButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					insertNewRow();
				}
			});
		}
		return insertButton;
	}

	protected JButton getSaveButton() {
		if (saveButton == null) {
			saveButton = new JButton("Save");
			saveButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					stopTableCellEditing();
					saveChanges();
				}
			});
		}
		return saveButton;
	}

	protected JButton getEditButton() {
		if (editButton == null) {
			editButton = new JButton("Edit");
			editButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					editSelectedRow();
				}
			});
		}
		return editButton;
	}

	protected JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					stopTableCellEditing();
					cancelEditing();
				}
			});
		}
		return cancelButton;
	}

	@Override
	protected JPanel getCommandPanel() {
		if (commandPanel == null) {
			commandPanel = super.getCommandPanel();
			commandPanel.add(getCancelButton(), 0);
			commandPanel.add(getSaveButton(), 0);
			commandPanel.add(getDeleteButton(), 0);
			commandPanel.add(getEditButton(), 0);
			commandPanel.add(getInsertButton(), 0);
		}
		return commandPanel;
	}

	protected JComboBox getTableList() {
		if (tableList == null) {
			Object[] tables = database.getTableNames().toArray();
			tableList = new JComboBox(tables);
			tableList.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					loadTableData();
				}
			});
		}
		return tableList;
	}

	@Override
	protected JTable getDataTable() {
		if (dataTable == null) {
			dataTable = super.getDataTable();
			dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			dataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					rowSelected();
				}
			});
		}
		return dataTable;
	}

	@Override
	protected DefaultTableModel getDataModel() {
		if (dataModel == null) {
			dataModel = new DefaultTableModel() {
				private static final long serialVersionUID = -8483310131588419982L;

				@Override
				public boolean isCellEditable(int row, int column) {
					if (editingRow == row) {
						if (inserting) {
							return true;
						}
						return !primaryKeyIndexes.contains(column);
					}
					return false;
				}
			};
		}

		return dataModel;
	}

	protected void loadTableData() {
		Object selection = tableList.getSelectedItem();
		if (selection != null) {
			String tableName = String.valueOf(selection);
			dao = new Dao(database, tableName);

			reloadTable("select * from " + tableName);
		}
	}

	@Override
	protected void executeReloadDataProcess() {
		super.executeReloadDataProcess();
		loadPrimaryKeyIndexes();
	}

	private void loadPrimaryKeyIndexes() {
		primaryKeyIndexes.clear();
		Set<String> primaryKeys = dao.getPrimaryKeys();
		String[] columns = data.getColumnNames();
		for (int i = 0; i < columns.length; i++) {
			String column = columns[i].toLowerCase();
			if (primaryKeys.contains(column)) {
				primaryKeyIndexes.add(i);
			}
		}
		getCellRenderer().setDisabledColumnIndexes(primaryKeyIndexes);
	}

	private void saveChanges() {
		if (this.isEditing()) {
			Map<String, Object> rowData = createRowData(editingRow);
			try {
				if (inserting) {
					dao.insert(rowData);
				} else {
					dao.update(rowData);
				}
			} catch (Throwable ex) {
				cancelEditing();
				WindowUtils.showErrorMessage(this, ex);
			}
			setEditingRow(-1, false);
			cancelEditing();
		}
		enableComponents();
	}

	private void editSelectedRow() {
		setEditingRow(getDataTable().getSelectedRow(), false);
		dataBeforeUpdate = getDataOfRow(editingRow).toArray();
		enableComponents();
	}

	private void insertNewRow() {
		Object[] newRow = new Object[data.getColumnNames().length];
		getDataModel().addRow(newRow);

		int lastRow = getDataModel().getRowCount() - 1;
		setEditingRow(lastRow, true);

		getDataTable().getSelectionModel().setSelectionInterval(lastRow, lastRow);
		enableComponents();
	}

	private void setEditingRow(int row, boolean inserting) {
		this.editingRow = row;
		this.inserting = inserting;
		getCellRenderer().setEditingRow(row, inserting);
		getDataTable().repaint();
	}

	private void deleteSelectedRow() {
		if (isRowSelected()) {
			Map<String, Object> rowData = createRowData(getSelectedRow());
			try {
				dao.delete(rowData);
			} catch (Throwable ex) {
				WindowUtils.showErrorMessage(this, ex);
			}
			getDataModel().removeRow(getSelectedRow());
		}
		enableComponents();
	}

	private void rowSelected() {
		if (!isEditing()) {
			enableComponents();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void cancelEditing() {
		if (editingRow >= 0) {
			if (inserting) {
				getDataModel().removeRow(editingRow);
			} else {
				getDataModel().getDataVector().set(editingRow, new Vector(Arrays.asList(dataBeforeUpdate)));
			}
		}

		setEditingRow(-1, false);
		enableComponents();
	}

	private void enableComponents() {
		boolean editing = isEditing();
		getCancelButton().setEnabled(editing);
		getInsertButton().setEnabled(!editing);
		getEditButton().setEnabled(!editing && isRowSelected());
		getExecuteQueryButton().setEnabled(!editing);
		getSaveButton().setEnabled(editing);
		getDeleteButton().setEnabled(!editing && isRowSelected());
		getTableList().setEnabled(!editing);
		getQueryInput().setEnabled(!editing);
		getQueryList().setEnabled(!editing);
	}

	private boolean isRowSelected() {
		return getSelectedRow() >= 0;
	}

	private boolean isEditing() {
		return editingRow >= 0;
	}

	private int getSelectedRow() {
		return getDataTable().getSelectedRow();
	}

	private Vector<?> getDataOfRow(int rowIndex) {
		Vector<?> row = (Vector<?>) getDataModel().getDataVector().get(rowIndex);
		return row;
	}

	private Map<String, Object> createRowData(int rowIndex) {
		Vector<?> row = getDataOfRow(rowIndex);
		Map<String, Object> rowData = createRowData(row);
		return rowData;
	}

	private Map<String, Object> createRowData(Vector<?> row) {
		Map<String, Object> out = new HashMap<String, Object>();

		String[] columns = data.getColumnNames();
		for (int i = 0; i < columns.length; i++) {
			out.put(columns[i].toLowerCase(), row.get(i));
		}

		return out;
	}

	private void stopTableCellEditing() {
		TableCellEditor editor = getDataTable().getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
	}

	public static void main(String... args) {
		DatabaseAccessor sourceDatabase = new DatabaseAccessor("usda_db", "source/db", "source/db/queries.properties");
		WindowUtils.displayInExecutableWindow(new DaoTable(sourceDatabase));
	}
}
