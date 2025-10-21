import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SupermarketSystemV3 extends JFrame {

    private java.util.List<Product> products = new ArrayList<>();
    private File productFile = new File("products.txt");
    private double totalSales = 0;

    private JTable productTable;
    private DefaultTableModel productModel;
    private JTextArea billArea;
    private JComboBox<String> productDropdown;
    private JTextField qtyField, discountField, searchField;
    private JLabel unitPriceLabel, salesLabel;

    private Map<Product,Integer> soldItems = new HashMap<>();
    private Map<Product,Double> discountMap = new HashMap<>();

    private final String correctUsername = "admin";
    private final String correctPassword = "1234";

    public SupermarketSystemV3() {
        System.out.println("Running from: " + new File(".").getAbsolutePath());
        showLogin();
    }

    // ---------- LOGIN PANEL ----------
    private void showLogin() {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); }
        catch(Exception e){}

        JFrame loginFrame = new JFrame("Login - Supermarket Billing");
        loginFrame.setSize(400, 220);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel loginPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0,0,new Color(135,206,250),0,getHeight(),Color.WHITE);
                g2.setPaint(gp);
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };
        loginPanel.setLayout(new GridLayout(3,2,10,10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");

        loginPanel.add(new JLabel("Username:")); loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:")); loginPanel.add(passwordField);
        loginPanel.add(new JLabel()); loginPanel.add(loginButton);

        styleButton(loginButton);
        loginButton.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            if(user.equals(correctUsername) && pass.equals(correctPassword)) {
                JOptionPane.showMessageDialog(null,"Login Successful!");
                loginFrame.dispose();
                SwingUtilities.invokeLater(() -> SupermarketSystemV3.this.initGUI());
            } else JOptionPane.showMessageDialog(null,"Invalid credentials!","Error",JOptionPane.ERROR_MESSAGE);
        });

        loginFrame.add(loginPanel);
        loginFrame.setVisible(true);
    }

    // ---------- MAIN GUI ----------
    private void initGUI() {
        loadProducts();

        setTitle("Supermarket Billing System");
        setSize(1000,700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();

        tabs.add("Product Management", createProductManagementPanel());
        tabs.add("Billing", createBillingPanel());
        tabs.add("Sales Report", createSalesReportPanel());

        add(tabs);
        setVisible(true);
    }

    // ---------- PRODUCT CLASS ----------
    static class Product {
        String id,name;
        double price;
        int stock;
        Product(String id,String name,double price,int stock){
            this.id=id.trim(); this.name=name.trim(); this.price=price; this.stock=stock;
        }
    }

    // ---------- LOAD & SAVE ----------
    private void loadProducts() {
        try {
            if (!productFile.exists()) productFile.createNewFile();
        } catch (IOException e) { e.printStackTrace(); }

        products.clear();
        try(BufferedReader br=new BufferedReader(new FileReader(productFile))){
            String line;
            while((line=br.readLine())!=null && !line.isBlank()){
                String[] p=line.split(",");
                if(p.length==4)
                    products.add(new Product(p[0],p[1],Double.parseDouble(p[2]),Integer.parseInt(p[3])));
            }
        } catch(Exception e){ e.printStackTrace(); }
    }

    private void saveProducts(){
        try(PrintWriter pw=new PrintWriter(new FileWriter(productFile))){
            for(Product p: products) pw.println(p.id+","+p.name+","+p.price+","+p.stock);
        } catch(Exception e){ e.printStackTrace(); }
    }

    // ---------- PRODUCT MANAGEMENT PANEL ----------
    private JPanel createProductManagementPanel(){
        JPanel panel=new JPanel(new BorderLayout(10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel topPanel=new JPanel(new BorderLayout(5,5));
        searchField=new JTextField();
        JButton searchBtn=new JButton("Search");
        styleButton(searchBtn);
        searchBtn.addActionListener(e->filterProducts());
        topPanel.add(new JLabel("Search:"),BorderLayout.WEST);
        topPanel.add(searchField,BorderLayout.CENTER);
        topPanel.add(searchBtn,BorderLayout.EAST);
        panel.add(topPanel,BorderLayout.NORTH);

        String[] columns={"ID","Name","Price","Stock"};
        productModel=new DefaultTableModel(columns,0);
        productTable=new JTable(productModel){
            public Component prepareRenderer(TableCellRenderer renderer,int row,int column){
                Component c=super.prepareRenderer(renderer,row,column);
                if(!isRowSelected(row)){
                    int stock=(int)getModel().getValueAt(row,3);
                    c.setBackground(stock<=5?new Color(255,182,193):(row%2==0?Color.WHITE:new Color(230,230,250)));
                }
                return c;
            }
        };
        styleTable(productTable);
        productTable.setDefaultEditor(Object.class, null);
        refreshProductTable();
        panel.add(new JScrollPane(productTable),BorderLayout.CENTER);

        JPanel btnPanel=new JPanel(new FlowLayout(FlowLayout.CENTER,15,10));
        JButton addBtn=new JButton("Add"); JButton updateBtn=new JButton("Update");
        JButton deleteBtn=new JButton("Delete"); JButton refreshBtn=new JButton("Refresh");
        JButton[] buttons={addBtn,updateBtn,deleteBtn,refreshBtn};
        for(JButton b:buttons) styleButton(b);

        addBtn.addActionListener(e->addProduct());
        updateBtn.addActionListener(e->updateProduct());
        deleteBtn.addActionListener(e->deleteProduct());
        refreshBtn.addActionListener(e->refreshProductTable());

        btnPanel.add(addBtn); btnPanel.add(updateBtn); btnPanel.add(deleteBtn); btnPanel.add(refreshBtn);
        panel.add(btnPanel,BorderLayout.SOUTH);

        return panel;
    }

    private void filterProducts(){
        String query=searchField.getText().trim().toLowerCase();
        productModel.setRowCount(0);
        for(Product p: products){
            if(p.name.toLowerCase().contains(query) || p.id.toLowerCase().contains(query))
                productModel.addRow(new Object[]{p.id,p.name,p.price,p.stock});
        }
    }

    private void refreshProductTable(){
        productModel.setRowCount(0);
        for(Product p: products) productModel.addRow(new Object[]{p.id,p.name,p.price,p.stock});
        refreshProductDropdown();
    }

    private void addProduct(){
        JTextField idField=new JTextField(); JTextField nameField=new JTextField();
        JTextField priceField=new JTextField(); JTextField stockField=new JTextField();
        Object[] fields={"Product ID:",idField,"Name:",nameField,"Price:",priceField,"Stock:",stockField};
        int option=JOptionPane.showConfirmDialog(null,fields,"Add Product",JOptionPane.OK_CANCEL_OPTION);
        if(option==JOptionPane.OK_OPTION){
            try{
                String id=idField.getText().trim(); String name=nameField.getText().trim();
                double price=Double.parseDouble(priceField.getText().trim());
                int stock=Integer.parseInt(stockField.getText().trim());
                if(id.isEmpty()||name.isEmpty()) throw new Exception();
                products.add(new Product(id,name,price,stock));
                saveProducts(); refreshProductTable();
            } catch(Exception ex){ JOptionPane.showMessageDialog(null,"Invalid input!"); }
        }
    }

    private void updateProduct(){
        int row=productTable.getSelectedRow();
        if(row>=0){
            Product p=products.get(row);
            try{
                double price=Double.parseDouble(JOptionPane.showInputDialog("Enter new price:",p.price));
                int stock=Integer.parseInt(JOptionPane.showInputDialog("Enter new stock:",p.stock));
                p.price=price; p.stock=stock; saveProducts(); refreshProductTable();
            }catch(Exception ex){ JOptionPane.showMessageDialog(null,"Invalid input!");}
        }else JOptionPane.showMessageDialog(null,"Select a product to update!");
    }

    private void deleteProduct(){
        int row=productTable.getSelectedRow();
        if(row>=0){
            int confirm=JOptionPane.showConfirmDialog(null,"Are you sure to delete this product?","Confirm",JOptionPane.YES_NO_OPTION);
            if(confirm==JOptionPane.YES_OPTION){
                products.remove(row); saveProducts(); refreshProductTable();
            }
        }else JOptionPane.showMessageDialog(null,"Select a product to delete!");
    }

    private void styleTable(JTable table){
        table.setFillsViewportHeight(true); table.setRowHeight(28);
        table.setGridColor(Color.LIGHT_GRAY);
        table.getTableHeader().setBackground(new Color(70,130,180));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Arial",Font.BOLD,14));
    }

    private void styleButton(JButton btn){
        btn.setBackground(new Color(70,130,180)); btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Tahoma",Font.BOLD,14)); btn.setFocusPainted(false);
        btn.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){ btn.setBackground(Color.GREEN);}
            public void mouseExited(MouseEvent e){ btn.setBackground(new Color(70,130,180));}
        });
    }

    // ---------- BILLING PANEL ----------
    private JPanel createBillingPanel(){
        JPanel panel=new JPanel(new BorderLayout(10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel inputPanel=new JPanel(new GridLayout(4,2,5,5));
        inputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLUE,2),"Billing Info"));

        productDropdown=new JComboBox<>(); refreshProductDropdown();
        qtyField=new JTextField(); discountField=new JTextField(); unitPriceLabel=new JLabel("Unit Price: ₹0");

        inputPanel.add(new JLabel("Select Product:")); inputPanel.add(productDropdown);
        inputPanel.add(new JLabel("Quantity:")); inputPanel.add(qtyField);
        inputPanel.add(new JLabel("Discount (%):")); inputPanel.add(discountField);
        inputPanel.add(new JLabel("Price Info:")); inputPanel.add(unitPriceLabel);

        panel.add(inputPanel,BorderLayout.NORTH);

        billArea=new JTextArea(); billArea.setFont(new Font("Monospaced",Font.PLAIN,14));
        billArea.setEditable(false); billArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.add(new JScrollPane(billArea),BorderLayout.CENTER);

        JPanel btnPanel=new JPanel(new FlowLayout(FlowLayout.CENTER,15,10));
        JButton addBtn=new JButton("Add Item"); JButton finalizeBtn=new JButton("Finalize Bill");
        styleButton(addBtn); styleButton(finalizeBtn); btnPanel.add(addBtn); btnPanel.add(finalizeBtn);
        panel.add(btnPanel,BorderLayout.SOUTH);

        final double[] total={0};

        productDropdown.addActionListener(e->{
            String name=(String)productDropdown.getSelectedItem();
            for(Product p:products){ if(p.name.equals(name)) { unitPriceLabel.setText("Unit Price: ₹"+p.price); break;}}
        });

        addBtn.addActionListener(e->{
            String name=(String)productDropdown.getSelectedItem();
            int qty; double discount=0;
            try { qty=Integer.parseInt(qtyField.getText().trim()); discount=Double.parseDouble(discountField.getText().trim()); }
            catch(Exception ex){ JOptionPane.showMessageDialog(null,"Enter valid quantity/discount!"); return;}

            for(Product p:products){
                if(p.name.equals(name)){
                    if(p.stock>=qty){
                        double subtotal=p.price*qty;
                        double finalAmt=subtotal-(subtotal*discount/100);
                        total[0]+=finalAmt;
                        billArea.append(String.format("%-15s x%-3d = ₹%.2f\n",p.name,qty,finalAmt));
                        p.stock-=qty; saveProducts(); refreshProductTable();

                        // Track sold items for CSV
                        soldItems.put(p, soldItems.getOrDefault(p,0)+qty);
                        discountMap.put(p, discount);
                    } else { JOptionPane.showMessageDialog(null,"Not enough stock!"); return;}
                    break;
                }
            }
            qtyField.setText(""); discountField.setText("");
        });

        finalizeBtn.addActionListener(e->{
            if(!billArea.getText().trim().isEmpty()){
                String time=new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String date=new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                try(PrintWriter pw=new PrintWriter(new File("Bill_"+time+".txt"))){
                    pw.println(billArea.getText());
                } catch(Exception ex){ ex.printStackTrace();}

                // Append each sold item to daily_sales.csv
                try(PrintWriter csvWriter=new PrintWriter(new FileWriter("daily_sales.csv", true))){
                    for(Product p: soldItems.keySet()){
                        int qty = soldItems.get(p);
                        double discount = discountMap.getOrDefault(p,0.0);
                        double subtotal = p.price*qty;
                        double finalAmt = subtotal - (subtotal*discount/100);
                        csvWriter.printf("%s,%s,%s,%d,%.2f,%.2f,%.2f%n",
                            date,p.id,p.name,qty,p.price,discount,finalAmt);
                    }
                } catch(Exception ex){ ex.printStackTrace(); }

                billArea.append(String.format("\nTotal: ₹%.2f\n--- Bill Finalized ---\n\n",total[0]));
                totalSales+=total[0];
                JOptionPane.showMessageDialog(null,"Bill finalized! Total: ₹"+total[0]);
                total[0]=0; billArea.setText("");
                soldItems.clear(); discountMap.clear();
            } else JOptionPane.showMessageDialog(null,"No items in bill!");
        });

        return panel;
    }

    private void refreshProductDropdown(){
        if(productDropdown==null) return;
        productDropdown.removeAllItems();
        for(Product p:products) productDropdown.addItem(p.name);
    }

    // ---------- SALES REPORT PANEL ----------
    private JPanel createSalesReportPanel(){
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        salesLabel=new JLabel("Total Sales of the Day: ₹"+totalSales,SwingConstants.CENTER);
        salesLabel.setFont(new Font("Verdana",Font.BOLD,20));
        panel.add(salesLabel,BorderLayout.CENTER);

        JButton refreshBtn=new JButton("Refresh"); styleButton(refreshBtn);
        refreshBtn.addActionListener(e->{
            // Optionally read daily_sales.csv to calculate today's total
            double dailyTotal = 0;
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            try(BufferedReader br=new BufferedReader(new FileReader("daily_sales.csv"))){
                String line;
                while((line=br.readLine())!=null){
                    String[] parts = line.split(",");
                    if(parts.length==7 && parts[0].equals(today)){
                        dailyTotal += Double.parseDouble(parts[6]);
                    }
                }
            } catch(Exception ex){ ex.printStackTrace(); }
            salesLabel.setText("Total Sales of the Day: ₹" + dailyTotal);
        });
        panel.add(refreshBtn,BorderLayout.SOUTH);

        return panel;
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(SupermarketSystemV3::new);
    }
}
