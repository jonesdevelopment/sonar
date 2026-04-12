/*
 * Copyright (C) 2025 CaptchaGenerator Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.captcha;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class CaptchaGeneratorUI extends JFrame {
  private static final char[] DICTIONARY = {
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k',
    'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
    'x', 'y', 'z', '2', '3', '4', '5', '6', '7', '8', '9'
  };

  private final JLabel imageLabel;
  private final JTextField answerField;
  private final JButton generateButton;
  private final JButton saveButton;
  private final JComboBox<String> typeComboBox;
  private final JSpinner lengthSpinner;
  private final SpinnerNumberModel lengthModel;

  private BufferedImage currentImage;
  private String currentAnswer;

  private final StandardCaptchaGenerator standardGenerator;
  private final ComplexCaptchaGenerator complexGenerator;
  private final LegacyCaptchaGenerator legacyGenerator;
  private final Random random = new Random();

  public CaptchaGeneratorUI() {
    super("Captcha Generator");

    standardGenerator = new StandardCaptchaGenerator(null);
    complexGenerator = new ComplexCaptchaGenerator(null);
    legacyGenerator = new LegacyCaptchaGenerator(null);

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(10, 10));

    // Image panel
    imageLabel = new JLabel();
    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    imageLabel.setPreferredSize(new Dimension(300, 300));
    imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    add(imageLabel, BorderLayout.CENTER);

    // Control panel
    JPanel controlPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    // Captcha type
    gbc.gridx = 0;
    gbc.gridy = 0;
    controlPanel.add(new JLabel("Type:"), gbc);
    gbc.gridx = 1;
    typeComboBox = new JComboBox<>(new String[]{"Standard", "Complex", "Legacy"});
    controlPanel.add(typeComboBox, gbc);

    // Length
    gbc.gridx = 0;
    gbc.gridy = 1;
    controlPanel.add(new JLabel("Length:"), gbc);
    gbc.gridx = 1;
    lengthModel = new SpinnerNumberModel(5, 3, 10, 1);
    lengthSpinner = new JSpinner(lengthModel);
    controlPanel.add(lengthSpinner, gbc);

    // Generate button
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    generateButton = new JButton("Generate Captcha");
    generateButton.addActionListener(e -> generateCaptcha());
    controlPanel.add(generateButton, gbc);

    // Answer field
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 1;
    controlPanel.add(new JLabel("Answer:"), gbc);
    gbc.gridx = 1;
    answerField = new JTextField(15);
    answerField.setEditable(false);
    controlPanel.add(answerField, gbc);

    // Save button
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 2;
    saveButton = new JButton("Save Image");
    saveButton.addActionListener(e -> saveImage());
    controlPanel.add(saveButton, gbc);

    add(controlPanel, BorderLayout.SOUTH);

    pack();
    setLocationRelativeTo(null);
    setResizable(false);

    // Generate initial captcha
    generateCaptcha();
  }

  private void generateCaptcha() {
    int length = lengthModel.getNumber().intValue();
    char[] answer = new char[length];
    for (int i = 0; i < length; i++) {
      answer[i] = DICTIONARY[random.nextInt(DICTIONARY.length)];
    }
    currentAnswer = new String(answer);

    String type = (String) typeComboBox.getSelectedItem();
    switch (type) {
      case "Complex":
        currentImage = complexGenerator.createImage(answer);
        break;
      case "Legacy":
        currentImage = legacyGenerator.createImage(answer);
        break;
      default:
        currentImage = standardGenerator.createImage(answer);
        break;
    }

    imageLabel.setIcon(new ImageIcon(currentImage.getScaledInstance(256, 256, Image.SCALE_SMOOTH)));
    answerField.setText(currentAnswer);
  }

  private void saveImage() {
    if (currentImage == null) {
      JOptionPane.showMessageDialog(this, "No image to save!", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save Captcha Image");
    fileChooser.setSelectedFile(new File("captcha.png"));
    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Images", "png"));

    int userSelection = fileChooser.showSaveDialog(this);
    if (userSelection == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      if (!file.getName().toLowerCase().endsWith(".png")) {
        file = new File(file.getAbsolutePath() + ".png");
      }
      try {
        ImageIO.write(currentImage, "png", file);
        JOptionPane.showMessageDialog(this, "Image saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Failed to save image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception e) {
        e.printStackTrace();
      }
      new CaptchaGeneratorUI().setVisible(true);
    });
  }
}
