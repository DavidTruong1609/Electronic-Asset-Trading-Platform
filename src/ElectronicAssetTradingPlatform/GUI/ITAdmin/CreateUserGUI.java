package ElectronicAssetTradingPlatform.GUI.ITAdmin;

import ElectronicAssetTradingPlatform.Exceptions.EmptyFieldException;
import ElectronicAssetTradingPlatform.Exceptions.UserTypeException;
import ElectronicAssetTradingPlatform.Server.NetworkDataSource;
import ElectronicAssetTradingPlatform.Users.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

class CreateUserGUI extends JFrame {
    private final ITAdmin loggedInUser;
    private final NetworkDataSource data;
    private JTextField username;
    private JComboBox userType;
    private JTextField unitName;
    private JTextArea messaging;
    private JButton createUserButton;

    /**
     * Constructor sets up user interface, adds listeners and displays.
     */
    public CreateUserGUI(ITAdmin loggedInUser, NetworkDataSource data) {
        this.loggedInUser = loggedInUser;
        this.data = data;

        initUI();

        // add listeners to interactive components
        addWindowListener(new ClosingListener());
        createUserButton.addActionListener(new ButtonListener());

        // decorate the frame and make it visible
        setTitle("CREATE User");
        setMinimumSize(new Dimension(400, 300));
        pack();
        setVisible(true);
    }

    /**
     * Initialise UI
     */
    private void initUI() {
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        contentPane.add(Box.createVerticalStrut(20));
        contentPane.add(createUserPanel());
    }

    /**
     * Makes the Create User form
     *
     * @return JPanel of create form
     */
    public JPanel createUserPanel() {
        JPanel displayPanel = new JPanel();
        GroupLayout layout = new GroupLayout(displayPanel);
        displayPanel.setLayout(layout);

        // Turn on automatically adding gaps between components
        layout.setAutoCreateGaps(true);

        // Turn on automatically creating gaps between components that touch
        // the edge of the container and the container.
        layout.setAutoCreateContainerGaps(true);

        JLabel usernameLabel = new JLabel("Username");
        JLabel userTypeLabel = new JLabel("User Type");
        JLabel unitNameLabel = new JLabel("Unit Name");

        username = new JTextField(20);
        userType = new JComboBox(UsersFactory.UserType.values());
        unitName = new JTextField(20);
        messaging = new JTextArea();
        messaging.setEditable(false);
        messaging.setLineWrap(true);
        messaging.setWrapStyleWord(true);
        createUserButton = new JButton("Create");

        // Create a sequential group for the horizontal axis.
        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        hGroup.addGroup(layout.createParallelGroup()
                .addComponent(usernameLabel)
                .addComponent(userTypeLabel)
                .addComponent(unitNameLabel));
        hGroup.addGroup(layout.createParallelGroup()
                .addComponent(username)
                .addComponent(userType)
                .addComponent(unitName)
                .addComponent(createUserButton, GroupLayout.Alignment.CENTER)
                .addComponent(messaging, GroupLayout.Alignment.CENTER));
        layout.setHorizontalGroup(hGroup);

        // Create a sequential group for the vertical axis.
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(usernameLabel)
                .addComponent(username));
        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(userTypeLabel)
                .addComponent(userType));
        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(unitNameLabel)
                .addComponent(unitName));
        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(createUserButton));
        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(messaging));
        layout.setVerticalGroup(vGroup);

        return displayPanel;
    }

    /**
     * Handles events on the UI
     */
    private class ButtonListener implements ActionListener {

        /**
         * Any action is performed
         */
        public void actionPerformed(ActionEvent e) {
            JButton source = (JButton) e.getSource();
            if (source == createUserButton) {
                createUserPressed();
            }
        }

        /**
         * Create user
         */
        private void createUserPressed() {
            String usernameIn = username.getText();
            String unitNameIn = unitName.getText();
            String userTypeIn = Objects.requireNonNull(userType.getSelectedItem()).toString();

            // Check inputs are not null
            String output = "";
            try {
                ITAdmin.checkInputEmpty(usernameIn);
                ITAdmin.checkInputEmpty(userTypeIn);
                ITAdmin.checkInputEmpty(userTypeIn);

                User user = loggedInUser.createUser(usernameIn, unitNameIn, userTypeIn);
                output = data.storeUser(user);
            } catch (EmptyFieldException | UserTypeException e) {
                // Empty input error
                output = "Input is empty or invalid, please enter correct details into all fields.";
            }

            messaging.setText(output);

            CommonComponents.displayDBUser(usernameIn, data);
        }
    }

    /**
     * Closes the window
     */
    private class ClosingListener extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            dispose();
        }
    }
}
