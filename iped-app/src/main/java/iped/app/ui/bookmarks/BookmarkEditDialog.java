package iped.app.ui.bookmarks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import iped.app.ui.Messages;

public class BookmarkEditDialog extends JDialog {
    private static final long serialVersionUID = -8204366293115657785L;

    private String newName;
    private Color newColor, selColor;

    private JButton selButton;

    public BookmarkEditDialog(JDialog parent, String currentName, Color currentColor) {
        super(parent, ModalityType.APPLICATION_MODAL);
        setTitle(Messages.getString("BookmarksManager.Edit.Title"));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(content);

        // Bookmark name
        JPanel p1 = new JPanel(new BorderLayout(2, 2));
        add(p1, BorderLayout.NORTH);

        p1.add(new JLabel(Messages.getString("BookmarksManager.Edit.Name") + ":"), BorderLayout.NORTH);

        JTextField txtName = new JTextField();
        txtName.setText(currentName);
        txtName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtName.selectAll();
            }
        });
        p1.add(txtName, BorderLayout.CENTER);

        // Bookmark color
        if (currentColor == null) {
            currentColor = BookmarkStandardColors.defaultColor;
        }
        JPanel p2 = new JPanel(new BorderLayout(2, 2));
        add(p2, BorderLayout.CENTER);

        p2.add(new JLabel(Messages.getString("BookmarksManager.Edit.Color") + ":"), BorderLayout.NORTH);

        int numColors = BookmarkStandardColors.colors.length + 1;
        JPanel colorGrid = new JPanel(new GridLayout(BookmarkStandardColors.numStandardColors / 9, 10, 2, 2));
        int x = 0;
        int y = 0;
        boolean seen = false;
        selColor = currentColor;
        for (int i = 0; i < numColors; i++) {
            int idx = x < 9 ? x + y * 9 : BookmarkStandardColors.numStandardColors + y;
            Color color = idx < BookmarkStandardColors.colors.length ? BookmarkStandardColors.colors[idx] : null;
            boolean checked = false;
            if (color == null) {
                if (seen) {
                    break;
                }
                color = currentColor;
                checked = true;
            } else if (color.equals(currentColor)) {
                checked = true;
                seen = true;
            }
            BookmarkIcon icon = BookmarkIcon.getIcon(color, checked);
            JButton but = new JButton(icon);
            if (checked) {
                selButton = but;
            }
            colorGrid.add(but);
            but.setFocusable(true);
            but.setFocusPainted(false);
            but.setBorder(null);
            but.setContentAreaFilled(false);
            but.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (selButton != null) {
                        ((BookmarkIcon) (selButton.getIcon())).setChecked(false);
                        selButton.repaint();
                    }
                    selColor = icon.getColor();
                    icon.setChecked(true);
                    selButton = but;
                }
            });

            if (++x == 10) {
                x = 0;
                y++;
            }
        }
        p2.add(colorGrid, BorderLayout.CENTER);

        // Buttons
        JPanel p3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
        add(p3, BorderLayout.SOUTH);

        Dimension dim = new Dimension(100, 30);
        JButton butOk = new JButton();
        butOk.setText(UIManager.get("OptionPane.okButtonText").toString());
        butOk.setPreferredSize(dim);
        p3.add(butOk);

        JButton butCancel = new JButton();
        butCancel.setPreferredSize(dim);
        butCancel.setText(UIManager.get("OptionPane.cancelButtonText").toString());
        p3.add(butCancel);

        butOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                newName = txtName.getText().trim();
                newColor = selColor;
                setVisible(false);
            }
        });

        butCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        pack();
    }

    public String getNewName() {
        return newName;
    }

    public Color getNewColor() {
        return newColor;
    }
}
