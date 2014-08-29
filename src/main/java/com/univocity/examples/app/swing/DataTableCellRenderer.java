/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app.swing;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class DataTableCellRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = -6473189450055913121L;

	private int editingRow = -1;
	private Set<Integer> disabledColumnIndexes = Collections.emptySet();
	private boolean inserting = false;

	private static final Color background0 = Color.WHITE;
	private static final Color background1 = new Color(220, 220, 220);
	private static final Color disabledFontColor = Color.DARK_GRAY;
	private static final Font disabledFont = new Font("Arial", Font.BOLD | Font.ITALIC, 12);
	private static final Font enabledFont = new Font("Arial", Font.PLAIN, 11);

	private static final Border border = LineBorder.createBlackLineBorder();

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		c.setBackground(row % 2 == 0 ? background0 : background1);

		if (!(c instanceof JComponent)) {
			return c;
		}

		JComponent cellComponent = ((JComponent) c);

		if (disabledColumnIndexes.contains(column)) {
			displayDisabled(cellComponent);
		} else {
			displayEnabled(cellComponent);
		}

		if (editingRow == row) {
			if (!disabledColumnIndexes.contains(column) || inserting) {
				displayCellForEditing(cellComponent);
			}
		}

		return c;
	}

	private void displayEnabled(JComponent cellComponent) {
		cellComponent.setForeground(Color.BLACK);
		cellComponent.setFont(enabledFont);
	}

	private void displayDisabled(JComponent cellComponent) {
		cellComponent.setForeground(disabledFontColor);
		cellComponent.setFont(disabledFont);
	}

	private void displayCellForEditing(JComponent cellComponent) {
		displayEnabled(cellComponent);
		cellComponent.setBorder(border);
		cellComponent.setBackground(background0);
	}

	public void setEditingRow(int editingRow, boolean inserting) {
		this.editingRow = editingRow;
		this.inserting = inserting;
	}

	public void setDisabledColumnIndexes(Set<Integer> disabledColumnIndexes) {
		this.disabledColumnIndexes = disabledColumnIndexes;
		if (disabledColumnIndexes == null) {
			this.disabledColumnIndexes = Collections.emptySet();
		}
	}

}
