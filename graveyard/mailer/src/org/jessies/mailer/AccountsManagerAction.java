package org.jessies.mailer;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class AccountsManagerAction extends AbstractAction {
    private Mailer mailer;
    
    private JList accountsList;
    private DefaultListModel accountsListModel;
    
    private AddAccountAction addAccountAction = new AddAccountAction();
    private EditAccountAction editAccountAction = new EditAccountAction();
    private RemoveAccountAction removeAccountAction = new RemoveAccountAction();
    
    public AccountsManagerAction(Mailer mailer) {
        super("Accounts...");
        this.mailer = mailer;
    }
    
    public void actionPerformed(ActionEvent e) {
        accountsListModel = new DefaultListModel();
        updateAccountsListModel();
        
        accountsList = new JList(accountsListModel);
        accountsList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                // In the list, just show the email address (plus some padding).
                String string = ((AccountInfo) value).emailAddress() + "        ";
                return super.getListCellRendererComponent(list, string, index, isSelected, cellHasFocus);
            }
        });
        accountsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountsList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                addAccountAction.setEnabled(true);
                editAccountAction.setEnabled(accountsList.isSelectionEmpty() == false);
                removeAccountAction.setEnabled(accountsList.isSelectionEmpty() == false);
            }
        });
        accountsList.setSelectedIndex(0);
        
        JButton addButton = new JButton(addAccountAction);
        JButton removeButton = new JButton(removeAccountAction);
        JButton editButton = new JButton(editAccountAction);
        ComponentUtilities.tieButtonSizes(addButton, removeButton, editButton);
        
        JPanel accountButtons = new JPanel();
        accountButtons.setLayout(new BoxLayout(accountButtons, BoxLayout.PAGE_AXIS));
        accountButtons.add(addButton);
        accountButtons.add(removeButton);
        accountButtons.add(editButton);
        
        JPanel accountUi = new JPanel(new BorderLayout());
        accountUi.add(new JScrollPane(accountsList), BorderLayout.CENTER);
        accountUi.add(accountButtons, BorderLayout.EAST);
        
        FormBuilder form = new FormBuilder(mailer, "Account Manager");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("", accountUi);
        form.showNonModal();
    }
    
    private void updateAccountsListModel() {
        accountsListModel.clear();
        for (AccountInfo account : mailer.accounts()) {
            accountsListModel.addElement(account);
        }
    }
    
    private class AddAccountAction extends AbstractAction {
        public AddAccountAction() {
            super("Add...");
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            AccountInfo newAccount = new AccountInfo();
            boolean okay = newAccount.editAccount(mailer);
            if (okay) {
                mailer.accounts().add(newAccount);
                updateAccountsListModel();
                AccountInfo.writeToDisk(mailer.accounts());
            }
        }
    }
    
    private class RemoveAccountAction extends AbstractAction {
        public RemoveAccountAction() {
            super("Remove");
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            mailer.accounts().remove(accountsList.getSelectedValue());
            updateAccountsListModel();
            AccountInfo.writeToDisk(mailer.accounts());
        }
    }
    
    private class EditAccountAction extends AbstractAction {
        public EditAccountAction() {
            super("Edit...");
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            ((AccountInfo) accountsList.getSelectedValue()).editAccount(mailer);
            updateAccountsListModel();
            AccountInfo.writeToDisk(mailer.accounts());
        }
    }
}
