import skeen.SkeenNode;
import util.ArgsParser;

public class MainServer {
    public static void main(String args[]){
        ArgsParser p = ArgsParser.getServerParser(args);
        new SkeenNode(p.getId(), p);
    }
}