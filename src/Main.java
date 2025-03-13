/**
 * Author: Abdullahi Sheikdon
 *
 * comment: This was a fun (albeit extremely frustrating) side project
 *          to expand my resume. This is my spin on the windows notepad app.
 */

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

public class Main {
    private JFrame frame;
    private JTextPane textPane;
    private JFileChooser fileChooser;
    private JComboBox<String> fontComboBox;
    private JComboBox<Integer> fontSizeComboBox;
    private UndoManager undoManager; // for undo functionality

    private JPanel findPanel;
    private JTextField searchField;
    private JButton nextButton;
    private JButton prevButton;
    private JButton closeFindButton;
    private java.util.List<int[]> searchResults = new ArrayList<>();
    private int currentSearchIndex = -1;
    private Highlighter.HighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

    public Main() {
        frame = new JFrame("Java Word Processor");
        textPane = new JTextPane();
        fileChooser = new JFileChooser();

        undoManager = new UndoManager();
        textPane.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        setupMenuBar();

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        JToolBar toolBar = setupToolBar();
        topPanel.add(toolBar);
        setupFindPanel();
        topPanel.add(findPanel);
        findPanel.setVisible(false);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(textPane), BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");

        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> SwingUtilities.invokeLater(Main::new));

        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exitItem = new JMenuItem("Exit");
        JMenuItem findItem = new JMenuItem("Find");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem cutItem = new JMenuItem("Cut");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");
        JMenuItem deleteItem = new JMenuItem("Delete");

        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile());
        exitItem.addActionListener(e -> System.exit(0));

        findItem.addActionListener(e -> toggleFindPanel());

        // undo
        undoItem.addActionListener(e -> {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
        });
        cutItem.addActionListener(e -> textPane.cut());
        copyItem.addActionListener(e -> textPane.copy());
        pasteItem.addActionListener(e -> textPane.paste());
        deleteItem.addActionListener(e -> textPane.replaceSelection(""));

        // add items to the list
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(exitItem);
        viewMenu.add(findItem);
        editMenu.add(undoItem);
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.add(deleteItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        frame.setJMenuBar(menuBar);
    }

    private JToolBar setupToolBar() {
        JToolBar toolBar = new JToolBar();
        int iconWidth = 24;
        int iconHeight = 24;

        JButton boldButton = new JButton(new ImageIcon(getScaledImage("/resources/bold.png", iconWidth, iconHeight)));
        JButton italicButton = new JButton(new ImageIcon(getScaledImage("/resources/italic.png", iconWidth, iconHeight)));
        JButton underlineButton = new JButton(new ImageIcon(getScaledImage("/resources/underline.png", iconWidth, iconHeight)));

        boldButton.setToolTipText("Bold");
        italicButton.setToolTipText("Italic");
        underlineButton.setToolTipText("Underline");

        boldButton.addActionListener(e -> toggleStyle(StyleConstants.Bold));
        italicButton.addActionListener(e -> toggleStyle(StyleConstants.Italic));
        underlineButton.addActionListener(e -> toggleStyle(StyleConstants.Underline));

        toolBar.add(boldButton);
        toolBar.add(italicButton);
        toolBar.add(underlineButton);

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontComboBox = new JComboBox<>(fonts);
        fontComboBox.addActionListener(e -> changeFont());

        Integer[] fontSizes = {8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 48, 56, 64};
        fontSizeComboBox = new JComboBox<>(fontSizes);
        fontSizeComboBox.setSelectedItem(12);
        fontSizeComboBox.addActionListener(e -> changeFont());

        toolBar.add(new JLabel(" Font: "));
        toolBar.add(fontComboBox);
        toolBar.add(new JLabel(" Size: "));
        toolBar.add(fontSizeComboBox);

        JButton alignLeftButton = new JButton(new ImageIcon(getScaledImage("/resources/align_left.png", iconWidth, iconHeight)));
        JButton alignCenterButton = new JButton(new ImageIcon(getScaledImage("/resources/align_center.png", iconWidth, iconHeight)));
        JButton alignRightButton = new JButton(new ImageIcon(getScaledImage("/resources/align_right.png", iconWidth, iconHeight)));

        alignLeftButton.setToolTipText("Align Left");
        alignCenterButton.setToolTipText("Align Center");
        alignRightButton.setToolTipText("Align Right");

        alignLeftButton.addActionListener(e -> toggleAlignment(StyleConstants.ALIGN_LEFT));
        alignCenterButton.addActionListener(e -> toggleAlignment(StyleConstants.ALIGN_CENTER));
        alignRightButton.addActionListener(e -> toggleAlignment(StyleConstants.ALIGN_RIGHT));

        toolBar.addSeparator();
        toolBar.add(alignLeftButton);
        toolBar.add(alignCenterButton);
        toolBar.add(alignRightButton);

        JButton highlightButton = new JButton("Highlight");
        highlightButton.addActionListener(e -> changeHighlightColor());

        JButton fontColorButton = new JButton("Font Color");
        fontColorButton.addActionListener(e -> changeFontColor());

        toolBar.addSeparator();
        toolBar.add(highlightButton);
        toolBar.add(fontColorButton);

        return toolBar;
    }

    private void setupFindPanel() {
        findPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel findLabel = new JLabel("Find:");
        searchField = new JTextField(20);
        prevButton = new JButton("Prev");
        nextButton = new JButton("Next");
        closeFindButton = new JButton("X");

        findPanel.add(findLabel);
        findPanel.add(searchField);
        findPanel.add(prevButton);
        findPanel.add(nextButton);
        findPanel.add(closeFindButton);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateHighlights(); }
            public void removeUpdate(DocumentEvent e) { updateHighlights(); }
            public void changedUpdate(DocumentEvent e) { updateHighlights(); }
        });

        nextButton.addActionListener(e -> nextOccurrence());
        prevButton.addActionListener(e -> previousOccurrence());
        closeFindButton.addActionListener(e -> {
            findPanel.setVisible(false);
            textPane.getHighlighter().removeAllHighlights();
        });
    }

    private void updateHighlights() {
        Highlighter highlighter = textPane.getHighlighter();
        highlighter.removeAllHighlights();
        searchResults.clear();
        currentSearchIndex = -1;

        try {
            String text = textPane.getDocument().getText(0, textPane.getDocument().getLength());
            String searchTerm = searchField.getText();
            if (searchTerm == null || searchTerm.isEmpty()) {
                return;
            }
            String lowerText = text.toLowerCase();
            String lowerSearchTerm = searchTerm.toLowerCase();

            int index = 0;
            while ((index = lowerText.indexOf(lowerSearchTerm, index)) >= 0) {
                int end = index + searchTerm.length();
                highlighter.addHighlight(index, end, highlightPainter);
                searchResults.add(new int[]{index, end});
                index = end;
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void nextOccurrence() {
        if (searchResults.isEmpty()) return;
        currentSearchIndex = (currentSearchIndex + 1) % searchResults.size();
        int[] pos = searchResults.get(currentSearchIndex);
        textPane.setCaretPosition(pos[0]);
        textPane.requestFocusInWindow();
    }

    private void previousOccurrence() {
        if (searchResults.isEmpty()) return;
        currentSearchIndex = (currentSearchIndex - 1 + searchResults.size()) % searchResults.size();
        int[] pos = searchResults.get(currentSearchIndex);
        textPane.setCaretPosition(pos[0]);
        textPane.requestFocusInWindow();
    }

    private void toggleFindPanel() {
        boolean isVisible = findPanel.isVisible();
        findPanel.setVisible(!isVisible);
        if (!isVisible) {
            searchField.requestFocusInWindow();
        } else {
            textPane.getHighlighter().removeAllHighlights();
        }
        frame.revalidate();
    }

    private Image getScaledImage(String path, int width, int height) {
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(path));
            Image img = icon.getImage();
            return img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openFile() {
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileChooser.getSelectedFile()))) {
                textPane.setText("");
                String line;
                while ((line = reader.readLine()) != null) {
                    textPane.setText(textPane.getText() + line + "\n");
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error opening file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        int returnValue = fileChooser.showSaveDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                writer.write(textPane.getText());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleStyle(Object style) {
        StyledDocument doc = textPane.getStyledDocument();
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();
        if (start != end) {
            MutableAttributeSet attr = new SimpleAttributeSet(doc.getCharacterElement(start).getAttributes());
            boolean isSet = false;
            if (style == StyleConstants.Bold) isSet = StyleConstants.isBold(attr);
            if (style == StyleConstants.Italic) isSet = StyleConstants.isItalic(attr);
            if (style == StyleConstants.Underline) isSet = StyleConstants.isUnderline(attr);

            if (style == StyleConstants.Bold) StyleConstants.setBold(attr, !isSet);
            if (style == StyleConstants.Italic) StyleConstants.setItalic(attr, !isSet);
            if (style == StyleConstants.Underline) StyleConstants.setUnderline(attr, !isSet);

            doc.setCharacterAttributes(start, end - start, attr, false);
        }
    }

    private void toggleAlignment(int alignment) {
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setAlignment(attr, alignment);

        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();

        if (start != end) {
            doc.setParagraphAttributes(start, end - start, attr, false);
        } else {
            doc.setParagraphAttributes(0, doc.getLength(), attr, false);
        }
    }

    private void changeFont() {
        String selectedFont = (String) fontComboBox.getSelectedItem();
        int selectedSize = (int) fontSizeComboBox.getSelectedItem();

        StyledDocument doc = textPane.getStyledDocument();
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();

        if (start != end) {
            MutableAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setFontFamily(attr, selectedFont);
            StyleConstants.setFontSize(attr, selectedSize);
            doc.setCharacterAttributes(start, end - start, attr, false);
        } else {
            textPane.setFont(new Font(selectedFont, Font.PLAIN, selectedSize));
        }
    }

    private void changeHighlightColor() {
        Color color = JColorChooser.showDialog(frame, "Choose Highlight Color", Color.YELLOW);
        if (color != null) {
            StyledDocument doc = textPane.getStyledDocument();
            int start = textPane.getSelectionStart();
            int end = textPane.getSelectionEnd();
            if (start != end) {
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setBackground(attr, color);
                doc.setCharacterAttributes(start, end - start, attr, false);
            }
        }
    }

    private void changeFontColor() {
        Color color = JColorChooser.showDialog(frame, "Choose Font Color", Color.BLACK);
        if (color != null) {
            StyledDocument doc = textPane.getStyledDocument();
            int start = textPane.getSelectionStart();
            int end = textPane.getSelectionEnd();
            if (start != end) {
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, color);
                doc.setCharacterAttributes(start, end - start, attr, false);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
