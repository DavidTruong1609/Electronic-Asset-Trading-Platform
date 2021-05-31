package ElectronicAssetTradingPlatform.GUI.OrgUnitMembersandLeader;

import ElectronicAssetTradingPlatform.AssetTrading.BuyOffer;
import ElectronicAssetTradingPlatform.AssetTrading.BuyOfferData;
import ElectronicAssetTradingPlatform.Exceptions.DatabaseException;
import ElectronicAssetTradingPlatform.Server.NetworkDataSource;
import ElectronicAssetTradingPlatform.Users.OrganisationalUnitMembers;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.TreeMap;


public class BuyTabGUI extends JPanel {
    // Global variables
    private OrganisationalUnitMembers loggedInMember;
    private NetworkDataSource data;
    private JPanel wrapper;
    private JPanel orgBuyOffersPanel;
    private JLabel orgBuyOfferLabel;
    private JLabel welcomeMessage;
    private JPanel marketBuyOffersPanel;
    private JScrollPane scrollPanel;
    private JLabel marketBuyOffersLabel;

    TreeMap<Integer, BuyOffer> buyOffers;


    public BuyTabGUI(OrganisationalUnitMembers member, NetworkDataSource dataSource) {
        loggedInMember = member;
        data = dataSource;
        // create a wrapper to put elements in
        wrapper = Helper.createPanel(Color.WHITE);
        BoxLayout boxlayout = new BoxLayout(wrapper, BoxLayout.Y_AXIS);
        wrapper.setLayout(boxlayout);
        wrapper.setPreferredSize(new Dimension(850, 600));
        this.add(wrapper);
        // add a welcome message into the wrapper
        welcomeMessage = Helper.createLabel(memberTextDisplay(), 16);
        welcomeMessage.setHorizontalAlignment(JLabel.CENTER);
        welcomeMessage.setBorder(new EmptyBorder(10, 0, 10, 0));
        wrapper.add(welcomeMessage);
        // add org buy offer panel
        orgBuyOffersPanel = Helper.createPanel(Color.WHITE);
        orgBuyOfferLabel = Helper.createLabel(member.getUnitName() + "'s Buy Offers:", 20);
        orgBuyOfferLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
        // add a label to the org buy offer panel
        orgBuyOffersPanel.add(orgBuyOfferLabel);
        // add a table to the org buy offer panel
        JTable buyOffersTable = unitBuyOffersTable();
        int buyTableHeight = buyOffersTable.getPreferredSize().height + 25;
        // if the buy table's height is greater than 250
        if (buyTableHeight >= 250) {
            // create scroll panel with table inside
           scrollPanel = new JScrollPane(buyOffersTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
           // set the org buy offer panel to a FIXED 325
           orgBuyOffersPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 325));
           orgBuyOffersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 325));
           // set the scroll panel to a FIXED 250
           scrollPanel.setPreferredSize(new Dimension(850, 250));
           scrollPanel.setMaximumSize(new Dimension(850, 250));
        }
        else {
            // create scroll panel with table inside, but never allow vertical scrolling
            scrollPanel = new JScrollPane(buyOffersTable, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            int height = buyOffersTable.getPreferredSize().height + 125;
            // set height of panel to the buy offer table's size + 125 (VARIABLE size)
            orgBuyOffersPanel.setPreferredSize(new Dimension(825, height));
            orgBuyOffersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            // set height of scroll panel to a VARIABLE size equal to the buy offers table's height
            scrollPanel.setPreferredSize(new Dimension(850, buyOffersTable.getPreferredSize().height + 25));
        }
        scrollPanel.getViewport().setBackground(Color.WHITE);
        // add the scroll panel to the org buy offer's panel
        orgBuyOffersPanel.add(scrollPanel);
        // add panel to wrapper
        wrapper.add(orgBuyOffersPanel);

        marketBuyOffersPanel = Helper.createPanel(Color.GRAY);
        marketBuyOffersLabel = Helper.createLabel("Market Buy Offers", 20);
        marketBuyOffersPanel.add(marketBuyOffersLabel);
        wrapper.add(marketBuyOffersPanel);



    }

    private String memberTextDisplay() {
        String memberTextDisplay = "";
        memberTextDisplay += "Welcome" + loggedInMember.getUsername();
        // this.add(new JTextArea("welcome " + loggedInMember.getUsername()));
        try {
            float credits = data.getCredits(loggedInMember.getUnitName());
            System.out.println(credits);
            memberTextDisplay += "\t" + loggedInMember.getUnitName() + ": " + Float.toString(credits) + " credits";
        }
        catch (DatabaseException e) {
            e.printStackTrace();
        }
        return memberTextDisplay;
    }

    private TreeMap<Integer, BuyOffer> getUnitBuyOffers() throws Exception {
        buyOffers = null;
        buyOffers = BuyOfferData.getInstance().getOrgOffersMap(loggedInMember.getUnitName());


        if (buyOffers == null) {
            throw new Exception(); // maybe don't 'throw' an exception just display nothing
        }

        return buyOffers;

    }

    // https://stackoverflow.com/questions/17627431/auto-resizing-the-jtable-column-widths by Paul Vargas
    public void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 15; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width +1 , width);
            }
            if(width > 300)
                width=300;
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    // TODO : REMOVE ORG UNIT AS A COLUMN, AND ISNTEAD HAVE A BUTTON TO EDIT THE ASSET OFFER LISTING
    private JTable unitBuyOffersTable() {
        String data[][] = getRowData();
        String columns[] = { "Offer ID", "Asset Name", "Quantity", "Price", "Offer Creator", "Edit/Delete"};
        JTable buyOffersTable = new JTable(data, columns);
        resizeColumnWidth(buyOffersTable);
        buyOffersTable.setRowHeight(25);
        buyOffersTable.setFont(new Font ( "Dialog", Font.PLAIN, 14));

//        Font font = buyOffersTable.getFont();
//        Map attributes = font.getAttributes();
//
//        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
//        buyOffersTable.setFont(font.deriveFont(attributes));





        buyOffersTable.getTableHeader().setPreferredSize(new Dimension(150,25));
        buyOffersTable.getTableHeader().setFont(new Font ( "Dialog", Font.BOLD, 14));
        buyOffersTable.getTableHeader().setReorderingAllowed(false);
        // set button column for Edit/Delete column
        buyOffersTable.getColumn("Edit/Delete").setCellRenderer(new ButtonRenderer());
        buyOffersTable.getColumn("Edit/Delete").setCellEditor(new ButtonEditor(new JCheckBox()));

        // center table cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment( JLabel.CENTER );
        for(int x = 0; x < columns.length; x++){
            buyOffersTable.getColumnModel().getColumn(x).setCellRenderer( centerRenderer );
        }
        // set an underline for the edit/delete buttons
        // https://forums.codeguru.com/showthread.php?38965-Change-background-color-of-one-column-in-JTable#:~:text=Re%3A%20Change%20background%20color%20of,before%20returning%20the%20renderer%2C%20e.g.
        buyOffersTable.getColumn("Edit/Delete").setCellRenderer(
            new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row,
                                                               int column) {
                    setText(value.toString());
                    setHorizontalAlignment(JLabel.CENTER);
                    Font font = buyOffersTable.getFont();
                    setFont(font);
                    Map attributes = font.getAttributes();
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    setFont(font.deriveFont(attributes));
                    return this;
                }
            });
        // prevent editing of cells in table
        buyOffersTable.setDefaultEditor(Object.class, null);
        // set the preferred size of the table
        Dimension preferredSize = new Dimension(825, buyOffersTable.getRowCount() * 25);

        if (preferredSize.height < 225) {
            System.out.println("setting a preferred size for the table");
            buyOffersTable.setPreferredSize(preferredSize);
        }
        return buyOffersTable;
    }


    private String[][] getRowData() {
        try {
            buyOffers = getUnitBuyOffers();
        }

        catch (Exception e) {
            e.printStackTrace();
        }

        String[][] data = new String[buyOffers.size()][];
        int count = 0;
        for(Map.Entry<Integer, BuyOffer> entry : buyOffers.entrySet()) {
            //Integer key = entry.getKey();
            BuyOffer value = entry.getValue();


            data[count] = new String[] {
                String.valueOf(value.getOfferID()),
                value.getAssetName(),
                String.valueOf(value.getQuantity()),
                String.valueOf(value.getPricePerUnit()),
                value.getUsername(),
                "Edit/Delete"
            };
            count++;
        }
        return data;
    }


}

// Button Renderer and Editor classes from: https://camposha.info/java-jtable-buttoncolumn-tutorial/

class ButtonRenderer extends JButton implements TableCellRenderer {

    public ButtonRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        System.out.println("button renderer working");
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(UIManager.getColor("Button.background"));
        }
        setText((value == null) ? "" : value.toString());
        return this;
    }
}


class ButtonEditor extends DefaultCellEditor {

    protected JButton button;
    private String label;
    private boolean isPushed;

    public ButtonEditor(JCheckBox checkBox) {
        super(checkBox);
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        if (isSelected) {
            button.setForeground(table.getSelectionForeground());
            button.setBackground(table.getSelectionBackground());
        } else {
            button.setForeground(table.getForeground());
            button.setBackground(table.getBackground());
            Font font = button.getFont();
            Map attributes = font.getAttributes();
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            button.setFont(font.deriveFont(attributes));
        }
        label = (value == null) ? "" : value.toString();
        button.setText(label);
        isPushed = true;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        if (isPushed) {
            JOptionPane.showMessageDialog(button, label + ": Ouch!");
        }
        isPushed = false;
        return label;
    }

    @Override
    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }

    @Override
    protected void fireEditingStopped() {
        super.fireEditingStopped();
    }
}