/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

public class GlassPane extends JComponent implements KeyListener {
	private static final long serialVersionUID = -1474008961739943201L;

	private final static Color DEFAULT_BACKGROUND = new Color(200, 200, 200, 125);
	private final static Border MESSAGE_BORDER = new EmptyBorder(10, 10, 10, 10);

	private JLabel message = new JLabel();

	@SuppressWarnings("unchecked")
	public GlassPane() {
		setOpaque(false);
		setBackground(DEFAULT_BACKGROUND);

		setForeground(Color.BLACK);

		setLayout(new GridBagLayout());
		add(message, new GridBagConstraints());

		message.setOpaque(true);
		message.setBorder(MESSAGE_BORDER);

		addMouseListener(new MouseAdapter() {
		});
		addMouseMotionListener(new MouseMotionAdapter() {
		});

		addKeyListener(this);

		setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
		setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getSize().width, getSize().height);
	}

	@Override
	public void setBackground(Color background) {
		super.setBackground(background);

		Color messageBackground = new Color(background.getRed(), background.getGreen(), background.getBlue());
		message.setBackground(messageBackground);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		e.consume();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		e.consume();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		e.consume();
	}

	public void activate() {
		activate(null);
	}

	public void activate(String text) {
		if (text != null && text.length() > 0) {
			message.setVisible(true);
			message.setText(text);
			message.setForeground(getForeground());
		} else {
			message.setVisible(false);
		}

		setVisible(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		requestFocusInWindow();
	}

	public void deactivate() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setCursor(null);
				setVisible(false);
			}
		});
	}

	public static void main(String[] args) {
		final GlassPane glassPane = new GlassPane();

		final JTextField textField = new JTextField();

		final JButton button = new JButton("Click Me");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				glassPane.activate("Please Wait...");

				Thread thread = new Thread() {
					@Override
					public void run() {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
						}

						glassPane.deactivate();
					}
				};
				thread.start();
			}
		});

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setGlassPane(glassPane);
		frame.getContentPane().add(new JLabel("NORTH"), BorderLayout.NORTH);
		frame.getContentPane().add(button);
		frame.getContentPane().add(new JTextField(), BorderLayout.SOUTH);
		frame.getContentPane().add(textField, BorderLayout.SOUTH);
		frame.setSize(300, 300);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
