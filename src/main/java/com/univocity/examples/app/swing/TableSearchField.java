/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app.swing;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class TableSearchField extends JTextField {

	private static final long serialVersionUID = 5671253307650335821L;

	private final Set<JTable> tables = new LinkedHashSet<JTable>();

	private final Map<JTable, Integer> nextRows = new HashMap<JTable, Integer>();
	private final Map<JTable, Integer> previousRows = new HashMap<JTable, Integer>();

	private final StringBuilder rowContent = new StringBuilder();

	private String previousSearch = null;
	private boolean findingNext = true;

	public TableSearchField() {
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				find();
			}
		});
	}

	public void searchNext() {
		findingNext = true;
		find();
	}

	public void searchPrevious() {
		findingNext = false;
		find();
	}

	private void find() {
		String value = getText();

		if (value.trim().isEmpty()) {
			return;
		}

		if (!value.equals(previousSearch)) {
			nextRows.clear();
			previousRows.clear();
			previousSearch = value;
		}

		for (JTable table : tables) {
			if (table.getRowCount() == 0) {
				continue;
			}

			int selected = table.getSelectedRow();
			if (selected != -1) {
				nextRows.put(table, selected + 1);
				previousRows.put(table, selected - 1);
			}

			table.clearSelection();

			find(value, table);
		}
	}

	private String getRowContent(JTable table, int row, int cols) {
		rowContent.setLength(0);

		for (int col = 0; col < cols; col++) {
			if (rowContent.length() > 0) {
				rowContent.append(", ");
			}
			Object value = table.getValueAt(row, col);
			if (value != null) {
				rowContent.append(value);
			}
		}
		return rowContent.toString().toLowerCase();
	}

	private boolean match(JTable table, int row, int cols, String search) {
		String content = getRowContent(table, row, cols);
		if (content.contains(search.toLowerCase())) {
			table.scrollRectToVisible(table.getCellRect(row, 0, true));
			if (table.getSelectedRow() != row) {
				table.setRowSelectionInterval(row, row);
			}
			nextRows.put(table, row + 1);
			previousRows.put(table, row - 1);
			return true;
		}
		return false;
	}

	private void find(String search, JTable table) {
		int rows = table.getRowCount();
		int cols = table.getColumnCount();

		Integer start = findingNext ? nextRows.get(table) : previousRows.get(table);
		if (start == null || start >= rows) {
			start = 0;
		}

		if (start < 0) {
			start = rows - 1;
		}

		int row = start;

		int count = 0;
		while (count++ < rows) {
			if (match(table, row, cols, search)) {
				return;
			}

			if (findingNext) {
				row = (row + 1) % rows;
			} else {
				row = row - 1 >= 0 ? row - 1 : rows - 1;
			}
		}
	}

	public void addTable(final JTable table) {
		tables.add(table);

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int row = table.getSelectedRow();
					if (row != -1) {
						String content = getRowContent(table, row, table.getColumnCount());
						setText(content);
					}
				}
			}
		});
	}
}
