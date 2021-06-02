package ElectronicAssetTradingPlatform.GUI.OrgUnitMembersandLeader;

import ElectronicAssetTradingPlatform.AssetTrading.OrganisationalUnit;
import ElectronicAssetTradingPlatform.AssetTrading.TradeHistory;
import ElectronicAssetTradingPlatform.Database.MarketplaceHistoryDataSource;
import ElectronicAssetTradingPlatform.Server.NetworkDataSource;
import ElectronicAssetTradingPlatform.Users.OrganisationalUnitMembers;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Date;
import java.util.Collection;
import java.util.TreeMap;

public class OrgUnitTabGUI extends JPanel {
    private NetworkDataSource dataSource;
    private OrganisationalUnitMembers member;

    public OrgUnitTabGUI(OrganisationalUnitMembers member, NetworkDataSource dataSource) {
        this.member = member;
        this.dataSource = dataSource;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel tradeHistoryLabel = Helper.createLabel("Your Unit Trade History", 14);
        tradeHistoryLabel.setBorder(new EmptyBorder(20, 0, 5, 0));
        this.add(tradeHistoryLabel);
        this.add(unitTradeHistoryTable());
    }

    public JPanel unitTradeHistoryTable() {
        String[] columnNames = {"Name", "Quantity", "Price", "Total", "Trade Date", "Created by", "To/From"};

        // TODO: replace mock (db) with server data
        TreeMap<Integer, TradeHistory> data = new TreeMap<>();
        data.put(0, new TradeHistory("iPhone 16", 5, 11.25F, new Date(System.currentTimeMillis()), "User1", new OrganisationalUnit("org3", 10)));
        data.put(1, new TradeHistory("iPhone 11", 3, 10, new Date(System.currentTimeMillis()), "User4", new OrganisationalUnit("org10", 10)));
        data.put(2, new TradeHistory("iPhone 17", 25, 10.5F, new Date(System.currentTimeMillis()), "User2", new OrganisationalUnit("org12", 10)));
        data.put(3, new TradeHistory("iPhone 10", 5, 11, new Date(System.currentTimeMillis()), "User4", new OrganisationalUnit("org11", 10)));
        data.put(4, new TradeHistory("iPhone 15", 6, 13, new Date(System.currentTimeMillis()), "User2", new OrganisationalUnit("org10", 10)));
        data.put(5, new TradeHistory("iPhone 16", 5, 10, new Date(System.currentTimeMillis()), "User2", new OrganisationalUnit("org3", 10)));
        data.put(6, new TradeHistory("iPhone 11", 8, 14, new Date(System.currentTimeMillis()), "User1", new OrganisationalUnit("org5", 10)));
        data.put(7, new TradeHistory("iPhone 16", 5, 11.5F, new Date(System.currentTimeMillis()), "User1", new OrganisationalUnit("org4", 10)));
        data.put(8, new TradeHistory("iPhone 11", 10, 12, new Date(System.currentTimeMillis()), "User4", new OrganisationalUnit("org7", 10)));
        data.put(9, new TradeHistory("iPhone 11", 5, 10, new Date(System.currentTimeMillis()), "User1", new OrganisationalUnit("org7", 10)));

        Collection values = data.values();
        //dataSource.

        // Create Object[][] for table data
        Object[][] tableData = new Object[data.size()][];
        int count = 0;
        for (Object row : values) {
            TradeHistory rowData = (TradeHistory) row;
            int quantity = rowData.getTradedQuantity();
            float price = rowData.getPrice();
            tableData[count] = new Object[]{
                    rowData.getAssetName(),
                    quantity,
                    price,
                    quantity * price,
                    rowData.getDateFulfilled().toString(),
                    rowData.getCreatorUsername(),
                    rowData.getunitOfTrader().getUnitName()
            };
            count++;
        }

        JTable table = new JTable(tableData, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(700, table.getRowCount() * table.getRowHeight()));
        Helper.formatTable(table);
        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setPreferredSize(new Dimension(700, 300));
        tablePane.setVisible(true);

        JPanel panel = new JPanel();
        panel.add(tablePane);
        panel.setBorder(new EmptyBorder(10, -10, 10, -10));

        return panel;
    }
}
