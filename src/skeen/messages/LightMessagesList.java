package skeen.messages;

import java.util.Iterator;

public class LightMessagesList implements Iterable<LightMessage> {
    private Item first = null, last = null;
    private int size = 0;
    
    public Item getFirst() {
        return first;
    }

    public Item getLast() {
        return last;
    }

    public int size() {
        return this.size;
    }

    public void add(LightMessage m) {
        Item newItem = new Item(m);
        if(first == null){
            first = last = newItem;
        }
        else {
            newItem.setPrev(last);
            last.setNext(newItem);
            last = newItem;
        }
        size++;
    }

    public void setAsFirst(int id) {
        Item i = getFirst();
        while(i != null){
            if(id == i.get().getId()){
                i.setPrev(null);
                first = i;
                return;
            }
            i = i.getNext();
        }

        // TODO:
        // teria que ajustar o size aqui para ficar totalmente consistente
        // atualmente porem, isso nao afeta em nada
    }

    public String toString(){
        String s="{";
        Item i = getFirst();
        while(i != null){
            s += i.get().getId();
            if(i.getNext() !=null)
                s+="-";
            i = i.getNext();
        }
        return s+"}";
    }

    public ItemHst addHst(LightMessage m) {
        ItemHst newItem = new ItemHst(m);
        if(first == null){
            first = last = newItem;
            newItem.setIdx(0);
        }
        else {
            newItem.setIdx(((ItemHst)last).getIdx()+1);
            newItem.setPrev(last);
            last.setNext(newItem);
            last = newItem;
        }
        size++;
        return newItem;
    }

    public class Item {
        private LightMessage m;
        private Item prev,next;    
        public Item (LightMessage m){
            this.m = m;
            prev = null;
            next = null;
        }
        public LightMessage get(){
            return m;
        }
        public Item getPrev() {
            return prev;
        }
        public void setPrev(Item prev) {
            this.prev = prev;
        }
        public Item getNext() {
            return next;
        }
        public void setNext(Item next) {
            this.next = next;
        }
        @Override
        public int hashCode() {
            return m.getId();
        }
        @Override
        public boolean equals(Object o){
            return get().equals(((Item) o).get());
        }
        @Override
        public String toString(){
            return String.valueOf(m.getId());
        }
    }

    public class ItemHst extends Item {
        private int idx;
        public ItemHst(LightMessage m) {
            super(m);
        }
        public int getIdx() {
            return idx;
        }
        public void setIdx(int idx) {
            this.idx = idx;
        }
    }

    @Override
    public Iterator<LightMessage> iterator() {
        return new LMIterator(this);
    }

    public class LMIterator implements Iterator<LightMessage>{
        private LightMessage current;
        private Item item;
        public LMIterator(LightMessagesList l){
            if(l != null && l.getFirst() != null){
                current = l.getFirst().get();
                item = l.getFirst();
            }
        }
        @Override
        public boolean hasNext() {
            return (current != null);
        }
        @Override
        public LightMessage next() {
            LightMessage lm = current;
            current = null;
            if(item.getNext() != null) {
                current = item.getNext().get();
                item = item.getNext();
            }
            return lm;
        }
    }
}
