package util;

public class OrderItem {
    private int itemid;
    private int qty;
    public OrderItem(int itemid, int qty) {
        this.itemid = itemid;
        this.qty = qty;
    }
    public int getId() {
        return itemid;
    }
    public int getQty() {
        return qty;
    }
    
}
